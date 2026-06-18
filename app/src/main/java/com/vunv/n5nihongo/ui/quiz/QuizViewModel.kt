package com.vunv.n5nihongo.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.vunv.n5nihongo.data.auth.AuthRepository
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Word
import com.vunv.n5nihongo.data.quiz.LESSON_QUIZ_PASS_PERCENT
import com.vunv.n5nihongo.data.quiz.QuizCategory
import com.vunv.n5nihongo.data.quiz.QuizPromptType
import com.vunv.n5nihongo.data.repository.QuizRepository
import com.vunv.n5nihongo.data.repository.UserProgressSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizQuestion(
    val category: QuizCategory,
    val word: Word?,
    val prompt: String,
    val promptType: QuizPromptType,
    val options: List<String>,
    val correctAnswer: String,
    val audioText: String
)

data class QuizUiState(
    val isLoading: Boolean = true,
    val currentQuestion: QuizQuestion? = null,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswerCorrect: Boolean? = null,
    val isFinished: Boolean = false,
    val passPercent: Int = LESSON_QUIZ_PASS_PERCENT,
    val lessonPassed: Boolean = false
)

class QuizViewModel(
    application: Application,
    private val lessonId: Int,
    private val selectedWordIds: List<Int>
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val authRepository = AuthRepository()
    private var tts: TextToSpeech? = TextToSpeech(application, this)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.JAPANESE
        }
    }

    fun playAudio(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    private val database = AppDatabaseProvider.getDatabase(application)
    private val repository = QuizRepository(
        wordDao = database.wordDao(),
        grammarDao = database.grammarDao(),
        kanjiDao = database.kanjiDao(),
        userProgressDao = database.userProgressDao()
    )

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var questions: List<QuizQuestion> = emptyList()

    init {
        loadQuiz()
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            // Tự động kiểm tra và seed cơ sở dữ liệu nếu trống hoặc chưa kịp tải (self-healing)
            val lessonDao = database.lessonDao()
            if (lessonDao.getLessonCount() == 0) {
                val dataRepository = com.vunv.n5nihongo.data.repository.DataRepository(
                    context = getApplication(),
                    lessonDao = lessonDao,
                    wordDao = database.wordDao(),
                    grammarDao = database.grammarDao(),
                    kanjiDao = database.kanjiDao()
                )
                dataRepository.seedDatabase()
            }

            val generated = repository.buildLessonQuiz(
                lessonId = lessonId,
                selectedWordIds = selectedWordIds
            )
            questions = generated.map { g ->
                QuizQuestion(
                    category = g.category,
                    word = g.word,
                    prompt = g.prompt,
                    promptType = g.promptType,
                    options = g.options,
                    correctAnswer = g.correctAnswer,
                    audioText = g.audioText
                )
            }

            val firstQuestion = questions.firstOrNull()
            _uiState.value = QuizUiState(
                isLoading = false,
                currentQuestion = firstQuestion,
                questionIndex = 0,
                totalQuestions = questions.size
            )
        }
    }

    fun submitAnswer(answer: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.selectedAnswer != null) {
            return
        }

        val isCorrect = answer == question.correctAnswer
        _uiState.value = state.copy(
            selectedAnswer = answer,
            isAnswerCorrect = isCorrect,
            correctAnswers = if (isCorrect) state.correctAnswers + 1 else state.correctAnswers
        )
    }

    fun moveToNextQuestion() {
        val state = _uiState.value
        if (state.selectedAnswer == null) {
            return
        }

        val nextIndex = state.questionIndex + 1
        if (nextIndex >= questions.size) {
            finishQuiz()
            return
        }

        _uiState.value = state.copy(
            currentQuestion = questions[nextIndex],
            questionIndex = nextIndex,
            selectedAnswer = null,
            isAnswerCorrect = null
        )
    }

    private fun finishQuiz() {
        val state = _uiState.value
        val scorePercent = if (state.totalQuestions == 0) 0
        else (state.correctAnswers * 100) / state.totalQuestions
        // Only mark lesson as passed if it's a standard full lesson exam
        val passed = selectedWordIds.isEmpty() && scorePercent >= LESSON_QUIZ_PASS_PERCENT

        _uiState.value = state.copy(
            isFinished = true,
            lessonPassed = passed
        )
        viewModelScope.launch {
            val userId = com.vunv.n5nihongo.data.auth.getCurrentUserId(getApplication())
            if (selectedWordIds.isEmpty()) {
                repository.updateLessonScore(
                    userId = userId,
                    lessonId = lessonId,
                    correctCount = state.correctAnswers,
                    totalQuestions = state.totalQuestions
                )
                // Trigger immediate cloud sync of lesson progress to Firestore if logged in
                if (userId.isNotBlank()) {
                    try {
                        val syncRepository = UserProgressSyncRepository(database.userProgressDao())
                        syncRepository.syncProgressForUser(userId)
                    } catch (e: Exception) {
                        android.util.Log.e("QuizViewModel", "Lỗi đồng bộ: ${e.message}", e)
                    }
                }
            }
            if (state.correctAnswers > 0) {
                authRepository.updateUserProgress(state.correctAnswers * 10, getApplication())
            }
        }
    }

    companion object {
        fun factory(
            lessonId: Int,
            selectedWordIds: List<Int> = emptyList()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return QuizViewModel(application, lessonId, selectedWordIds) as T
            }
        }
    }
}

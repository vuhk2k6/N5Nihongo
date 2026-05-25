package com.vunv.n5nihongo.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.quiz.LessonQuizGenerator
import com.vunv.n5nihongo.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

data class AiQuizUiState(
    val isLoading: Boolean = true,
    val questions: List<AiQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val selectedOption: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val isFinished: Boolean = false,
    val aiAnalysis: String? = null,
    val isAiThinking: Boolean = false,
    val errorMessage: String? = null
)

class AiQuizViewModel(
    application: Application,
    private val prompt: String,
    private val aiRepository: AiRepository = AiRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AiQuizUiState())
    val uiState: StateFlow<AiQuizUiState> = _uiState.asStateFlow()

    private val database = AppDatabaseProvider.getDatabase(application)
    private val wordDao = database.wordDao()
    private val grammarDao = database.grammarDao()
    private val kanjiDao = database.kanjiDao()

    private val quizGenerator = LessonQuizGenerator()

    init {
        generateQuizOffline()
    }

    /**
     * Sinh đề thi trắc nghiệm cố định offline từ database local để tránh 100% lỗi rate limit của Gemini API.
     */
    private fun generateQuizOffline() {
        viewModelScope.launch {
            try {
                // 1. Kiểm tra nếu là yêu cầu kiểm tra tổng hợp hoặc bài thi sẵn không chỉ định bài cụ thể
                if (isGeneralPrompt(prompt)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            questions = PresetQuizData.PRESET_N5_QUESTIONS.shuffled(),
                            currentIndex = 0
                        )
                    }
                    return@launch
                }

                val lessonId = extractLessonId(prompt)

                // Truy vấn từ local DB
                var words = wordDao.getWordsByLessonOnce(lessonId)
                var grammars = grammarDao.getGrammarByLessonOnce(lessonId)
                var kanjis = kanjiDao.getKanjiByLessonOnce(lessonId)

                if (words.isEmpty() && grammars.isEmpty() && kanjis.isEmpty()) {
                    // Cơ chế tự vá lỗi (self-healing): tự động seed cơ sở dữ liệu nếu trống hoặc chưa kịp tải
                    val dataRepository = com.vunv.n5nihongo.data.repository.DataRepository(
                        context = getApplication(),
                        lessonDao = database.lessonDao(),
                        wordDao = database.wordDao(),
                        grammarDao = database.grammarDao(),
                        kanjiDao = database.kanjiDao()
                    )
                    dataRepository.seedDatabase()

                    // Thử truy vấn lại sau khi đã seed
                    words = wordDao.getWordsByLessonOnce(lessonId)
                    grammars = grammarDao.getGrammarByLessonOnce(lessonId)
                    kanjis = kanjiDao.getKanjiByLessonOnce(lessonId)
                }

                if (words.isEmpty() && grammars.isEmpty() && kanjis.isEmpty()) {
                    throw Exception("Không tìm thấy dữ liệu bài học này trong cơ sở dữ liệu local. Vui lòng học lý thuyết trước!")
                }

                // Sử dụng LessonQuizGenerator có sẵn để sinh bộ đề offline chất lượng cao, nhanh chóng và mượt mà
                val generatedQuestions = quizGenerator.buildQuiz(
                    lessonId = lessonId,
                    words = words,
                    grammars = grammars,
                    kanjis = kanjis
                )

                if (generatedQuestions.isEmpty()) {
                    throw Exception("Không tạo được câu hỏi trắc nghiệm từ dữ liệu local.")
                }

                val aiQuestions = generatedQuestions.map { g ->
                    AiQuestion(
                        question = g.prompt,
                        options = g.options,
                        correctAnswer = g.correctAnswer,
                        explanation = "Trong tiếng Nhật N5, đáp án chính xác cho câu hỏi này là '${g.correctAnswer}'. Hãy tham khảo nhận xét chuyên sâu từ AI Sensei ở cuối bài để ôn tập tốt hơn nhé! 🌸"
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = aiQuestions,
                        currentIndex = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "AI Sensei gặp sự cố biên soạn đề thi: ${e.localizedMessage ?: "Lỗi tải dữ liệu local"}. Bạn vui lòng thử lại nhé!"
                    )
                }
            }
        }
    }

    private fun extractLessonId(prompt: String): Int {
        val numberRegex = Regex("Bài\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val matchResult = numberRegex.find(prompt)
        if (matchResult != null) {
            return matchResult.groups[1]?.value?.toIntOrNull() ?: 4
        }
        if (prompt.contains("số đếm", ignoreCase = true) || prompt.contains("ngày tháng", ignoreCase = true)) {
            return 3
        }
        if (prompt.contains("chữ cái", ignoreCase = true) || prompt.contains("hiragana", ignoreCase = true)) {
            return 1
        }
        if (prompt.contains("katakana", ignoreCase = true)) {
            return 2
        }
        // Mặc định chọn ngẫu nhiên bài từ 4 đến 25
        return (4..25).random()
    }

    private fun isGeneralPrompt(prompt: String): Boolean {
        val hasLesson = prompt.contains(Regex("Bài\\s*\\d+", RegexOption.IGNORE_CASE))
        val hasTopic = prompt.contains("số đếm", ignoreCase = true) ||
                       prompt.contains("ngày tháng", ignoreCase = true) ||
                       prompt.contains("chữ cái", ignoreCase = true) ||
                       prompt.contains("hiragana", ignoreCase = true) ||
                       prompt.contains("katakana", ignoreCase = true)
        return !hasLesson && !hasTopic
    }

    fun submitAnswer(option: String) {
        val state = _uiState.value
        if (state.isAnswered) return

        val currentQuestion = state.questions.getOrNull(state.currentIndex) ?: return
        val isCorrect = option == currentQuestion.correctAnswer

        _uiState.update {
            it.copy(
                selectedOption = option,
                isAnswered = true,
                isCorrect = isCorrect,
                correctCount = if (isCorrect) state.correctCount + 1 else state.correctCount
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isAnswered) return

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.questions.size) {
            _uiState.update {
                it.copy(isFinished = true)
            }
            fetchAiEvaluation(state.correctCount + if (state.isCorrect) 1 else 0, state.questions.size)
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedOption = null,
                    isAnswered = false,
                    isCorrect = false
                )
            }
        }
    }

    /**
     * Triệu gọi Gemini API để nhận xét trực tiếp kết quả làm bài sau khi hoàn thành bài test cố định.
     */
    private fun fetchAiEvaluation(correct: Int, total: Int) {
        _uiState.update { it.copy(isAiThinking = true, aiAnalysis = "") }

        viewModelScope.launch {
            try {
                val scorePercent = if (total > 0) (correct * 100) / total else 0
                val promptEvaluation = """
                    Hãy đóng vai AI Sensei tiếng Nhật N5 cực kỳ thân thiện và tâm lý. Hãy viết một đoạn nhận xét học tập ngắn gọn, ấm áp (khoảng 100-150 từ) bằng tiếng Việt cho học sinh vừa hoàn thành bài trắc nghiệm bài học:
                    - Tên bài trắc nghiệm: $prompt
                    - Kết quả làm bài: $correct/$total câu đúng ($scorePercent%)
                    
                    Yêu cầu nhận xét:
                    1. Đưa ra lời khuyên học tập thực tế và khích lệ nhiệt tình.
                    2. Ở cuối bài nhận xét, hãy chèn 1-2 nút điều hướng bài học chuyển tiếp phù hợp giúp học viên ôn tập tốt hơn.
                       Cú pháp nút điều hướng chính xác: [Tên hiển thị đầy cảm xúc](navigate:route_name)
                       Các route hợp lệ được hỗ trợ gồm:
                       - 'alphabet/1' hoặc 'alphabet/2' (Luyện chữ cái)
                       - 'numbersTime' (Học số đếm & thời gian)
                       - 'kanji' (Học lại chữ Hán N5)
                       - 'lessonDetail/<id>' với <id> từ 4 đến 25 (ví dụ: [Vào học bài 4 thôi nào 📝](navigate:lessonDetail/4))
                       - 'aiAssistant' (Hỏi đáp trực tiếp tự do với AI)
                       
                    Hãy làm cho học viên cảm thấy được quan tâm và hào hứng học tập!
                """.trimIndent()

                var accumulatedText = ""
                aiRepository.askAiStream(promptEvaluation).collect { chunk ->
                    val chunkText = chunk.text ?: ""
                    accumulatedText += chunkText
                    _uiState.update { it.copy(aiAnalysis = accumulatedText) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        aiAnalysis = "Chào mừng bạn đã hoàn thành bài thi trắc nghiệm! AI Sensei tạm thời gặp sự cố kết nối mạng nên chưa thể viết nhận xét phân tích trực tiếp ngay được, nhưng điểm số của bạn cực kỳ tuyệt vời! Hãy tiếp tục chăm chỉ học tập nhé! 🌸"
                    )
                }
            } finally {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    companion object {
        fun factory(prompt: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return AiQuizViewModel(application, prompt) as T
            }
        }
    }
}

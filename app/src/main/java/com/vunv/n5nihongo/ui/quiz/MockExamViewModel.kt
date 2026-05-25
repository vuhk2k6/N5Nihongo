package com.vunv.n5nihongo.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.quiz.GeneratedQuizQuestion
import com.vunv.n5nihongo.data.quiz.MockExamGenerator
import com.vunv.n5nihongo.data.quiz.QuizCategory
import com.vunv.n5nihongo.data.repository.AiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SkillsBreakdown(
    val correctVocab: Int = 0,
    val totalVocab: Int = 0,
    val correctGrammar: Int = 0,
    val totalGrammar: Int = 0,
    val correctKanji: Int = 0,
    val totalKanji: Int = 0
)

data class MockExamUiState(
    val isLoading: Boolean = true,
    val questions: List<GeneratedQuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, String> = emptyMap(), // questionIndex -> chosenOption
    val isFinished: Boolean = false,
    val correctCount: Int = 0,
    val totalQuestions: Int = 0,
    val timeRemaining: Int = 2700, // 45 minutes in seconds
    val skillsBreakdown: SkillsBreakdown = SkillsBreakdown(),
    val aiAnalysis: String? = null,
    val isAiThinking: Boolean = false,
    val errorMessage: String? = null
)

class MockExamViewModel(
    application: Application,
    private val aiRepository: AiRepository = AiRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MockExamUiState())
    val uiState: StateFlow<MockExamUiState> = _uiState.asStateFlow()

    private val database = AppDatabaseProvider.getDatabase(application)
    private val wordDao = database.wordDao()
    private val grammarDao = database.grammarDao()
    private val kanjiDao = database.kanjiDao()

    private val examGenerator = MockExamGenerator()
    private var timerJob: Job? = null

    init {
        loadExam()
    }

    private fun loadExam() {
        viewModelScope.launch {
            try {
                // Tải dữ liệu song song từ local DB
                var allWords = wordDao.getAllWordsOnce()
                var allGrammar = grammarDao.getAllGrammarOnce()
                var allKanji = kanjiDao.getAllKanjiOnce()

                if (allWords.isEmpty() && allGrammar.isEmpty() && allKanji.isEmpty()) {
                    // Cơ chế tự vá lỗi (self-healing): tự động seed cơ sở dữ liệu nếu trống hoặc chưa kịp tải
                    val dataRepository = com.vunv.n5nihongo.data.repository.DataRepository(
                        context = getApplication(),
                        lessonDao = database.lessonDao(),
                        wordDao = database.wordDao(),
                        grammarDao = database.grammarDao(),
                        kanjiDao = database.kanjiDao()
                    )
                    dataRepository.seedDatabase()

                    // Truy vấn lại sau khi đã seed
                    allWords = wordDao.getAllWordsOnce()
                    allGrammar = grammarDao.getAllGrammarOnce()
                    allKanji = kanjiDao.getAllKanjiOnce()
                }

                if (allWords.isEmpty() && allGrammar.isEmpty() && allKanji.isEmpty()) {
                    throw Exception("Cơ sở dữ liệu đang trống. Vui lòng học các bài học trước để tạo ngân hàng đề!")
                }

                val examQuestions = examGenerator.buildMockExam(allWords, allGrammar, allKanji)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = examQuestions,
                        totalQuestions = examQuestions.size,
                        timeRemaining = 2700 // 45 phút
                    )
                }

                startTimer()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Không thể tải đề thi thử: ${e.localizedMessage ?: "Lỗi không xác định"}"
                    )
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0 && !_uiState.value.isFinished) {
                delay(1000)
                _uiState.update {
                    val nextTime = it.timeRemaining - 1
                    if (nextTime <= 0) {
                        // Tự động nộp bài khi hết giờ
                        timerJob?.cancel()
                        submitExamInternal(it)
                        it.copy(timeRemaining = 0, isFinished = true)
                    } else {
                        it.copy(timeRemaining = nextTime)
                    }
                }
            }
        }
    }

    fun selectOption(questionIndex: Int, option: String) {
        if (_uiState.value.isFinished) return
        _uiState.update {
            val updated = it.selectedAnswers.toMutableMap()
            updated[questionIndex] = option
            it.copy(selectedAnswers = updated)
        }
    }

    fun setCurrentIndex(index: Int) {
        if (index in 0 until _uiState.value.questions.size) {
            _uiState.update { it.copy(currentIndex = index) }
        }
    }

    fun submitExam() {
        timerJob?.cancel()
        submitExamInternal(_uiState.value)
    }

    private fun submitExamInternal(state: MockExamUiState) {
        if (state.isFinished) return

        var correct = 0
        var vocabCorrect = 0
        var vocabTotal = 0
        var grammarCorrect = 0
        var grammarTotal = 0
        var kanjiCorrect = 0
        var kanjiTotal = 0

        state.questions.forEachIndexed { index, question ->
            val chosen = state.selectedAnswers[index]
            val isCorrect = chosen == question.correctAnswer

            when (question.category) {
                QuizCategory.VOCABULARY -> {
                    vocabTotal++
                    if (isCorrect) {
                        vocabCorrect++
                        correct++
                    }
                }
                QuizCategory.GRAMMAR -> {
                    grammarTotal++
                    if (isCorrect) {
                        grammarCorrect++
                        correct++
                    }
                }
                QuizCategory.KANJI -> {
                    kanjiTotal++
                    if (isCorrect) {
                        kanjiCorrect++
                        correct++
                    }
                }
            }
        }

        val breakdown = SkillsBreakdown(
            correctVocab = vocabCorrect,
            totalVocab = vocabTotal,
            correctGrammar = grammarCorrect,
            totalGrammar = grammarTotal,
            correctKanji = kanjiCorrect,
            totalKanji = kanjiTotal
        )

        _uiState.update {
            it.copy(
                isFinished = true,
                correctCount = correct,
                skillsBreakdown = breakdown
            )
        }

        fetchAiAnalysis(correct, state.questions.size, breakdown, state.questions, state.selectedAnswers)
    }

    private fun fetchAiAnalysis(
        correct: Int,
        total: Int,
        breakdown: SkillsBreakdown,
        questions: List<GeneratedQuizQuestion>,
        selectedAnswers: Map<Int, String>
    ) {
        _uiState.update { it.copy(isAiThinking = true, aiAnalysis = "") }

        viewModelScope.launch {
            try {
                // Tạo danh sách các câu trả lời sai để gửi AI phân tích sâu sắc
                val wrongQuestionsInfo = StringBuilder()
                var wrongCount = 0
                questions.forEachIndexed { index, question ->
                    val chosen = selectedAnswers[index]
                    if (chosen != question.correctAnswer && wrongCount < 5) { // Giới hạn gửi tối đa 5 câu sai để tránh quá tải tokens
                        wrongCount++
                        wrongQuestionsInfo.append("- Câu hỏi: ${question.prompt}\n")
                        wrongQuestionsInfo.append("  + Đáp án đúng: ${question.correctAnswer}\n")
                        wrongQuestionsInfo.append("  + Đáp án bạn đã chọn: ${chosen ?: "Không trả lời"}\n\n")
                    }
                }

                val prompt = """
                    Hãy phân tích và viết một bài nhận xét chuyên sâu bằng tiếng Việt để tư vấn học tập cho học viên tiếng Nhật N5 vừa hoàn thành đề thi thử JLPT N5 chuẩn hóa với kết quả như sau:
                    - Điểm số chung: $correct/$total câu đúng (${if (total > 0) (correct * 100) / total else 0}%)
                    - Phân tích kỹ năng:
                      + Từ vựng (Vocabulary): ${breakdown.correctVocab}/${breakdown.totalVocab} đúng
                      + Ngữ pháp (Grammar): ${breakdown.correctGrammar}/${breakdown.totalGrammar} đúng
                      + Chữ Hán (Kanji): ${breakdown.correctKanji}/${breakdown.totalKanji} đúng

                    ${if (wrongCount > 0) "Dưới đây là một số câu tiêu biểu học viên đã làm sai để bạn phân tích lý do sai và sửa lỗi giúp họ:\n$wrongQuestionsInfo" else "Học viên đã làm đúng hoàn hảo tất cả các câu!"}

                    Yêu cầu bài nhận xét:
                    1. Giọng điệu của một AI Sensei cực kỳ thân thiện, khích lệ và truyền cảm hứng.
                    2. Đánh giá rõ rệt đâu là kỹ năng điểm mạnh, đâu là lỗ hổng kiến thức cần cải thiện gấp.
                    3. Chỉ ra mẹo khắc phục lỗi sai từ vựng hoặc ngữ pháp (nếu có câu sai được liệt kê ở trên).
                    4. ĐẶC BIỆT: Ở cuối nhận xét, bạn HÃY CHÈN 2-3 NÚT ĐIỀU HƯỚNG chuyển tiếp bài học để học viên có thể nhấp vào và đi học lại ngay phần yếu đó.
                       Cú pháp chèn nút chính xác: [Tên nút hiển thị đầy đủ cảm xúc](navigate:route_name)
                       Các route được hệ thống hỗ trợ gồm:
                       - 'alphabet/1' hoặc 'alphabet/2' (Luyện chữ cái)
                       - 'numbersTime' (Học số đếm & thời gian)
                       - 'kanji' (Học lại 80+ chữ Hán N5)
                       - 'lessonDetail/<id>' với <id> từ 4 đến 25 (Học lại từ vựng & ngữ pháp của bài học đó, ví dụ: [Học lại Ngữ pháp Bài 4 📚](navigate:lessonDetail/4))
                       - 'aiAssistant' (Trò chuyện hỏi đáp tự do với AI)
                       
                    Hãy đưa ra lời khuyên chân thành và cấu trúc nút điều hướng chính xác để giúp học viên nâng cao điểm số!
                """.trimIndent()

                var accumulatedText = ""
                aiRepository.askAiStream(prompt).collect { chunk ->
                    val chunkText = chunk.text ?: ""
                    accumulatedText += chunkText
                    _uiState.update { it.copy(aiAnalysis = accumulatedText) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        aiAnalysis = "Chào mừng bạn đã hoàn thành bài thi thử! AI Sensei tạm thời gặp sự cố kết nối mạng nên không thể biên soạn báo cáo phân tích chi tiết ngay lúc này. Tuy nhiên, điểm số của bạn đã được lưu lại rất xuất sắc! Hãy tiếp tục rèn luyện chăm chỉ nhé! 🌸"
                    )
                }
            } finally {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return MockExamViewModel(application) as T
            }
        }
    }
}

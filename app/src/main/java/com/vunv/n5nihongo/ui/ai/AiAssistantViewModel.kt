package com.vunv.n5nihongo.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vunv.n5nihongo.data.repository.AiRepository
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActionSuggestion(
    val title: String,
    val route: String
)

data class Message(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestions: List<ActionSuggestion> = emptyList()
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isAiThinking: Boolean = false,
    val errorMessage: String? = null
)

class AiAssistantViewModel(
    application: Application,
    private val aiRepository: AiRepository = AiRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val database = AppDatabaseProvider.getDatabase(application)
    private val userProgressDao = database.userProgressDao()

    init {
        refreshGreeting()
    }

    fun refreshGreeting() {
        viewModelScope.launch {
            try {
                val userId = com.vunv.n5nihongo.data.auth.getCurrentUserId(getApplication())
                val progressList = userProgressDao.getAllProgressOnce(userId)
                val completedLessons = progressList.filter { it.isCompleted }.map { it.lessonId }.toSet()

                val nextStepRoute: String
                val nextStepTitle: String
                val nextStepReason: String
                val secondarySuggestions = mutableListOf<ActionSuggestion>()

                if (1 !in completedLessons) {
                    nextStepRoute = "alphabet/1"
                    nextStepTitle = "Học Hiragana (Bài 1) 🌸"
                    nextStepReason = "Konnichiwa! Rất vui được đồng hành cùng bạn học tiếng Nhật N5. Theo lộ trình học tập, bạn nên bắt đầu từ bảng chữ cái Hiragana để xây dựng nền móng vững chắc nhất nhé! Hãy cùng cố gắng nào! ✨"
                    secondarySuggestions.add(ActionSuggestion("Luyện trắc nghiệm AI chữ cái", "aiQuiz?prompt=trắc nghiệm chữ cái Hiragana tiếng Nhật"))
                } else if (2 !in completedLessons) {
                    nextStepRoute = "alphabet/2"
                    nextStepTitle = "Học Katakana (Bài 2) 🚀"
                    nextStepReason = "Chào mừng bạn quay lại! Chúc mừng bạn đã chinh phục xong bảng chữ cái Hiragana rất xuất sắc! Bước tiếp theo cực kỳ quan trọng trong lộ trình học tập của bạn là làm chủ bảng chữ cái Katakana để tự tin đọc phiên âm các từ mượn nước ngoài nha!"
                    secondarySuggestions.add(ActionSuggestion("Luyện trắc nghiệm AI Katakana", "aiQuiz?prompt=trắc nghiệm chữ cái Katakana tiếng Nhật"))
                } else if (3 !in completedLessons) {
                    nextStepRoute = "numbersTime"
                    nextStepTitle = "Học Số đếm & Thời gian (Bài 3) ⏱️"
                    nextStepReason = "Thật tuyệt vời! Bạn đã làm chủ hoàn toàn cả hai bảng chữ cái Hiragana và Katakana rồi! Tiếp theo, hãy chuyển sang Bài 3 để cùng học cách đếm số, cách hỏi và trả lời giờ giấc, ngày tháng tiếng Nhật nhé!"
                    secondarySuggestions.add(ActionSuggestion("Luyện trắc nghiệm AI số đếm", "aiQuiz?prompt=trắc nghiệm số đếm ngày tháng giờ giấc tiếng Nhật N5"))
                } else {
                    // Find first incomplete lesson from 4 to 25
                    val nextLessonId = (4..25).firstOrNull { it !in completedLessons }
                    if (nextLessonId != null) {
                        nextStepRoute = "lessonDetail/$nextLessonId"
                        nextStepTitle = "Học tiếp Bài $nextLessonId 📚"
                        nextStepReason = "Chào mừng bạn! Lộ trình N5 của bạn đang tiến triển cực kỳ tốt. Bài học tiếp theo bạn cần chinh phục chính là Bài $nextLessonId. Hãy mở chi tiết bài học để cùng ôn luyện từ vựng, ngữ pháp và chữ Hán N5 ngay nhé!"
                        secondarySuggestions.add(ActionSuggestion("Luyện trắc nghiệm AI Bài $nextLessonId", "aiQuiz?prompt=trắc nghiệm từ vựng ngữ pháp tiếng Nhật N5 Bài $nextLessonId"))
                    } else {
                        nextStepRoute = "leaderboard"
                        nextStepTitle = "Luyện đề trắc nghiệm tổng hợp 🏆"
                        nextStepReason = "Thật kinh ngạc! Bạn đã xuất sắc hoàn thành trọn vẹn toàn bộ 25 bài học của lộ trình N5 rồi! AI Sensei vô cùng khâm phục bạn. Để duy trì phản xạ tiếng Nhật tốt nhất, bạn hãy luyện tập trắc nghiệm tổng hợp nhé!"
                        secondarySuggestions.add(ActionSuggestion("Luyện trắc nghiệm AI tổng hợp N5", "aiQuiz?prompt=đề kiểm tra tổng hợp kiến thức tiếng Nhật N5"))
                    }
                }

                val finalSuggestions = listOf(ActionSuggestion(nextStepTitle, nextStepRoute)) + secondarySuggestions

                _uiState.update {
                    it.copy(
                        isAiThinking = false,
                        messages = listOf(
                            Message(
                                content = nextStepReason,
                                isUser = false,
                                suggestions = finalSuggestions
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiThinking = false,
                        messages = listOf(
                            Message(
                                content = "Chào mừng bạn quay trở lại! AI Sensei sẵn sàng đồng hành cùng bạn học tiếng N5 đúng trọng tâm. Hãy nhắn cho mình chủ đề bạn muốn học hoặc chọn gợi ý bên dưới nhé!",
                                isUser = false,
                                suggestions = listOf(
                                    ActionSuggestion("Luyện bảng chữ Hiragana", "alphabet/1"),
                                    ActionSuggestion("Học chữ Hán Kanji N5", "kanji"),
                                    ActionSuggestion("Luyện thi Trắc nghiệm", "leaderboard")
                                )
                            )
                        )
                    )
                }
            }
        }
    }

    private fun parseSuggestions(text: String): Pair<String, List<ActionSuggestion>> {
        val regex = Regex("\\[([^\\]]+)\\]\\(navigate:([^\\)]+)\\)")
        val suggestions = mutableListOf<ActionSuggestion>()
        val matches = regex.findAll(text)
        for (match in matches) {
            val title = match.groups[1]?.value.orEmpty().trim()
            val route = match.groups[2]?.value.orEmpty().trim()
            if (title.isNotEmpty() && route.isNotEmpty()) {
                suggestions.add(ActionSuggestion(title, route))
            }
        }
        var cleanedText = regex.replace(text, "")
        cleanedText = cleanedText.trim()
        return Pair(cleanedText, suggestions)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = Message(content = text, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isAiThinking = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                var aiResponseAccumulated = ""
                val aiInitialMessage = Message(content = "", isUser = false)
                _uiState.update { it.copy(messages = it.messages + aiInitialMessage) }

                aiRepository.askAiStream(text).collect { chunk ->
                    val newText = chunk.text ?: ""
                    aiResponseAccumulated += newText
                    
                    val (cleanedContent, parsedSuggestions) = parseSuggestions(aiResponseAccumulated)
                    
                    _uiState.update { state ->
                        val updatedMessages = state.messages.toMutableList()
                        if (updatedMessages.isNotEmpty()) {
                            updatedMessages[updatedMessages.lastIndex] = Message(
                                content = cleanedContent,
                                isUser = false,
                                suggestions = parsedSuggestions
                            )
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = formatAiError(e),
                        messages = it.messages.dropLast(1)
                    )
                }
            } finally {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    fun checkWriting(sentence: String) {
        if (sentence.isBlank()) return

        val userMessage = Message(content = "📝 Kiểm tra giúp tôi câu này: \"$sentence\"", isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isAiThinking = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                var aiResponseAccumulated = ""
                val aiInitialMessage = Message(content = "", isUser = false)
                _uiState.update { it.copy(messages = it.messages + aiInitialMessage) }

                aiRepository.correctGrammar(sentence).collect { chunk ->
                    val newText = chunk.text ?: ""
                    aiResponseAccumulated += newText
                    
                    val (cleanedContent, parsedSuggestions) = parseSuggestions(aiResponseAccumulated)
                    
                    _uiState.update { state ->
                        val updatedMessages = state.messages.toMutableList()
                        if (updatedMessages.isNotEmpty()) {
                            updatedMessages[updatedMessages.lastIndex] = Message(
                                content = cleanedContent,
                                isUser = false,
                                suggestions = parsedSuggestions
                            )
                        }
                        state.copy(messages = updatedMessages)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = formatAiError(e),
                        messages = it.messages.dropLast(1)
                    )
                }
            } finally {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    private fun formatAiError(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("quota", ignoreCase = true) || 
            msg.contains("exhausted", ignoreCase = true) || 
            msg.contains("limit", ignoreCase = true) ||
            msg.contains("429") -> {
                "AI Sensei hiện tại đang hơi bận biên soạn bài học cho nhiều học sinh khác một chút. 🌸 Bạn vui lòng đợi khoảng 15-20 giây rồi gửi lại tin nhắn nhé! Cảm ơn bạn đã kiên nhẫn học tập cùng AI Sensei! ✨"
            }
            msg.contains("network", ignoreCase = true) || 
            msg.contains("connect", ignoreCase = true) || 
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("Host", ignoreCase = true) -> {
                "AI Sensei tạm thời bị gián đoạn kết nối internet. 🌸 Bạn vui lòng kiểm tra lại kết nối mạng Wi-Fi/4G của thiết bị và thử lại nhé!"
            }
            else -> {
                "AI Sensei gặp một sự cố nhỏ khi phản hồi. 🌸 Chi tiết: ${e.localizedMessage ?: "Lỗi không xác định"}. Vui lòng thử lại sau nhé!"
            }
        }
    }

    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return AiAssistantViewModel(application) as T
            }
        }
    }
}

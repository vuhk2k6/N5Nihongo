package com.vunv.n5nihongo.ui.home

import android.app.Application
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Word
import com.vunv.n5nihongo.data.model.Grammar
import com.vunv.n5nihongo.data.repository.AiRepository
import com.vunv.n5nihongo.ui.components.rememberDeferredJapaneseSpeaker
import com.vunv.n5nihongo.ui.components.LessonScreenLoading
import com.vunv.n5nihongo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// --- Data Models ---
data class KaiwaHint(
    val japanese: String,
    val vietnamese: String
)

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val translation: String? = null,
    val feedback: String? = null,
    val hints: List<KaiwaHint> = emptyList(),
    val isTranslationVisible: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class KaiwaUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isAiThinking: Boolean = false,
    val errorMessage: String? = null,
    val currentHints: List<KaiwaHint> = emptyList()
)

// --- ViewModel ---
class KaiwaPracticeViewModel(
    application: Application,
    private val lessonId: Int,
    private val aiRepository: AiRepository = AiRepository()
) : AndroidViewModel(application) {

    private val database = AppDatabaseProvider.getDatabase(application)
    private val wordDao = database.wordDao()
    private val grammarDao = database.grammarDao()

    private val _uiState = MutableStateFlow(KaiwaUiState())
    val uiState: StateFlow<KaiwaUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var systemInstructionText = ""
    private val gson = Gson()

    init {
        loadLessonScopeAndStart()
    }

    private fun loadLessonScopeAndStart() {
        viewModelScope.launch(Dispatchers.IO) {
            val words = wordDao.getWordsByLessonOnce(lessonId)
            val grammars = grammarDao.getGrammarByLessonOnce(lessonId)

            val wordsScope = words.joinToString("; ") { "${it.kanji.ifBlank { it.furigana }} (${it.meaning})" }
            val grammarScope = grammars.joinToString("; ") { "${it.title}: ${it.explanation}" }

            systemInstructionText = """
                Bạn là Yamada-san (山田さん), một người bạn thân thiện, nhiệt tình người Nhật Bản.
                Nhiệm vụ của bạn là thực hành hội thoại Kaiwa N5 thực tế với học viên theo kiến thức của Bài ${lessonId - 3} trong giáo trình Minna no Nihongo.
                
                TỪ VỰNG ĐƯỢC PHÉP DÙNG: $wordsScope
                NGỮ PHÁP ĐƯỢC PHÉP DÙNG: $grammarScope
                
                QUY TẮC PHẢN HỒI:
                1. Hãy đóng vai Yamada-san trò chuyện bằng tiếng Nhật cực kỳ đơn giản (Trình độ N5). Mỗi câu trả lời của bạn tối đa 2-3 câu ngắn gọn.
                2. Bạn bắt buộc phải chèn Furigana/Hiragana cho chữ Hán dạng [Hán tự](Furigana) để người học dễ đọc. Ví dụ: [私](わたし)は[学生](がくせい)です.
                3. Ở cuối phản hồi tiếng Nhật, bạn BẮT BUỘC chèn thêm 2 thẻ XML sau để truyền metadata:
                   - Thẻ bản dịch nghĩa tiếng Việt ẩn: <translation>bản dịch tiếng Việt của toàn bộ câu Yamada-san vừa nói</translation>
                   - Thẻ gợi ý câu trả lời nhanh cho học viên (2 câu gợi ý): <hints>[{"japanese":"câu gợi ý tiếng Nhật 1","vietnamese":"nghĩa 1"},{"japanese":"câu gợi ý tiếng Nhật 2","vietnamese":"nghĩa 2"}]</hints>
                4. ĐẶC BIỆT: Nếu trong tin nhắn trước đó của học viên có lỗi sai ngữ pháp, chính tả, hoặc dùng sai trợ từ (như は, が, を, に...), bạn hãy thêm thẻ góp ý sửa lỗi chi tiết bằng tiếng Việt ở đầu câu trả lời: <feedback>chỉ ra lỗi sai của học viên, giải thích ngắn gọn tại sao sai và đưa ra câu sửa đúng kèm Furigana</feedback>.
                
                Yamada-san hãy bắt đầu câu chào hỏi đầu tiên để mở màn hội thoại!
            """.trimIndent()

            _isLoading.value = false
            startConversation()
        }
    }

    private fun startConversation() {
        _uiState.update { it.copy(isAiThinking = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                var aiResponseAccumulated = ""
                val initialAiMessage = ChatMessage(content = "", isUser = false)
                _uiState.update { it.copy(messages = it.messages + initialAiMessage) }

                aiRepository.askKaiwaStream(
                    systemInstructionText = systemInstructionText,
                    prompt = "Bắt đầu chào học viên ngắn gọn."
                ).collect { chunk ->
                    val newText = chunk.text ?: ""
                    aiResponseAccumulated += newText
                    
                    val parsed = parseResponse(aiResponseAccumulated)
                    
                    _uiState.update { state ->
                        val updated = state.messages.toMutableList()
                        if (updated.isNotEmpty()) {
                            updated[updated.lastIndex] = parsed
                        }
                        state.copy(
                            messages = updated,
                            currentHints = parsed.hints
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Không thể bắt đầu hội thoại: ${e.localizedMessage}",
                        messages = emptyList()
                    )
                }
            } finally {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isAiThinking) return

        val userMessage = ChatMessage(content = text, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isAiThinking = true,
                errorMessage = null,
                currentHints = emptyList()
            )
        }

        viewModelScope.launch {
            try {
                // Build simple dialogue history for context
                val conversationHistory = _uiState.value.messages.takeLast(6).joinToString("\n") { msg ->
                    if (msg.isUser) "Học viên: ${msg.content}" else "Yamada-san: ${msg.content}"
                }
                
                val promptText = """
                    Lịch sử hội thoại:
                    $conversationHistory
                    
                    Tin nhắn mới nhất của Học viên: "$text"
                    Hãy phản hồi Yamada-san bám sát quy tắc!
                """.trimIndent()

                var aiResponseAccumulated = ""
                val initialAiMessage = ChatMessage(content = "", isUser = false)
                _uiState.update { it.copy(messages = it.messages + initialAiMessage) }

                aiRepository.askKaiwaStream(
                    systemInstructionText = systemInstructionText,
                    prompt = promptText
                ).collect { chunk ->
                    val newText = chunk.text ?: ""
                    aiResponseAccumulated += newText
                    
                    val parsed = parseResponse(aiResponseAccumulated)
                    
                    _uiState.update { state ->
                        val updated = state.messages.toMutableList()
                        if (updated.isNotEmpty()) {
                            updated[updated.lastIndex] = parsed
                        }
                        state.copy(
                            messages = updated,
                            currentHints = parsed.hints
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Lỗi phản hồi: ${e.localizedMessage}",
                        messages = it.messages.dropLast(1)
                    )
                }
            } finally {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    fun toggleTranslationVisibility(index: Int) {
        _uiState.update { state ->
            val updated = state.messages.toMutableList()
            if (index in updated.indices) {
                val msg = updated[index]
                updated[index] = msg.copy(isTranslationVisible = !msg.isTranslationVisible)
            }
            state.copy(messages = updated)
        }
    }

    private fun parseResponse(raw: String): ChatMessage {
        val feedbackRegex = Regex("<feedback>(.*?)</feedback>", RegexOption.DOT_MATCHES_ALL)
        val translationRegex = Regex("<translation>(.*?)</translation>", RegexOption.DOT_MATCHES_ALL)
        val hintsRegex = Regex("<hints>(.*?)</hints>", RegexOption.DOT_MATCHES_ALL)

        val feedback = feedbackRegex.find(raw)?.groups?.get(1)?.value?.trim()
        val translation = translationRegex.find(raw)?.groups?.get(1)?.value?.trim()
        val hintsRaw = hintsRegex.find(raw)?.groups?.get(1)?.value?.trim()

        var cleanContent = raw
            .replace(feedbackRegex, "")
            .replace(translationRegex, "")
            .replace(hintsRegex, "")
            .trim()

        val parsedHints = mutableListOf<KaiwaHint>()
        if (!hintsRaw.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<KaiwaHint>>() {}.type
                val parsed: List<KaiwaHint>? = gson.fromJson(hintsRaw, type)
                if (parsed != null) {
                    parsedHints.addAll(parsed)
                }
            } catch (e: Exception) {
                // Ignore parsing issues during streaming construction
            }
        }

        return ChatMessage(
            content = cleanContent,
            isUser = false,
            translation = translation,
            feedback = feedback,
            hints = parsedHints
        )
    }

    companion object {
        fun factory(lessonId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return KaiwaPracticeViewModel(application, lessonId) as T
            }
        }
    }
}

// --- Composable UI ---
@Composable
fun KaiwaPracticeRoute(
    lessonId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: KaiwaPracticeViewModel = viewModel(factory = KaiwaPracticeViewModel.factory(lessonId))
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        LessonScreenLoading()
    } else {
        KaiwaPracticeScreen(
            lessonId = lessonId,
            viewModel = viewModel,
            onBack = onBack,
            modifier = modifier
        )
    }
}

@Composable
fun KaiwaPracticeScreen(
    lessonId: Int,
    viewModel: KaiwaPracticeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val speak = rememberDeferredJapaneseSpeaker()

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                textInput = spokenText
            }
        }
    }

    // Auto-play Yamada-san's completed replies for a fully spoken hands-free 1v1 experience!
    LaunchedEffect(uiState.messages.size, uiState.isAiThinking) {
        if (!uiState.isAiThinking && uiState.messages.isNotEmpty()) {
            val lastMessage = uiState.messages.last()
            if (!lastMessage.isUser && lastMessage.content.isNotBlank()) {
                val cleanSpeech = lastMessage.content
                    .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)")) { it.groupValues[1] }
                speak(cleanSpeech)
            }
        }
    }

    // Auto-scroll to bottom of chat
    LaunchedEffect(uiState.messages.size, uiState.isAiThinking) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MintPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MintLight.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌸", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hội thoại AI Sensei",
                            fontWeight = FontWeight.Bold,
                            color = PremiumDark,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Luyện tập theo Bài ${lessonId - 3}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBackground)
        ) {
            // Chat Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.messages.indices.toList()) { index ->
                    val msg = uiState.messages[index]
                    if (msg.isUser) {
                        UserChatBubble(msg)
                    } else {
                        AiChatBubble(
                            msg = msg,
                            onPlaySpeech = {
                                // Extract clean text ignoring furigana bracket patterns like [私](わたし) -> 私
                                val cleanSpeech = msg.content
                                    .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)")) { it.groupValues[1] }
                                speak(cleanSpeech)
                            },
                            onToggleTranslation = { viewModel.toggleTranslationVisibility(index) }
                        )
                    }
                }

                if (uiState.isAiThinking && uiState.messages.isNotEmpty() && uiState.messages.last().content.isEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MintPrimary
                            )
                            Text("AI Sensei đang gõ...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            // Error display
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick reply chips
            AnimatedVisibility(
                visible = uiState.currentHints.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "💡 Gợi ý trả lời:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 18.dp, bottom = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.currentHints.forEach { hint ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MintPrimary.copy(alpha = 0.08f))
                                    .clickable {
                                        textInput = hint.japanese
                                        speak(hint.japanese)
                                        keyboardController?.show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = hint.japanese,
                                        fontWeight = FontWeight.Bold,
                                        color = MintPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = hint.vietnamese,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Chat Input Bar
            Surface(
                color = SurfaceWhite,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Trò chuyện bằng tiếng Nhật...", fontSize = 14.sp) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintPrimary,
                            unfocusedBorderColor = DisabledGray,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói tiếng Nhật (ja-JP)...")
                                    }
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        // Voice search not supported
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Nói trực tiếp",
                                    tint = MintPrimary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        },
                        enabled = textInput.isNotBlank() && !uiState.isAiThinking,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (textInput.isNotBlank() && !uiState.isAiThinking) MintPrimary else DisabledGray,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gửi",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- Chat Bubble Helpers ---
@Composable
private fun UserChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 2.dp
            ),
            colors = CardDefaults.cardColors(containerColor = MintPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = msg.content,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun AiChatBubble(
    msg: ChatMessage,
    onPlaySpeech: () -> Unit,
    onToggleTranslation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Grammar Feedback Panel (Displayed if AI has corrections)
            msg.feedback?.let { fb ->
                if (fb.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentYellow.copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentYellow)
                            Column {
                                Text("Gợi ý sửa lỗi ngữ pháp:", fontWeight = FontWeight.Bold, color = PremiumDark, fontSize = 13.sp)
                                Text(fb, color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Chat content Card
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 2.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Japanese Text (Parsed with Furigana if possible)
                    JapaneseTextWithFurigana(msg.content)

                    // Expandable Vietnamese Translation
                    msg.translation?.let { trans ->
                        if (trans.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            AnimatedVisibility(visible = msg.isTranslationVisible) {
                                Text(
                                    text = trans,
                                    color = MintPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPlaySpeech,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Phát âm",
                                tint = MintPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (msg.translation != null && msg.translation.isNotBlank()) {
                            TextButton(
                                onClick = onToggleTranslation,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (msg.isTranslationVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TextSecondary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (msg.isTranslationVisible) "Ẩn nghĩa" else "Dịch nghĩa",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Furigana rendering parser ---
@Composable
private fun JapaneseTextWithFurigana(text: String) {
    val regex = Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)")
    val matches = regex.findAll(text).toList()

    if (matches.isEmpty()) {
        Text(text, fontSize = 17.sp, color = TextPrimary, lineHeight = 24.sp)
        return
    }

    FlowLayout(modifier = Modifier.fillMaxWidth()) {
        var lastIdx = 0
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            
            if (start > lastIdx) {
                val plainPart = text.substring(lastIdx, start)
                for (i in plainPart.indices) {
                    val charStr = plainPart[i].toString()
                    Text(
                        text = charStr,
                        fontSize = 17.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            val kanji = match.groups[1]?.value.orEmpty()
            val furi = match.groups[2]?.value.orEmpty()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
            ) {
                Text(furi, fontSize = 10.sp, color = MintPrimary, fontWeight = FontWeight.SemiBold)
                Text(kanji, fontSize = 17.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            lastIdx = end
        }
        if (lastIdx < text.length) {
            val plainPart = text.substring(lastIdx)
            for (i in plainPart.indices) {
                val charStr = plainPart[i].toString()
                Text(
                    text = charStr,
                    fontSize = 17.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FlowLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        
        var xPosition = 0
        var yPosition = 0
        var maxHeight = 0
        
        val childPositions = mutableListOf<Pair<Int, Int>>()
        
        for (placeable in placeables) {
            if (xPosition + placeable.width > layoutWidth) {
                xPosition = 0
                yPosition += maxHeight
                maxHeight = 0
            }
            childPositions.add(Pair(xPosition, yPosition))
            xPosition += placeable.width
            maxHeight = maxOf(maxHeight, placeable.height)
        }
        
        layout(
            width = layoutWidth,
            height = maxOf(yPosition + maxHeight, 0)
        ) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = childPositions[index]
                placeable.placeRelative(x, y)
            }
        }
    }
}

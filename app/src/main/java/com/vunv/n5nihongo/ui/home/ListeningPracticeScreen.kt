package com.vunv.n5nihongo.ui.home

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Word
import com.vunv.n5nihongo.ui.components.rememberDeferredJapaneseSpeaker
import com.vunv.n5nihongo.ui.components.cleanKanjiReading
import com.vunv.n5nihongo.ui.components.LessonScreenLoading
import com.vunv.n5nihongo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- Data Models for MCQ Question ---
data class McqQuestion(
    val word: Word,
    val options: List<String>,
    val correctIndex: Int,
    var selectedIndex: Int? = null,
    var isAnswered: Boolean = false
)

// --- ViewModel ---
class ListeningPracticeViewModel(
    application: Application,
    private val lessonId: Int
) : AndroidViewModel(application) {

    private val wordDao = AppDatabaseProvider.getDatabase(application).wordDao()

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // MCQ States
    private val _mcqQuestions = MutableStateFlow<List<McqQuestion>>(emptyList())
    val mcqQuestions: StateFlow<List<McqQuestion>> = _mcqQuestions.asStateFlow()

    private val _currentMcqIndex = MutableStateFlow(0)
    val currentMcqIndex: StateFlow<Int> = _currentMcqIndex.asStateFlow()

    private val _mcqScore = MutableStateFlow(0)
    val mcqScore: StateFlow<Int> = _mcqScore.asStateFlow()

    private val _isMcqFinished = MutableStateFlow(false)
    val isMcqFinished: StateFlow<Boolean> = _isMcqFinished.asStateFlow()

    // Dictation States
    private val _currentDictationWord = MutableStateFlow<Word?>(null)
    val currentDictationWord: StateFlow<Word?> = _currentDictationWord.asStateFlow()

    private val _dictationInput = MutableStateFlow("")
    val dictationInput: StateFlow<String> = _dictationInput.asStateFlow()

    private val _dictationChecked = MutableStateFlow(false)
    val dictationChecked: StateFlow<Boolean> = _dictationChecked.asStateFlow()

    private val _isDictationCorrect = MutableStateFlow(false)
    val isDictationCorrect: StateFlow<Boolean> = _isDictationCorrect.asStateFlow()

    private val _dictationHintLevel = MutableStateFlow(0) // 0: No hint, 1: length, 2: first char
    val dictationHintLevel: StateFlow<Int> = _dictationHintLevel.asStateFlow()

    // Passive Listening States
    private val _passiveIndex = MutableStateFlow(0)
    val passiveIndex: StateFlow<Int> = _passiveIndex.asStateFlow()

    private val _isPassivePlaying = MutableStateFlow(false)
    val isPassivePlaying: StateFlow<Boolean> = _isPassivePlaying.asStateFlow()

    private val _showPassiveMeaning = MutableStateFlow(false)
    val showPassiveMeaning: StateFlow<Boolean> = _showPassiveMeaning.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val lessonWords = wordDao.getWordsByLessonOnce(lessonId)
            _words.value = lessonWords
            _isLoading.value = false
            resetMcq()
            nextDictationWord()
        }
    }

    // --- MCQ Logic ---
    fun resetMcq() {
        val wordList = _words.value.shuffled()
        if (wordList.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            val qList = wordList.take(10).map { correctWord ->
                val wrongPool = wordList.filter { it.id != correctWord.id }
                val wrongOptions = wrongPool.shuffled().take(3).map { it.meaning }
                
                // If wrong options are not enough, pad with some defaults
                val finalWrongOptions = if (wrongOptions.size < 3) {
                    wrongOptions + listOf("Tôi", "Bạn", "Chào hỏi").take(3 - wrongOptions.size)
                } else wrongOptions

                val allOptions = (finalWrongOptions + correctWord.meaning).shuffled()
                val correctIndex = allOptions.indexOf(correctWord.meaning)

                McqQuestion(
                    word = correctWord,
                    options = allOptions,
                    correctIndex = correctIndex
                )
            }
            _mcqQuestions.value = qList
            _currentMcqIndex.value = 0
            _mcqScore.value = 0
            _isMcqFinished.value = false
        }
    }

    fun selectMcqOption(optionIndex: Int) {
        val currentList = _mcqQuestions.value.toMutableList()
        val index = _currentMcqIndex.value
        if (index >= currentList.size) return
        val question = currentList[index]
        if (question.isAnswered) return

        val updatedQuestion = question.copy(
            selectedIndex = optionIndex,
            isAnswered = true
        )
        currentList[index] = updatedQuestion
        _mcqQuestions.value = currentList

        if (optionIndex == question.correctIndex) {
            _mcqScore.update { it + 1 }
        }
    }

    fun nextMcqQuestion() {
        val nextIndex = _currentMcqIndex.value + 1
        if (nextIndex < _mcqQuestions.value.size) {
            _currentMcqIndex.value = nextIndex
        } else {
            _isMcqFinished.value = true
        }
    }

    // --- Dictation Logic ---
    fun nextDictationWord() {
        val wordList = _words.value
        if (wordList.isEmpty()) return
        _currentDictationWord.value = wordList.random()
        _dictationInput.value = ""
        _dictationChecked.value = false
        _isDictationCorrect.value = false
        _dictationHintLevel.value = 0
    }

    fun setDictationInput(input: String) {
        _dictationInput.value = input
    }

    fun checkDictation() {
        val word = _currentDictationWord.value ?: return
        val input = _dictationInput.value.trim().lowercase()
        val cleanFurigana = cleanKanjiReading(word.furigana).lowercase()
        val cleanKanji = cleanKanjiReading(word.kanji).lowercase()
        
        val isCorrect = input == cleanFurigana || 
                        input == cleanKanji || 
                        input == word.furigana.lowercase() || 
                        input == word.kanji.lowercase()
        
        _isDictationCorrect.value = isCorrect
        _dictationChecked.value = true
    }

    fun requestDictationHint() {
        _dictationHintLevel.update { (it + 1).coerceAtMost(2) }
    }

    // --- Passive Listening Logic ---
    fun startPassiveAutoplay(speak: (String) -> Unit) {
        if (_isPassivePlaying.value) return
        _isPassivePlaying.value = true
        viewModelScope.launch {
            while (_isPassivePlaying.value) {
                val list = _words.value
                if (list.isEmpty()) {
                    _isPassivePlaying.value = false
                    break
                }
                val currentWord = list[_passiveIndex.value]
                _showPassiveMeaning.value = false
                
                // 1. Speak Japanese word (use furigana if available, fallback to kanji)
                val jaSpeech = if (currentWord.furigana.isNotBlank()) currentWord.furigana else currentWord.kanji
                speak(jaSpeech)
                
                // Wait for speech to complete + delay
                delay(2500)
                if (!_isPassivePlaying.value) break

                // 2. Show meaning & Speak again (optional or just show)
                _showPassiveMeaning.value = true
                delay(2000)
                if (!_isPassivePlaying.value) break

                // 3. Move to next index
                val nextIdx = (_passiveIndex.value + 1) % list.size
                _passiveIndex.value = nextIdx
            }
        }
    }

    fun pausePassive() {
        _isPassivePlaying.value = false
    }

    fun togglePassivePlayPause(speak: (String) -> Unit) {
        if (_isPassivePlaying.value) {
            pausePassive()
        } else {
            startPassiveAutoplay(speak)
        }
    }

    fun nextPassive(speak: (String) -> Unit) {
        val list = _words.value
        if (list.isEmpty()) return
        pausePassive()
        _passiveIndex.update { (it + 1) % list.size }
        _showPassiveMeaning.value = false
        viewModelScope.launch {
            delay(100)
            val nextWord = list[_passiveIndex.value]
            speak(if (nextWord.furigana.isNotBlank()) nextWord.furigana else nextWord.kanji)
        }
    }

    fun prevPassive(speak: (String) -> Unit) {
        val list = _words.value
        if (list.isEmpty()) return
        pausePassive()
        _passiveIndex.update { (it - 1 + list.size) % list.size }
        _showPassiveMeaning.value = false
        viewModelScope.launch {
            delay(100)
            val prevWord = list[_passiveIndex.value]
            speak(if (prevWord.furigana.isNotBlank()) prevWord.furigana else prevWord.kanji)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _isPassivePlaying.value = false
    }

    companion object {
        fun factory(lessonId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return ListeningPracticeViewModel(application, lessonId) as T
            }
        }
    }
}

// --- Composable Screens ---
@Composable
fun ListeningPracticeRoute(
    lessonId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ListeningPracticeViewModel = viewModel(factory = ListeningPracticeViewModel.factory(lessonId))
    val isLoading by viewModel.isLoading.collectAsState()
    val words by viewModel.words.collectAsState()

    if (isLoading) {
        LessonScreenLoading()
    } else {
        ListeningPracticeScreen(
            lessonId = lessonId,
            words = words,
            viewModel = viewModel,
            onBack = onBack,
            modifier = modifier
        )
    }
}

@Composable
fun ListeningPracticeScreen(
    lessonId: Int,
    words: List<Word>,
    viewModel: ListeningPracticeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Trắc nghiệm", "Chính tả", "Nghe thụ động", "Hội thoại N5")
    val speak = rememberDeferredJapaneseSpeaker()

    // Stop passive player if user leaves passive tab
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex != 2) {
            viewModel.pausePassive()
        }
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            Column {
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
                    Text(
                        text = "Luyện Nghe - Bài ${lessonId - 3}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PremiumDark,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SurfaceWhite,
                    contentColor = MintPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MintPrimary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == index) MintPrimary else TextSecondary
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBackground)
        ) {
            if (words.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có từ vựng cho bài học này để luyện nghe.", color = TextSecondary)
                }
            } else {
                when (selectedTabIndex) {
                    0 -> McqTabContent(viewModel, speak)
                    1 -> DictationTabContent(viewModel, speak)
                    2 -> PassiveTabContent(viewModel, speak)
                    3 -> DialogueQuizTabContent(lessonId, speak)
                }
            }
        }
    }
}

// --- Tab 1: Audio MCQ Composable ---
@Composable
private fun McqTabContent(
    viewModel: ListeningPracticeViewModel,
    speak: (String) -> Unit
) {
    val questions by viewModel.mcqQuestions.collectAsState()
    val currentIndex by viewModel.currentMcqIndex.collectAsState()
    val score by viewModel.mcqScore.collectAsState()
    val isFinished by viewModel.isMcqFinished.collectAsState()

    if (questions.isEmpty()) return

    if (isFinished) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Hoàn thành Luyện Nghe!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PremiumDark
                    )
                    Text(
                        text = "Kết quả: $score / ${questions.size} câu trả lời đúng.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    
                    val ratingMsg = when {
                        score >= 9 -> "Xuất sắc! Khả năng nghe của bạn rất nhạy bén! 🌟"
                        score >= 7 -> "Khá lắm! Hãy phát huy tiếp tục nhé! 👍"
                        else -> "Đừng nản lòng! Luyện tập nhiều lần sẽ giúp tai bạn nhạy âm hơn! 🌸"
                    }
                    Text(
                        text = ratingMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MintPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = { viewModel.resetMcq() },
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Luyện nghe lại")
                    }
                }
            }
        }
        return
    }

    val currentQuestion = questions[currentIndex]

    // Auto speak Japanese when question loads
    LaunchedEffect(currentQuestion.word.id) {
        delay(200)
        speak(if (currentQuestion.word.furigana.isNotBlank()) currentQuestion.word.furigana else currentQuestion.word.kanji)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Câu hỏi ${currentIndex + 1}/${questions.size}",
                fontWeight = FontWeight.Bold,
                color = PremiumDark
            )
            Text(
                text = "Điểm số: $score",
                fontWeight = FontWeight.Bold,
                color = MintPrimary
            )
        }
        LinearProgressIndicator(
            progress = (currentIndex + 1).toFloat() / questions.size.toFloat(),
            color = MintPrimary,
            trackColor = DisabledGray,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        )

        // Audio Player Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        speak(if (currentQuestion.word.furigana.isNotBlank()) currentQuestion.word.furigana else currentQuestion.word.kanji)
                    },
                    modifier = Modifier
                        .size(100.dp)
                        .background(MintPrimary.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Phát âm",
                        tint = MintPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chạm để nghe lại âm thanh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                if (currentQuestion.isAnswered) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = currentQuestion.word.kanji.ifBlank { currentQuestion.word.furigana },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PremiumDark
                    )
                    if (currentQuestion.word.kanji.isNotBlank()) {
                        Text(
                            text = "(${currentQuestion.word.furigana})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            currentQuestion.options.forEachIndexed { optIndex, optionText ->
                val isCorrect = optIndex == currentQuestion.correctIndex
                val isSelected = optIndex == currentQuestion.selectedIndex
                val isAnswered = currentQuestion.isAnswered

                val btnBgColor = when {
                    !isAnswered -> Color.White
                    isCorrect -> MintPrimary.copy(alpha = 0.2f)
                    isSelected -> ErrorRed.copy(alpha = 0.2f)
                    else -> Color.White
                }
                val borderStrokeColor = when {
                    !isAnswered -> Color.Transparent
                    isCorrect -> MintPrimary
                    isSelected -> ErrorRed
                    else -> Color.Transparent
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = btnBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isAnswered) {
                            viewModel.selectMcqOption(optIndex)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected || (isAnswered && isCorrect)) PremiumDark else TextPrimary
                        )
                        if (isAnswered) {
                            if (isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintPrimary)
                            } else if (isSelected) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        // Next Button
        AnimatedVisibility(
            visible = currentQuestion.isAnswered,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Button(
                onClick = { viewModel.nextMcqQuestion() },
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (currentIndex == questions.size - 1) "Xem kết quả" else "Câu tiếp theo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

// --- Tab 2: Dictation Composable ---
@Composable
private fun DictationTabContent(
    viewModel: ListeningPracticeViewModel,
    speak: (String) -> Unit
) {
    val word by viewModel.currentDictationWord.collectAsState()
    val input by viewModel.dictationInput.collectAsState()
    val checked by viewModel.dictationChecked.collectAsState()
    val isCorrect by viewModel.isDictationCorrect.collectAsState()
    val hintLevel by viewModel.dictationHintLevel.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    if (word == null) return

    // Auto speak when new word is chosen
    LaunchedEffect(word!!.id) {
        delay(200)
        speak(if (word!!.furigana.isNotBlank()) word!!.furigana else word!!.kanji)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            speak(if (word!!.furigana.isNotBlank()) word!!.furigana else word!!.kanji)
                        },
                        modifier = Modifier
                            .size(90.dp)
                            .background(MintPrimary.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Phát âm",
                            tint = MintPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hãy nghe và viết lại bằng Hiragana hoặc Kanji",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // Hints display
                    if (hintLevel > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentYellow.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            val cleanFuri = cleanKanjiReading(word!!.furigana)
                            val hintText = if (hintLevel == 1) {
                                "Gợi ý: Từ này có ${cleanFuri.length} ký tự."
                            } else {
                                "Gợi ý: Bắt đầu bằng chữ '${cleanFuri.take(1)}' (${cleanFuri.length} ký tự)."
                            }
                            Text(hintText, color = PremiumDark, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Input Field
        item {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    if (!checked) viewModel.setDictationInput(it)
                },
                placeholder = { Text("Nhập Hiragana hoặc Kanji nghe được...") },
                singleLine = true,
                readOnly = checked,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MintPrimary,
                    unfocusedBorderColor = DisabledGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onAny = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (!checked) viewModel.checkDictation()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!checked) {
                    Button(
                        onClick = { viewModel.requestDictationHint() },
                        enabled = hintLevel < 2,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Gợi ý")
                    }
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.checkDictation()
                        },
                        enabled = input.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                    ) {
                        Text("Kiểm tra", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.nextDictationWord() },
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Từ tiếp theo", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        // Result Card
        if (checked) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) MintPrimary.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isCorrect) MintPrimary else ErrorRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = if (isCorrect) "Hoàn toàn chính xác!" else "Chưa chính xác rồi!",
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) MintPrimary else ErrorRed,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Đáp án đúng: ${word!!.kanji.ifBlank { word!!.furigana }} (${word!!.furigana})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PremiumDark
                            )
                            Text(
                                text = "Ý nghĩa: ${word!!.meaning}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 3: Passive Listening Autoplay Composable ---
@Composable
private fun PassiveTabContent(
    viewModel: ListeningPracticeViewModel,
    speak: (String) -> Unit
) {
    val words by viewModel.words.collectAsState()
    val passiveIdx by viewModel.passiveIndex.collectAsState()
    val isPlaying by viewModel.isPassivePlaying.collectAsState()
    val showMeaning by viewModel.showPassiveMeaning.collectAsState()

    if (words.isEmpty()) return
    val currentWord = words[passiveIdx]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Disk / Art Card
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Autoplay circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            if (isPlaying) MintPrimary.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayDisabled,
                        contentDescription = null,
                        tint = if (isPlaying) MintPrimary else TextSecondary,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Japanese Text
                Text(
                    text = currentWord.kanji.ifBlank { currentWord.furigana },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = PremiumDark,
                    textAlign = TextAlign.Center
                )
                if (currentWord.kanji.isNotBlank()) {
                    Text(
                        text = "(${currentWord.furigana})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Animated Vietnamese Meaning
                AnimatedVisibility(
                    visible = showMeaning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = currentWord.meaning,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = MintPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(MintPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Music Controls Bar
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.prevPassive(speak) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Trước", modifier = Modifier.size(36.dp), tint = PremiumDark)
                }

                IconButton(
                    onClick = { viewModel.togglePassivePlayPause(speak) },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MintPrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Chạy/Dừng",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.nextPassive(speak) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Tiếp theo", modifier = Modifier.size(36.dp), tint = PremiumDark)
                }
            }
        }

        // Playlist View Header
        Text(
            text = "Danh sách ôn tập (${words.size} từ)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PremiumDark,
            modifier = Modifier.align(Alignment.Start)
        )

        // Playlist Items
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(words) { index, item ->
                    val isCurrent = index == passiveIdx
                    val rowBgColor = if (isCurrent) MintPrimary.copy(alpha = 0.08f) else Color.Transparent
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(rowBgColor)
                            .clickable {
                                viewModel.pausePassive()
                                viewModel.nextPassive { speak(item.furigana.ifBlank { item.kanji }) }
                                // Quick cheat to set specific index
                                viewModel.pausePassive()
                                viewModel.prevPassive { }
                                repeat((index - passiveIdx + words.size) % words.size) {
                                    viewModel.nextPassive { }
                                }
                                speak(item.furigana.ifBlank { item.kanji })
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrent && isPlaying) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Text("${index + 1}.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = item.kanji.ifBlank { item.furigana },
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) MintPrimary else PremiumDark
                            )
                            if (item.kanji.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "(${item.furigana})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = item.meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueQuizTabContent(
    lessonId: Int,
    speak: (String) -> Unit
) {
    val exercise = remember(lessonId) {
        com.vunv.n5nihongo.data.model.ListeningExercisesData.getExerciseForLesson(lessonId)
    }

    if (exercise == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Đang cập nhật bài nghe hội thoại cho bài học này. Vui lòng quay lại sau!",
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val authRepository = remember { com.vunv.n5nihongo.data.auth.AuthRepository() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val CoralOrange = Color(0xFFFF5722)

    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }

    var activeLineIndex by remember { mutableStateOf<Int?>(null) }
    var isPlayingDialogue by remember { mutableStateOf(false) }

    // Auto dialogue play routine
    val playDialogue: () -> Unit = {
        if (!isPlayingDialogue) {
            isPlayingDialogue = true
            coroutineScope.launch {
                for (i in exercise.dialogue.indices) {
                    if (!isPlayingDialogue) break
                    activeLineIndex = i
                    speak(exercise.dialogue[i].text)
                    val duration = exercise.dialogue[i].text.length * 280L + 1500L
                    delay(duration)
                }
                activeLineIndex = null
                isPlayingDialogue = false
            }
        } else {
            isPlayingDialogue = false
            activeLineIndex = null
        }
    }

    // Stop speaking when leaving or disposing
    DisposableEffect(Unit) {
        onDispose {
            isPlayingDialogue = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Situation Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exercise.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )
                        IconButton(
                            onClick = playDialogue,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isPlayingDialogue) CoralOrange.copy(alpha = 0.15f) else MintPrimary.copy(alpha = 0.15f)
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlayingDialogue) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Phát hội thoại",
                                tint = if (isPlayingDialogue) CoralOrange else MintPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "🎧 Bối cảnh: ${exercise.situation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PremiumDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Dialogue Lines Section
        item {
            Text(
                text = "Hội thoại nghe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PremiumDark
            )
        }

        itemsIndexed(exercise.dialogue) { index, line ->
            val isCurrent = index == activeLineIndex
            val isLeft = index % 2 == 0
            
            val bubbleBgColor = if (isCurrent) {
                MintPrimary.copy(alpha = 0.12f)
            } else if (isLeft) {
                SurfaceWhite
            } else {
                Color(0xFFE8F5E9)
            }

            val alignment = if (isLeft) Alignment.Start else Alignment.End

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = alignment
            ) {
                Text(
                    text = line.speaker,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLeft) MintPrimary else Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                Card(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isLeft) 0.dp else 16.dp,
                        bottomEnd = if (isLeft) 16.dp else 0.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = bubbleBgColor),
                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, MintPrimary) else null,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable { speak(line.text) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = PremiumDark
                        )
                        if (isAnswered || showExplanation) {
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = line.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Quiz Question Section
        item {
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CÂU HỎI LUYỆN NGHE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralOrange
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = exercise.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = PremiumDark
                    )
                    Spacer(Modifier.height(12.dp))

                    exercise.options.forEachIndexed { optIndex, optionText ->
                        val isCorrectOpt = optIndex == exercise.correctIndex
                        val isSelectedOpt = optIndex == selectedOption

                        val optionBg = when {
                            isAnswered && isCorrectOpt -> Color(0xFFE8F5E9)
                            isAnswered && isSelectedOpt && !isCorrectOpt -> Color(0xFFFFEBEE)
                            isSelectedOpt -> MintPrimary.copy(alpha = 0.08f)
                            else -> Color.Transparent
                        }

                        val optionBorderColor = when {
                            isAnswered && isCorrectOpt -> Color(0xFF4CAF50)
                            isAnswered && isSelectedOpt && !isCorrectOpt -> Color(0xFFEF5350)
                            isSelectedOpt -> MintPrimary
                            else -> Color.Black.copy(alpha = 0.1f)
                        }

                        val optionTextColor = when {
                            isAnswered && isCorrectOpt -> Color(0xFF2E7D32)
                            isAnswered && isSelectedOpt && !isCorrectOpt -> Color(0xFFC62828)
                            isSelectedOpt -> MintPrimary
                            else -> PremiumDark
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = optionBg),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, optionBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!isAnswered) {
                                        selectedOption = optIndex
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${(65 + optIndex).toChar()}.",
                                    fontWeight = FontWeight.Bold,
                                    color = optionTextColor
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = optionText,
                                    fontWeight = FontWeight.Medium,
                                    color = optionTextColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (!isAnswered) {
                        Button(
                            onClick = {
                                if (selectedOption != null) {
                                    isAnswered = true
                                    showExplanation = true
                                    if (selectedOption == exercise.correctIndex) {
                                        coroutineScope.launch {
                                            authRepository.updateUserProgress(15, context)
                                        }
                                    }
                                }
                            },
                            enabled = selectedOption != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kiểm tra câu trả lời", color = Color.White)
                        }
                    } else {
                        val isWin = selectedOption == exercise.correctIndex
                        val bannerColor = if (isWin) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        val textColor = if (isWin) Color(0xFF2E7D32) else Color(0xFFC62828)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bannerColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isWin) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = textColor
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isWin) "Chính xác! Bạn nhận được +15 XP 🎉" else "Chưa đúng rồi! Hãy xem lời giải nhé.",
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "💡 Giải thích: ${exercise.explanation}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PremiumDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

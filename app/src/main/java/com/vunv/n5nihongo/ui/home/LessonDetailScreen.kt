package com.vunv.n5nihongo.ui.home

import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MenuBook
import com.vunv.n5nihongo.ui.theme.MintPrimary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.data.local.AppDatabaseProvider
import com.vunv.n5nihongo.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.vunv.n5nihongo.ui.alphabet.DrawPracticeCanvas
import com.vunv.n5nihongo.ui.alphabet.KanjiVgStrokeDiagramPreview
import com.vunv.n5nihongo.ui.components.JapaneseText
import com.vunv.n5nihongo.ui.components.JapaneseAudioButton
import com.vunv.n5nihongo.ui.components.StrokeOrderAnimatedSection
import com.vunv.n5nihongo.ui.components.bestJapaneseReading
import com.vunv.n5nihongo.ui.components.LessonScreenLoading
import com.vunv.n5nihongo.ui.components.rememberDeferredJapaneseSpeaker
import androidx.compose.ui.unit.sp
import com.vunv.n5nihongo.ui.theme.LightBackground

data class LessonWordUiModel(
    val id: Int,
    val furigana: String,
    val kanji: String,
    val meaning: String,
    val type: String,
    val isFavorite: Boolean
)

data class GrammarUiModel(
    val id: Int,
    val title: String,
    val structure: String,
    val explanation: String,
    val examples: List<ExampleUiModel>
)

data class ExampleUiModel(
    val japanese: String,
    val vietnamese: String
)

data class KanjiUiModel(
    val id: Int,
    val character: String,
    val onyomi: String,
    val kunyomi: String,
    val meaning: String,
    val strokeCount: Int
)

data class LessonDetailUiState(
    val isLoading: Boolean = true,
    val words: List<LessonWordUiModel> = emptyList(),
    val grammars: List<GrammarUiModel> = emptyList(),
    val kanjis: List<KanjiUiModel> = emptyList()
)

private fun lessonScreenTitle(lessonId: Int): String = when (lessonId) {
    1 -> "Bài 1: Bảng chữ cái Hiragana (Cơ bản)"
    2 -> "Bài 2: Bảng chữ cái Katakana (Cơ bản)"
    3 -> "Bài 3: Số đếm & Thời gian"
    else -> "Bài ${lessonId - 3} - Chi tiết"
}

class LessonDetailViewModel(
    application: Application,
    private val lessonId: Int
) : AndroidViewModel(application) {

    private val database = AppDatabaseProvider.getDatabase(application)
    private val wordDao = database.wordDao()
    private val grammarDao = database.grammarDao()
    private val kanjiDao = database.kanjiDao()
    private val gson = com.google.gson.Gson()

    init {
        // Data is now seeded globally by LearningPathViewModel using DataRepository
    }

    val uiState: StateFlow<LessonDetailUiState> = combine(
        wordDao.getWordsByLesson(lessonId),
        grammarDao.getGrammarByLesson(lessonId),
        kanjiDao.getKanjiByLesson(lessonId)
    ) { words, grammars, kanjis ->
        LessonDetailUiState(
            isLoading = false,
            words = words.toLessonWordUiModels(),
            grammars = grammars.map { it.toUiModel(gson) },
            kanjis = kanjis.map { it.toUiModel() }
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LessonDetailUiState(isLoading = true)
        )

    fun toggleFavorite(wordId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            wordDao.updateFavoriteStatus(wordId, isFavorite)
        }
    }

    companion object {
        fun factory(lessonId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return LessonDetailViewModel(application, lessonId) as T
            }
        }
    }
}

private fun List<Word>.toLessonWordUiModels(): List<LessonWordUiModel> =
    this.sortedBy { it.id }.map { word ->
        LessonWordUiModel(
            id = word.id,
            furigana = word.furigana,
            kanji = word.kanji,
            meaning = word.meaning,
            type = word.type,
            isFavorite = word.isFavorite
        )
    }

private fun com.vunv.n5nihongo.data.model.Grammar.toUiModel(gson: com.google.gson.Gson): GrammarUiModel {
    val examplesList: List<ExampleUiModel> = try {
        val type = object : com.google.gson.reflect.TypeToken<List<ExampleUiModel>>() {}.type
        gson.fromJson<List<ExampleUiModel>>(this.examples, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    return GrammarUiModel(
        id = this.id,
        title = this.title,
        structure = this.structure,
        explanation = this.explanation,
        examples = examplesList
    )
}

private fun com.vunv.n5nihongo.data.model.Kanji.toUiModel(): KanjiUiModel {
    return KanjiUiModel(
        id = this.id,
        character = this.character,
        onyomi = this.onyomi,
        kunyomi = this.kunyomi,
        meaning = this.meaning,
        strokeCount = this.strokeCount
    )
}

@Composable
fun LessonDetailRoute(
    lessonId: Int,
    onStartQuiz: (Int, List<Int>) -> Unit = { _, _ -> },
    onStartFlashcard: (Int, List<Int>) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: LessonDetailViewModel = viewModel(factory = LessonDetailViewModel.factory(lessonId))
    val uiState by viewModel.uiState.collectAsState()

    LessonDetailScreen(
        lessonId = lessonId,
        uiState = uiState,
        onStartQuiz = onStartQuiz,
        onStartFlashcard = onStartFlashcard,
        onToggleFavorite = viewModel::toggleFavorite,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun LessonDetailScreen(
    lessonId: Int,
    uiState: LessonDetailUiState,
    onStartQuiz: (Int, List<Int>) -> Unit,
    onStartFlashcard: (Int, List<Int>) -> Unit,
    onToggleFavorite: (Int, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val tabs = listOf("Từ vựng", "Ngữ pháp", "Kanji")
    val speak = rememberDeferredJapaneseSpeaker()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Quay lại")
        }

        Text(
            text = lessonScreenTitle(lessonId),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (uiState.isLoading) {
            LessonScreenLoading(modifier = Modifier.weight(1f))
            return
        }

        val words = uiState.words
        val grammars = uiState.grammars
        val kanjis = uiState.kanjis

        when (lessonId) {
            1 -> {
                HiraganaTableScreen(
                    words = words,
                    onSpeak = speak,
                    onStartQuiz = { selectedIds -> onStartQuiz(lessonId, selectedIds) },
                    onStartFlashcard = { selectedIds -> onStartFlashcard(lessonId, selectedIds) }
                )
            }
            2 -> {
                KatakanaTableScreen(
                    words = words,
                    onSpeak = speak,
                    onStartQuiz = { selectedIds -> onStartQuiz(lessonId, selectedIds) },
                    onStartFlashcard = { selectedIds -> onStartFlashcard(lessonId, selectedIds) }
                )
            }
            3 -> {
                LessonThreeScreen(modifier = Modifier.weight(1f))
            }
            else -> {
                androidx.compose.material3.TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        androidx.compose.material3.Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Crossfade(
                    targetState = selectedTabIndex,
                    animationSpec = tween(220),
                    label = "tab_crossfade",
                    modifier = Modifier.weight(1f)
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> VocabularyTabContent(words, speak, onToggleFavorite)
                        1 -> GrammarTabContent(grammars, speak)
                        2 -> KanjiTabContent(kanjis, speak)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Luyện tập cùng AI Sensei 🌸",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )
                        Text(
                            text = "Kiểm tra tổng hợp: 10 từ vựng + toàn bộ ngữ pháp + 5 kanji. Đạt ≥80% để hoàn thành bài.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onStartQuiz(lessonId, emptyList()) },
                                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kiểm tra")
                            }
                            Button(
                                onClick = { onStartFlashcard(lessonId, words.map { it.id }) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Flashcard")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabularyTabContent(
    words: List<LessonWordUiModel>,
    speak: (String) -> Unit,
    onToggleFavorite: (Int, Boolean) -> Unit
) {
    Text(
        text = "Tổng số từ: ${words.size}",
        style = MaterialTheme.typography.bodyMedium
    )

    if (words.isEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Chưa có dữ liệu từ vựng cho bài này.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(words, key = { it.id }) { word ->
            LessonWordRow(
                word = word,
                onSpeak = speak,
                onToggleFavorite = { isFav -> onToggleFavorite(word.id, isFav) }
            )
            Divider()
        }
    }
}

@Composable
private fun GrammarTabContent(
    grammars: List<GrammarUiModel>,
    speak: (String) -> Unit
) {
    if (grammars.isEmpty()) {
        Text("Chưa có dữ liệu ngữ pháp.")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(grammars) { grammar ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = grammar.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Cấu trúc: ${grammar.structure}", color = MaterialTheme.colorScheme.primary)
                    Text(text = grammar.explanation)
                    androidx.compose.material3.HorizontalDivider()
                    Text("Ví dụ:", fontWeight = FontWeight.SemiBold)
                    grammar.examples.forEach { example ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(example.japanese, fontWeight = FontWeight.Medium)
                                Text(example.vietnamese, style = MaterialTheme.typography.bodySmall)
                            }
                            JapaneseAudioButton(text = example.japanese, onSpeak = speak)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanjiTabContent(
    kanjis: List<KanjiUiModel>,
    speak: (String) -> Unit
) {
    if (kanjis.isEmpty()) {
        Text("Chưa có dữ liệu Kanji.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(kanjis) { kanji ->
            val reading = bestJapaneseReading(kanji.character, kanji.kunyomi, kanji.onyomi)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clickable { speak(reading) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = kanji.character, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        KanjiVgStrokeDiagramPreview(
                            character = kanji.character,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(text = kanji.meaning, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    if (kanji.onyomi.isNotBlank()) {
                        Text(
                            text = "On: ${kanji.onyomi}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (kanji.kunyomi.isNotBlank()) {
                        Text(
                            text = "Kun: ${kanji.kunyomi}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    JapaneseAudioButton(
                        text = reading,
                        onSpeak = speak,
                        size = 32.dp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiraganaTableScreen(
    words: List<LessonWordUiModel>,
    onSpeak: (String) -> Unit,
    onStartQuiz: (List<Int>) -> Unit,
    onStartFlashcard: (List<Int>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Int>() }
    val speak = onSpeak
    var strokeSheetChar by remember { mutableStateOf<String?>(null) }
    val strokeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectableWords = words.filter { it.type == "hiragana" && it.furigana.isNotBlank() }
    val hiraganaByChar = words.associateBy { it.furigana }
    val openStroke: (String) -> Unit = { ch ->
        if (ch.isNotBlank()) strokeSheetChar = ch
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Chạm ô: chọn / nghe — giữ ô (long press): sơ đồ nét KanjiVG + luyện viết.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (words.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Chưa có dữ liệu Hiragana cho bài 1.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Đã chọn: ${selectedIds.size}/${selectableWords.size} chữ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    item { HiraganaRow(label = "A", chars = listOf("あ", "い", "う", "え", "お"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "KA", chars = listOf("か", "き", "く", "け", "こ"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "SA", chars = listOf("さ", "し", "す", "せ", "そ"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "TA", chars = listOf("た", "ち", "つ", "て", "と"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "NA", chars = listOf("な", "に", "ぬ", "ね", "の"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "HA", chars = listOf("は", "ひ", "ふ", "へ", "ほ"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "MA", chars = listOf("ま", "み", "む", "め", "も"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "YA", chars = listOf("や", "", "ゆ", "", "よ"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "RA", chars = listOf("ら", "り", "る", "れ", "ろ"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "WA", chars = listOf("わ", "", "を", "", "ん"), hiraganaByChar = hiraganaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onStartFlashcard(selectedIds.toList()) },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tạo Flashcard")
                            }
                            Button(
                                onClick = { onStartQuiz(selectedIds.toList()) },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Làm trắc nghiệm")
                            }
                        }
                    }
                }
            }
        }

        if (strokeSheetChar != null) {
            val ch = strokeSheetChar!!
            ModalBottomSheet(
                onDismissRequest = { strokeSheetChar = null },
                sheetState = strokeSheetState
            ) {
                KanaStrokeLessonSheetContent(
                    character = ch,
                    onPlayAudio = { speak(ch) },
                    onClose = { strokeSheetChar = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KatakanaTableScreen(
    words: List<LessonWordUiModel>,
    onSpeak: (String) -> Unit,
    onStartQuiz: (List<Int>) -> Unit,
    onStartFlashcard: (List<Int>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Int>() }
    val speak = onSpeak
    var strokeSheetChar by remember { mutableStateOf<String?>(null) }
    val strokeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectableWords = words.filter { it.type == "katakana" && it.furigana.isNotBlank() }
    val katakanaByChar = words.associateBy { it.furigana }
    val openStroke: (String) -> Unit = { ch ->
        if (ch.isNotBlank()) strokeSheetChar = ch
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Chạm ô: chọn / nghe — giữ ô (long press): sơ đồ nét KanjiVG + luyện viết.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (words.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Chưa có dữ liệu Katakana cho bài 2.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Đã chọn: ${selectedIds.size}/${selectableWords.size} chữ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    item { HiraganaRow(label = "A", chars = listOf("ア", "イ", "ウ", "エ", "オ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "KA", chars = listOf("カ", "キ", "ク", "ケ", "コ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "SA", chars = listOf("サ", "シ", "ス", "セ", "ソ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "TA", chars = listOf("タ", "チ", "ツ", "テ", "ト"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "NA", chars = listOf("ナ", "ニ", "ヌ", "ネ", "ノ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "HA", chars = listOf("ハ", "ヒ", "フ", "ヘ", "ホ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "MA", chars = listOf("マ", "ミ", "ム", "メ", "モ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "YA", chars = listOf("ヤ", "", "ユ", "", "ヨ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "RA", chars = listOf("ラ", "リ", "ル", "レ", "ロ"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item { HiraganaRow(label = "WA", chars = listOf("ワ", "", "ヲ", "", "ン"), hiraganaByChar = katakanaByChar, selectedIds = selectedIds, onSpeak = speak, onOpenStrokeDiagram = openStroke) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onStartFlashcard(selectedIds.toList()) },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tạo Flashcard")
                            }
                            Button(
                                onClick = { onStartQuiz(selectedIds.toList()) },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Làm trắc nghiệm")
                            }
                        }
                    }
                }
            }
        }

        if (strokeSheetChar != null) {
            val ch = strokeSheetChar!!
            ModalBottomSheet(
                onDismissRequest = { strokeSheetChar = null },
                sheetState = strokeSheetState
            ) {
                KanaStrokeLessonSheetContent(
                    character = ch,
                    onPlayAudio = { speak(ch) },
                    onClose = { strokeSheetChar = null }
                )
            }
        }
    }
}

@Composable
private fun KanaStrokeLessonSheetContent(
    character: String,
    onPlayAudio: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = character,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onPlayAudio) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Nghe phát âm",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = "Sơ đồ nét KanjiVG (CC BY-SA 3.0): nền mờ có số; nét màu chạy theo thứ tự.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        StrokeOrderAnimatedSection(
            character = character,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            height = 200.dp
        )
        Text(
            text = "Luyện viết",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            DrawPracticeCanvas(
                guideCharacter = character,
                modifier = Modifier.fillMaxSize()
            )
        }
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text("Đóng")
        }
    }
}

@Composable
private fun HiraganaRow(
    label: String,
    chars: List<String>,
    hiraganaByChar: Map<String, LessonWordUiModel>,
    selectedIds: MutableList<Int>,
    onSpeak: (String) -> Unit,
    onOpenStrokeDiagram: (String) -> Unit
) {
    val labelWidth = 56.dp
    val gap = 10.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - labelWidth - (gap * 5)) / 5

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            Surface(
                modifier = Modifier.width(labelWidth),
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            chars.forEach { ch ->
                val model = hiraganaByChar[ch]
                HiraganaCell(
                    wordId = model?.id,
                    furigana = ch,
                    romaji = model?.meaning.orEmpty(),
                    isSelected = model?.id != null && selectedIds.contains(model.id),
                    onTap = {
                        if (ch.isNotBlank()) onSpeak(ch)
                        model?.id?.let { wordId ->
                            if (selectedIds.contains(wordId)) {
                                selectedIds.remove(wordId)
                            } else {
                                selectedIds.add(wordId)
                            }
                        }
                    },
                    onStrokeLongPress = if (ch.isNotBlank()) {
                        { onOpenStrokeDiagram(ch) }
                    } else {
                        null
                    },
                    modifier = Modifier.width(cellWidth)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiraganaCell(
    wordId: Int?,
    furigana: String,
    romaji: String,
    isSelected: Boolean,
    onTap: () -> Unit,
    onStrokeLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(86.dp)
            .combinedClickable(
                enabled = wordId != null,
                onClick = { onTap() },
                onLongClick = onStrokeLongPress
            ),
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (furigana.isNotBlank()) furigana else "",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (furigana.isNotBlank()) {
                    if (romaji.isBlank()) "—" else romaji
                } else {
                    ""
                },
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LessonWordRow(
    word: LessonWordUiModel,
    onSpeak: (String) -> Unit = {},
    onToggleFavorite: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                JapaneseText(
                    mainText = word.kanji.ifBlank { word.furigana },
                    furiganaText = if (word.kanji.isNotBlank()) word.furigana else "",
                    mainFontSize = 24.sp,
                    furiganaFontSize = 12.sp
                )
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Loại: ${word.type}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            JapaneseAudioButton(
                text = word.furigana.ifBlank { word.kanji },
                onSpeak = onSpeak
            )
            IconButton(
                onClick = { onToggleFavorite(!word.isFavorite) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (word.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


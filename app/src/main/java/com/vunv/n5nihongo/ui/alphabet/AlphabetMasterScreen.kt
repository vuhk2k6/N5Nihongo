package com.vunv.n5nihongo.ui.alphabet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.runtime.collectAsState
import com.vunv.n5nihongo.ui.components.StrokeOrderAnimatedSection
import com.vunv.n5nihongo.ui.components.LessonScreenLoading
import com.vunv.n5nihongo.ui.components.rememberDeferredJapaneseSpeaker
import com.vunv.n5nihongo.ui.components.StrokeRulesDialog
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.data.model.Word

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphabetMasterRoute(
    typeId: Int, // 1 = Hiragana, 2 = Katakana
    onStartQuiz: (Int, List<Int>) -> Unit = { _, _ -> },
    onStartFlashcard: (Int, List<Int>) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val viewModel: AlphabetViewModel = viewModel(factory = AlphabetViewModel.factory(typeId))
    val uiState by viewModel.uiState.collectAsState()
    
    val isHiragana = typeId == 1
    val title = if (isHiragana) "Bảng chữ cái Hiragana" else "Bảng chữ cái Katakana"
    
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<Word?>(null) }
    var showStrokeRules by remember { mutableStateOf(false) }

    val speak = rememberDeferredJapaneseSpeaker()

    if (showStrokeRules) {
        StrokeRulesDialog(onDismissRequest = { showStrokeRules = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showStrokeRules = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Quy tắc nét",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { 
                        isSelectionMode = !isSelectionMode 
                        if (!isSelectionMode) viewModel.clearSelection()
                    }) {
                        Text(if (isSelectionMode) "Hủy" else "Chọn chữ")
                    }
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center,
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    if (uiState.selectedIds.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                val selectedList = uiState.selectedIds.toList()
                                isSelectionMode = false
                                viewModel.clearSelection()
                                onStartFlashcard(typeId, selectedList)
                            },
                            icon = { Icon(Icons.Default.Layers, contentDescription = null) },
                            text = { Text("Thẻ học (${uiState.selectedIds.size})") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                val selectedList = uiState.selectedIds.toList()
                                isSelectionMode = false
                                viewModel.clearSelection()
                                onStartQuiz(typeId, selectedList)
                            },
                            icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                            text = { Text("Kiểm tra (${uiState.selectedIds.size})") },
                            containerColor = MintPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { onStartFlashcard(typeId, uiState.words.map { it.id }) },
                        icon = { Icon(Icons.Default.Layers, contentDescription = null) },
                        text = { Text("Thẻ học chữ cái") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { onStartQuiz(typeId, uiState.words.map { it.id }) },
                        icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                        text = { Text("Luyện trắc nghiệm") },
                        containerColor = MintPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSelectionMode) {
                Text(
                    "Chọn các chữ bạn muốn kiểm tra trí nhớ:",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.isLoading) {
                LessonScreenLoading(modifier = Modifier.fillMaxSize())
            } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.words) { word ->
                    val isSelected = uiState.selectedIds.contains(word.id)
                    KanaCard(
                        word = word,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(word.id)
                            } else {
                                selectedItemForDetail = word
                            }
                        }
                    )
                }
            }
            }
        }

        if (selectedItemForDetail != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedItemForDetail = null }
            ) {
                KanaDetailContent(
                    word = selectedItemForDetail!!,
                    onPlayAudio = { speak(selectedItemForDetail!!.furigana) }
                )
            }
        }
    }
}

@Composable
fun KanaCard(
    word: Word, 
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MintPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).align(Alignment.TopEnd).padding(4.dp),
                    tint = MintPrimary
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = word.furigana,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun KanaDetailContent(
    word: Word,
    onPlayAudio: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = word.furigana,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onPlayAudio) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Audio", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            text = "Romaji: ${word.meaning}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Thứ tự và chiều viết nét",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        StrokeOrderAnimatedSection(
            character = word.furigana,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            height = 200.dp
        )
        Text(
            text = "Nét sáng chạy theo đúng thứ tự; nền mờ có số đánh dấu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Luyện viết (Draw over)",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            DrawPracticeCanvas(
                guideCharacter = word.furigana,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = "Sơ đồ thứ tự nét hiển thị trong ô — vẽ đè lên để luyện.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

package com.vunv.n5nihongo.ui.kanji

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.data.model.Kanji
import com.vunv.n5nihongo.data.model.Word
import com.vunv.n5nihongo.ui.alphabet.DrawPracticeCanvas
import com.vunv.n5nihongo.ui.components.JapaneseAudioButton
import com.vunv.n5nihongo.ui.components.StrokeOrderAnimatedSection
import com.vunv.n5nihongo.ui.components.LessonScreenLoading
import com.vunv.n5nihongo.ui.components.rememberDeferredJapaneseSpeaker
import com.vunv.n5nihongo.ui.theme.MintLight
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SurfaceWhite
import com.vunv.n5nihongo.ui.theme.TextPrimary
import com.vunv.n5nihongo.ui.theme.TextSecondary

@Composable
fun KanjiDetailRoute(
    kanjiId: Int,
    onBack: () -> Unit
) {
    val viewModel: KanjiDetailViewModel =
        viewModel(factory = KanjiDetailViewModel.factory(kanjiId))
    val uiState by viewModel.uiState.collectAsState()
    KanjiDetailScreen(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KanjiDetailScreen(
    uiState: KanjiDetailUiState,
    onBack: () -> Unit
) {
    val speak = rememberDeferredJapaneseSpeaker()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.kanji?.let { "${it.character} — ${it.meaning}" } ?: "Chi tiết Kanji",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LessonScreenLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            uiState.kanji == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không tìm thấy chữ Hán này", color = TextSecondary)
                }
            }
            else -> {
                val kanji = uiState.kanji!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MintLight.copy(alpha = 0.18f)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { StrokeOrderAnimationCard(character = kanji.character) }
                    item { StrokePracticeCard(character = kanji.character) }
                    item { KanjiInfoCard(kanji = kanji) }
                    item { ExamplesHeader(count = uiState.examples.size) }
                    items(uiState.examples, key = { it.id }) { word ->
                        ExampleWordCard(word = word, onSpeak = speak)
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StrokeOrderAnimationCard(character: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Thứ tự và chiều viết nét",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            StrokeOrderAnimatedSection(
                character = character,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                height = 240.dp
            )
            Text(
                text = "Nền mờ có số thứ tự; nét sáng màu chạy lần lượt theo đúng hướng viết (lặp lại liên tục).",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StrokePracticeCard(character: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tập viết",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MintLight.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = MintPrimary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                DrawPracticeCanvas(
                    guideCharacter = character,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "Vẽ chồng lên sơ đồ thứ tự nét trong ô (số và màu nét).",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun KanjiInfoCard(kanji: Kanji) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MintLight.copy(alpha = 0.35f))
                    .border(
                        width = 1.dp,
                        color = MintPrimary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = kanji.character,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = kanji.meaning,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                InfoLine(label = "Trình độ", value = "N5", valueColor = MintPrimary)
                InfoLine(label = "Số nét", value = kanji.strokeCount.toString())
                InfoLine(label = "Âm Kun", value = kanji.kunyomi.ifBlank { "-" })
                InfoLine(label = "Âm On", value = kanji.onyomi.ifBlank { "-" })
            }
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExamplesHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Từ vựng có chứa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "$count từ",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ExampleWordCard(word: Word, onSpeak: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = word.kanji.ifBlank { word.furigana },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (word.kanji.isNotBlank() && word.furigana.isNotBlank() && word.furigana != word.kanji) {
                    Text(
                        text = word.furigana,
                        style = MaterialTheme.typography.bodySmall,
                        color = MintPrimary
                    )
                }
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            JapaneseAudioButton(
                text = word.furigana.ifBlank { word.kanji },
                onSpeak = onSpeak
            )
        }
    }
}

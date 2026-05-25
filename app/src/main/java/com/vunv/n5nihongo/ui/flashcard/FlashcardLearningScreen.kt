package com.vunv.n5nihongo.ui.flashcard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.ui.components.JapaneseAudioButton
import com.vunv.n5nihongo.ui.components.JapaneseText
import com.vunv.n5nihongo.ui.components.rememberDeferredJapaneseSpeaker
import kotlinx.coroutines.launch

@Composable
fun FlashcardLearningRoute(
    lessonId: Int? = null,
    selectedWordIds: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
    viewModel: FlashcardViewModel = viewModel(
        factory = FlashcardViewModel.factory(
            lessonId = lessonId,
            selectedWordIds = selectedWordIds
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FlashcardLearningScreen(
        uiState = uiState,
        onKnownClick = viewModel::handleKnownWord,
        onUnknownClick = viewModel::handleUnknownWord,
        lessonId = lessonId,
        modifier = modifier
    )
}

@Composable
private fun FlippableCardContent(
    isFlipped: Boolean,
    modifier: Modifier = Modifier,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "flashcard_flip"
    )
    val density = LocalDensity.current

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density.density
        }
    ) {
        if (rotation <= 90f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                front()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentAlignment = Alignment.Center
            ) {
                back()
            }
        }
    }
}

@Composable
private fun FlashcardLearningScreen(
    uiState: FlashcardUiState,
    onKnownClick: () -> Unit,
    onUnknownClick: () -> Unit,
    lessonId: Int? = null,
    modifier: Modifier = Modifier
) {
    val swipeOffsetX = remember(uiState.currentWord?.id) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val swipeThreshold = 220f
    val speak = rememberDeferredJapaneseSpeaker()

    val isAlphabet = lessonId == 1 || lessonId == 2
    val titleText = if (isAlphabet) "Thẻ học chữ cái" else "Thẻ học từ vựng"
    val descriptionText = if (isAlphabet) "Mặt trước: chữ cái — chạm thẻ để lật xem phiên âm." else "Mặt trước: chữ Nhật — chạm thẻ để lật xem nghĩa."

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Đã xem: ${uiState.reviewedCount} | Còn lại: ${uiState.remainingCount}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.isCompleted || uiState.currentWord == null) {
            Text(
                text = "Bạn đã hoàn thành bộ flashcard.",
                style = MaterialTheme.typography.titleMedium
            )
            return
        }

        val word = uiState.currentWord
        var isFlipped by remember(word.id) { mutableStateOf(false) }
        LaunchedEffect(word.id) { isFlipped = false }

        val isSwipingRight = swipeOffsetX.value > 0f
        val swipeRotation = (swipeOffsetX.value / 30f).coerceIn(-14f, 14f)
        val cardColor by animateFloatAsState(
            targetValue = (kotlin.math.abs(swipeOffsetX.value) / swipeThreshold).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
            label = "cardTint"
        )

        val frontJapanese = word.kanji.ifBlank { word.furigana }
        val frontFurigana = if (word.kanji.isNotBlank()) word.furigana else ""

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .graphicsLayer { translationX = swipeOffsetX.value }
                .rotate(swipeRotation)
                .pointerInput(word.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                swipeOffsetX.snapTo(swipeOffsetX.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            when {
                                swipeOffsetX.value >= swipeThreshold -> {
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(420f, spring(stiffness = Spring.StiffnessLow))
                                        onKnownClick()
                                        swipeOffsetX.snapTo(0f)
                                    }
                                }
                                swipeOffsetX.value <= -swipeThreshold -> {
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(-420f, spring(stiffness = Spring.StiffnessLow))
                                        onUnknownClick()
                                        swipeOffsetX.snapTo(0f)
                                    }
                                }
                                else -> {
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(0f, spring(dampingRatio = 0.7f))
                                    }
                                }
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isSwipingRight) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f + (0.25f * cardColor))
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f + (0.2f * cardColor))
                }
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .clickable { isFlipped = !isFlipped }
            ) {
                FlippableCardContent(
                    isFlipped = isFlipped,
                    modifier = Modifier.fillMaxSize(),
                    front = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            JapaneseText(
                                mainText = frontJapanese,
                                furiganaText = frontFurigana,
                                mainFontSize = 40.sp,
                                furiganaFontSize = 16.sp,
                                centered = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            JapaneseAudioButton(
                                text = word.furigana.ifBlank { word.kanji },
                                onSpeak = speak,
                                size = 44.dp
                            )
                            Text(
                                text = if (isAlphabet) "Chạm để xem phiên âm" else "Chạm để xem nghĩa",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    back = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = word.meaning,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (word.romaji.isNotBlank()) {
                                Text(
                                    text = word.romaji,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (!isAlphabet) {
                                Text(
                                    text = "Loại: ${word.type}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "Chạm để quay lại",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                )
                SwipeHintBadge(
                    text = if (isSwipingRight) "ĐÃ THUỘC" else "CẦN HỌC LẠI",
                    modifier = Modifier.align(Alignment.TopCenter),
                    alpha = cardColor
                )
            }
        }

        Text("Vuốt trái: Cần học lại  |  Vuốt phải: Đã thuộc", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onUnknownClick, modifier = Modifier.fillMaxWidth()) {
            Text("Đánh dấu Cần học lại")
        }
    }
}

@Composable
private fun SwipeHintBadge(
    text: String,
    modifier: Modifier = Modifier,
    alpha: Float
) {
    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Text(
            text = text,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

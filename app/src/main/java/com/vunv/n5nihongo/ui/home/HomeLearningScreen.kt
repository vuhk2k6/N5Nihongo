package com.vunv.n5nihongo.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.ui.theme.DisabledGray
import com.vunv.n5nihongo.ui.theme.SkySecondary

@Composable
fun HomeLearningRoute(
    modifier: Modifier = Modifier,
    viewModel: LearningPathViewModel = viewModel(factory = LearningPathViewModel.factory),
    onLessonClick: (Int) -> Unit = {}
) {
    val lessons by viewModel.lessonsUiState.collectAsStateWithLifecycle()
    HomeLearningScreen(
        lessons = lessons,
        modifier = modifier,
        onLessonClick = onLessonClick
    )
}

@Composable
private fun HomeLearningScreen(
    lessons: List<LessonUiModel>,
    modifier: Modifier = Modifier,
    onLessonClick: (Int) -> Unit
) {
    if (lessons.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có bài học",
                style = MaterialTheme.typography.titleMedium
            )
        }
        return
    }

    val pulseTransition = rememberInfiniteTransition(label = "roadmapPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // We calculate a predefined path.
    // Index modulo 4 to create a zigzag pattern.
    // 0 -> center, 1 -> right, 2 -> center, 3 -> left
    fun getOffsetForIndex(index: Int): Float {
        return when (index % 4) {
            0 -> 0f
            1 -> 60f
            2 -> 0f
            3 -> -60f
            else -> 0f
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Hành trình chinh phục N5",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        itemsIndexed(lessons, key = { _, lesson -> lesson.lessonId }) { index, lesson ->
            val isCurrentLesson = lesson.isEnabled && !lesson.isCompleted
            val xOffsetDp = getOffsetForIndex(index).dp
            val nextOffsetDp = getOffsetForIndex(index + 1).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Draw line to next node
                        if (index < lessons.size - 1) {
                            val strokeWidth = 12.dp.toPx()
                            val startX = center.x + xOffsetDp.toPx()
                            val startY = center.y
                            val endX = center.x + nextOffsetDp.toPx()
                            val endY = center.y + 140.dp.toPx() // rough estimate of item height
                            
                            val isNextEnabled = lessons[index + 1].isEnabled
                            val lineColor = if (lesson.isCompleted && isNextEnabled) SkySecondary else DisabledGray.copy(alpha = 0.5f)
                            
                            drawLine(
                                color = lineColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                RoadmapNode(
                    lesson = lesson,
                    isCurrentLesson = isCurrentLesson,
                    pulseScale = if (isCurrentLesson) pulseScale else 1f,
                    modifier = Modifier.offset(x = xOffsetDp),
                    onClick = { onLessonClick(lesson.lessonId) }
                )
            }
        }
    }
}

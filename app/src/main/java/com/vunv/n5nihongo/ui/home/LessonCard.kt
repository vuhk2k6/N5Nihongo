package com.vunv.n5nihongo.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vunv.n5nihongo.ui.theme.AccentYellow
import com.vunv.n5nihongo.ui.theme.DisabledGray
import com.vunv.n5nihongo.ui.theme.LightBackground
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SkySecondary

@Composable
fun LessonCard(
    lesson: LessonUiModel,
    isCurrentLesson: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "lessonCardScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                shadowElevation = 14.dp.toPx()
                shape = RoundedCornerShape(24.dp)
                clip = true
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, SkySecondary.copy(alpha = 0.22f))
                )
            )
            .clickable(
                enabled = lesson.isEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .alpha(if (lesson.isEnabled) 1f else 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(lesson.lessonTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (lesson.isCompleted) "Đã hoàn thành" else "Đang học",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                LessonBadge(lesson = lesson, isCurrentLesson = isCurrentLesson)
            }

            LinearProgressIndicator(
                progress = { lesson.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MintPrimary,
                trackColor = LightBackground
            )
        }
    }
}

@Composable
private fun LessonBadge(lesson: LessonUiModel, isCurrentLesson: Boolean) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = if (isCurrentLesson) MintPrimary.copy(alpha = 0.35f) else Color.Transparent,
                radius = size.minDimension / 2f
            )
            drawCircle(
                color = if (lesson.isEnabled) SkySecondary else DisabledGray,
                radius = size.minDimension / 2.8f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
        when {
            !lesson.isEnabled -> Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
            lesson.isCompleted -> Icon(Icons.Default.Star, contentDescription = null, tint = AccentYellow)
            else -> Text(text = "${(lesson.progress * 100).toInt()}%", color = Color.White)
        }
    }
}

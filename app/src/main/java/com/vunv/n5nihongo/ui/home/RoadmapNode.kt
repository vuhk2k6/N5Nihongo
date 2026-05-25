package com.vunv.n5nihongo.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vunv.n5nihongo.ui.theme.AccentYellow
import com.vunv.n5nihongo.ui.theme.DisabledGray
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SkySecondary

@Composable
fun RoadmapNode(
    lesson: LessonUiModel,
    isCurrentLesson: Boolean,
    /** Scale nhấp nháy cho bài đang học — do parent hoisting một [rememberInfiniteTransition] duy nhất. */
    pulseScale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else pulseScale,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 320f),
        label = "nodeScale"
    )

    val nodeColor = when {
        lesson.isCompleted -> SkySecondary
        isCurrentLesson -> MintPrimary
        else -> DisabledGray
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clickable(
                    enabled = lesson.isEnabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrentLesson) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.3f),
                        radius = size.minDimension / 1.8f
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = nodeColor,
                shadowElevation = if (lesson.isEnabled) 8.dp else 2.dp,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        !lesson.isEnabled -> Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White)
                        lesson.isCompleted -> Icon(Icons.Default.Star, contentDescription = "Completed", tint = AccentYellow, modifier = Modifier.size(32.dp))
                        else -> Text(
                            text = lesson.lessonId.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        Text(
            text = lesson.lessonTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (lesson.isEnabled) MaterialTheme.colorScheme.onBackground else DisabledGray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

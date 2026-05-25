package com.vunv.n5nihongo.ui.kanji

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

data class KanjiPracticeResult(
    val bitmap: Bitmap,
    val strokeCount: Int,
    val score: Int
)

@Composable
fun KanjiWritingPractice(
    modifier: Modifier = Modifier,
    kanjiGuide: String,
    expectedStrokeCount: Int,
    onSaved: (KanjiPracticeResult) -> Unit
) {
    val strokePaths = remember { mutableStateListOf<Path>() }
    var activePath by remember { mutableStateOf<Path?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var strokeCount by remember { mutableIntStateOf(0) }
    val strokeWidthPx = with(LocalDensity.current) { 7.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tập viết Kanji",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .onSizeChanged { canvasSize = it }
            ) {
                Text(
                    text = kanjiGuide,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = Path().apply {
                                        moveTo(offset.x, offset.y)
                                    }
                                    activePath = newPath
                                    strokePaths.add(newPath)
                                    strokeCount += 1
                                },
                                onDrag = { change, _ ->
                                    val currentPath = activePath ?: return@detectDragGestures
                                    currentPath.lineTo(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    activePath = null
                                },
                                onDragCancel = {
                                    activePath = null
                                }
                            )
                        }
                ) {
                    strokePaths.forEach { path ->
                        drawPath(
                            path = path,
                            color = Color.Black,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
                        )
                    }
                }
            }
        }

        Text(
            text = "Số nét: $strokeCount / mục tiêu: $expectedStrokeCount",
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    strokePaths.clear()
                    activePath = null
                    strokeCount = 0
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Xóa")
            }

            Button(
                onClick = {
                    if (canvasSize == IntSize.Zero) {
                        return@Button
                    }
                    val bitmap = renderPathsToBitmap(
                        size = canvasSize,
                        paths = strokePaths,
                        strokeWidthPx = strokeWidthPx
                    )
                    val score = evaluateByStrokeCount(strokeCount, expectedStrokeCount)
                    onSaved(
                        KanjiPracticeResult(
                            bitmap = bitmap,
                            strokeCount = strokeCount,
                            score = score
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Lưu")
            }
        }
    }
}

private fun evaluateByStrokeCount(actual: Int, expected: Int): Int {
    if (expected <= 0) {
        return 0
    }
    val diffRatio = kotlin.math.abs(actual - expected).toFloat() / expected.toFloat()
    val rawScore = (1f - diffRatio).coerceIn(0f, 1f)
    return (rawScore * 100).toInt()
}

private fun renderPathsToBitmap(
    size: IntSize,
    paths: List<Path>,
    strokeWidthPx: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = android.graphics.Color.BLACK
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    paths.forEach { path ->
        canvas.drawPath(path.asAndroidPath(), paint)
    }

    return bitmap
}

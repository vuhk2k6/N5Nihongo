package com.vunv.n5nihongo.ui.components

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.PathParser as ComposeVectorPathParser
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.PathParser as CorePathParser
import com.vunv.n5nihongo.ui.alphabet.loadKanjiVgPictureDrawable
import kotlin.math.max
import kotlin.math.min
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val strokeDataCache = ConcurrentHashMap<String, StrokeOrderData>()

private fun svgXmlWithoutDoctype(xml: String): String =
    xml.replaceFirst(Regex("<!DOCTYPE\\s+[\\s\\S]*?\\]\\s*>\\s*", RegexOption.IGNORE_CASE), "")

private fun sanitizeKanjiVgSvg(xml: String): String {
    var s = svgXmlWithoutDoctype(xml)
    s = s.replace(Regex("paint-order:\\s*[^;]+;?", RegexOption.IGNORE_CASE), "")
    return s
}

private data class StrokeOrderData(
    val viewBoxW: Float,
    val viewBoxH: Float,
    val strokes: List<AndroidPath>,
    val background: PictureDrawable
)

private fun androidPathFromSvgPathData(d: String): AndroidPath? {
    runCatching { CorePathParser.createPathFromPathData(d) }.getOrNull()?.let { return it }
    return runCatching {
        val parser = ComposeVectorPathParser()
        parser.parsePathString(d)
        parser.toPath().asAndroidPath()
    }.getOrNull()
}

private suspend fun loadStrokeOrderData(context: android.content.Context, character: String): StrokeOrderData? =
    withContext(Dispatchers.IO) {
        val trimmed = character.trim()
        if (trimmed.isEmpty()) return@withContext null
        val key = "%05x".format(Character.codePointAt(trimmed, 0))
        strokeDataCache[key]?.let { return@withContext it }

        val background = loadKanjiVgPictureDrawable(context, trimmed) ?: return@withContext null

        val meta = runCatching {
            val hex = key
            val raw = context.assets.open("kanjivg/$hex.svg")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val cleaned = sanitizeKanjiVgSvg(raw)

            val vb = Regex("""viewBox="\s*([\d.]+)\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)\s*"""")
                .find(cleaned)
            val vw = vb?.groupValues?.getOrNull(3)?.toFloatOrNull() ?: 109f
            val vh = vb?.groupValues?.getOrNull(4)?.toFloatOrNull() ?: 109f

            val pathTag = Regex("""<path\s+([^>]+)/>""")
            val strokeEntries = mutableListOf<Pair<Int, String>>()
            for (m in pathTag.findAll(cleaned)) {
                val attrs = m.groupValues[1]
                val id = Regex("""id="kvg:[^"]+-s(\d+)"""").find(attrs)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: continue
                val d = Regex("""d="([^"]+)"""").find(attrs)?.groupValues?.getOrNull(1) ?: continue
                strokeEntries.add(id to d)
            }
            strokeEntries.sortBy { it.first }
            val paths = strokeEntries.mapNotNull { (_, d) -> androidPathFromSvgPathData(d) }
            Triple(vw, vh, paths)
        }.getOrNull()

        val result = if (meta != null) {
            StrokeOrderData(viewBoxW = meta.first, viewBoxH = meta.second, strokes = meta.third, background = background)
        } else {
            StrokeOrderData(viewBoxW = 109f, viewBoxH = 109f, strokes = emptyList(), background = background)
        }
        strokeDataCache[key] = result
        result
    }

private fun strokeHue(index: Int, total: Int): Float =
    if (total <= 1) 0f else (index / max(total - 1, 1).toFloat()) * 0.78f * 360f

/**
 * Stroke-order diagram from bundled KanjiVG SVGs: dim full diagram (stroke numbers) plus animated
 * bright strokes when path data parses; otherwise the same SVG is shown clearly without animation.
 */
@Composable
fun StrokeOrderAnimatedSection(
    character: String,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    strokeWidthDp: Dp = 4.5.dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val charKey = remember(character) { character.trim() }
    val cacheKey = remember(charKey) {
        if (charKey.isEmpty()) null else "%05x".format(charKey.codePointAt(0))
    }
    var loadState by remember(charKey) {
        val cached = cacheKey?.let { strokeDataCache[it] }
        mutableStateOf(if (cached != null) LoadUi.Ready(cached) else LoadUi.Loading)
    }
    var strokeIdx by remember(charKey) { mutableIntStateOf(0) }
    val trim = remember(charKey) { Animatable(0f) }
    val segmentPath = remember(charKey) { AndroidPath() }
    val measure = remember(charKey) { PathMeasure() }
    val strokePaint = remember(charKey) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }
    val hsvScratch = remember(charKey) { floatArrayOf(0f, 0.82f, 0.92f) }

    LaunchedEffect(charKey) {
        if (charKey.isBlank()) {
            loadState = LoadUi.Missing
            return@LaunchedEffect
        }
        val key = "%05x".format(charKey.codePointAt(0))
        val data = strokeDataCache[key] ?: run {
            loadState = LoadUi.Loading
            loadStrokeOrderData(context, charKey)
        }
        if (data == null) {
            loadState = LoadUi.Missing
            return@LaunchedEffect
        }
        loadState = LoadUi.Ready(data)
        strokeIdx = 0
        trim.snapTo(0f)
        if (data.strokes.isEmpty()) {
            return@LaunchedEffect
        }
        while (isActive) {
            for (i in data.strokes.indices) {
                strokeIdx = i
                trim.snapTo(0f)
                trim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                )
                delay(90)
            }
            delay(850)
        }
    }

    val strokeWidthPx = with(density) { strokeWidthDp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (val state = loadState) {
            LoadUi.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Đang tải sơ đồ nét…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LoadUi.Missing -> {
                Text(
                    text = "Không có sơ đồ nét cho ký tự này.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is LoadUi.Ready -> {
                val data = state.data
                val underlayAlpha = if (data.strokes.isEmpty()) 0.92f else 0.28f
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setBackgroundColor(android.graphics.Color.WHITE)
                        }
                    },
                    update = { iv ->
                        iv.setImageDrawable(data.background)
                        iv.alpha = underlayAlpha
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (data.strokes.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val scale = min(w / data.viewBoxW, h / data.viewBoxH)
                        val tx = (w - data.viewBoxW * scale) / 2f
                        val ty = (h - data.viewBoxH * scale) / 2f

                        drawIntoCanvas { canvas ->
                            val nc = canvas.nativeCanvas
                            nc.save()
                            nc.translate(tx, ty)
                            nc.scale(scale, scale)

                            val total = data.strokes.size
                            val strokeW = (strokeWidthPx / scale).coerceAtLeast(2.5f)
                            strokePaint.strokeWidth = strokeW
                            for (i in 0 until strokeIdx) {
                                hsvScratch[0] = strokeHue(i, total).coerceIn(0f, 360f)
                                strokePaint.color = android.graphics.Color.HSVToColor(hsvScratch)
                                nc.drawPath(data.strokes[i], strokePaint)
                            }
                            if (strokeIdx < total) {
                                val path = data.strokes[strokeIdx]
                                measure.setPath(path, false)
                                val len = measure.length
                                if (len > 0f) {
                                    segmentPath.rewind()
                                    measure.getSegment(0f, len * trim.value, segmentPath, true)
                                    hsvScratch[0] = strokeHue(strokeIdx, total).coerceIn(0f, 360f)
                                    strokePaint.color = android.graphics.Color.HSVToColor(hsvScratch)
                                    nc.drawPath(segmentPath, strokePaint)
                                }
                            }
                            nc.restore()
                        }
                    }
                }
            }
        }
    }
}

private sealed class LoadUi {
    data object Loading : LoadUi()
    data object Missing : LoadUi()
    data class Ready(val data: StrokeOrderData) : LoadUi()
}

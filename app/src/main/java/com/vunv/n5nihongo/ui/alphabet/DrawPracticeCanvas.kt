package com.vunv.n5nihongo.ui.alphabet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.PictureDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.Charsets

/**
 * A native Android View that handles drawing with zero Compose overhead.
 * All touch events and rendering happen entirely on the native side.
 */
class DrawingView(context: Context) : View(context) {

    private var bitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null

    private val drawPaint = Paint().apply {
        color = AndroidColor.BLACK
        strokeWidth = 14f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private var lastX = 0f
    private var lastY = 0f

    init {
        setBackgroundColor(AndroidColor.TRANSPARENT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(bitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastX = x
                lastY = y
            }

            MotionEvent.ACTION_MOVE -> {
                drawCanvas?.drawLine(lastX, lastY, x, y, drawPaint)
                lastX = x
                lastY = y
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                // nothing
            }
        }
        return true
    }

    fun clearCanvas() {
        drawCanvas?.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }
}

private fun svgXmlWithoutDoctype(xml: String): String =
    xml.replaceFirst(Regex("<!DOCTYPE\\s+[\\s\\S]*?\\]\\s*>\\s*", RegexOption.IGNORE_CASE), "")

private fun sanitizeKanjiVgSvg(xml: String): String {
    var s = svgXmlWithoutDoctype(xml)
    s = s.replace(Regex("paint-order:\\s*[^;]+;?", RegexOption.IGNORE_CASE), "")
    return s
}

internal suspend fun loadKanjiVgPictureDrawable(context: Context, character: String) =
    withContext(Dispatchers.IO) {
        runCatching {
            val t = character.trim()
            if (t.isEmpty()) return@runCatching null
            val cp = Character.codePointAt(t, 0)
            val hex = "%05x".format(cp)
            val raw =
                context.assets.open("kanjivg/$hex.svg").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val cleaned = sanitizeKanjiVgSvg(raw)
            val svg = SVG.getFromString(cleaned)
            PictureDrawable(svg.renderToPicture())
        }.getOrNull()
    }

/**
 * Chỉ hiển thị sơ đồ nét KanjiVG (tĩnh, có số thứ tự) — dùng trong lưới Kanji / ô nhỏ, không animation.
 * Nguồn: `assets/kanjivg/<codepoint>.svg` (KanjiVG, CC BY-SA 3.0).
 */
@Composable
fun KanjiVgStrokeDiagramPreview(
    character: String,
    modifier: Modifier = Modifier,
    imageAlpha: Float = 1f
) {
    val context = LocalContext.current
    var drawable by remember(character) { mutableStateOf<PictureDrawable?>(null) }
    var loadDone by remember(character) { mutableStateOf(false) }

    LaunchedEffect(character) {
        drawable = null
        loadDone = false
        if (character.isBlank()) {
            loadDone = true
            return@LaunchedEffect
        }
        drawable = loadKanjiVgPictureDrawable(context, character)
        loadDone = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            drawable != null -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setBackgroundColor(AndroidColor.WHITE)
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        }
                    },
                    update = {
                        it.setImageDrawable(drawable)
                        it.alpha = imageAlpha
                    }
                )
            }
            !loadDone -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp
                )
            }
            else -> {
                Text(
                    text = character,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

/**
 * @param guideCharacter Ký tự cần luyện; nếu có file `assets/kanjivg/<codepoint>.svg` thì hiển thị
 * sơ đồ thứ tự nét KanjiVG ngay trong ô, phía dưới nét vẽ tay.
 */
@Composable
fun DrawPracticeCanvas(
    guideCharacter: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawingView = remember { DrawingView(context) }

    var guideDrawable by remember(guideCharacter) { mutableStateOf<PictureDrawable?>(null) }
    var guideFinished by remember(guideCharacter) { mutableStateOf(false) }

    LaunchedEffect(guideCharacter) {
        guideDrawable = null
        guideFinished = false
        if (guideCharacter.isNullOrBlank()) {
            guideFinished = true
            return@LaunchedEffect
        }
        guideDrawable = loadKanjiVgPictureDrawable(context, guideCharacter)
        guideFinished = true
    }

    val showGuide = !guideCharacter.isNullOrBlank()

    Box(modifier = modifier) {
        if (showGuide) {
            val ch = guideCharacter!!
            val drawable = guideDrawable
            if (drawable != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setBackgroundColor(AndroidColor.WHITE)
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        }
                    },
                    update = { it.setImageDrawable(drawable) }
                )
            } else if (guideFinished) {
                Text(
                    text = ch,
                    fontSize = 160.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = ch,
                    fontSize = 160.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        AndroidView(
            factory = { drawingView },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = { drawingView.clearCanvas() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Xóa nét vẽ",
                tint = androidx.compose.ui.graphics.Color.Gray
            )
        }
    }
}

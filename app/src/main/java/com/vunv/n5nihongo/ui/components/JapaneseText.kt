package com.vunv.n5nihongo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
@Composable
fun JapaneseText(
    mainText: String,
    furiganaText: String,
    mainFontSize: TextUnit = 24.sp,
    furiganaFontSize: TextUnit = 12.sp,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    centered: Boolean = false
) {
    val showRuby = furiganaText.isNotBlank() &&
        mainText.isNotBlank() &&
        furiganaText != mainText

    if (!showRuby) {
        Text(
            text = mainText.ifBlank { furiganaText },
            fontSize = mainFontSize,
            color = textColor,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = if (centered) modifier.fillMaxWidth() else modifier
        )
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val mainTextStyle = TextStyle(
        fontSize = mainFontSize,
        color = textColor,
        fontWeight = FontWeight.Normal
    )
    val furiganaTextStyle = TextStyle(
        fontSize = furiganaFontSize,
        color = textColor.copy(alpha = 0.8f),
        fontWeight = FontWeight.Light
    )

    val mainTextLayoutResult = textMeasurer.measure(text = mainText, style = mainTextStyle)
    val furiganaTextLayoutResult = textMeasurer.measure(text = furiganaText, style = furiganaTextStyle)

    val canvasWidth = maxOf(mainTextLayoutResult.size.width, furiganaTextLayoutResult.size.width)
    val canvasHeight = mainTextLayoutResult.size.height + furiganaTextLayoutResult.size.height
    val density = LocalDensity.current

    val boxAlignment = if (centered) Alignment.Center else Alignment.TopStart
    val canvasModifier = if (centered) {
        Modifier
            .fillMaxWidth()
            .height(with(density) { canvasHeight.toDp() })
    } else {
        Modifier
            .width(with(density) { canvasWidth.toDp() })
            .height(with(density) { canvasHeight.toDp() })
    }

    Box(
        modifier = modifier.padding(vertical = 4.dp),
        contentAlignment = boxAlignment
    ) {
        Canvas(modifier = canvasModifier) {
            val mainY = furiganaTextLayoutResult.size.height.toFloat()
            val furiganaX = if (centered) (size.width - furiganaTextLayoutResult.size.width) / 2f else 0f
            val mainX = if (centered) (size.width - mainTextLayoutResult.size.width) / 2f else 0f

            drawText(
                textMeasurer = textMeasurer,
                text = furiganaText,
                style = furiganaTextStyle,
                topLeft = Offset(furiganaX, 0f)
            )
            drawText(
                textMeasurer = textMeasurer,
                text = mainText,
                style = mainTextStyle,
                topLeft = Offset(mainX, mainY)
            )
        }
    }
}

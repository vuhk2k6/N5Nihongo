package com.vunv.n5nihongo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color(0x33000000),
                ambientColor = Color(0x11000000)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.85f)) // Translucent white
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f), // Thin white border for glass edge reflection
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

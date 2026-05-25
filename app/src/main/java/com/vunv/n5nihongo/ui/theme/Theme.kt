package com.vunv.n5nihongo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = MintPrimary,
    secondary = SkySecondary,
    background = LightBackground,
    surface = SurfaceWhite,
    surfaceVariant = GlassWhite,
    onPrimary = SurfaceWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val DarkColors = darkColorScheme(
    primary = SkySecondary,
    secondary = MintPrimary
)

@Composable
fun N5NihongoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = n5Typography,
        content = content
    )
}

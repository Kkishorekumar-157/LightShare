package com.lightshare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = darkColorScheme(
    primary = Gold, onPrimary = Color.Black, secondary = Gold,
    background = Ink, onBackground = Color.White,
    surface = Charcoal, onSurface = Color.White,
    surfaceVariant = Charcoal2, onSurfaceVariant = Mute,
    outline = Color(0xFF3A3A40), error = Color(0xFFFF6B6B)
)

@Composable
fun LightShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = LightShareTypography, content = content)
}

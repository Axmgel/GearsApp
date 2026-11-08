package com.axmgel.gearsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Steel = Color(0xFF37474F)
private val Brass = Color(0xFFB8860B)

private val LightColors = lightColorScheme(
    primary = Steel,
    secondary = Brass,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90A4AE),
    secondary = Color(0xFFE0B04A),
)

@Composable
fun GearsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

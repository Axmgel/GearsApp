package com.axmgel.gearsapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Paleta "Minimalist Dark": fondo casi negro, superficies grises y un solo acento ámbar. */
object Palette {
    val Background = Color(0xFF0E1014)
    val Surface = Color(0xFF15181E)
    val SurfaceHigh = Color(0xFF1C2028)
    val SurfaceHighest = Color(0xFF232832)
    val Outline = Color(0xFF2E343E)
    val Text = Color(0xFFE6E8EB)
    val TextMuted = Color(0xFF9AA1AC)
    val Accent = Color(0xFFE0B04A)
    val AccentDim = Color(0xFF2A2312)
    val Teal = Color(0xFF4FB6A8)
    val TealDim = Color(0xFF14302C)
    val Error = Color(0xFFF2706B)
}

private val MinimalistDark = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Color(0xFF1A1405),
    primaryContainer = Palette.AccentDim,
    onPrimaryContainer = Palette.Accent,
    secondary = Palette.TextMuted,
    onSecondary = Palette.Background,
    secondaryContainer = Palette.SurfaceHigh,
    onSecondaryContainer = Palette.Text,
    tertiary = Palette.Teal,
    tertiaryContainer = Palette.TealDim,
    onTertiaryContainer = Palette.Text,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Background,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextMuted,
    surfaceContainerLowest = Palette.Background,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Outline,
    outlineVariant = Palette.Outline,
    error = Palette.Error,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun GearsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MinimalistDark, shapes = AppShapes, content = content)
}

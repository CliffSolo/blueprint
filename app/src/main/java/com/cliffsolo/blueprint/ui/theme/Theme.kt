package com.cliffsolo.blueprint.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Blueprint paper: pale blue in light mode, deep navy in dark mode, with a fine drafting grid. */
private val Light = lightColorScheme(
    primary = Color(0xFF1F5FAE), onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E5F8), onPrimaryContainer = Color(0xFF0B2E57),
    secondary = Color(0xFFE8833A), onSecondary = Color.White,
    background = Color(0xFFF4F8FD), onBackground = Color(0xFF12355B),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF12355B),
    surfaceVariant = Color(0xFFE6EEF8), onSurfaceVariant = Color(0xFF5B7290),
    outline = Color(0xFFB9CCE3), outlineVariant = Color(0xFFDCE7F3),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8DB8F2), onPrimary = Color(0xFF0D1828),
    primaryContainer = Color(0xFF1C3A63), onPrimaryContainer = Color(0xFFD6E5F8),
    secondary = Color(0xFFF29A55), onSecondary = Color(0xFF2A1405),
    background = Color(0xFF0D1828), onBackground = Color(0xFFDCE8F7),
    surface = Color(0xFF142439), onSurface = Color(0xFFDCE8F7),
    surfaceVariant = Color(0xFF1A2D47), onSurfaceVariant = Color(0xFF8FA6C2),
    outline = Color(0xFF2C4364), outlineVariant = Color(0xFF1B2D47),
)

val Mono = FontFamily.Monospace

private val BlueprintType = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 0.5.sp),
    )
}

@Composable
fun BlueprintTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = BlueprintType, content = content)
}

/** Draws the drafting-paper grid behind a screen. */
fun Modifier.blueprintGrid(line: Color, spacing: Int = 20): Modifier = drawBehind {
    val step = spacing.dp.toPx()
    var x = 0f
    while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    var y = 0f
    while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1f); y += step }
}

fun parseHex(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

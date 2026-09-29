package com.secount.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secount.app.logic.isSystemDark

val Brand = Color(0xFFFF5D97)
val BrandDark = Color(0xFFC2185B)
val Gold = Color(0xFFFFC973)
val Success = Color(0xFF10B981)

// Modern warm neutrals keep the Valentine palette polished and approachable.
private val Ink = Color(0xFF20191E)
private val InkMuted = Color(0xFF71646C)
private val DarkInk = Color(0xFFF9F1F5)
private val DarkMuted = Color(0xFFD1C3CA)
private val LightBg = Color(0xFFFFF8FA)
private val LightSurface = Color(0xFFFFFCFD)
private val DarkBg = Color(0xFF10080D)
private val DarkSurface = Color(0xFF21131B)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(lineHeight = bodyLarge.lineHeight * 1.08f),
        bodyMedium = bodyMedium.copy(color = InkMuted),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

data class NamedTheme(
    val name: String,
    val light: ColorScheme,
    val dark: ColorScheme
)

private fun lightOf(primary: Color, bg: Color, surface: Color, secondary: Color = Gold) =
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Ink,
        secondaryContainer = secondary.copy(alpha = 0.20f),
        onSecondaryContainer = Ink,
        surface = surface,
        surfaceContainer = surface,
        surfaceContainerLow = bg,
        surfaceContainerHigh = Color(0xFFFFF4F7),
        onSurface = Ink,
        surfaceVariant = surface,
        onSurfaceVariant = InkMuted,
        outline = Color(0xFFE5D8DE),
        outlineVariant = Color(0xFFF0E7EB),
        background = bg,
        onBackground = Ink
    )

private fun darkOf(primary: Color, bg: Color, surface: Color, secondary: Color = Gold) =
    darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.22f),
        onPrimaryContainer = DarkInk,
        secondary = secondary,
        onSecondary = Ink,
        secondaryContainer = secondary.copy(alpha = 0.20f),
        onSecondaryContainer = DarkInk,
        surface = surface,
        surfaceContainer = surface,
        surfaceContainerLow = bg,
        surfaceContainerHigh = Color(0xFF302029),
        onSurface = DarkInk,
        onSurfaceVariant = DarkMuted,
        surfaceVariant = surface,
        outline = Color(0xFF604A55),
        outlineVariant = Color(0xFF4B3942),
        background = bg,
        onBackground = DarkInk
    )

val THEMES = listOf(
    NamedTheme(
        "Valentine",
        lightOf(Brand, LightBg, LightSurface),
        darkOf(Color(0xFFFF6FA5), DarkBg, DarkSurface)
    ),
    NamedTheme(
        "Midnight Android",
        lightOf(Color(0xFF0E9F6E), Color(0xFFF2F7F3), Color(0xFFFBFEFC), Color(0xFF3DDC84)),
        darkOf(Color(0xFF3DDC84), Color(0xFF0B1220), Color(0xFF14202E), Color(0xFF3DDC84))
    ),
    NamedTheme(
        "Ocean",
        lightOf(Color(0xFF0284C7), Color(0xFFF0F9FF), Color(0xFFFBFDFF)),
        darkOf(Color(0xFF38BDF8), Color(0xFF082F49), Color(0xFF0C4A6E))
    ),
    NamedTheme(
        "Sunset",
        lightOf(Color(0xFFEA580C), Color(0xFFFFF7ED), Color(0xFFFFFCF8)),
        darkOf(Color(0xFFFB923C), Color(0xFF431407), Color(0xFF7C2D12))
    ),
    NamedTheme(
        "Forest",
        lightOf(Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFFFBFFFC)),
        darkOf(Color(0xFF4ADE80), Color(0xFF052E16), Color(0xFF14532D))
    ),
    NamedTheme(
        "Lavender",
        lightOf(Color(0xFF6D64FF), Color(0xFFF5F3FF), Color(0xFFFCFBFF)),
        darkOf(Color(0xFFA5B4FC), Color(0xFF1E1B4B), Color(0xFF312E81))
    ),
    NamedTheme(
        "Porcelain",
        lightOf(Color(0xFF475569), Color(0xFFF8FAFC), Color(0xFFFFFFFF)),
        darkOf(Color(0xFFE2E8F0), Color(0xFF0F172A), Color(0xFF1E293B))
    )
)

fun themeByName(name: String): NamedTheme =
    THEMES.firstOrNull { it.name == name } ?: THEMES[0]

@Composable
fun SecountTheme(
    themeName: String = THEMES[0].name,
    darkMode: String = "System",
    content: @Composable () -> Unit
) {
    val t = themeByName(themeName)
    val dark = when (darkMode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemDark()
    }
    MaterialTheme(
        colorScheme = if (dark) t.dark else t.light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

package com.secount.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secount.app.logic.isSystemDark

val Brand = Color(0xFFFF5D97)
val BrandDark = Color(0xFFC2185B)
val Gold = Color(0xFFFFC973)
val Success = Color(0xFF10B981)
val SuccessDark = Color(0xFF34D399)
val Warning = Color(0xFFF59E0B)
val Info = Color(0xFF38BDF8)

// Warm, high-legibility neutrals. Body text is near-black on tinted white
// so the app stays readable for everyone, including low-vision users.
private val Ink = Color(0xFF22161D)
private val InkMuted = Color(0xFF6B5B64)
private val DarkInk = Color(0xFFFFF4F8)
private val DarkMuted = Color(0xFFE3CBD6)
private val LightBg = Color(0xFFFFF6F9)
private val LightSurface = Color(0xFFFFFFFF)
private val LightContainer = Color(0xFFFFEDF3)
private val DarkBg = Color(0xFF12080E)
private val DarkSurface = Color(0xFF221219)
private val DarkContainer = Color(0xFF37202B)

// Ultra-modern expressive shapes: big, friendly, consistent radii.
private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

// Approachable type: slightly larger, generous line height, semibold titles.
private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = bodyLarge.lineHeight * 1.25f),
        bodyMedium = bodyMedium.copy(lineHeight = bodyMedium.lineHeight * 1.3f, color = InkMuted),
        bodySmall = bodySmall.copy(lineHeight = bodySmall.lineHeight * 1.3f),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

data class NamedTheme(
    val name: String,
    val tagline: String,
    val light: ColorScheme,
    val dark: ColorScheme
)

private fun lightOf(primary: Color, bg: Color, surface: Color, secondary: Color = Gold) =
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.14f),
        onPrimaryContainer = Color(0xFF7A1039),
        secondary = secondary,
        onSecondary = Ink,
        secondaryContainer = secondary.copy(alpha = 0.28f),
        onSecondaryContainer = Ink,
        tertiary = Success,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD9F8E8),
        onTertiaryContainer = Color(0xFF065F46),
        surface = surface,
        surfaceContainer = surface,
        surfaceContainerLow = bg,
        surfaceContainerLowest = Color.White,
        surfaceContainerHigh = Color(0xFFFFF0F5),
        surfaceContainerHighest = Color(0xFFFFE4EE),
        surfaceDim = bg,
        surfaceBright = Color.White,
        onSurface = Ink,
        surfaceVariant = LightContainer,
        onSurfaceVariant = InkMuted,
        outline = Color(0xFFE3CBD6),
        outlineVariant = Color(0xFFF4E2EA),
        background = bg,
        onBackground = Ink,
        error = Color(0xFFDC2626),
        onError = Color.White,
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D)
    )

private fun darkOf(primary: Color, bg: Color, surface: Color, secondary: Color = Gold) =
    darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF2B0716),
        primaryContainer = primary.copy(alpha = 0.24f),
        onPrimaryContainer = DarkInk,
        secondary = secondary,
        onSecondary = Color(0xFF2B1A05),
        secondaryContainer = secondary.copy(alpha = 0.22f),
        onSecondaryContainer = DarkInk,
        tertiary = SuccessDark,
        onTertiary = Color(0xFF052E1B),
        tertiaryContainer = Color(0xFF064E3B),
        onTertiaryContainer = Color(0xFFD1FAE5),
        surface = surface,
        surfaceContainer = surface,
        surfaceContainerLow = bg,
        surfaceContainerLowest = Color(0xFF0A0508),
        surfaceContainerHigh = Color(0xFF322028),
        surfaceContainerHighest = Color(0xFF453038),
        surfaceDim = bg,
        surfaceBright = Color(0xFF3A2830),
        onSurface = DarkInk,
        onSurfaceVariant = DarkMuted,
        surfaceVariant = DarkContainer,
        outline = Color(0xFF6E4E5C),
        outlineVariant = Color(0xFF4E3540),
        background = bg,
        onBackground = DarkInk,
        error = Color(0xFFFCA5A5),
        onError = Color(0xFF450A0A),
        errorContainer = Color(0xFF7F1D1D),
        onErrorContainer = Color(0xFFFEE2E2)
    )

val THEMES = listOf(
    NamedTheme(
        "Valentine",
        "Warm & friendly",
        lightOf(Brand, LightBg, LightSurface),
        darkOf(Color(0xFFFF77AB), DarkBg, DarkSurface)
    ),
    NamedTheme(
        "Midnight Android",
        "Fresh & calm",
        lightOf(Color(0xFF0E9F6E), Color(0xFFF1F7F3), Color(0xFFFBFEFC), Color(0xFF3DDC84)),
        darkOf(Color(0xFF3DDC84), Color(0xFF0B1220), Color(0xFF14202E), Color(0xFF3DDC84))
    ),
    NamedTheme(
        "Ocean",
        "Clear & focused",
        lightOf(Color(0xFF0284C7), Color(0xFFF0F9FF), Color(0xFFFBFDFF)),
        darkOf(Color(0xFF38BDF8), Color(0xFF082F49), Color(0xFF0C4A6E))
    ),
    NamedTheme(
        "Sunset",
        "Cozy & bold",
        lightOf(Color(0xFFEA580C), Color(0xFFFFF7ED), Color(0xFFFFFCF8)),
        darkOf(Color(0xFFFB923C), Color(0xFF431407), Color(0xFF7C2D12))
    ),
    NamedTheme(
        "Forest",
        "Natural & steady",
        lightOf(Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFFFBFFFC)),
        darkOf(Color(0xFF4ADE80), Color(0xFF052E16), Color(0xFF14532D))
    ),
    NamedTheme(
        "Lavender",
        "Soft & dreamy",
        lightOf(Color(0xFF6D64FF), Color(0xFFF5F3FF), Color(0xFFFCFBFF)),
        darkOf(Color(0xFFA5B4FC), Color(0xFF1E1B4B), Color(0xFF312E81))
    ),
    NamedTheme(
        "Porcelain",
        "Minimal & sharp",
        lightOf(Color(0xFF475569), Color(0xFFF8FAFC), Color(0xFFFFFFFF)),
        darkOf(Color(0xFFE2E8F0), Color(0xFF0F172A), Color(0xFF1E293B))
    )
)

fun themeByName(name: String): NamedTheme =
    THEMES.firstOrNull { it.name == name } ?: THEMES[0]

/** Signature gradient for hero headers — always derived from the active theme. */
@Composable
fun heroGradient(): Brush {
    val c = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(c.primary, c.primary.copy(alpha = 0.82f), c.tertiary.copy(alpha = 0.9f)))
}

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

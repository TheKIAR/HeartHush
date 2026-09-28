package com.secount.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.secount.app.logic.isSystemDark

val Brand = Color(0xFFFF5D97)
val BrandDark = Color(0xFFC2185B)
val Gold = Color(0xFFFFC973)
val Success = Color(0xFF10B981)

data class NamedTheme(
    val name: String,
    val light: ColorScheme,
    val dark: ColorScheme
)

private fun lightOf(primary: Color, bg: Color, surface: Color, secondary: Color = Gold) =
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = secondary,
        secondaryContainer = surface,
        surface = surface,
        onSurface = Color.Black,
        surfaceVariant = surface,
        onSurfaceVariant = Color.Black,
        background = bg,
        onBackground = Color.Black
    )

private fun darkOf(primary: Color, bg: Color, surface: Color, secondary: Color = Gold) =
    darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = secondary,
        secondaryContainer = surface,
        surface = surface,
        onSurface = Color.White,
        surfaceVariant = surface,
        onSurfaceVariant = Color.White,
        background = bg,
        onBackground = Color.White
    )

val THEMES = listOf(
    NamedTheme(
        "Valentine",
        lightOf(Color(0xFFFF5D97), Color(0xFFFFF7F9), Color(0xFFFFE9F0)),
        darkOf(Color(0xFFFF5D97), Color(0xFF14040D), Color(0xFF2A0A1B))
    ),
    NamedTheme(
        "Midnight Android",
        lightOf(Color(0xFF0E9F6E), Color(0xFFF2F7F3), Color(0xFFE2F3E9), Color(0xFF3DDC84)),
        darkOf(Color(0xFF3DDC84), Color(0xFF0B1220), Color(0xFF14202E), Color(0xFF3DDC84))
    ),
    NamedTheme(
        "Ocean",
        lightOf(Color(0xFF0284C7), Color(0xFFF0F9FF), Color(0xFFE0F2FE)),
        darkOf(Color(0xFF38BDF8), Color(0xFF082F49), Color(0xFF0C4A6E))
    ),
    NamedTheme(
        "Sunset",
        lightOf(Color(0xFFEA580C), Color(0xFFFFF7ED), Color(0xFFFFEDD5)),
        darkOf(Color(0xFFFB923C), Color(0xFF431407), Color(0xFF7C2D12))
    ),
    NamedTheme(
        "Forest",
        lightOf(Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFFDCFCE7)),
        darkOf(Color(0xFF4ADE80), Color(0xFF052E16), Color(0xFF14532D))
    ),
    NamedTheme(
        "Lavender",
        lightOf(Color(0xFF6D64FF), Color(0xFFF5F3FF), Color(0xFFEDE9FE)),
        darkOf(Color(0xFFA5B4FC), Color(0xFF1E1B4B), Color(0xFF312E81))
    ),
    NamedTheme(
        "Porcelain",
        lightOf(Color(0xFF475569), Color(0xFFFFFFFF), Color(0xFFF1F5F9)),
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
        // Read the OS directly: Compose's isSystemInDarkTheme() misses
        // Windows dark mode when running from the jar.
        else -> isSystemDark()
    }
    MaterialTheme(
        colorScheme = if (dark) t.dark else t.light,
        content = content
    )
}

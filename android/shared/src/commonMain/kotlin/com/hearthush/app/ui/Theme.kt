package com.hearthush.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFFFF5D97)
val BrandDark = Color(0xFFC2185B)
val Gold = Color(0xFFFFC973)
val Success = Color(0xFF10B981)

private val LightScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = Color(0xFFA06E14),
    secondaryContainer = Color(0xFFFFE4EB),
    surface = Color(0xFFFFF7F9),
    surfaceVariant = Color(0xFFFFE9F0),
    background = Color(0xFFFFF7F9)
)

private val DarkScheme = darkColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = Gold,
    secondaryContainer = Color(0xFF4A0A28),
    surface = Color(0xFF1F0613),
    surfaceVariant = Color(0xFF2A0A1B),
    background = Color(0xFF14040D)
)

@Composable
fun HeartHushTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}

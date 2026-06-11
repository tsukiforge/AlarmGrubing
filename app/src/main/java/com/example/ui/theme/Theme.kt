package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object AppThemeState {
    var themeMode by mutableStateOf("system") // "system", "light", "dark"
    var sakuraEnabled by mutableStateOf(true)
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF729F), // Gorgeous vibrant Sakura Pink
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD1DC), // Soft cute pastel pink
    onPrimaryContainer = Color(0xFF5C001C),
    secondary = Color(0xFF8E24AA), // Elegant Lavender violet
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5F5), // Light warm lilac
    onSecondaryContainer = Color(0xFF4A0072),
    background = Color(0xFFFFF7F9), // Fluffy cream-pink canvas
    onBackground = Color(0xFF3C121E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF3C121E),
    surfaceVariant = Color(0xFFFFF0F3), // Velvet light pink details
    onSurfaceVariant = Color(0xFF8C4C5E),
    outline = Color(0xFFFFB3C6),
    outlineVariant = Color(0xFFFFD1DC)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF729F), // Glowing magic pink
    onPrimary = Color(0xFF5C001C),
    primaryContainer = Color(0xFF8F123C), // Sunset berry deep wine
    onPrimaryContainer = Color(0xFFFFD1DC),
    secondary = Color(0xFFB39DDB), // Lunar light violet
    onSecondary = Color(0xFF311B92),
    secondaryContainer = Color(0xFF5E35B1), // Cyber purple
    onSecondaryContainer = Color(0xFFEDE7F6),
    background = Color(0xFF14101A), // Celestial deep violet background
    onBackground = Color(0xFFFFF0F5),
    surface = Color(0xFF221A2A), // Cozy lavender/violet card
    onSurface = Color(0xFFFFF0F5),
    surfaceVariant = Color(0xFF2E243A), // Midnight lavender highlights
    onSurfaceVariant = Color(0xFFFFC0D9),
    outline = Color(0xFF7C4DFF),
    outlineVariant = Color(0xFF21005D)
)

private val StarlightColorScheme = darkColorScheme(
    primary = Color(0xFFFFB7C5), // Soft pastel Sakura pink
    onPrimary = Color(0xFF3C0E1E), // Deep midnight violet-magenta
    primaryContainer = Color(0xFF6A1B4D), // Soft cosmic purple-magenta
    onPrimaryContainer = Color(0xFFFFD1DC),
    secondary = Color(0xFFD1C4E9), // Dreamy pastel purple star glow
    onSecondary = Color(0xFF311B92),
    secondaryContainer = Color(0xFF4A148C), // Deep midnight purple
    onSecondaryContainer = Color(0xFFEDE7F6),
    background = Color(0xFF0F0A1C), // Deep starlight cosmic navy/violet
    onBackground = Color(0xFFFFF0F5),
    surface = Color(0xFF1D142C), // Translucent soft purple-indigo obsidian card
    onSurface = Color(0xFFFFF0F5),
    surfaceVariant = Color(0xFF2C1E42), // Rich twilight orchid highlights
    onSurfaceVariant = Color(0xFFFFC0D9),
    outline = Color(0xFFD1C4E9),
    outlineVariant = Color(0xFF4A148C)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (AppThemeState.themeMode) {
        "starlight" -> StarlightColorScheme
        "dark" -> DarkColorScheme
        "light" -> LightColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

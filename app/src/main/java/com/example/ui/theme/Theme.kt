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
    var sakuraEnabled by mutableStateOf(false) // disabled by default
}

private val LightColorScheme = lightColorScheme(
    primary = CatBrown,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEF1), // Clean aqua container matching #BEE1E6
    onPrimaryContainer = CatDarkBrown,
    secondary = CatSoftOrange,
    onSecondary = Color.White,
    secondaryContainer = CatWarmGray,
    onSecondaryContainer = CatMidnight,
    background = CatCream,
    onBackground = CatMidnight,
    surface = CatSoftWhite,
    onSurface = CatMidnight,
    surfaceVariant = CatWarmGray,
    onSurfaceVariant = CatDarkBrown,
    outline = CatSoftOrange,
    outlineVariant = CatWarmGray
)

private val DarkColorScheme = darkColorScheme(
    primary = CatNightBrown,
    onPrimary = CatNightBg,
    primaryContainer = Color(0xFF1D373F), // Dark deep teal-slate container
    onPrimaryContainer = CatNightText,
    secondary = CatNightAccent,
    onSecondary = CatNightBg,
    secondaryContainer = CatNightSurface,
    onSecondaryContainer = CatNightText,
    background = CatNightBg,
    onBackground = CatNightText,
    surface = CatNightSurface,
    onSurface = CatNightText,
    surfaceVariant = Color(0xFF1B2F35), // Cohesive dark slate-teal variant
    onSurfaceVariant = CatNightAccent,
    outline = CatNightBrown,
    outlineVariant = CatNightSurface
)

// Deep Space Cat Theme
private val StarlightColorScheme = darkColorScheme(
    primary = Color(0xFFE8A87C), // Space peach
    onPrimary = Color(0xFF1E100D),
    primaryContainer = Color(0xFF301B28), // Nebula violet
    onPrimaryContainer = Color(0xFFFDE9DF),
    secondary = Color(0xFFC38D9E), // Cosmic rose pink
    onSecondary = Color(0xFF331620),
    secondaryContainer = Color(0xFF1D142C), // Deep cosmic obsidian card
    onSurface = Color(0xFFFFF8F0),
    background = Color(0xFF0F0A1C), // Deep space blue/violet
    onBackground = Color(0xFFFFF8F0),
    surface = Color(0xFF1C132B),
    surfaceVariant = Color(0xFF2B1D3D),
    onSurfaceVariant = Color(0xFFF3C68F),
    outline = Color(0xFFE8A87C),
    outlineVariant = Color(0xFF1D142C)
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

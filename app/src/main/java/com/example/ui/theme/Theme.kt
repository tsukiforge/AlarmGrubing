package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C109E),
    onPrimaryContainer = TextLight,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    tertiary = PinkAccent,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = TextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force a stunning, unified premium dark aesthetic for night/early-morning use
    val colorScheme = DarkColorScheme 

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

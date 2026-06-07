package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IndigoPrimary = Color(0xFFFF729F) // Cute Vibrant Sakura Pink
val IndigoLight = Color(0xFFFFC0D9)   // Sweet Baby Pink
val CyanAccent = Color(0xFFB39DDB)    // Magical Starlight Lavender
val PinkAccent = Color(0xFFFF529D)    // Accent Hot Sakura Pink

val BackgroundDark: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceDark: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceDarkElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val TextLight: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant


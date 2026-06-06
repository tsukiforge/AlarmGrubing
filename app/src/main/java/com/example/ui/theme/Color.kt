package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IndigoPrimary = Color(0xFF6750A4)
val IndigoLight = Color(0xFFB09FFF)
val CyanAccent = Color(0xFF21005D)
val PinkAccent = Color(0xFF6750A4)

val BackgroundDark: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceDark: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceDarkElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val TextLight: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

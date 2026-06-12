package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cat Theme — Light Mode
val CatCream        = Color(0xFFFFF8F0)   // background utama
val CatSoftWhite    = Color(0xFFFFFFFF)   // surface card
val CatWarmGray     = Color(0xFFF0EBE3)   // surface variant
val CatBrown        = Color(0xFF8B5E3C)   // primary (warna bulu kucing)
val CatSoftOrange   = Color(0xFFE8916A)   // secondary / accent
val CatPink         = Color(0xFFFFB3C1)   // tertiary (hidung kucing)
val CatDarkBrown    = Color(0xFF4A2E1A)   // on-primary text
val CatMidnight     = Color(0xFF1C1410)   // on-background text

// Cat Theme — Dark Mode
val CatNightBg      = Color(0xFF1A1410)   // background gelap
val CatNightSurface = Color(0xFF2A2118)   // surface card gelap
val CatNightBrown   = Color(0xFFD4956A)   // primary gelap
val CatNightAccent  = Color(0xFFFFB085)   // accent gelap
val CatNightText    = Color(0xFFFFF8F0)   // text gelap

// Backward compatibility colors mapped to new Cat Theme
val IndigoPrimary = CatNightBrown
val IndigoLight = CatNightAccent
val CyanAccent = CatPink
val PinkAccent = CatSoftOrange

val BackgroundDark: Color @Composable get() = MaterialTheme.colorScheme.background
val SurfaceDark: Color @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceDarkElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val TextLight: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

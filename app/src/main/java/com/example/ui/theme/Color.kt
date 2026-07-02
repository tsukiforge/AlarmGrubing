package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cat Theme — Light Mode
val CatCream        = Color(0xFFBEE1E6)   // background utama (EXACTLY #BEE1E6)
val CatSoftWhite    = Color(0xFFFFFFFF)   // surface card
val CatWarmGray     = Color(0xFFD3EBEE)   // surface variant
val CatBrown        = Color(0xFF205E6A)   // primary (modern deep slate-teal)
val CatSoftOrange   = Color(0xFF438F9E)   // secondary / accent
val CatPink         = Color(0xFF4EA1B0)   // tertiary (accent / floating button)
val CatDarkBrown    = Color(0xFF0F2C33)   // on-primary text
val CatMidnight     = Color(0xFF11292E)   // on-background text

// Cat Theme — Dark Mode
val CatNightBg      = Color(0xFF121F24)   // background gelap (deep navy slate)
val CatNightSurface = Color(0xFF1B2E35)   // surface card gelap
val CatNightBrown   = Color(0xFF87C9D6)   // primary gelap
val CatNightAccent  = Color(0xFFA6DBE6)   // accent gelap
val CatNightText    = Color(0xFFEFF7F8)   // text gelap

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

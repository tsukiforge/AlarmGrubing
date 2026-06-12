package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CatCream
import com.example.ui.theme.CatSoftWhite
import com.example.ui.theme.CatWarmGray
import com.example.ui.theme.CatBrown
import com.example.ui.theme.CatSoftOrange
import com.example.ui.theme.CatPink
import com.example.ui.theme.CatDarkBrown
import com.example.ui.theme.CatMidnight
import com.example.ui.theme.CatNightBg
import com.example.ui.theme.CatNightSurface
import com.example.ui.theme.CatNightBrown
import com.example.ui.theme.CatNightAccent
import com.example.ui.theme.CatNightText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var isMeowSyncing by remember { mutableStateOf(false) }
    var meowSyncCount by remember { mutableIntStateOf(0) }

    // Floating animation for cat ears/icon
    val infiniteTransition = rememberInfiniteTransition(label = "CatFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatOffset"
    )

    // Base theme parameters
    val isDark = MaterialTheme.colorScheme.background != CatCream
    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(CatNightBg, Color(0xFF0F191D)))
    } else {
        Brush.verticalGradient(listOf(CatCream, Color(0xFFEFF7F8)))
    }

    val contentColor = if (isDark) CatNightText else CatMidnight
    val cardColor = if (isDark) CatNightSurface else CatSoftWhite
    val accentColor = if (isDark) CatNightAccent else CatSoftOrange
    val primaryColor = if (isDark) CatNightBrown else CatBrown
    val titleTextColor = if (isDark) CatNightAccent else CatBrown

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tentang Aplikasi 🐱", fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.titleLarge.fontFamily) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Setelan"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(bgGradient)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Cat floating logo & title
            Box(
                modifier = Modifier
                    .offset(y = floatOffset.dp)
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cat_ears),
                    contentDescription = "Logo Kucing Emas",
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alarm Sync grup 🐱",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = titleTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    fontSize = 28.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Satu-satunya aplikasi alarm gila sinkronisasi grup berkema Kucing paling responsif, dinamis, bersemangat, dan anti-blokir seluruh jagat raya! Meowww!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Tech Specs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🐾 Spesifikasi Aplikasi",
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SpecLine(label = "Versi Aplikasi", value = "4.2.0-Feline🐾", contentColor = contentColor)
                    SpecLine(label = "Tanggal Rilis", value = "12 Juni 2026", contentColor = contentColor)
                    SpecLine(label = "Framework UI", value = "JetCompose M3 🚀", contentColor = contentColor)
                    SpecLine(label = "Lokal Database", value = "Room SQL v2.6 ✅", contentColor = contentColor)
                    SpecLine(label = "Sistem Sinkronisasi", value = "Firebase REST RTDB 🔥", contentColor = contentColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Team Credits Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🐈 Tim Pengembang",
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ContributorLine(name = "Meow Developer 🐾", role = "Pencipta Solusi & Logika", contentColor = contentColor)
                    ContributorLine(name = "Chibi Designer 🌸", role = "Pakar Visual Mewah", contentColor = contentColor)
                    ContributorLine(name = "Claws Team ⭐", role = "Pasukan Penguji Bug", contentColor = contentColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Interactive custom action: "Sinkronisasi Paws Manual"
            Button(
                onClick = {
                    if (!isMeowSyncing) {
                        scope.launch {
                            isMeowSyncing = true
                            delay(2000)
                            isMeowSyncing = false
                            meowSyncCount++
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isMeowSyncing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Menghubungkan Cakar ke Server...", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cat_paw),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (meowSyncCount == 0) "LAKUKAN SIMULASI MEOW 🐾" else "SIMULASI KEMBALI (${meowSyncCount}x)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            AnimatedVisibility(
                visible = meowSyncCount > 0 && !isMeowSyncing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = "Purr-fect! Koneksi sinkronisasi cakar berhasil terhubung secara real-time! 🐈✨",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
fun SpecLine(label: String, value: String, contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = contentColor.copy(alpha = 0.6f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
fun ContributorLine(name: String, role: String, contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(CatPink)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Text(text = role, fontSize = 11.sp, color = contentColor.copy(alpha = 0.6f))
        }
    }
}

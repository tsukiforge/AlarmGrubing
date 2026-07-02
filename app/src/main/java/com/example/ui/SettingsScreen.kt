package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.R
import com.example.ui.AodSettingsScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.data.helper.NetworkConnectionHelper
import java.io.File
fun SettingsScreen(
    viewModel: AlarmViewModel,
    profilePicTrigger: Boolean,
    onProfilePicChanged: () -> Unit,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE) }
    val userName by viewModel.userName.collectAsState()
    var textInput by remember { mutableStateOf(userName) }
    var serverUrlInput by remember { mutableStateOf(prefs.getString("sync_server_url", "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/") ?: "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/") }
    
    // Theme options
    var selectedThemeMode by remember { mutableStateOf(AppThemeState.themeMode) }

    // Feedback bottom sheet states
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var feedbackTypeSelected by remember { mutableStateOf(com.example.ui.FeedbackType.GENERAL) }
    
    // Notification permission state
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val createDocLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(context, uri) { success, msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val openDocLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(context, uri) { success, msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val file = File(context.filesDir, "profile_pic.png")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                prefs.edit().putBoolean("has_custom_profile_pic", true).apply()
                onProfilePicChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextLight
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pengaturan & Profil",
                        color = TextLight,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Konfigurasi identitas & performa aplikasi",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Beautiful Profile Picture Avatar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val profileFile = File(context.filesDir, "profile_pic.png")
                    val hasCustomPic = prefs.getBoolean("has_custom_profile_pic", false) && profileFile.exists()
                    val profileBitmap = remember(profilePicTrigger, hasCustomPic) {
                        if (hasCustomPic) {
                            try {
                                BitmapFactory.decodeFile(profileFile.absolutePath)
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.2f))
                            .border(2.dp, IndigoPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = "Foto Profil Anda",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = TextLight,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { profileImageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ganti Foto", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (hasCustomPic) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        if (profileFile.exists()) profileFile.delete()
                                        prefs.edit().putBoolean("has_custom_profile_pic", false).apply()
                                        onProfilePicChanged()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Hapus", color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        singleLine = true,
                        label = { Text("Nama Pengguna (Display Name)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = IndigoPrimary,
                            unfocusedLabelColor = TextMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Theme Mode Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Mode Tampilan (Theme)",
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("light" to "Putih Smooth ☀️", "dark" to "Gelap 🌙")
                        modes.forEach { (mode, label) ->
                            val isSelected = selectedThemeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) IndigoPrimary else SurfaceDark)
                                    .clickable {
                                        selectedThemeMode = mode
                                        AppThemeState.themeMode = mode
                                        prefs.edit().putString("theme_mode", mode).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            val aodPrefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
            var aodEnabled by remember { mutableStateOf(aodPrefs.getBoolean("aod_enabled", false)) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📱 Always-On Display (AOD)",
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Aktifkan Standby AOD", color = TextLight, fontSize = 13.sp)
                            Text(
                                text = "AOD otomatis aktif saat layar HP dimatikan (kunci ponsel)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = aodEnabled,
                            onCheckedChange = { isEnabled ->
                                aodEnabled = isEnabled
                                aodPrefs.edit().putBoolean("aod_enabled", isEnabled).apply()
                                val serviceIntent = Intent(context, com.example.alarm.AodService::class.java)
                                if (isEnabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                } else {
                                    context.stopService(serviceIntent)
                                }
                            }
                        )
                    }
                }
            }

            var forceFullVolume by remember { mutableStateOf(prefs.getBoolean("force_full_volume", false)) }
            var autoSpeakerHeadset by remember { mutableStateOf(prefs.getBoolean("auto_speaker_headset", false)) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚙️ Pengaturan Alarm",
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Paksa Volume Alarm Full", color = TextLight, fontSize = 13.sp)
                        Switch(
                            checked = forceFullVolume,
                            onCheckedChange = {
                                forceFullVolume = it
                                prefs.edit().putBoolean("force_full_volume", it).apply()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Auto Pindah Speaker saat Headset", color = TextLight, fontSize = 13.sp)
                            Text(
                                text = "Alarm akan berbunyi dari speaker meskipun headset terpasang",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoSpeakerHeadset,
                            onCheckedChange = {
                                autoSpeakerHeadset = it
                                prefs.edit().putBoolean("auto_speaker_headset", it).apply()
                            }
                        )
                    }
                    
                }
            }

            // Sync Server Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Server Sinkronisasi Grup",
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/" to "Firebase (Anti-Blokir) 🌸",
                            "https://kvdb.io/" to "KVDB (Biasa) 🌐"
                        ).forEach { (url, label) ->
                            val isSelected = serverUrlInput.trim().lowercase() == url.lowercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) IndigoPrimary else SurfaceDark)
                                    .clickable {
                                        serverUrlInput = url
                                        android.widget.Toast.makeText(context, "URL diubah ke: $url", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        singleLine = true,
                        label = { Text("URL Server Sinkronisasi (Kustom)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = IndigoPrimary,
                            unfocusedLabelColor = TextMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Notification Permission Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            if (!hasNotificationPermission) {
                                onRequestNotificationPermission()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Izin Berdering di Background 🔔", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (hasNotificationPermission) "Izin diberikan secara penuh" else "Izinkan notifikasi agar alarm berdering lancar",
                            color = if (hasNotificationPermission) IndigoLight else Color.Yellow,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasNotificationPermission) IndigoPrimary.copy(alpha = 0.2f) else Color.Yellow.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasNotificationPermission) "AKTIF" else "IZINKAN",
                            color = if (hasNotificationPermission) IndigoLight else Color.Yellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Backup and Restore Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💾 BACKUP & RESTORE",
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Amankan alarm, catatan, dan pengaturan ke perangkat as file JSON, atau pulihkan jika berganti HP. Aman dan tidak memerlukan cloud. 🌸",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { openDocLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                        ) {
                            Text("Restorasi", color = TextLight, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val df = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
                                createDocLauncher.launch("Sakura_Backup_${df.format(java.util.Date())}.json")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Backup Data", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Dukungan & Masukan Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🐾 DUKUNGAN & MASUKAN",
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Option 1: Kirim Masukan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                feedbackTypeSelected = com.example.ui.FeedbackType.GENERAL
                                showFeedbackSheet = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🐾 Kirim Masukan", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Kirim saran atau cakar tanggapan untuk pengembangan aplikasi", color = TextMuted, fontSize = 10.sp)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider(color = SurfaceDark.copy(alpha = 0.5f), thickness = 1.dp)

                    // Option 2: Beri Rating
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(webIntent)
                                    } catch (ex: Exception) {
                                        android.widget.Toast.makeText(context, "Play Store tidak ditemukan", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⭐ Beri Rating di PlayStore", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Beri bintang 5 untuk mendukung pengembang makin semangat", color = TextMuted, fontSize = 10.sp)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider(color = SurfaceDark.copy(alpha = 0.5f), thickness = 1.dp)

                    // Option 3: Laporkan Bug
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                feedbackTypeSelected = com.example.ui.FeedbackType.BUG
                                showFeedbackSheet = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🐛 Laporkan Bug", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Laporkan masalah cakar atau kegagalan sinkronisasi alarm", color = TextMuted, fontSize = 10.sp)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }

                    HorizontalDivider(color = SurfaceDark.copy(alpha = 0.5f), thickness = 1.dp)

                    // Option 4: Tentang Aplikasi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAbout() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ℹ️ Tentang Aplikasi", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Ketahui detail pengembang, versi, spesifikasi, dan rahasia meow", color = TextMuted, fontSize = 10.sp)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Info & Bypass ISP block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Info Cloud & Bypass ISP",
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kini layanan sinkronisasi grup menggunakan Firebase Realtime Database secara default yang dijamin 100% cepat dan anti-blokir oleh seluruh ISP di Indonesia!\n\n" +
                               "Jika Anda memilih server kustom eksternal seperti kvdb.io yang terblokir, Anda dapat mengaktifkan DNS Pribadi (Private DNS) Google atau Cloudflare di pengaturan HP Anda untuk solusi bypass permanen tanpa VPN dan hemat baterai.",
                        color = TextLight,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            
            // Save & Apply Button
            Button(
                onClick = {
                    val finalName = textInput.trim().ifEmpty { userName }
                    viewModel.updateUserName(finalName)
                    prefs.edit().putString("sync_server_url", serverUrlInput).apply()
                    com.example.data.api.NetworkClient.updateBaseUrl(serverUrlInput)
                    android.widget.Toast.makeText(context, "Profil & Pengaturan Tersimpan!", android.widget.Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("TERAPKAN & SIMPAN PROFIL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (showFeedbackSheet) {
            com.example.ui.FeedbackBottomSheet(
                initialType = feedbackTypeSelected,
                onDismiss = { showFeedbackSheet = false },
                onSubmit = {
                    showFeedbackSheet = false
                }
            )
        }
    }
}

@Composable

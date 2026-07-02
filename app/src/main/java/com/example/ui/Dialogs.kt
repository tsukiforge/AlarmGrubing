package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Alarm
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
fun EmptyStatePlaceholder(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), CircleShape)
                .padding(12.dp),
            tint = IndigoPrimary.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = TextLight,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun UserProfileAndSettingsDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    var textInput by remember { mutableStateOf(currentName) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE) }
    var serverUrlInput by remember { mutableStateOf(prefs.getString("sync_server_url", "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/") ?: "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/") }
    
    // Theme options
    var selectedThemeMode by remember { mutableStateOf(AppThemeState.themeMode) }
    
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

    // Update check states
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<GithubUpdateChecker.UpdateInfo?>(null) }
    var checkUpdateError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            SakuraOverlay {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                Text(
                    text = "⚙️ Pengaturan & Profil",
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Section
                Text(
                    text = "Profil Pengguna",
                    color = IndigoPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true,
                    label = { Text("Nama Anda") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = SurfaceDarkElevated,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = IndigoPrimary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Mode Section
                Text(
                    text = "Mode Tampilan (Theme)",
                    color = IndigoPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                                .background(if (isSelected) IndigoPrimary else SurfaceDarkElevated)
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

                Spacer(modifier = Modifier.height(16.dp))
                // Sync Server Section
                Text(
                    text = "Server Sinkronisasi Grup",
                    color = IndigoPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Fast protocol switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/" to "Firebase Client 🌸",
                        "https://kvdb.io/" to "KVDB (Biasa) 🌐"
                    ).forEach { (url, label) ->
                        val isSelected = serverUrlInput.trim().lowercase() == url.lowercase()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) IndigoPrimary else SurfaceDarkElevated)
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
                        unfocusedBorderColor = SurfaceDarkElevated,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = IndigoPrimary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Educational card to easily configure Private DNS (Bypass Internet Positif)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ Info Cloud & Bypass ISP",
                                color = Color(0xFFFFB300),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kini layanan sinkronisasi grup menggunakan Firebase Realtime Database secara default yang dijamin 100% cepat dan anti-blokir oleh seluruh ISP di Indonesia!\n\n" +
                                   "Jika Anda memilih server kustom eksternal seperti kvdb.io yang terblokir, Anda dapat mengaktifkan DNS Pribadi (Private DNS) Google atau Cloudflare di pengaturan HP Anda untuk solusi bypass permanen tanpa VPN dan hemat baterai.",
                            color = TextLight,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Salin Hostname DNS Berikut:",
                            color = TextLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        
                        listOf(
                            "dns.google" to "Google DNS (Rekomendasi Wifi/Seluler)",
                            "1dot1dot1dot1.cloudflare-dns.com" to "Cloudflare DNS (Paling Cepat)"
                        ).forEach { (dnsHost, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceDark)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = name, color = TextMuted, fontSize = 9.sp)
                                    Text(text = dnsHost, color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IndigoPrimary.copy(alpha = 0.2f))
                                        .clickable {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(dnsHost))
                                            android.widget.Toast.makeText(context, "Telah disalin: $dnsHost", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "Salin 📋", color = IndigoPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent("android.settings.PRIVATE_DNS_SETTINGS")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (ex: Exception) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            android.widget.Toast.makeText(context, "Silakan buka Pengaturan HP > Koneksi > DNS Pribadi", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Buka Pengaturan DNS HP ⚙️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Permission warning
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRequestNotificationPermission()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ Izin Notifikasi Belum Aktif\nKlik untuk mengaktifkan sekarang.",
                                color = Color(0xFFC62828),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Active",
                                tint = Color(0xFFC62828)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Update APK Section
                Text(
                    text = "Pembaruan Aplikasi",
                    color = IndigoPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = {
                        isCheckingUpdate = true
                        checkUpdateError = null
                        updateInfoState = null
                        scope.launch {
                            val info = GithubUpdateChecker.checkForUpdates()
                            isCheckingUpdate = false
                            if (info != null) {
                                if (info.errorMessage != null) {
                                    checkUpdateError = info.errorMessage
                                } else {
                                    updateInfoState = info
                                    if (!info.hasUpdate) {
                                        checkUpdateError = "Aplikasi Anda sudah versi terbaru (${com.example.BuildConfig.VERSION_NAME}). Tidak ada rilis pembaruan baru dari developer."
                                    }
                                }
                            } else {
                                checkUpdateError = "Gagal memproses hasil verifikasi pembaruan."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = IndigoPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa...", color = TextLight, fontSize = 12.sp)
                    } else {
                        Text("Periksa Update dari GitHub Release 🔄", color = TextLight, fontSize = 12.sp)
                    }
                }

                checkUpdateError?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = it, color = Color(0xFFC62828), fontSize = 11.sp)
                }

                updateInfoState?.let { info ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (info.hasUpdate) Color(0xFFFFE082) else Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (info.hasUpdate) {
                                Text(
                                    text = "Update Tersedia: ${info.latestVersion} 🔥",
                                    color = Color(0xFFE65100),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (info.releaseNotes.isNotEmpty()) {
                                    Text(
                                        text = info.releaseNotes.take(120) + if (info.releaseNotes.length > 120) "..." else "",
                                        color = Color(0xFFE65100),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Button(
                                    onClick = { GithubUpdateChecker.openUpdateUrl(context, info.downloadUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    Text("Unduh APK Baru 📥", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "Aplikasi Sudah Terupdate! (Versi: ${com.example.BuildConfig.VERSION_NAME}) ✅",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            prefs.edit().putString("sync_server_url", serverUrlInput).apply()
                            com.example.data.api.NetworkClient.updateBaseUrl(serverUrlInput)
                            onSaveName(textInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    isGroup: Boolean,
    alarmToEdit: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, hour: Int, minute: Int, daysOfWeek: String, ringtone: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(alarmToEdit?.title ?: "") }
    var hour by remember { mutableIntStateOf(alarmToEdit?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(alarmToEdit?.minute ?: 0) }
    var selectedTone by remember { mutableStateOf(alarmToEdit?.ringtoneUri ?: "default") }

    var previewingTone by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        com.example.audio.TonePreviewPlayer.setOnStateChangedListener {
            previewingTone = com.example.audio.TonePreviewPlayer.currentlyPlayingTone
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            com.example.audio.TonePreviewPlayer.setOnStateChangedListener(null)
            com.example.audio.TonePreviewPlayer.stop()
        }
    }

    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            if (alarmToEdit != null && alarmToEdit.daysOfWeek.isNotEmpty()) {
                val parsed = alarmToEdit.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                addAll(parsed)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            SakuraOverlay {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                item {
                    Text(
                        text = if (alarmToEdit != null) "Edit Alarm ✏️" else (if (isGroup) "Buat Alarm Kelompok 👥" else "Buat Alarm Pribadi ⏰"),
                        color = TextLight,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Pengingat") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = IndigoPrimary,
                            unfocusedLabelColor = TextMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Pilih Jam (Tap untuk memilih)", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var hourMenuExpanded by remember { mutableStateOf(false) }
                        var minuteMenuExpanded by remember { mutableStateOf(false) }

                        // Hour dropdown card
                        Box {
                            Card(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { hourMenuExpanded = true },
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d", hour),
                                        color = TextLight,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = hourMenuExpanded,
                                onDismissRequest = { hourMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 240.dp).background(SurfaceDarkElevated)
                            ) {
                                (0..23).forEach { h ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d", h),
                                                color = TextLight,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            hour = h
                                            hourMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = ":",
                            color = TextLight,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        // Minute dropdown card
                        Box {
                            Card(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { minuteMenuExpanded = true },
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d", minute),
                                        color = TextLight,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = minuteMenuExpanded,
                                onDismissRequest = { minuteMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 240.dp).background(SurfaceDarkElevated)
                            ) {
                                (0..59).forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = String.format(Locale.getDefault(), "%02d", m),
                                                color = TextLight,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            minute = m
                                            minuteMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Hari Pengulangan", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("M", "S", "S", "R", "K", "J", "S")
                        val dayIds = listOf(7, 1, 2, 3, 4, 5, 6)

                        for (i in days.indices) {
                            val dayId = dayIds[i]
                            val isSelected = selectedDays.contains(dayId)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) IndigoPrimary else SurfaceDarkElevated)
                                    .clickable {
                                        if (isSelected) selectedDays.remove(dayId) else selectedDays.add(
                                            dayId
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = days[i],
                                    color = if (isSelected) Color.White else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Suara Bell & Weker", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    var toneMenuExpanded by remember { mutableStateOf(false) }

                    val musicPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val uri = result.data?.data
                            if (uri != null) {
                                try {
                                    val timeMillis = System.currentTimeMillis()
                                    val destDir = File(context.filesDir, "custom_sounds")
                                    if (!destDir.exists()) destDir.mkdirs()
                                    val localFileName = "custom_music_${timeMillis}.mp3"
                                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                        File(destDir, localFileName).outputStream().use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }
                                    selectedTone = "local_file:$localFileName"
                                    android.widget.Toast.makeText(context, "Berhasil mengimpor musik HP! 📁", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Gagal mengimpor file musik", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    val customSoundsList = remember(selectedTone) {
                        val list = mutableListOf<Pair<String, String>>()
                        val destDir = File(context.filesDir, "custom_sounds")
                        if (destDir.exists()) {
                            destDir.listFiles()?.forEach { file ->
                                list.add("local_file:${file.name}" to "📁 Musik: ${file.name.take(24)}")
                            }
                        }
                        list
                    }

                    val tonesList = listOf(
                        "default" to "🎵 default bawaan hp",
                        "custom_1" to "🎐 Melody Chime (Aplikasi)",
                        "custom_2" to "📟 Retro Beep (Aplikasi)",
                        "custom_3" to "🌌 Echo Syzer (Aplikasi)",
                        "custom_sakura" to "🌸 Sakura Dream (Anime)",
                        "custom_anime" to "⚡ Shinobi Energetic (Hot)"
                    ) + customSoundsList

                    val currentToneLabel = tonesList.firstOrNull { it.first == selectedTone }?.second ?: "🎵 default bawaan hp"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDarkElevated)
                            .clickable { toneMenuExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentToneLabel,
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Pilih",
                                tint = TextMuted
                            )
                        }

                        DropdownMenu(
                            expanded = toneMenuExpanded,
                            onDismissRequest = { toneMenuExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .heightIn(max = 280.dp)
                                .background(SurfaceDarkElevated)
                        ) {
                            tonesList.forEach { p ->
                                val isThisTonePlaying = previewingTone == p.first
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = p.second,
                                                color = TextLight,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    com.example.audio.TonePreviewPlayer.play(context, p.first)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isThisTonePlaying) Icons.Filled.Close else Icons.Default.PlayArrow,
                                                    contentDescription = if (isThisTonePlaying) "Stop Preview" else "Play Preview",
                                                    tint = if (isThisTonePlaying) PinkAccent else TextMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedTone = p.first
                                        toneMenuExpanded = false
                                        com.example.audio.TonePreviewPlayer.stop()
                                    }
                                )
                            }
                            HorizontalDivider(color = SurfaceDark)
                            DropdownMenuItem(
                                text = {
                                    Text("📁 + Pilih Musik Baru dari HP...", color = PinkAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                },
                                onClick = {
                                    toneMenuExpanded = false
                                    android.widget.Toast.makeText(context, "Pilih Nada Custom via Dokumen", android.widget.Toast.LENGTH_SHORT).show()
                                    val intent = if (android.os.Build.VERSION.SDK_INT >= 19) {
                                         android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                                             addCategory(android.content.Intent.CATEGORY_OPENABLE)
                                             type = "audio/*"
                                         }
                                    } else {
                                         android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                             type = "audio/*"
                                         }
                                    }
                                    musicPickerLauncher.launch(intent)
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val DaysCsv = selectedDays.sorted().joinToString(",")
                                onSave(title, hour, minute, DaysCsv, selectedTone)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text(
                                if (alarmToEdit != null) "Simpan Perubahan" else "Simpan Alarm",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable

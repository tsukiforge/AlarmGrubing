package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Data model for schedules
data class HealthSchedule(
    val id: String,
    val name: String,
    val icon: String,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val isActive: Boolean,
    val days: List<String>
)

object HealthSocialDefaults {
    const val SOCIABUZZ_URL = "https://sociabuzz.com/zuax"
    const val SAWERIA_URL = "https://saweria.co/MahiroDev"
    val ALL_DAYS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
}

fun loadSchedules(context: Context): List<HealthSchedule> {
    val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
    val defaultDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum")
    val defaults = listOf(
        HealthSchedule("fokus", "Jadwal Fokus", "🎯", "08:00", "12:00", false, defaultDays),
        HealthSchedule("belajar", "Jadwal Belajar", "📚", "14:00", "16:00", false, defaultDays),
        HealthSchedule("tidur", "Jadwal Tidur", "🛌", "22:00", "05:00", false, listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")),
        HealthSchedule("kerja", "Jadwal Kerja", "💼", "09:00", "17:00", false, defaultDays),
        HealthSchedule("bermain", "Jadwal Bermain", "🎮", "18:00", "20:00", false, defaultDays)
    )
    
    return defaults.map { default ->
        val active = prefs.getBoolean("sched_${default.id}_active", default.isActive)
        val start = prefs.getString("sched_${default.id}_start", default.startTime) ?: default.startTime
        val end = prefs.getString("sched_${default.id}_end", default.endTime) ?: default.endTime
        val daysString = prefs.getString("sched_${default.id}_days", default.days.joinToString(",")) ?: default.days.joinToString(",")
        val daysList = if (daysString.isEmpty()) emptyList() else daysString.split(",")
        default.copy(isActive = active, startTime = start, endTime = end, days = daysList)
    }
}

fun saveSchedule(context: Context, schedule: HealthSchedule) {
    val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putBoolean("sched_${schedule.id}_active", schedule.isActive)
        .putString("sched_${schedule.id}_start", schedule.startTime)
        .putString("sched_${schedule.id}_end", schedule.endTime)
        .putString("sched_${schedule.id}_days", schedule.days.joinToString(","))
        .apply()
}

@Composable
fun HealthSocialScreen(context: Context, viewModel: Any? = null) {
    var schedules by remember { mutableStateOf(loadSchedules(context)) }
    
    val prefs = remember { context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE) }
    
    var bannerDismissed by remember {
        mutableStateOf(prefs.getBoolean("support_banner_dismissed", false))
    }
    
    var isPinEnabled by remember {
        mutableStateOf(prefs.getBoolean("pin_enabled", false))
    }
    
    var pinCode by remember {
        val encrypted = prefs.getString("pin_code", "") ?: ""
        mutableStateOf(if (encrypted.isNotEmpty()) SecurePinStorage.decryptPin(encrypted) else "")
    }
    
    var isAppLockEnabled by remember {
        mutableStateOf(prefs.getBoolean("app_lock_enabled", false))
    }
    
    var showLockConsentDialog by remember { mutableStateOf(false) }
    
    var showSupportSheet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showEditDialogFor by remember { mutableStateOf<HealthSchedule?>(null) }
    var pinDialogMode by remember { mutableStateOf("VERIFY") } // "SET", "VERIFY", "DISABLE"
    var onPinVerifiedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dukung Developer Banner (Buy Me a Coffee style)
            if (!bannerDismissed) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9A825).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .border(1.dp, Color(0xFFF9A825), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("☕", fontSize = 24.sp)
                                    Text(
                                        text = "Dukung Developer",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        prefs.edit().putBoolean("support_banner_dismissed", true).apply()
                                        bannerDismissed = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Banner",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Dukung terus pengembangan aplikasi ini agar selalu gratis dan bebas iklan dengan berdonasi kecil! ❤️",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { showSupportSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9A825)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Text(
                                    text = "Dukung via Sociabuzz / Saweria ☕",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // 2. PIN & App Lock Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security",
                                tint = Color(0xFFFF4081)
                            )
                            Text(
                                text = "Keamanan & Penguncian",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Switch PIN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gunakan PIN Keamanan",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Mencegah perubahan jadwal tanpa izin",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isPinEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        pinDialogMode = "SET"
                                        showPinDialog = true
                                    } else {
                                        pinDialogMode = "DISABLE"
                                        showPinDialog = true
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF4081),
                                    checkedTrackColor = Color(0xFFFF4081).copy(alpha = 0.5f)
                                )
                            )
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        
                        // Switch App Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kunci Aplikasi Saat Jadwal Berjalan",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Akan memblokir aplikasi saat jadwal aktif",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isAppLockEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showLockConsentDialog = true
                                    } else {
                                        isAppLockEnabled = false
                                        prefs.edit().putBoolean("app_lock_enabled", false).apply()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF4081),
                                    checkedTrackColor = Color(0xFFFF4081).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
            
            // 3. List Title
            item {
                Text(
                    text = "Daftar Jadwal Kesehatan & Fokus",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            
            // 4. Schedules list
            items(schedules) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onToggleChange = { isActive ->
                        val updated = schedule.copy(isActive = isActive)
                        saveSchedule(context, updated)
                        schedules = loadSchedules(context)
                    },
                    onEditClick = {
                        if (isPinEnabled) {
                            onPinVerifiedAction = { showEditDialogFor = schedule }
                            pinDialogMode = "VERIFY"
                            showPinDialog = true
                        } else {
                            showEditDialogFor = schedule
                        }
                    }
                )
            }
        }
        
        // Donation Bottom Sheet Dialog
        if (showSupportSheet) {
            DonationDialog(
                context = context,
                onDismiss = { showSupportSheet = false }
            )
        }
        
        // PIN Dialog
        if (showPinDialog) {
            PinInputDialog(
                mode = pinDialogMode,
                actualPin = pinCode,
                onDismiss = { showPinDialog = false },
                onSuccess = { enteredPin ->
                    if (pinDialogMode == "SET") {
                        val encrypted = SecurePinStorage.encryptPin(enteredPin)
                        prefs.edit()
                            .putBoolean("pin_enabled", true)
                            .putString("pin_code", encrypted)
                            .apply()
                        isPinEnabled = true
                        pinCode = enteredPin
                    } else if (pinDialogMode == "DISABLE") {
                        prefs.edit()
                            .putBoolean("pin_enabled", false)
                            .putString("pin_code", "")
                            .apply()
                        isPinEnabled = false
                        pinCode = ""
                    } else if (pinDialogMode == "VERIFY") {
                        onPinVerifiedAction?.invoke()
                    }
                    showPinDialog = false
                }
            )
        }
        
        // Edit Schedule Dialog
        if (showEditDialogFor != null) {
            EditScheduleDialog(
                schedule = showEditDialogFor!!,
                onDismiss = { showEditDialogFor = null },
                onSave = { updated ->
                    saveSchedule(context, updated)
                    schedules = loadSchedules(context)
                    showEditDialogFor = null
                }
            )
        }
        
        // Lock Consent & Honest Education Dialog
        if (showLockConsentDialog) {
            AlertDialog(
                onDismissRequest = { showLockConsentDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔐", fontSize = 24.sp)
                        Text(
                            text = "Persetujuan & Edukasi Penguncian",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Aplikasi ini berkomitmen penuh pada disiplin diri Anda. Fitur penguncian dirancang 100% offline tanpa iklan, monetisasi, atau pelacakan server.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        
                        Text(
                            text = "1. Kunci Aplikasi Internal (Sangat Direkomendasikan)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Mengunci akses ke pengaturan alarm, catatan dsb di aplikasi ini saat jadwal berjalan. Mencegah Anda mematikan alarm secara mendadak. GRATIS & TANPA IZIN APAPUN.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        
                        Text(
                            text = "2. Kunci Aplikasi Sistem (Opsional)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Jika Anda ingin memblokir aplikasi distraksi lain (sosial media dll), Android memerlukan izin Akses Penggunaan (Usage Stats) & Tampilkan di Atas Aplikasi Lain (Overlay). Izin ini murni diproses lokal di HP Anda.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        
                        if (!isPinEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚠️", fontSize = 14.sp)
                                    Text(
                                        text = "Peringatan: Anda belum mengatur PIN. Siapa saja dapat melewati layar kunci ini tanpa PIN (bypass 15 menit). Disarankan mengaktifkan PIN terlebih dahulu.",
                                        color = Color(0xFFFFB74D),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isAppLockEnabled = true
                            prefs.edit().putBoolean("app_lock_enabled", true).apply()
                            showLockConsentDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
                    ) {
                        Text("Aktifkan Kunci", color = Color.White, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLockConsentDialog = false }
                    ) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: HealthSchedule,
    onToggleChange: (Boolean) -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEditClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = if (schedule.isActive) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = if (schedule.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = schedule.icon, fontSize = 22.sp)
                }
                
                Column {
                    Text(
                        text = schedule.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${schedule.startTime} - ${schedule.endTime}",
                        color = if (schedule.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (schedule.days.size == 7) "Setiap Hari" else schedule.days.joinToString(", "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
            
            Switch(
                checked = schedule.isActive,
                onCheckedChange = { onToggleChange(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFF4081),
                    checkedTrackColor = Color(0xFFFF4081).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun DonationDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Dukung Developer ☕",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pilih platform donasi yang Anda inginkan. Anda akan diarahkan ke browser eksternal untuk melakukan pembayaran secara aman.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                
                // Sociabuzz Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            openUrl(context, HealthSocialDefaults.SOCIABUZZ_URL)
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFF2196F3))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2196F3), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Sociabuzz",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sociabuzz",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Mendukung pembayaran lokal Indonesia",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                // Saweria Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            openUrl(context, HealthSocialDefaults.SAWERIA_URL)
                            onDismiss()
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFFFF5722))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFF5722), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Saweria",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Saweria",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Dukungan QRIS, Gopay, OVO, dll",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Tutup", color = Color(0xFFFF4081))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun PinInputDialog(
    mode: String, // "SET", "DISABLE", "VERIFY"
    actualPin: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var pinValue by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    
    val titleText = when (mode) {
        "SET" -> "Buat PIN Baru"
        "DISABLE" -> "Matikan Keamanan PIN"
        else -> "Masukkan PIN Keamanan"
    }
    
    val descriptionText = when (mode) {
        "SET" -> "Tentukan 4 digit angka sebagai kode PIN keamanan Anda."
        "DISABLE" -> "Masukkan PIN lama Anda untuk mengkonfirmasi penonaktifan PIN."
        else -> "Masukkan PIN 4-digit Anda untuk membuka kunci aksi ini."
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = titleText,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = descriptionText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                
                OutlinedTextField(
                    value = pinValue,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            pinValue = input
                            errorMsg = ""
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("xxxx", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinValue.length != 4) {
                        errorMsg = "PIN harus terdiri dari 4 digit angka!"
                    } else if ((mode == "VERIFY" || mode == "DISABLE") && pinValue != actualPin) {
                        errorMsg = "PIN yang dimasukkan salah!"
                    } else {
                        onSuccess(pinValue)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
            ) {
                Text("Konfirmasi", color = Color.White, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EditScheduleDialog(
    schedule: HealthSchedule,
    onDismiss: () -> Unit,
    onSave: (HealthSchedule) -> Unit
) {
    var startText by remember { mutableStateOf(schedule.startTime) }
    var endText by remember { mutableStateOf(schedule.endTime) }
    var selectedDays by remember { mutableStateOf(schedule.days.toSet()) }
    var errorMsg by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "Edit ${schedule.name}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        label = { Text("Mulai", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        label = { Text("Selesai", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                        placeholder = { Text("12:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                }
                
                Text(
                    text = "Hari Aktif:",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                
                // Day chips using standard Row wrappers for safety
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daysRow1 = listOf("Sen", "Sel", "Rab", "Kam")
                    val daysRow2 = listOf("Jum", "Sab", "Min")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysRow1.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            selectedDays - day
                                        } else {
                                            selectedDays + day
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysRow2.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            selectedDays - day
                                        } else {
                                            selectedDays + day
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Fill space to keep it symmetric
                        Box(modifier = Modifier.weight(1f))
                    }
                }
                
                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeRegex = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$".toRegex()
                    if (!timeRegex.matches(startText) || !timeRegex.matches(endText)) {
                        errorMsg = "Format waktu harus HH:mm (contoh: 08:30)!"
                    } else if (selectedDays.isEmpty()) {
                        errorMsg = "Pilih minimal satu hari aktif!"
                    } else {
                        onSave(
                            schedule.copy(
                                startTime = startText,
                                endTime = endText,
                                days = selectedDays.toList()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
            ) {
                Text("Simpan", color = Color.White, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

fun isAppCurrentlyLocked(context: Context): HealthSchedule? {
    val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
    val isLockEnabled = prefs.getBoolean("app_lock_enabled", false)
    if (!isLockEnabled) return null
    
    val bypassUntil = prefs.getLong("bypass_until", 0L)
    if (System.currentTimeMillis() < bypassUntil) {
        return null
    }
    
    val schedules = loadSchedules(context)
    val activeSchedules = schedules.filter { it.isActive }
    if (activeSchedules.isEmpty()) return null
    
    val calendar = java.util.Calendar.getInstance()
    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    
    val dayName = when (dayOfWeek) {
        java.util.Calendar.MONDAY -> "Sen"
        java.util.Calendar.TUESDAY -> "Sel"
        java.util.Calendar.WEDNESDAY -> "Rab"
        java.util.Calendar.THURSDAY -> "Kam"
        java.util.Calendar.FRIDAY -> "Jum"
        java.util.Calendar.SATURDAY -> "Sab"
        java.util.Calendar.SUNDAY -> "Min"
        else -> ""
    }
    
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val currentTimeStr = sdf.format(calendar.time)
    val currentMinutes = HealthTimeChecker.timeToMinutes(currentTimeStr)
    
    for (schedule in activeSchedules) {
        if (HealthTimeChecker.isScheduleActive(
                schedule.startTime,
                schedule.endTime,
                schedule.days,
                dayName,
                currentMinutes
            )) {
            return schedule
        }
    }
    
    return null
}

fun setTemporaryBypass(context: Context, minutes: Int) {
    val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
    val bypassTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
    prefs.edit().putLong("bypass_until", bypassTime).apply()
}

object HealthTimeChecker {
    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }

    fun getYesterdayDayName(today: String): String {
        val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val idx = days.indexOf(today)
        if (idx == -1) return ""
        val prevIdx = if (idx == 0) 6 else idx - 1
        return days[prevIdx]
    }

    fun isScheduleActive(
        startTime: String,
        endTime: String,
        scheduledDays: List<String>,
        today: String,
        currentMinutes: Int
    ): Boolean {
        val startMin = timeToMinutes(startTime)
        val endMin = timeToMinutes(endTime)
        
        val isOvernight = endMin < startMin
        
        if (!isOvernight) {
            // Same day schedule
            return scheduledDays.contains(today) && currentMinutes in startMin until endMin
        } else {
            // Overnight schedule
            // Scenario A: Starts today, running into night
            if (scheduledDays.contains(today) && currentMinutes >= startMin) {
                return true
            }
            // Scenario B: Started yesterday, ending today morning
            val yesterday = getYesterdayDayName(today)
            if (scheduledDays.contains(yesterday) && currentMinutes < endMin) {
                return true
            }
            return false
        }
    }
}

object SecurePinStorage {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "HealthSocialPinKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        try {
            initKeyStore()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initKeyStore() {
        val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, 
                ANDROID_KEYSTORE
            )
            keyGenerator.init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or 
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): javax.crypto.SecretKey {
        val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as javax.crypto.SecretKey
    }

    fun encryptPin(pin: String): String {
        if (pin.isEmpty()) return ""
        return try {
            val cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION)
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decryptPin(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
            if (combined.size < 12) return ""
            
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION)
            val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}


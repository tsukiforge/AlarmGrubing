package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AppLockHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

// Data model for schedules
data class HealthSchedule(
    val id: String,
    val name: String,
    val icon: String,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val isActive: Boolean,
    val days: List<String>,
    val lockedApps: List<String> = emptyList()
)

object HealthSocialDefaults {
    const val SOCIABUZZ_URL = "https://sociabuzz.com/zuax"
    const val SAWERIA_URL = "https://saweria.co/MahiroDev"
    val ALL_DAYS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val PRESET_ICONS = listOf("🎯", "📚", "🛌", "💼", "🎮", "🧘", "🏃", "💻", "🎨", "🧪", "✍️", "⚽", "🏋️", "💡", "🚀")
}

fun loadPinConfig(context: Context): Pair<Boolean, String> {
    val file = File(context.filesDir, "pin_config.json")
    if (!file.exists()) {
        val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("pin_enabled", false)
        val encrypted = prefs.getString("pin_code", "") ?: ""
        val pin = if (encrypted.isNotEmpty()) SecurePinStorage.decryptPin(encrypted) else ""
        savePinConfig(context, enabled, pin)
        return Pair(enabled, pin)
    }
    return try {
        val json = JSONObject(file.readText())
        val enabled = json.optBoolean("pin_enabled", false)
        val encrypted = json.optString("pin_code", "")
        val pin = if (encrypted.isNotEmpty()) SecurePinStorage.decryptPin(encrypted) else ""
        Pair(enabled, pin)
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(false, "")
    }
}

fun savePinConfig(context: Context, enabled: Boolean, pin: String) {
    try {
        val file = File(context.filesDir, "pin_config.json")
        val json = JSONObject()
        json.put("pin_enabled", enabled)
        val encrypted = if (pin.isNotEmpty()) SecurePinStorage.encryptPin(pin) else ""
        json.put("pin_code", encrypted)
        file.writeText(json.toString(4))
        
        // Also update shared prefs for fast lookup
        val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("pin_enabled", enabled)
            .putString("pin_code", encrypted)
            .apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadSchedules(context: Context): List<HealthSchedule> {
    val file = File(context.filesDir, "health_schedules.json")
    val defaultDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum")
    val defaults = listOf(
        HealthSchedule("fokus", "Jadwal Fokus", "🎯", "08:00", "12:00", false, defaultDays),
        HealthSchedule("belajar", "Jadwal Belajar", "📚", "14:00", "16:00", false, defaultDays),
        HealthSchedule("tidur", "Jadwal Tidur", "🛌", "22:00", "05:00", false, listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")),
        HealthSchedule("kerja", "Jadwal Kerja", "💼", "09:00", "17:00", false, defaultDays),
        HealthSchedule("bermain", "Jadwal Bermain", "🎮", "18:00", "20:00", false, defaultDays)
    )
    
    if (!file.exists()) {
        saveSchedulesToJson(context, defaults)
        return defaults
    }
    
    return try {
        val jsonArray = JSONArray(file.readText())
        val list = mutableListOf<HealthSchedule>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.optString("id", UUID.randomUUID().toString())
            val name = obj.optString("name", "Jadwal")
            val icon = obj.optString("icon", "🎯")
            val startTime = obj.optString("startTime", "08:00")
            val endTime = obj.optString("endTime", "12:00")
            val isActive = obj.optBoolean("isActive", false)
            
            val daysArray = obj.optJSONArray("days")
            val daysList = mutableListOf<String>()
            if (daysArray != null) {
                for (j in 0 until daysArray.length()) {
                    daysList.add(daysArray.getString(j))
                }
            } else {
                daysList.addAll(defaultDays)
            }
            
            val lockedAppsList = mutableListOf<String>()
            if (obj.has("lockedApps")) {
                val appsArray = obj.getJSONArray("lockedApps")
                for (j in 0 until appsArray.length()) {
                    lockedAppsList.add(appsArray.getString(j))
                }
            }
            
            list.add(HealthSchedule(id, name, icon, startTime, endTime, isActive, daysList, lockedAppsList))
        }
        if (list.isEmpty()) defaults else list
    } catch (e: Exception) {
        e.printStackTrace()
        defaults
    }
}

fun saveSchedulesToJson(context: Context, schedules: List<HealthSchedule>) {
    try {
        val jsonArray = JSONArray()
        for (sched in schedules) {
            val obj = JSONObject()
            obj.put("id", sched.id)
            obj.put("name", sched.name)
            obj.put("icon", sched.icon)
            obj.put("startTime", sched.startTime)
            obj.put("endTime", sched.endTime)
            obj.put("isActive", sched.isActive)
            
            val daysArray = JSONArray()
            sched.days.forEach { daysArray.put(it) }
            obj.put("days", daysArray)
            
            val appsArray = JSONArray()
            sched.lockedApps.forEach { appsArray.put(it) }
            obj.put("lockedApps", appsArray)
            
            jsonArray.put(obj)
        }
        val file = File(context.filesDir, "health_schedules.json")
        file.writeText(jsonArray.toString(4))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun saveSchedule(context: Context, schedule: HealthSchedule) {
    val current = loadSchedules(context).toMutableList()
    val index = current.indexOfFirst { it.id == schedule.id }
    if (index != -1) {
        current[index] = schedule
    } else {
        current.add(schedule)
    }
    saveSchedulesToJson(context, current)
}

fun deleteSchedule(context: Context, scheduleId: String) {
    val current = loadSchedules(context).filter { it.id != scheduleId }
    saveSchedulesToJson(context, current)
}

@Composable
fun HealthSocialScreen(context: Context, viewModel: Any? = null) {
    var schedules by remember { mutableStateOf(loadSchedules(context)) }
    val prefs = remember { context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE) }
    
    var bannerDismissed by remember {
        mutableStateOf(prefs.getBoolean("support_banner_dismissed", false))
    }
    
    val pinConfig = remember { loadPinConfig(context) }
    var isPinEnabled by remember { mutableStateOf(pinConfig.first) }
    var pinCode by remember { mutableStateOf(pinConfig.second) }
    
    var isAppLockEnabled by remember {
        mutableStateOf(prefs.getBoolean("app_lock_enabled", false))
    }
    
    var hasUsageStats by remember { mutableStateOf(AppLockHelper.hasUsageStatsPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(AppLockHelper.isAccessibilityServiceEnabled(context)) }
    
    var showLockConsentDialog by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showEditDialogFor by remember { mutableStateOf<HealthSchedule?>(null) }
    var isCreatingNewSchedule by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf("VERIFY") } // "SET", "VERIFY", "DISABLE"
    var onPinVerifiedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    // Check currently active schedule
    val activeRunningSchedule = remember(schedules, isAppLockEnabled) {
        isAppCurrentlyLocked(context)
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dukung Developer Banner
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
                                    Text("☕", fontSize = 22.sp)
                                    Text(
                                        text = "Dukung Developer",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
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
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "Dukung terus pengembangan aplikasi ini agar selalu gratis dan bebas iklan dengan berdonasi kecil! ❤️",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
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
            
            // 2. Active Status Live Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeRunningSchedule != null) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (activeRunningSchedule != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (activeRunningSchedule != null) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = activeRunningSchedule?.icon ?: "🛡️", fontSize = 24.sp)
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (activeRunningSchedule != null) "Jadwal Sedang Aktif" else "Mode Fokus & Kesehatan",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (activeRunningSchedule != null)
                                    "${activeRunningSchedule.name} (${activeRunningSchedule.startTime} - ${activeRunningSchedule.endTime}) • ${activeRunningSchedule.lockedApps.size} aplikasi terkunci"
                                else
                                    "Kunci aplikasi & batasi distraksi sesuai jadwal pilihan Anda.",
                                color = if (activeRunningSchedule != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            
            // 3. Security & App Lock Switch Card
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
                                text = "Keamanan & Penguncian Aplikasi",
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
                                    text = if (isPinEnabled) "PIN Aktif (Melindungi jadwal)" else "Mencegah perubahan jadwal tanpa izin",
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
                                    text = "Aktifkan pemblokiran aplikasi saat jadwal aktif",
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
            
            // 4. System Permissions Guidance Card
            if (isAppLockEnabled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Izin Sistem Kunci Aplikasi Eksternal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Text(
                                text = "Untuk memblokir aplikasi lain (Sosmed, Game dll) secara otomatis di HP Anda, aktifkan dua izin sistem berikut:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            
                            // Permission 1: Usage Access
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("1. Akses Penggunaan (Usage Stats)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = if (hasUsageStats) "✅ Izin Diberikan" else "❌ Belum Diaktifkan",
                                        fontSize = 10.sp,
                                        color = if (hasUsageStats) Color(0xFF2E7D32) else Color.Red
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                        } catch (e: Exception) {
                                            openUrl(context, "intent:action=android.settings.USAGE_ACCESS_SETTINGS#Intent;end")
                                        }
                                    }
                                ) {
                                    Text(if (hasUsageStats) "Atur" else "Aktifkan", fontSize = 11.sp, color = Color(0xFFFF4081))
                                }
                            }
                            
                            // Permission 2: Accessibility Service
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("2. Layanan Aksesibilitas (Realtime Lock)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = if (hasAccessibility) "✅ Izin Diberikan" else "❌ Belum Diaktifkan",
                                        fontSize = 10.sp,
                                        color = if (hasAccessibility) Color(0xFF2E7D32) else Color.Red
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                        } catch (e: Exception) {
                                            openUrl(context, "intent:action=android.settings.ACCESSIBILITY_SETTINGS#Intent;end")
                                        }
                                    }
                                ) {
                                    Text(if (hasAccessibility) "Atur" else "Aktifkan", fontSize = 11.sp, color = Color(0xFFFF4081))
                                }
                            }
                        }
                    }
                }
            }
            
            // 5. Schedules Header with "+ Tambah Jadwal Baru"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Jadwal Kesehatan & Fokus",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Button(
                        onClick = {
                            val newSched = HealthSchedule(
                                id = UUID.randomUUID().toString(),
                                name = "Jadwal Baru",
                                icon = "🎯",
                                startTime = "08:00",
                                endTime = "12:00",
                                isActive = true,
                                days = listOf("Sen", "Sel", "Rab", "Kam", "Jum"),
                                lockedApps = emptyList()
                            )
                            isCreatingNewSchedule = true
                            if (isPinEnabled) {
                                onPinVerifiedAction = { showEditDialogFor = newSched }
                                pinDialogMode = "VERIFY"
                                showPinDialog = true
                            } else {
                                showEditDialogFor = newSched
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Tambah", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // 6. Schedules List
            items(schedules) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onToggleChange = { isActive ->
                        if (isActive) {
                            if (isPinEnabled) {
                                onPinVerifiedAction = {
                                    val updated = schedule.copy(isActive = true)
                                    saveSchedule(context, updated)
                                    schedules = loadSchedules(context)
                                }
                                pinDialogMode = "VERIFY"
                                showPinDialog = true
                            } else {
                                val updated = schedule.copy(isActive = true)
                                saveSchedule(context, updated)
                                schedules = loadSchedules(context)
                            }
                        } else {
                            val updated = schedule.copy(isActive = false)
                            saveSchedule(context, updated)
                            schedules = loadSchedules(context)
                        }
                    },
                    onEditClick = {
                        isCreatingNewSchedule = false
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
                        savePinConfig(context, true, enteredPin)
                        isPinEnabled = true
                        pinCode = enteredPin
                    } else if (pinDialogMode == "DISABLE") {
                        savePinConfig(context, false, "")
                        isPinEnabled = false
                        pinCode = ""
                    } else if (pinDialogMode == "VERIFY") {
                        onPinVerifiedAction?.invoke()
                    }
                    showPinDialog = false
                }
            )
        }
        
        // Edit or Add Schedule Dialog
        if (showEditDialogFor != null) {
            EditScheduleDialog(
                schedule = showEditDialogFor!!,
                isNewSchedule = isCreatingNewSchedule,
                onDismiss = { showEditDialogFor = null },
                onSave = { updated ->
                    saveSchedule(context, updated)
                    schedules = loadSchedules(context)
                    showEditDialogFor = null
                },
                onDelete = { idToDelete ->
                    deleteSchedule(context, idToDelete)
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
                            text = "1. Kunci Aplikasi Internal (Gratis & Tanpa Izin)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Mengunci akses ke alarm, catatan, dan fitur utama di aplikasi ini saat jadwal berjalan agar Anda tidak mematikan alarm secara mendadak.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        
                        Text(
                            text = "2. Kunci Aplikasi Sistem (Akses Penggunaan & Aksesibilitas)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Memblokir aplikasi distraksi lain (Instagram, WhatsApp, Mobile Legends dll). Menggunakan Layanan Aksesibilitas lokal untuk penguncian instan tanpa bocor ke internet.",
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
                                        text = "Saran: Aktifkan PIN Keamanan agar jadwal Anda tidak dapat diubah oleh siapapun saat penguncian aktif.",
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
                        text = if (schedule.days.size == 7) "Setiap Hari • 🔒 ${schedule.lockedApps.size} App" else "${schedule.days.joinToString(", ")} • 🔒 ${schedule.lockedApps.size} App",
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

data class AppInfo(val name: String, val packageName: String, val icon: ImageBitmap? = null)

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun EditScheduleDialog(
    schedule: HealthSchedule,
    isNewSchedule: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (HealthSchedule) -> Unit,
    onDelete: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }
    
    // Step 1 States
    var scheduleName by remember { mutableStateOf(schedule.name) }
    var selectedIcon by remember { mutableStateOf(schedule.icon) }
    var startText by remember { mutableStateOf(schedule.startTime) }
    var endText by remember { mutableStateOf(schedule.endTime) }
    var selectedDays by remember { mutableStateOf(schedule.days.toSet()) }
    var errorMsg by remember { mutableStateOf("") }
    
    // Step 2 States
    var selectedApps by remember { mutableStateOf(schedule.lockedApps.toSet()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // App info fetching
    val installedApps = remember { mutableStateListOf<AppInfo>() }
    var isAppsLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val list = pm.queryIntentActivities(intent, 0)
                val apps = list.mapNotNull { resolveInfo ->
                    try {
                        val appName = resolveInfo.loadLabel(pm).toString()
                        val packageName = resolveInfo.activityInfo.packageName
                        val drawable = resolveInfo.loadIcon(pm)
                        val bitmap = drawableToBitmap(drawable)?.asImageBitmap()
                        AppInfo(name = appName, packageName = packageName, icon = bitmap)
                    } catch (e: Exception) {
                        null
                    }
                }.distinctBy { it.packageName }.sortedBy { it.name }
                
                withContext(Dispatchers.Main) {
                    installedApps.clear()
                    installedApps.addAll(apps)
                    isAppsLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbacks = listOf(
                    AppInfo("WhatsApp", "com.whatsapp"),
                    AppInfo("Instagram", "com.instagram.android"),
                    AppInfo("TikTok", "com.zhiliaoapp.musically"),
                    AppInfo("YouTube", "com.google.android.youtube"),
                    AppInfo("Facebook", "com.facebook.katana"),
                    AppInfo("Twitter / X", "com.twitter.android"),
                    AppInfo("Telegram", "org.telegram.messenger"),
                    AppInfo("Mobile Legends", "com.mobile.legends")
                )
                withContext(Dispatchers.Main) {
                    installedApps.clear()
                    installedApps.addAll(fallbacks)
                    isAppsLoading = false
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentStep == 1) "Langkah 1/2: Nama & Jadwal" else "Langkah 2/2: Pilih Aplikasi Terkunci",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (!isNewSchedule && currentStep == 1) {
                    IconButton(
                        onClick = { onDelete(schedule.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Jadwal",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        text = {
            if (currentStep == 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Schedule Name
                    OutlinedTextField(
                        value = scheduleName,
                        onValueChange = { scheduleName = it },
                        label = { Text("Nama Jadwal", fontSize = 11.sp) },
                        placeholder = { Text("misal: Jadwal Belajar / Fokus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                    
                    // Icon Picker
                    Text("Pilih Ikon:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HealthSocialDefaults.PRESET_ICONS.take(7).forEach { iconStr ->
                            val isSelected = selectedIcon == iconStr
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .clickable { selectedIcon = iconStr },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(iconStr, fontSize = 18.sp)
                            }
                        }
                    }
                    
                    // Time Inputs
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
                                            selectedDays = if (isSelected) selectedDays - day else selectedDays + day
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
                                            selectedDays = if (isSelected) selectedDays - day else selectedDays + day
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
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Centang aplikasi yang ingin dikunci oleh sistem saat jadwal ini berjalan aktif:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama/paket aplikasi...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        if (isAppsLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFFF4081), modifier = Modifier.size(24.dp))
                            }
                        } else {
                            val filtered = installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
                            if (filtered.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada aplikasi ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filtered) { app ->
                                        val isChecked = selectedApps.contains(app.packageName)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedApps = if (isChecked) selectedApps - app.packageName else selectedApps + app.packageName
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                if (app.icon != null) {
                                                    Image(
                                                        bitmap = app.icon,
                                                        contentDescription = app.name,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("📱", fontSize = 14.sp)
                                                    }
                                                }
                                                
                                                Column {
                                                    Text(app.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedApps = if (checked == true) selectedApps + app.packageName else selectedApps - app.packageName
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF4081))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep == 1) {
                Button(
                    onClick = {
                        val startParts = startText.split(":")
                        val endParts = endText.split(":")
                        if (scheduleName.trim().isEmpty()) {
                            errorMsg = "Nama jadwal tidak boleh kosong!"
                        } else if (startParts.size != 2 || endParts.size != 2 || 
                            startParts[0].toIntOrNull() == null || startParts[1].toIntOrNull() == null ||
                            endParts[0].toIntOrNull() == null || endParts[1].toIntOrNull() == null ||
                            startParts[0].toInt() !in 0..23 || startParts[1].toInt() !in 0..59 ||
                            endParts[0].toInt() !in 0..23 || endParts[1].toInt() !in 0..59) {
                            errorMsg = "Format waktu harus HH:mm (contoh: 08:00)!"
                        } else if (selectedDays.isEmpty()) {
                            errorMsg = "Pilih minimal satu hari aktif!"
                        } else {
                            errorMsg = ""
                            currentStep = 2
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
                ) {
                    Text("Selanjutnya ➡️", color = Color.White, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = {
                        onSave(
                            schedule.copy(
                                name = scheduleName.ifEmpty { schedule.name },
                                icon = selectedIcon,
                                startTime = startText,
                                endTime = endText,
                                days = selectedDays.toList(),
                                lockedApps = selectedApps.toList(),
                                isActive = true
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
                ) {
                    Text("Selesai & Simpan 💾", color = Color.White, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            if (currentStep == 2) {
                TextButton(onClick = { currentStep = 1 }) {
                    Text("⬅️ Kembali", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
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
            if (scheduledDays.contains(today) && currentMinutes >= startMin) {
                return true
            }
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
            // Fallback XOR or Base64 if KeyStore unavailable
            android.util.Base64.encodeToString(pin.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        }
    }

    fun decryptPin(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
            if (combined.size < 12) {
                return String(combined, Charsets.UTF_8)
            }
            
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
            try {
                val decoded = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP)
                String(decoded, Charsets.UTF_8)
            } catch (ex: Exception) {
                ""
            }
        }
    }
}

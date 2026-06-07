package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.example.data.api.GithubUpdateChecker
import com.example.ui.theme.AppThemeState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarm.AlarmRingingService
import android.app.KeyguardManager
import android.view.WindowManager
import com.example.data.helper.NetworkConnectionHelper
import com.example.data.model.Alarm
import com.example.ui.AlarmViewModel
import com.example.ui.SyncStatus
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import java.util.*

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private var isRingingState = mutableStateOf(false)
    private var ringingAlarmIdState = mutableStateOf<String?>(null)
    private var ringingAlarmTitleState = mutableStateOf<String?>(null)
    private var ringingAlarmIsGroupState = mutableStateOf(false)
    private var ringingAlarmGroupCodeState = mutableStateOf<String?>(null)

    private val ringingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val ringing = intent.getBooleanExtra("IS_RINGING", false)
                isRingingState.value = ringing
                if (ringing) {
                    ringingAlarmIdState.value = intent.getStringExtra("ALARM_ID")
                    ringingAlarmTitleState.value = intent.getStringExtra("ALARM_TITLE")
                    ringingAlarmIsGroupState.value = intent.getBooleanExtra("ALARM_IS_GROUP", false)
                    ringingAlarmGroupCodeState.value = intent.getStringExtra("ALARM_GROUP_CODE")
                } else {
                    ringingAlarmIdState.value = null
                    ringingAlarmTitleState.value = null
                    ringingAlarmIsGroupState.value = false
                    ringingAlarmGroupCodeState.value = null
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure system flags to show on lock screen / wake up layout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            try {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                keyguardManager.requestDismissKeyguard(this, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        isRingingState.value = AlarmRingingService.isRinging
        ringingAlarmIdState.value = AlarmRingingService.activeAlarmId
        ringingAlarmTitleState.value = AlarmRingingService.activeAlarmTitle
        ringingAlarmIsGroupState.value = AlarmRingingService.activeAlarmIsGroup
        ringingAlarmGroupCodeState.value = AlarmRingingService.activeAlarmGroupCode

        val filter = IntentFilter("ALARM_RINGING_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(ringingReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(ringingReceiver, filter)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("alarm_grup_prefs", MODE_PRIVATE) }
            LaunchedEffect(Unit) {
                AppThemeState.themeMode = prefs.getString("theme_mode", "system") ?: "system"
            }

            MyApplicationTheme {
                val viewModel: AlarmViewModel = viewModel()
                val isRinging by isRingingState
                val rAlarmTitle by ringingAlarmTitleState
                val rAlarmIsGroup by ringingAlarmIsGroupState
                val rAlarmGroupCode by ringingAlarmGroupCodeState

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundDark
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainScreenContent(
                            viewModel = viewModel,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = isRinging,
                            enter = fadeIn(animationSpec = tween(500)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(500)) + shrinkVertically()
                        ) {
                            RingingOverlay(
                                title = rAlarmTitle ?: "Alarm",
                                isGroup = rAlarmIsGroup,
                                groupCode = rAlarmGroupCode,
                                onDismiss = {
                                    val stopIntent = Intent(this@MainActivity, AlarmRingingService::class.java).apply {
                                        action = "STOP"
                                    }
                                    this@MainActivity.startService(stopIntent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(ringingReceiver)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}

@Composable
fun MainScreenContent(
    viewModel: AlarmViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Pribadi, 1: Grup
    val personalAlarms by viewModel.personalAlarms.collectAsState()
    val groupAlarms by viewModel.groupAlarms.collectAsState()
    val joinedGroupCode by viewModel.joinedGroupCode.collectAsState()
    val joinedGroupName by viewModel.joinedGroupName.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isCreator by viewModel.isCreator.collectAsState()
    val isOfflineGroup by viewModel.isOfflineGroup.collectAsState()

    val context = LocalContext.current
    val isConnected by NetworkConnectionHelper.observeConnection(context).collectAsState(initial = NetworkConnectionHelper.isConnected(context))
    val networkType = remember(isConnected) { NetworkConnectionHelper.getNetworkType(context) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showUserNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Alarm Sync ⏰",
                    color = TextLight,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grup real-time cloud-sync",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEADDFF))
                    .clickable { showUserNameDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = userName,
                    color = Color(0xFF21005D),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Hubungan Internet: Aktif ($networkType)" else "Status: Offline (Tidak ada jaringan data atau wifi) ⚠️",
                    color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .padding(4.dp)
        ) {
            TabButton(
                title = "⏰ Personal",
                isActive = activeTab == 0,
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "👥 Grup Alarm",
                isActive = activeTab == 1,
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeTab == 0) {
                if (personalAlarms.isEmpty()) {
                    EmptyStatePlaceholder(
                        title = "Belum ada alarm pribadi",
                        subtitle = "Buat alarm untuk pengingat aktifitas diri kamu.",
                        icon = Icons.Default.Notifications
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(personalAlarms, key = { it.id }) { alarm ->
                            AlarmCard(
                                alarm = alarm,
                                onToggle = { viewModel.toggleAlarm(alarm) },
                                onDelete = { viewModel.deleteAlarm(alarm) }
                            )
                        }
                    }
                }
            } else {
                if (joinedGroupCode == null) {
                    GroupOnboardingScreen(
                        viewModel = viewModel,
                        syncState = syncState
                    )
                } else {
                    GroupDashboardScreen(
                        viewModel = viewModel,
                        code = joinedGroupCode!!,
                        groupName = joinedGroupName,
                        alarms = groupAlarms,
                        syncState = syncState,
                        isOfflineGroup = isOfflineGroup,
                        isCreator = isCreator,
                        onAddAlarmClick = { showAddDialog = true }
                    )
                }
            }

            if (activeTab == 0 || joinedGroupCode != null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 8.dp),
                    containerColor = PinkAccent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambahkan Alarm",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    if (showUserNameDialog) {
        UserProfileAndSettingsDialog(
            currentName = userName,
            onDismiss = { showUserNameDialog = false },
            onSaveName = {
                viewModel.updateUserName(it)
                showUserNameDialog = false
            },
            onRequestNotificationPermission = onRequestNotificationPermission
        )
    }

    if (showAddDialog) {
        AddAlarmDialog(
            isGroup = activeTab == 1,
            onDismiss = { showAddDialog = false },
            onSave = { title, hour, minute, days, tone ->
                if (activeTab == 0) {
                    viewModel.addPersonalAlarm(title, hour, minute, days, tone)
                } else {
                    viewModel.addGroupAlarm(title, hour, minute, days, tone)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TabButton(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.Transparent,
        animationSpec = tween(300)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) TextLight else TextMuted,
        animationSpec = tween(300)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = contentColor,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isGroup = alarm.isGroup
    val cardBgColor = if (isGroup) Color(0xFFEADDFF) else Color.White
    val borderCol = if (isGroup) Color(0xFF21005D).copy(alpha = 0.08f) else Color(0xFFCAC4D0).copy(alpha = 0.5f)
    val contentColor = if (isGroup) Color(0xFF21005D) else TextLight
    val timeColor = if (isGroup) Color(0xFF21005D) else (if (alarm.isEnabled) IndigoPrimary else TextMuted)
    val subtitleColor = if (isGroup) Color(0xFF21005D).copy(alpha = 0.7f) else TextMuted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isGroup) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.title.ifEmpty { "Tanpa Judul" },
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formattedHour = String.format(Locale.getDefault(), "%02d", alarm.hour)
                val formattedMinute = String.format(Locale.getDefault(), "%02d", alarm.minute)
                Text(
                    text = "$formattedHour:$formattedMinute",
                    color = timeColor,
                    fontSize = 36.sp,
                    fontWeight = if (isGroup) FontWeight.Light else FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                val daysLabel = getDaysLabel(alarm.daysOfWeek)
                val soundLabel = when (alarm.ringtoneUri) {
                    "custom_1" -> "🎐 Melody Chime"
                    "custom_2" -> "📟 Retro Beep"
                    "custom_3" -> "🌌 Echo Syzer"
                    else -> "🎵 Default System"
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGroup) Color(0xFF21005D).copy(alpha = 0.08f) else Color(0xFFF4EFF4))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = daysLabel,
                            color = subtitleColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isGroup) Color(0xFF21005D).copy(alpha = 0.08f) else Color(0xFFF4EFF4))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = soundLabel,
                            color = subtitleColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isGroup && alarm.creatorName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dibuat oleh: ${alarm.creatorName}",
                        color = Color(0xFF21005D).copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IndigoPrimary,
                        uncheckedThumbColor = Color(0xFF938F99),
                        uncheckedTrackColor = Color(0xFFE7E0EC),
                        uncheckedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = if (isGroup) Color(0xFF21005D).copy(alpha = 0.6f) else PinkAccent.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Alarm?", color = TextLight) },
            text = { Text("Apakah kamu yakin ingin menghapus alarm ini?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Ya, Hapus", color = PinkAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun GroupOnboardingScreen(
    viewModel: AlarmViewModel,
    syncState: SyncStatus
) {
    var groupCodeInput by remember { mutableStateOf("") }
    var groupNameInput by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(IndigoPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Group logo",
                tint = IndigoPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isJoining) "Gabung Grup Alarm" else "Buat Grup Alarm Baru",
            color = TextLight,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isJoining) "Masukkan 4 digit kode dari temanmu untuk menyinkronkan alarm kelompok." else "Buat kode kamar grup kamu sendiri agar teman-teman bisa bergabung.",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isJoining) {
                    OutlinedTextField(
                        value = groupCodeInput,
                        onValueChange = {
                            if (it.length <= 4) groupCodeInput = it.filter { c -> c.isDigit() }
                        },
                        label = { Text("Kode Grup (4 digit)") },
                        placeholder = { Text("Contoh: 9736") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = IndigoPrimary,
                            unfocusedLabelColor = TextMuted
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Nama Kegiatan") },
                        placeholder = { Text("Contoh: Acara Temen / UKM Band") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = IndigoPrimary,
                            unfocusedLabelColor = TextMuted
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = Color(0xFFBA1A1A), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        if (isJoining) {
                            if (groupCodeInput.length != 4) {
                                errorMessage = "Kode harus 4 digit angka!"
                                return@Button
                            }
                            viewModel.joinGroup(groupCodeInput) { success, error ->
                                if (!success) {
                                    errorMessage = error ?: "Gagal bergabung. Periksa kode grup."
                                }
                            }
                        } else {
                            if (groupNameInput.isBlank()) {
                                errorMessage = "Nama kelompok tidak boleh kosong!"
                                return@Button
                            }
                            viewModel.createGroup(groupNameInput) { success, error ->
                                if (!success) {
                                    errorMessage = error ?: "Gagal membuat grup baru."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isJoining) "Gabung Sekarang 👥" else "Buat Kode Baru 🔐",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            isJoining = !isJoining
            errorMessage = null
        }) {
            Text(
                text = if (isJoining) "Klik di sini untuk membuat kode grup baru" else "Batal buat baru, gabung kode grup teman",
                color = IndigoLight,
                fontSize = 13.sp
            )
        }

        if (syncState is SyncStatus.Syncing) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(color = IndigoPrimary)
        }
    }
}

@Composable
fun GroupDashboardScreen(
    viewModel: AlarmViewModel,
    code: String,
    groupName: String,
    alarms: List<Alarm>,
    syncState: SyncStatus,
    isOfflineGroup: Boolean,
    isCreator: Boolean,
    onAddAlarmClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = groupName,
                            color = TextLight,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isOfflineGroup) Color.Gray
                                        else if (syncState is SyncStatus.Synced || syncState is SyncStatus.Success) Color.Green
                                        else if (syncState is SyncStatus.Syncing) Color.Yellow
                                        else Color.Red
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isOfflineGroup -> "Mode Offline (Disimpan lokal)"
                                    syncState is SyncStatus.Syncing -> "Sinkronisasi..."
                                    syncState is SyncStatus.Synced -> "Tersinkron real-time"
                                    syncState is SyncStatus.Success -> "Tersinkron real-time"
                                    else -> "Koneksi terputus"
                                },
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.leaveGroup() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Keluar Grup",
                            tint = Color(0xFFBA1A1A)
                        )
                    }
                }

                // Go Online / Reconnect cloud interface for offline fallback groups
                if (isOfflineGroup) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Kamar grup Anda saat ini offline. Tekan hubungkan jika perangkat Anda sudah memiliki akses data/Wi-Fi agar tersinkronisasi ke teman-teman.",
                                color = Color(0xFF5D4037),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            var reconnecting by remember { mutableStateOf(false) }
                            var reconnectMsg by remember { mutableStateOf<String?>(null) }
                            Button(
                                onClick = {
                                    reconnecting = true
                                    reconnectMsg = null
                                    viewModel.syncOfflineGroupToCloud { success, err ->
                                        reconnecting = false
                                        if (success) {
                                            reconnectMsg = "Grup telah berhasil di-online-kan! 🎉"
                                        } else {
                                            reconnectMsg = err ?: "Koneksi gagal. Periksa sinyal internet Anda."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                if (reconnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sedang menghubungkan...", color = Color.White, fontSize = 11.sp)
                                } else {
                                    Text("Online-kan Grup & Sinkron Server 🔄", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            reconnectMsg?.let { msg ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = msg,
                                    color = if (msg.contains("berhasil")) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "KODE SINKRONISASI", color = TextMuted, fontSize = 10.sp)
                        Text(
                            text = code,
                            color = CyanAccent,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndigoPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCreator) "Pemilik" else "Anggota",
                            color = IndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Beritahu kode di atas ke teman kamu agar bisa menyamakan jadual weker ini.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "📬 Alarm Kelompok (${alarms.size})",
            color = TextLight,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (alarms.isEmpty()) {
            EmptyStatePlaceholder(
                title = "Grup masih kosong",
                subtitle = "Klik tombol + di pojok bawah untuk menjadualkan alarm kelompok.",
                icon = Icons.Default.Person
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
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
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SurfaceDarkElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IndigoLight,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = TextLight,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
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
                    val modes = listOf("system" to "Sistem 📱", "light" to "Terang ☀️", "dark" to "Gelap 🌙")
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
                                imageVector = Icons.Default.KeyboardArrowRight,
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
                                updateInfoState = info
                            } else {
                                checkUpdateError = "Gagal memeriksa pembaruan. Silakan periksa koneksi."
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

@Composable
fun AddAlarmDialog(
    isGroup: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, hour: Int, minute: Int, daysOfWeek: String, ringtone: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var selectedTone by remember { mutableStateOf("default") }

    val selectedDays = remember { mutableStateListOf<Int>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = if (isGroup) "Buat Alarm Kelompok 👥" else "Buat Alarm Pribadi ⏰",
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

                    Text(text = "Pilih Jam", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Up", tint = IndigoPrimary)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", hour),
                                color = TextLight,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { hour = if (hour - 1 < 0) 23 else hour - 1 }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Down", tint = IndigoPrimary)
                            }
                        }

                        Text(
                            text = ":",
                            color = TextLight,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Up", tint = IndigoPrimary)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", minute),
                                color = TextLight,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { minute = if (minute - 5 < 0) 55 else minute - 5 }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Down", tint = IndigoPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val tones = listOf(
                    "default" to "🎵 default bawaan hp",
                    "custom_1" to "🎐 Melody Chime (Aplikasi)",
                    "custom_2" to "📟 Retro Beep (Aplikasi)",
                    "custom_3" to "🌌 Echo Syzer (Aplikasi)"
                )
                items(tones) { p ->
                    val isChecked = selectedTone == p.first
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isChecked) SurfaceDarkElevated else Color.Transparent)
                            .clickable { selectedTone = p.first }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isChecked,
                            onClick = { selectedTone = p.first },
                            colors = RadioButtonDefaults.colors(selectedColor = IndigoPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = p.second, color = TextLight, fontSize = 13.sp)
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
                                "Simpan Alarm",
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

@Composable
fun RingingOverlay(
    title: String,
    isGroup: Boolean,
    groupCode: String?,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0B1E),
                        Color(0xFF2C0A1E),
                        Color(0xFF07050A)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGroup) IndigoPrimary else Color.Gray)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isGroup) "⏰ ALARM GRUP ($groupCode)" else "⏰ ALARM PRIBADI",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title.ifEmpty { "Waktu Pengingat!" },
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(PinkAccent.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(PinkAccent.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PinkAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(54.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .width(200.dp)
                .height(48.dp)
        ) {
            Text(
                text = "MATIKAN",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize = 14.sp
            )
        }
    }
}

fun getDaysLabel(days: String): String {
    if (days.isBlank()) return "Sekali saja"
    val list = days.split(",").mapNotNull { it.toIntOrNull() }
    if (list.size == 7) return "Setiap hari"
    if (list.isEmpty()) return "Sekali saja"
    val shortDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    return list.map { shortDays[it - 1] }.joinToString(", ")
}

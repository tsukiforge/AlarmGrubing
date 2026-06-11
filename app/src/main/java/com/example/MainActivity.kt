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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.example.data.api.GithubUpdateChecker
import com.example.ui.theme.AppThemeState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale as drawScopeScale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            var currentScreen by remember { mutableStateOf("home") }
            var profilePicTrigger by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                AppThemeState.themeMode = prefs.getString("theme_mode", "system") ?: "system"
                AppThemeState.sakuraEnabled = prefs.getBoolean("sakura_enabled", true)
                var serverUrl = prefs.getString("sync_server_url", "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/") ?: "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/"
                if (serverUrl.contains("alarmsync-faizinu-default-rtdb")) {
                    serverUrl = "https://server-ba906-default-rtdb.asia-southeast1.firebasedatabase.app/"
                    prefs.edit().putString("sync_server_url", serverUrl).apply()
                }
                com.example.data.api.NetworkClient.updateBaseUrl(serverUrl)
            }

            MyApplicationTheme {
                val viewModel: AlarmViewModel = viewModel()
                val isRinging by isRingingState
                val rAlarmTitle by ringingAlarmTitleState
                val rAlarmIsGroup by ringingAlarmIsGroupState
                val rAlarmGroupCode by ringingAlarmGroupCodeState

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundDark)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_anime_background_1780840432597),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = if (isSystemInDarkTheme()) 0.22f else 0.42f
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                                    },
                                    label = "ScreenTransition"
                                ) { screen ->
                                    if (screen == "home") {
                                        MainScreenContent(
                                            viewModel = viewModel,
                                            profilePicTrigger = profilePicTrigger,
                                            onNavigateToSettings = { currentScreen = "settings" },
                                            onRequestNotificationPermission = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                        )
                                    } else {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            profilePicTrigger = profilePicTrigger,
                                            onProfilePicChanged = { profilePicTrigger = !profilePicTrigger },
                                            onBack = { currentScreen = "home" },
                                            onRequestNotificationPermission = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isRinging,
                        enter = fadeIn(animationSpec = tween(500)) + expandVertically(),
                        exit = fadeOut(animationSpec = tween(500)) + shrinkVertically(),
                        modifier = Modifier.fillMaxSize()
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
    profilePicTrigger: Boolean,
    onNavigateToSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Pribadi, 1: Grup, 2: Catatan
    val personalAlarms by viewModel.personalAlarms.collectAsState()
    val groupAlarms by viewModel.groupAlarms.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val joinedGroupCode by viewModel.joinedGroupCode.collectAsState()
    val joinedGroupName by viewModel.joinedGroupName.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val isCreator by viewModel.isCreator.collectAsState()
    val isOfflineGroup by viewModel.isOfflineGroup.collectAsState()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE) }
    val isConnected by NetworkConnectionHelper.observeConnection(context).collectAsState(initial = NetworkConnectionHelper.isConnected(context))
    val networkType = remember(isConnected) { NetworkConnectionHelper.getNetworkType(context) }

    var showAddDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
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
                    text = "Alarm Sync 🌸",
                    color = TextLight,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sekai Chibi Sync Scheduler 💫",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onNavigateToSettings() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
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

                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap.asImageBitmap(),
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
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
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = userName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        // --- 🌸 KAWAII ANIME COMPANION WIDGET 🌸 ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val timeString = remember {
                        val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                        sdf.format(java.util.Date())
                    }
                    var currentTime by remember { mutableStateOf(timeString) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                            currentTime = sdf.format(java.util.Date())
                            kotlinx.coroutines.delay(10000L)
                        }
                    }
                    Text(
                        text = currentTime,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "Waktu Perangkat Kamu ✨",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Anime Chibi Emoticon reactive companion with custom generated mascot image
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_anime_mascot_1780840450056),
                        contentDescription = "Sekai Chibi Mascot",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(2.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val kaomoji = when {
                            !isConnected -> "(｡•́︿•̀｡)睡" // crying/disconnected sleeping
                            activeTab == 0 -> "(｡- ω -)💤" // cozy sleeping
                            else -> "( ๑>ᴗ<๑ )✨" // excited synchronized!
                        }
                        val characterStatus = when {
                            !isConnected -> "Offline... θ"
                            activeTab == 0 -> "Hehe.. Nyaa~"
                            else -> "Aktif Sinkron!"
                        }
                        Text(
                            text = kaomoji,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = characterStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
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

        SakuraOverlay(
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
                                onDelete = { viewModel.deleteAlarm(alarm) },
                                onEdit = { alarmToEdit = alarm }
                            )
                        }
                    }
                }
            } else if (activeTab == 1) {
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
                        onAddAlarmClick = { showAddDialog = true },
                        onEditAlarmClick = { alarmToEdit = it }
                    )
                }
            } else {
                NotesScreen(
                    notes = notes,
                    onAddNote = { t, c, col -> viewModel.addNote(t, c, col) },
                    onUpdateNote = { note, t, c, col -> viewModel.updateNote(note, t, c, col) },
                    onDeleteNote = { id -> viewModel.deleteNote(id) }
                )
            }

            if (activeTab == 0 || (activeTab == 1 && joinedGroupCode != null)) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                .padding(4.dp)
        ) {
            TabButton(
                title = "⏰ Pribadi",
                isActive = activeTab == 0,
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "👥 Grup",
                isActive = activeTab == 1,
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "📝 Catatan",
                isActive = activeTab == 2,
                onClick = { activeTab = 2 },
                modifier = Modifier.weight(1f)
            )
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
            alarmToEdit = null,
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

    if (alarmToEdit != null) {
        AddAlarmDialog(
            isGroup = alarmToEdit!!.isGroup,
            alarmToEdit = alarmToEdit,
            onDismiss = { alarmToEdit = null },
            onSave = { title, hour, minute, days, tone ->
                viewModel.updateAlarm(alarmToEdit!!, title, hour, minute, days, tone)
                alarmToEdit = null
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
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(300)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isGroup = alarm.isGroup
    val cardBgColor = if (isGroup) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val borderCol = if (isGroup) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val contentColor = if (isGroup) MaterialTheme.colorScheme.onSecondaryContainer else TextLight
    val timeColor = if (isGroup) MaterialTheme.colorScheme.onSecondaryContainer else (if (alarm.isEnabled) IndigoPrimary else TextMuted)
    val subtitleColor = if (isGroup) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f) else TextMuted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    "custom_sakura" -> "🌸 Sakura Dream"
                    "custom_anime" -> "⚡ Shinobi Hot"
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
            .verticalScroll(rememberScrollState())
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

                    if (isNetworkError(it)) {
                        var showOnboardingDnsHelp by remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.12f))
                                .clickable { showOnboardingDnsHelp = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Bantuan DNS",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Terkendala ISP diblokir? Ketuk di sini untuk petunjuk aktifkan DNS Pribadi lewat HP ⚡",
                                color = Color(0xFFFFB300),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (showOnboardingDnsHelp) {
                            PrivateDnsHelpDialog(onDismiss = { showOnboardingDnsHelp = false })
                        }
                    }
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
    onAddAlarmClick: () -> Unit,
    onEditAlarmClick: (Alarm) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "📬 Alarm Kelompok (${alarms.size})",
                color = TextLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (alarms.isEmpty()) {
            item {
                EmptyStatePlaceholder(
                    title = "Grup masih kosong",
                    subtitle = "Klik tombol + di pojok bawah untuk menjadualkan alarm kelompok.",
                    icon = Icons.Default.Person
                )
            }
        } else {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { viewModel.toggleAlarm(alarm) },
                    onDelete = { viewModel.deleteAlarm(alarm) },
                    onEdit = { onEditAlarmClick(alarm) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
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

                    // If background synchronization encounters an ISP block or error, show actionable help banner
                    if (syncState is SyncStatus.Error && isNetworkError((syncState as SyncStatus.Error).error)) {
                        var showDashboardDnsHelp by remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDashboardDnsHelp = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Peringatan Sinkronisasi",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Koneksi Bermasalah: ${(syncState as SyncStatus.Error).error}. Tap untuk cara aktifkan DNS Pribadi Bebas Blokir ⚡",
                                    color = Color(0xFFFFB300),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (showDashboardDnsHelp) {
                            PrivateDnsHelpDialog(onDismiss = { showDashboardDnsHelp = false })
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

                                    val isError = !msg.contains("berhasil")
                                    if (isError && isNetworkError(msg)) {
                                        var showReconnectDnsHelp by remember { mutableStateOf(false) }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFB300).copy(alpha = 0.12f))
                                                .clickable { showReconnectDnsHelp = true }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Bantuan DNS",
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Gagal terhubung? Domain cloud kemungkinan diblokir oleh ISP Anda. Klik untuk panduan solusi DNS Pribadi ⚡",
                                                color = Color(0xFFFFB300),
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        if (showReconnectDnsHelp) {
                                            PrivateDnsHelpDialog(onDismiss = { showReconnectDnsHelp = false })
                                        }
                                    }
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
        }
    }
}

@Composable
fun NotesScreen(
    notes: List<com.example.data.model.Note>,
    onAddNote: (title: String, content: String, colorHex: String) -> Unit,
    onUpdateNote: (note: com.example.data.model.Note, title: String, content: String, colorHex: String) -> Unit,
    onDeleteNote: (id: String) -> Unit
) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<com.example.data.model.Note?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            EmptyStatePlaceholder(
                title = "Belum ada catatan 🌸",
                subtitle = "Catat tugas, ide, atau memo penting di sini. Bisa dipasang sebagai widget di layar HP kamu!",
                icon = Icons.Default.Edit
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 80.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { noteToEdit = note },
                        onDelete = { onDeleteNote(note.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddNoteDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 8.dp),
            containerColor = PinkAccent,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah Catatan",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showAddNoteDialog) {
        AddEditNoteDialog(
            note = null,
            onDismiss = { showAddNoteDialog = false },
            onSave = { title, content, color ->
                onAddNote(title, content, color)
                showAddNoteDialog = false
            }
        )
    }

    if (noteToEdit != null) {
        AddEditNoteDialog(
            note = noteToEdit,
            onDismiss = { noteToEdit = null },
            onSave = { title, content, color ->
                onUpdateNote(noteToEdit!!, title, content, color)
                noteToEdit = null
            }
        )
    }
}

@Composable
fun NoteCard(
    note: com.example.data.model.Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        Color(0xFFFFF0F2)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Tanpa Judul" },
                    color = Color(0xFF3E2723),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Catatan",
                        tint = Color(0xFFD84315),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.content,
                color = Color(0xFF4E342E),
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            val formattedDate = remember(note.lastUpdated) {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(note.lastUpdated))
            }

            Text(
                text = "Diperbarui: $formattedDate",
                color = Color(0xFF8D6E63),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Catatan?", color = TextLight) },
            text = { Text("Apakah kamu yakin ingin menghapus catatan ini?", color = TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Hapus", color = Color.Red, fontWeight = FontWeight.Bold)
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
fun AddEditNoteDialog(
    note: com.example.data.model.Note?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedColor by remember { mutableStateOf(note?.colorHex ?: "#FFF0F2") }

    val colors = listOf(
        "#FFF0F2" to "Sakura",
        "#FFF3E0" to "Peach",
        "#E8F5E9" to "Matcha",
        "#E3F2FD" to "Sky",
        "#F3E5F5" to "Lavender"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
            shape = RoundedCornerShape(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (note == null) "Tulis Catatan Baru 🌸" else "Edit Catatan 📝",
                        color = TextLight,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Catatan", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Detail Catatan", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Text("Pilih Warna Kertas Memo", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { pair ->
                            val c = Color(android.graphics.Color.parseColor(pair.first))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedColor = pair.first }
                                    .border(
                                        width = if (selectedColor == pair.first) 2.dp else 0.dp,
                                        color = if (selectedColor == pair.first) IndigoPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (content.isNotBlank()) {
                                    onSave(title, content, selectedColor)
                                }
                            },
                            enabled = content.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simpan", color = Color.White)
                        }
                    }
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
        Image(
            painter = painterResource(id = R.drawable.img_anime_mascot_1780840450056),
            contentDescription = "Mascot",
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                .padding(2.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "(｡· v ·｡)💤",
            fontSize = 32.sp,
            color = IndigoPrimary.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
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

                // Sakura Animation toggle section
                Text(
                    text = "Suara & Efek Visual Anime",
                    color = IndigoPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDarkElevated)
                        .clickable {
                            val nextVal = !AppThemeState.sakuraEnabled
                            AppThemeState.sakuraEnabled = nextVal
                            prefs.edit().putBoolean("sakura_enabled", nextVal).apply()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Animasi Kelopak Sakura", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Aktifkan efek kelopak bunga berguguran", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = AppThemeState.sakuraEnabled,
                        onCheckedChange = { nextVal ->
                            AppThemeState.sakuraEnabled = nextVal
                            prefs.edit().putBoolean("sakura_enabled", nextVal).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoPrimary,
                            uncheckedThumbColor = Color(0xFF938F99),
                            uncheckedTrackColor = Color(0xFFE7E0EC)
                        )
                    )
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
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
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
                                DropdownMenuItem(
                                    text = {
                                        Text(p.second, color = TextLight, fontSize = 13.sp)
                                    },
                                    onClick = {
                                        selectedTone = p.first
                                        toneMenuExpanded = false
                                    }
                                )
                            }
                            Divider(color = SurfaceDark)
                            DropdownMenuItem(
                                text = {
                                    Text("📁 + Pilih Musik Baru dari HP...", color = PinkAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                },
                                onClick = {
                                    toneMenuExpanded = false
                                    musicPickerLauncher.launch("audio/*")
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
                        Color(0xFF2E1533),
                        Color(0xFF1E0A24),
                        Color(0xFF0C0412)
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
                    .background(if (isGroup) IndigoPrimary else Color(0xFFFF529D))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isGroup) "🌸 ALARM GRUP SINKRON ($groupCode)" else "🌸 PENGINGAT MANDIRI",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ヾ(≧▽≦*)o BANGUNNYAA~! ✨",
                color = IndigoLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
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
                    .background(PinkAccent.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(PinkAccent.copy(alpha = 0.4f))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PinkAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "٩(ˊᗜˋ*)و",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(54.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .width(220.dp)
                .height(48.dp)
        ) {
            Text(
                text = "MATIKAN SEKARANG 🌸",
                color = Color(0xFF311B92),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 12.sp
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

fun isNetworkError(message: String?): Boolean {
    if (message == null) return false
    val m = message.lowercase()
    return m.contains("gagal") || m.contains("jaringan") || m.contains("koneksi") || m.contains("tidak ada koneksi") || m.contains("server") || m.contains("cloud") || m.contains("respon") || m.contains("respons") || m.contains("blokir") || m.contains("timeout") || m.contains("host") || m.contains("connect") || m.contains("failure") || m.contains("terputus")
}

@Composable
fun PrivateDnsHelpDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡ Solusi Bebas Blokir ISP", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Layanan sinkronisasi cloud menggunakan platform kvdb.io yang terkadang disaring atau diblokir oleh beberapa operator internet di Indonesia.\n\n" +
                           "Cara paling praktis dan permanen untuk mengatasinya adalah mengaktifkan DNS Pribadi (Google atau Cloudflare) di HP Anda:",
                    color = TextLight,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Salin Hostname DNS:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                listOf(
                    "dns.google" to "Google DNS (Disarankan)",
                    "1dot1dot1dot1.cloudflare-dns.com" to "Cloudflare DNS"
                ).forEach { (dnsHost, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDarkElevated)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Salin", color = IndigoLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Langkah Pengaturan:\n" +
                           "1. Klik tombol 'Buka Pengaturan DNS HP' di bawah.\n" +
                           "2. Pilih opsi 'DNS Pribadi' (Private DNS).\n" +
                           "3. Pilih 'Nama Host Penyedia DNS Pribadi'.\n" +
                           "4. Tempel hostname yang disalin di atas lalu simpan.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
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
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Buka Pengaturan DNS HP ⚙️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}

data class SakuraPetal(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val sizeDp: Float,
    val speedY: Float,
    val windX: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val alpha: Float
)

@Composable
fun SakuraFallingCanvas(modifier: Modifier = Modifier) {
    val petalCount = 18 // Subtle density to remain elegant and beautiful

    // Pre-allocate the petals to avoid allocations on frames
    val petals = remember {
        val random = java.util.Random(1337)
        List(petalCount) {
            SakuraPetal(
                initialXRatio = random.nextFloat(),
                initialYRatio = random.nextFloat(),
                sizeDp = 6f + random.nextFloat() * 10f, // 6dp to 16dp
                speedY = 0.12f + random.nextFloat() * 0.12f,
                windX = 0.04f + random.nextFloat() * 0.06f,
                swayAmplitude = 12f + random.nextFloat() * 20f,
                swayFrequency = 1f + random.nextFloat() * 1.2f,
                initialRotation = random.nextFloat() * 360f,
                rotationSpeed = 30f + random.nextFloat() * 60f,
                alpha = 0.35f + random.nextFloat() * 0.45f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sakura_infinite")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sakura_progress"
    )

    // A normalized notched petal path (width ~1f, height ~1f)
    val basePetalPath = remember {
        Path().apply {
            moveTo(0f, 0.4f)
            cubicTo(-0.4f, 0.2f, -0.5f, -0.2f, -0.2f, -0.4f)
            lineTo(0f, -0.2f)
            lineTo(0.2f, -0.4f)
            cubicTo(0.5f, -0.2f, 0.4f, 0.2f, 0f, 0.4f)
            close()
        }
    }

    val petalColor = Color(0xFFFFB7C5) // Soft, authentic Sakura pink

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width > 0 && height > 0) {
            petals.forEach { petal ->
                // Calculate dynamic coordinates with screen wrapping
                val rawY = (petal.initialYRatio + (progress * petal.speedY)) % 1.0f
                val paddingY = 40f
                val currentY = rawY * (height + paddingY * 2) - paddingY

                // Swaying horizontal motion
                val sway = kotlin.math.sin(progress * petal.swayFrequency * 2 * kotlin.math.PI.toFloat()) * petal.swayAmplitude
                val rawX = (petal.initialXRatio + (progress * petal.windX)) % 1.0f
                val currentX = (rawX * width + sway) % width

                val currentRotation = petal.initialRotation + (progress * petal.rotationSpeed)
                val petalSizePx = petal.sizeDp.dp.toPx()

                // Rotate, translate, scale & draw path efficiently
                rotate(currentRotation, pivot = Offset(currentX, currentY)) {
                    translate(left = currentX, top = currentY) {
                        drawScopeScale(scaleX = petalSizePx, scaleY = petalSizePx, pivot = Offset.Zero) {
                            drawPath(
                                path = basePetalPath,
                                color = petalColor.copy(alpha = petal.alpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SakuraOverlay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (AppThemeState.sakuraEnabled) {
            SakuraFallingCanvas()
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: AlarmViewModel,
    profilePicTrigger: Boolean,
    onProfilePicChanged: () -> Unit,
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE) }
    val userName by viewModel.userName.collectAsState()
    var textInput by remember { mutableStateOf(userName) }
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
                        imageVector = Icons.Default.ArrowBack,
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
                        val modes = listOf("system" to "Sistem 📱", "light" to "Terang ☀️", "dark" to "Gelap 🌙")
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

            // Sakura Toggle Section
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
                            val nextVal = !AppThemeState.sakuraEnabled
                            AppThemeState.sakuraEnabled = nextVal
                            prefs.edit().putBoolean("sakura_enabled", nextVal).apply()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Animasi Kelopak Sakura 🌸", color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Aktifkan efek kelopak bunga berguguran secara real-time", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = AppThemeState.sakuraEnabled,
                        onCheckedChange = { nextVal ->
                            AppThemeState.sakuraEnabled = nextVal
                            prefs.edit().putBoolean("sakura_enabled", nextVal).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoPrimary
                        )
                    )
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
    }
}



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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
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
import com.example.ui.CameraQrScannerDialog
import com.example.ui.ShowGroupQrDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
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
        
        com.example.alarm.MotivationScheduler.scheduleDailyMotivation(this)

        // AOD disabled entirely per user request
        // val aodPrefs = getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        // if (aodPrefs.getBoolean("aod_enabled", false)) { ... }

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
                val savedMode = prefs.getString("theme_mode", "light") ?: "light"
                AppThemeState.themeMode = if (savedMode == "dark") "dark" else "light"
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
                                painter = painterResource(id = R.drawable.bg_cat_pattern),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Inside,
                                alpha = if (isSystemInDarkTheme()) 0.05f else 0.12f
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
                                    } else if (screen == "about") {
                                        com.example.ui.AboutScreen(
                                            onBack = { currentScreen = "settings" }
                                        )
                                    } else {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            profilePicTrigger = profilePicTrigger,
                                            onProfilePicChanged = { profilePicTrigger = !profilePicTrigger },
                                            onBack = { currentScreen = "home" },
                                            onNavigateToAbout = { currentScreen = "about" },
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
                            },
                            onSnooze = {
                                val snoozeIntent = Intent(this@MainActivity, AlarmRingingService::class.java).apply {
                                    action = "SNOOZE"
                                }
                                this@MainActivity.startService(snoozeIntent)
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
    val isLockedSchedule = remember { mutableStateOf<com.example.ui.HealthSchedule?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            isLockedSchedule.value = com.example.ui.isAppCurrentlyLocked(context)
            kotlinx.coroutines.delay(5000)
        }
    }
    val prefs = remember { context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE) }
    val isConnected by NetworkConnectionHelper.observeConnection(context).collectAsState(initial = NetworkConnectionHelper.isConnected(context))
    val networkType = remember(isConnected) { NetworkConnectionHelper.getNetworkType(context) }
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
    var showUserNameDialog by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<GithubUpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            isCheckingUpdate = true
            try {
                val info = GithubUpdateChecker.checkForUpdates()
                updateInfoState = info
                if (info != null && info.hasUpdate) {
                    showUpdateDialog = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // --- Notification Bell Icon for Updates ---
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showUpdateDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Pemberitahuan Update",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (updateInfoState?.hasUpdate == true) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }
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
        }

        // --- Elegant Dialog for update notifications ---
        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pembaruan Aplikasi 🔔", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mengecek pembaruan aplikasi...",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            val info = updateInfoState
                            if (info != null) {
                                if (info.hasUpdate) {
                                    Text(
                                        text = "Versi baru tersedia! 🚀",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Versi Terbaru: ${info.latestVersion}",
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextLight,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (info.releaseNotes.isNotEmpty()) {
                                        Text(
                                            text = "Catatan Pembaruan:",
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = info.releaseNotes,
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 5,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    Text(
                                        text = "Klik tombol di bawah ini untuk mengunduh rilis berkas APK melalui browser Anda secara instan.",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                } else if (info.errorMessage != null) {
                                    Text(
                                        text = "Koneksi Bermasalah",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = info.errorMessage,
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text(
                                        text = "Aplikasi Anda Sudah Terupdate! ✨",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Anda menggunakan versi ${com.example.BuildConfig.VERSION_NAME} (code: ${com.example.BuildConfig.VERSION_CODE}) yang merupakan versi stabil terbaru.",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "Gagal memuat status pembaruan. Silakan klik tombol periksa ulang di bawah.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    val info = updateInfoState
                    if (info?.hasUpdate == true) {
                        Button(
                            onClick = {
                                showUpdateDialog = false
                                GithubUpdateChecker.openUpdateUrl(context, info.downloadUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Perbarui Sekarang 🚀", color = Color.White)
                        }
                    } else if (!isCheckingUpdate) {
                        Button(
                            onClick = {
                                isCheckingUpdate = true
                                coroutineScope.launch {
                                    try {
                                        updateInfoState = GithubUpdateChecker.checkForUpdates()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isCheckingUpdate = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Periksa Ulang 🔄", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Tutup", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // --- DASHBOARD WIDGETS ---
        com.example.ui.MorningWeatherWidget()

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

                // Feline Emoticon reactive companion with custom generated mascot image
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    val catMascotRes = when {
                        !isConnected -> R.drawable.img_cat_sleeping_1782296435114
                        activeTab == 0 -> R.drawable.img_cat_yawn_1782296451744
                        else -> R.drawable.img_cat_happy_1782296418426
                    }
                    Image(
                        painter = painterResource(id = catMascotRes),
                        contentDescription = "Feline Mascot Companion",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(2.dp),
                        contentScale = ContentScale.Fit
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
            } else if (activeTab == 2) {
                NotesScreen(
                    notes = notes,
                    onAddNote = { t, c, col -> viewModel.addNote(t, c, col) },
                    onUpdateNote = { note, t, c, col -> viewModel.updateNote(note, t, c, col) },
                    onDeleteNote = { id -> viewModel.deleteNote(id) }
                )
            } else if (activeTab == 3) {
                FileSharingScreenContent(
                    viewModel = viewModel
                )
            } else if (activeTab == 4) {
                com.example.ui.HealthSocialScreen(context = context)
            } else {
                com.example.ui.AodSettingsScreen(context = context)
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
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(
                title = "⏰ Diri",
                isActive = activeTab == 0,
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "👥 Grup",
                isActive = activeTab == 1,
                onClick = { 
                    activeTab = 1 
                    viewModel.forceSyncGroupAndCouple()
                },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "📝 Memo",
                isActive = activeTab == 2,
                onClick = { activeTab = 2 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "📁 Share",
                isActive = activeTab == 3,
                onClick = { activeTab = 3 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "❤️ Health",
                isActive = activeTab == 4,
                onClick = { activeTab = 4 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "📱 Layar",
                isActive = activeTab == 5,
                onClick = { activeTab = 5 },
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

    // --- App Lock Screen Overlay ---
    val currentLocked = isLockedSchedule.value
    if (currentLocked != null) {
        var showBypassPinDialog by remember { mutableStateOf(false) }
        var isPinEnabledLocal by remember {
            mutableStateOf(
                context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
                    .getBoolean("pin_enabled", false)
            )
        }
        var actualPinLocal by remember {
            val encrypted = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
                .getString("pin_code", "") ?: ""
            mutableStateOf(if (encrypted.isNotEmpty()) com.example.ui.SecurePinStorage.decryptPin(encrypted) else "")
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(enabled = false) {}, // absorb all touches
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentLocked.icon,
                    fontSize = 72.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Aplikasi Terkunci",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Jadwal ${currentLocked.name} sedang berjalan\n(${currentLocked.startTime} - ${currentLocked.endTime})",
                    color = Color(0xFFFF4081),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Text(
                    text = "Tetap fokus dan disiplin dengan komitmen kesehatan Anda. Selesaikan aktivitas Anda terlebih dahulu! ✨",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                if (isPinEnabledLocal) {
                    Button(
                        onClick = { showBypassPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Buka Sementara dengan PIN 🔑", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            com.example.ui.setTemporaryBypass(context, 15)
                            isLockedSchedule.value = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Buka Sementara (15 Menit) 🔓", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        if (showBypassPinDialog) {
            com.example.ui.PinInputDialog(
                mode = "VERIFY",
                actualPin = actualPinLocal,
                onDismiss = { showBypassPinDialog = false },
                onSuccess = {
                    com.example.ui.setTemporaryBypass(context, 15)
                    isLockedSchedule.value = null
                    showBypassPinDialog = false
                }
            )
        }
    }
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
            .padding(vertical = 10.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = contentColor,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
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

@OptIn(ExperimentalPermissionsApi::class)
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
                    var showCameraScanner by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            modifier = Modifier.weight(1f)
                        )

                        FilledIconButton(
                            onClick = { showCameraScanner = true },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Scan QR Code",
                                tint = Color.White
                            )
                        }
                    }

                    if (showCameraScanner) {
                        CameraQrScannerDialog(
                            onDismiss = { showCameraScanner = false },
                            onQrScanned = { result ->
                                showCameraScanner = false
                                errorMessage = null
                                
                                val parts = result.split("|")
                                if (parts.size != 2) {
                                    errorMessage = "QR Code tidak valid! Hanya gunakan QR Code dari aplikasi ini."
                                    return@CameraQrScannerDialog
                                }
                                val code = parts[0]
                                val timestampStr = parts[1]
                                val timestamp = timestampStr.toLongOrNull()
                                if (timestamp == null) {
                                    errorMessage = "Format QR Code tidak dikenali."
                                    return@CameraQrScannerDialog
                                }
                                val now = System.currentTimeMillis()
                                val tenMinutesInMs = 10 * 60 * 1000L
                                if (now - timestamp > tenMinutesInMs) {
                                    errorMessage = "QR Code sudah kedaluwarsa setelah 10 menit demi keamanan!"
                                    return@CameraQrScannerDialog
                                }
                                if (now < timestamp - 60000L) {
                                    errorMessage = "Perangkat Anda tidak sinkron dengan pembuat QR!"
                                    return@CameraQrScannerDialog
                                }

                                // Auto-fill code field
                                groupCodeInput = code

                                // Auto-join room!
                                viewModel.joinGroupViaQr(result) { success, error ->
                                    if (!success) {
                                        errorMessage = error ?: "Gagal bergabung lewat QR Code."
                                    }
                                }
                            }
                        )
                    }
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
                color = IndigoPrimary,
                fontSize = 13.sp,
                textDecoration = TextDecoration.Underline
            )
        }

        if (syncState is SyncStatus.Syncing) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(color = IndigoPrimary)
        }
    }
}

@Composable
fun MemberAvatar(
    member: com.example.data.model.MemberData,
    fallbackText: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(member.colorHex ?: "#FFB7B2"))),
        contentAlignment = Alignment.Center
    ) {
        val pBitmap = remember(member.profileImageBase64) {
            if (member.profileImageBase64 != null) {
                try {
                    val bytes = android.util.Base64.decode(member.profileImageBase64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        if (pBitmap != null) {
            Image(
                bitmap = pBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = fallbackText.take(2).uppercase(),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
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
    val members by viewModel.groupMembers.collectAsState()
    val myUid by viewModel.userId.collectAsState()
    val myName by viewModel.userName.collectAsState()
    val awakeStatuses by viewModel.awakeStatuses.collectAsState()
    var showMemberWakeDialog by remember { mutableStateOf(false) }
    var showExitChoiceDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showCoupleSimulation by remember { mutableStateOf(false) }
    var pendingAcceptRequest by remember { mutableStateOf<com.example.data.model.CouplePair?>(null) }
    var pendingAcceptPartnerName by remember { mutableStateOf("") }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isOfflineGroup) {
                                var isSyncingByHand by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        isSyncingByHand = true
                                        viewModel.forceSyncGroupAndCouple()
                                        android.widget.Toast.makeText(context, "Menyinkronkan data grup & anggota...", android.widget.Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            kotlinx.coroutines.delay(1000)
                                            isSyncingByHand = false
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sinkronkan Instan",
                                        tint = CyanAccent
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(
                                onClick = {
                                    showExitChoiceDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Keluar Grup",
                                    tint = Color(0xFFFF5252) // Bright red for better visibility
                                )
                            }
                        }
                    }

                    if (members.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(IndigoPrimary.copy(alpha = 0.1f))
                                .clickable { showMemberWakeDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((-6).dp)
                            ) {
                                members.take(10).forEach { member ->
                                    val status = awakeStatuses.find { it.userId == member.userId }
                                    val fallback = status?.nickname ?: member.userId
                                    MemberAvatar(
                                        member = member,
                                        fallbackText = fallback,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        fontSize = 11.sp
                                    )
                                }
                                if (members.size > 10) {
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = "+${members.size - 10}",
                                        color = CyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = "${members.size} Anggota Kamar",
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bangunkan 🔔",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Detail Anggota",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Display a beautiful helper text for inviting other members
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Menunggu anggota lain bergabung dengan kode: $code...",
                                color = TextMuted,
                                fontSize = 11.sp
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { showQrDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = CyanAccent),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Share via QR",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bagikan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
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



        item {
            Spacer(modifier = Modifier.height(10.dp))
            val awakeStatuses by viewModel.awakeStatuses.collectAsState()
            val activeCouple by viewModel.activeCouplePair.collectAsState()
            val pendingCoupleRequests by viewModel.pendingCoupleRequests.collectAsState()
            val myStatusObj = awakeStatuses.find { it.userId == myUid }
            val isMeAwake = myStatusObj?.isAwake ?: false
            val myCurrentNickname = myStatusObj?.nickname ?: ""

            val context = LocalContext.current
            val wakePrefs = remember { context.getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE) }
            val savedNickname = remember { wakePrefs.getString("wake_nickname", "") ?: "" }

            var localNickname by remember(myCurrentNickname) { 
                mutableStateOf(if (myCurrentNickname.isNotBlank()) myCurrentNickname else if (savedNickname.isNotBlank()) savedNickname else "") 
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "☀️ Status Bangun Kamar",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ROOM ${code}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔒 PRIVASI DIUTAMAKAN: Hanya anggota kamar ini yang dapat memantau status Anda. Tidak melacak lokasi/GPS. Seluruh data status otomatis dibersihkan total setelah 24 jam.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Nickname Samaran (Anonim)
                    OutlinedTextField(
                        value = localNickname,
                        onValueChange = { 
                            localNickname = it
                            wakePrefs.edit().putString("wake_nickname", it).apply()
                        },
                        label = { Text("Nama Panggilan / Samaran Anda", fontSize = 12.sp) },
                        placeholder = { Text("Contoh: Kawan Bobo (Kosongkan jika mau pake nama asli)", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = TextMuted
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Toggle Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Status Anda saat ini:", color = TextMuted, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isMeAwake) Color(0xFF2E7D32) else Color(0xFFC62828))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMeAwake) "SUDAH BANGUN 🙋‍♂️" else "BELUM BANGUN 😴",
                                    color = if (isMeAwake) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val finalNick = localNickname.trim().ifBlank { 
                                    val fallbackName = wakePrefs.getString("user_name", "") ?: ""
                                    if (fallbackName.isNotBlank()) fallbackName else "Anggota-${myUid}"
                                }
                                viewModel.updateMyAwakeStatus(!isMeAwake, finalNick) { success ->
                                    if (success) {
                                        wakePrefs.edit().putString("wake_nickname", localNickname).apply()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMeAwake) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isMeAwake) "Set Belum Bangun 😴" else "SAYA SUDAH BANGUN! ☀️",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // ======================= COUPLE SYNC MODE OPTIONAL LAYER =======================
                    if (activeCouple != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF1F2))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💕", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Couple Sync Mode Terhubung",
                                        color = Color(0xFF9F1239),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                
                                IconButton(
                                    onClick = { viewModel.unpair {} },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("💔", fontSize = 14.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isSelfA = activeCouple!!.partnerA == myUid
                                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                
                                val nameA = if (isSelfA) "${activeCouple!!.partnerAName} (Anda)" else activeCouple!!.partnerAName
                                val scoreA = activeCouple!!.scoreA
                                val streakA = activeCouple!!.streakA
                                val isAwakeA = activeCouple!!.lastWakeA == todayStr
                                
                                val nameB = if (!isSelfA) "${activeCouple!!.partnerBName} (Anda)" else activeCouple!!.partnerBName
                                val scoreB = activeCouple!!.scoreB
                                val streakB = activeCouple!!.streakB
                                val isAwakeB = activeCouple!!.lastWakeB == todayStr

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = nameA, color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = if (isAwakeA) "☀️ Bangun" else "💤 Tidur", color = Color(0xFF374151), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Poin: $scoreA | Streak: $streakA 🔥", color = Color(0xFF0284C7), fontSize = 9.sp)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF1F2))
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = nameB, color = Color(0xFF881337), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = if (isAwakeB) "☀️ Bangun" else "💤 Tidur", color = Color(0xFF374151), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Poin: $scoreB | Streak: $streakB 🔥", color = Color(0xFFDB2777), fontSize = 9.sp)
                                }
                            }
                            
                            if (activeCouple!!.syncBonusToday) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⚡ Sync Bonus Hari Ini Aktif! Kedua pasangan bangun selisih <10 m (+15 poin)!",
                                        color = Color(0xFFB45309),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        // SIMULATION CORNER FOR UNCOUPLED USERS
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFF7F8))
                                .border(1.dp, Color(0xFFBEE1E6), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (!showCoupleSimulation) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "💕 Penasaran Fitur Couple Sync?",
                                            color = Color(0xFF205E6A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Membangunkan pasangan secara real-time & kumpulkan streak poin bersama!",
                                            color = Color(0xFF438F9E),
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showCoupleSimulation = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF205E6A)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    ) {
                                        Text("Simulasi ⚡", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Simulation Is Active
                                var simMeAwake by remember { mutableStateOf(false) }
                                var simPartnerAwake by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💕", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Simulasi Couple Sync Mode",
                                            color = Color(0xFF205E6A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF205E6A))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("SIMULASI", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(
                                        onClick = { showCoupleSimulation = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("❌", fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEFF6FF))
                                            .clickable { simMeAwake = !simMeAwake }
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "Leon (Anda)", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = if (simMeAwake) "☀️ Bangun" else "💤 Tidur", color = Color(0xFF374151), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "Poin: 105 | Streak: 7 🔥", color = Color(0xFF0284C7), fontSize = 9.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "(Klik ubah status)", color = Color.Gray, fontSize = 8.sp)
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFF1F2))
                                            .clickable { simPartnerAwake = !simPartnerAwake }
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "Mia", color = Color(0xFF881337), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = if (simPartnerAwake) "☀️ Bangun" else "💤 Tidur", color = Color(0xFF374151), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "Poin: 90 | Streak: 5 🔥", color = Color(0xFFDB2777), fontSize = 9.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "(Klik ubah status)", color = Color.Gray, fontSize = 8.sp)
                                    }
                                }

                                if (simMeAwake && simPartnerAwake) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⚡ Sync Bonus Hari Ini Aktif! Kedua pasangan bangun selisih <10 m (+15 poin)!",
                                            color = Color(0xFFB45309),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 Kedua pasangan harus mengaktifkan Couple Sync di grup agar poin tersinkron secara real-time.",
                                    color = Color(0xFF438F9E),
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { showCoupleSimulation = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB14A5B)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Sembunyikan Simulasi", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Display pending couple invitations directed to me
                    val myReceivedRequests = pendingCoupleRequests.filter { it.partnerB == myUid }
                    if (myReceivedRequests.isNotEmpty() && activeCouple == null) {
                        myReceivedRequests.forEach { req ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF9C3))
                                    .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "💕 Undangan Pasangan",
                                    color = Color(0xFF713F12),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${req.partnerAName} ingin menyandingkan alarm berdua dengan Anda (Couple Sync) di grup ini.",
                                    color = Color(0xFF854D0E),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            pendingAcceptRequest = req
                                            pendingAcceptPartnerName = req.partnerAName
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("Terima 💕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.rejectOrCancelPairRequest(req) {} },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("Tolak ❌", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Display pending couple requests sent by me
                    val mySentRequests = pendingCoupleRequests.filter { it.partnerA == myUid }
                    if (mySentRequests.isNotEmpty() && activeCouple == null) {
                        mySentRequests.forEach { req ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💕 Menunggu Balasan...",
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    TextButton(
                                        onClick = { viewModel.rejectOrCancelPairRequest(req) {} },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Batalkan ❌", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Menunggu persetujuan dari ${req.partnerBName} untuk mengaktifkan Couple Sync.",
                                    color = Color(0xFF475569),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val displayMembers = if (isOfflineGroup && members.isEmpty()) {
                        listOf(
                            com.example.data.model.MemberData(userId = myUid, colorHex = "#FFFFFF", lastActive = System.currentTimeMillis(), profileImageBase64 = null),
                            com.example.data.model.MemberData(userId = "offline_partner", colorHex = "#FFDAC1", lastActive = System.currentTimeMillis(), profileImageBase64 = null)
                        )
                    } else {
                        members
                    }
                    val displayStatuses = if (isOfflineGroup && awakeStatuses.isEmpty()) {
                        listOf(
                            com.example.data.model.AwakeStatus(userId = myUid, isAwake = false, timestamp = System.currentTimeMillis(), nickname = myName),
                            com.example.data.model.AwakeStatus(userId = "offline_partner", isAwake = false, timestamp = System.currentTimeMillis(), nickname = "Pasangan Offline")
                        )
                    } else {
                        awakeStatuses
                    }

                    if (displayMembers.isEmpty()) {
                        Text(
                            text = "Belum ada anggota di grup ini.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            displayMembers.forEach { member ->
                                val status = displayStatuses.find { it.userId == member.userId }
                                val isAwake = status?.isAwake == true
                                val nickname = status?.nickname ?: "Anggota"
                                val isMe = member.userId == myUid
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isMe) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${nickname}${if (isMe) " (Anda)" else ""}",
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )

                                        if (!isMe) {
                                            Row(
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val hasActiveC = activeCouple != null
                                                val reqSent = pendingCoupleRequests.any { it.partnerA == myUid && it.partnerB == member.userId }
                                                val reqRecv = pendingCoupleRequests.any { it.partnerB == myUid && it.partnerA == member.userId }
                                                val isMyP = activeCouple != null && (activeCouple!!.partnerA == member.userId || activeCouple!!.partnerB == member.userId)

                                                if (isCreator) {
                                                    // Kick button for owner
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFFFEBEE))
                                                            .clickable {
                                                                viewModel.kickMember(member.userId) {}
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Out ❌", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                
                                                if (isMyP) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFFFF1F2))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("💕 Pasangan", color = Color(0xFFE11D48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else if (reqSent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFF1F5F9))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("💕 Dipinta", color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else if (reqRecv) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFFEF9C3))
                                                            .clickable {
                                                                val req = pendingCoupleRequests.find { it.partnerB == myUid && it.partnerA == member.userId }
                                                                if (req != null) {
                                                                    pendingAcceptRequest = req
                                                                    pendingAcceptPartnerName = nickname
                                                                }
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("💕 Terima?", color = Color(0xFF854D0E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else if (!hasActiveC && isAwake) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFECEFF1))
                                                            .clickable {
                                                                viewModel.sendPairRequest(member.userId, nickname) {}
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("💕 Pair", color = Color(0xFF607D8B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        item {
            Spacer(modifier = Modifier.height(10.dp))
            AwakeStatusControlCard(viewModel = viewModel, code = code)
        }
    }

    if (pendingAcceptRequest != null) {
        AlertDialog(
            onDismissRequest = { pendingAcceptRequest = null },
            title = { Text("Konfirmasi Pasangan Sekamar", fontWeight = FontWeight.Bold, color = TextLight) },
            text = { Text("Apakah Anda yakin ingin berpasangan dengan $pendingAcceptPartnerName? Mode pasangan akan menyinkronkan alarm kamar secara real-time.", color = TextLight) },
            confirmButton = {
                Button(
                    onClick = {
                        val reqToAccept = pendingAcceptRequest
                        if (reqToAccept != null) {
                            viewModel.acceptPairRequest(reqToAccept) {}
                        }
                        pendingAcceptRequest = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Terima", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingAcceptRequest = null }
                ) {
                    Text("Batal", color = TextMuted)
                }
            }
        )
    }

    if (showExitChoiceDialog) {
        val showFullOptions = isCreator && !isOfflineGroup
        AlertDialog(
            onDismissRequest = { showExitChoiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showFullOptions) Icons.Default.Settings else Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFBA1A1A),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showFullOptions) "Keluar atau Tutup Kamar?" else "Keluar dari Kamar?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextLight)
                }
            },
            text = {
                if (showFullOptions) {
                    Text(
                        text = "Anda adalah pembuat kamar alarm ini. Anda dapat memilih:\n\n" +
                                "1. ⚠️ TUTUP KAMAR & HAPUS CHAT: Menghapus seluruh data kamar alarm, seluruh member, dan semua riwayat obrolan secara permanen dan aman dari server cloud.\n\n" +
                                "2. 🚶 KELUAR KAMAR SAJA: Keluar dari kamar tanpa menghapus data server cloud.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                } else {
                    Text(
                        text = "Apakah Anda yakin ingin keluar dari kamar alarm ini? Anda tidak akan menerima alarm grup lagi.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                if (showFullOptions) {
                    Button(
                        onClick = {
                            showExitChoiceDialog = false
                            viewModel.closeGroupAndCleanChat()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                    ) {
                        Text("Tutup Kamar & Hapus Chat 🔒", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            showExitChoiceDialog = false
                            viewModel.leaveGroup()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                    ) {
                        Text("Keluar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                Row {
                    if (showFullOptions) {
                        TextButton(
                            onClick = {
                                showExitChoiceDialog = false
                                viewModel.leaveGroup()
                            }
                        ) {
                            Text("Keluar Kamar Saja", fontSize = 11.sp)
                        }
                    }
                    TextButton(
                        onClick = { showExitChoiceDialog = false }
                    ) {
                        Text("Batal", fontSize = if (showFullOptions) 11.sp else 12.sp, color = TextMuted)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showMemberWakeDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showMemberWakeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kelola & Bangunkan Kamar 📢⏰", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextLight)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    Text(
                        text = "Kirim sinyal getar & peringatan alarm instan ke handphone anggota kamar grup terpilih sekarang juga!",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (members.isEmpty()) {
                        Text(
                            text = "Tidak ada anggota lain yang aktif saat ini.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(members) { member ->
                                val isMe = member.userId == myUid
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val status = awakeStatuses.find { it.userId == member.userId }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val fallback = status?.nickname ?: member.userId
                                            MemberAvatar(
                                                member = member,
                                                fallbackText = fallback,
                                                modifier = Modifier.size(32.dp),
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = member.userId,
                                                    color = TextLight,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                val nowMs = System.currentTimeMillis()
                                                val daysInactive = if (member.lastActive > 0) (nowMs - member.lastActive) / (1000 * 60 * 60 * 24) else 0L
                                                var activeText = if (isMe) "Kamu (Aktif)" else {
                                                    when {
                                                        member.lastActive == 0L -> "Status tidak diketahui"
                                                        daysInactive >= 30 -> "Mungkin uninstalled (>30 hari)"
                                                        daysInactive >= 7 -> "Mungkin uninstalled (>7 hari)"
                                                        daysInactive >= 1 -> "Tidak aktif $daysInactive hari"
                                                        else -> "Aktif baru saja"
                                                    }
                                                }
                                                if (member.batteryLevel != null) {
                                                    val batteryIcon = if (member.batteryLevel > 20) "🔋" else "🪫"
                                                    activeText += " • $batteryIcon ${member.batteryLevel}%"
                                                }
                                                val activeColor = if (isMe) IndigoPrimary else if (daysInactive >= 7) Color.Red else TextMuted
                                                Text(
                                                    text = activeText,
                                                    color = activeColor,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        if (!isMe) {
                                            var isWaking by remember { mutableStateOf(false) }
                                            val (allowed, msg) = viewModel.checkWakeCooldown(member.userId)
                                            Column(horizontalAlignment = Alignment.End) {
                                                Button(
                                                    onClick = {
                                                        isWaking = true
                                                        viewModel.wakeUpMember(member.userId) { success ->
                                                            isWaking = false
                                                            if (success) {
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Berhasil mengirim alarm sinyal ke ${member.userId}!",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Gagal, sedang cooldown atau masalah jaringan.",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (allowed) IndigoPrimary else Color.Gray),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    enabled = !isWaking && allowed
                                                ) {
                                                    if (isWaking) {
                                                        CircularProgressIndicator(
                                                            color = Color.White,
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        Text("Bangunkan 🔔", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                if (!allowed && msg.isNotEmpty()) {
                                                    Text(text = msg, fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMemberWakeDialog = false }) {
                    Text("Selesai", color = IndigoPrimary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    if (showQrDialog) {
        ShowGroupQrDialog(groupCode = code, onDismiss = { showQrDialog = false })
    }

    if (!isOfflineGroup) {
        val chatMessages by viewModel.chatMessages.collectAsState()
        FloatingActionButton(
            onClick = { showChatDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp, start = 8.dp),
            containerColor = IndigoPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Buka Chat Kamar",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chat (${chatMessages.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    }

    if (showChatDialog) {
        GroupChatDialog(
            viewModel = viewModel,
            onDismiss = { showChatDialog = false },
            onNavigateToCouple = {
                showChatDialog = false
                scope.launch {
                    try {
                        listState.animateScrollToItem(4)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}

@Composable
fun AwakeStatusControlCard(viewModel: com.example.ui.AlarmViewModel, code: String) {
    val awakeStatuses by viewModel.awakeStatuses.collectAsState()
    val myUid by viewModel.userId.collectAsState()
    val currentUserName by viewModel.userName.collectAsState()

    val myStatus = awakeStatuses.find { it.userId == myUid }
    val myIsAwake = myStatus?.isAwake ?: false
    val currentNickname = myStatus?.nickname ?: currentUserName

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status Sudah Bangun ⏰",
                    color = TextLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Compact Segmented Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val context = LocalContext.current
                
                // Option: Awake
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (myIsAwake) Color(0xFF2E7D32) else Color.Transparent)
                        .clickable {
                            val finalNick = currentNickname.ifBlank { "Anonim" }
                            viewModel.updateMyAwakeStatus(isAwake = true, nickname = finalNick) { success ->
                                if (success) {
                                    android.widget.Toast.makeText(context, "Status diperbarui: Sudah Bangun! ☀️", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("☀️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sudah Bangun",
                            color = if (myIsAwake) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Option: Sleeping
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!myIsAwake && myStatus != null) Color(0xFFBA1A1A) else Color.Transparent)
                        .clickable {
                            val finalNick = currentNickname.ifBlank { "Anonim" }
                            viewModel.updateMyAwakeStatus(isAwake = false, nickname = finalNick) { success ->
                                if (success) {
                                    android.widget.Toast.makeText(context, "Status diperbarui: Masih Tidur 💤", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("💤", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Masih Tidur",
                            color = if (!myIsAwake && myStatus != null) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val otherStatuses = awakeStatuses.filter { it.userId != myUid }
            
            if (awakeStatuses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Kehadiran Anggota Kamar",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndigoPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${awakeStatuses.count { it.isAwake }} / ${awakeStatuses.size} Terjaga",
                            color = IndigoPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    if (myStatus != null) {
                        item {
                            AwakeMemberStatusRow(status = myStatus, isMe = true)
                        }
                    }
                    items(otherStatuses, key = { it.userId }) { status ->
                        AwakeMemberStatusRow(status = status, isMe = false)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupChatDialog(
    viewModel: com.example.ui.AlarmViewModel,
    onDismiss: () -> Unit,
    onNavigateToCouple: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? android.app.Activity }
    
    // REQUIREMENT: Screenshot/Screen recording protection
    // NOTE: FLAG_SECURE is disabled here to prevent the web stream preview in AI Studio from going completely black.
    // In production, please uncomment this safety flag to secure against screenshots/mirroring.
    DisposableEffect(activity) {
        // activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            // activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val chatMessages by viewModel.chatMessages.collectAsState()
    val myUid by viewModel.userId.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cute mascot illustration from sent image
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_fg_new_1781280494329),
                            contentDescription = "Mascot Meong",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "💬 Chat Keamanan Kamar",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Hanya member • Anti-Screen/Export 🔒",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Prominent banner for Couple Sync Mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable {
                            onNavigateToCouple()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💕", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Hubungkan Pasangan (Couple Sync)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9F1239)
                                )
                                Text(
                                    text = "Sinkronisasi bel alarm real-time berdua sekamar",
                                    fontSize = 10.sp,
                                    color = Color(0xFFBE123C)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Atur",
                            tint = Color(0xFF9F1239),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Chat Messages Container (Nested bounded scroll)
                val scrollState = rememberScrollState()
                LaunchedEffect(chatMessages.size) {
                    if (chatMessages.isNotEmpty()) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    if (chatMessages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Belum ada pesan.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sapa anggota kamar kamu di sini!",
                                color = TextMuted.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chatMessages.forEach { msg ->
                                val isMe = msg.senderId == myUid
                                val alignment = if (isMe) Alignment.End else Alignment.Start
                                val bubbleColor = if (isMe) Color(0xFF6750A4) else Color(0xFFE8E0E9).copy(alpha = 0.85f)
                                val textColor = if (isMe) Color.White else Color(0xFF1D1B20)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = alignment
                                ) {
                                    // Sender name & time indicator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isMe) "Anda" else msg.senderNickname,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        // Timestamp formatted simply
                                        val timeStr = remember(msg.timestamp) {
                                            val cal = Calendar.getInstance()
                                            cal.timeInMillis = msg.timestamp
                                            val hours = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
                                            val mins = String.format("%02d", cal.get(Calendar.MINUTE))
                                            "$hours:$mins"
                                        }
                                        Text(
                                            text = timeStr,
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    }

                                    // Message Bubble
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isMe) 12.dp else 2.dp,
                                                    bottomEnd = if (isMe) 2.dp else 12.dp
                                                )
                                            )
                                            .background(bubbleColor)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = msg.messageText,
                                            color = textColor,
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            maxLines = 8,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text Input and Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { 
                            if (it.length <= 150) {
                                messageText = it 
                            }
                        },
                        placeholder = { Text("Ketik pesan...", fontSize = 12.sp) },
                        supportingText = {
                            Text(
                                text = "${messageText.length}/150",
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        maxLines = 2,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = TextLight),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    IconButton(
                        onClick = {
                            if (messageText.trim().isNotEmpty()) {
                                isSending = true
                                viewModel.sendChatMessage(messageText) { success, error ->
                                    isSending = false
                                    if (success) {
                                        messageText = ""
                                    } else if (error != null) {
                                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSending && messageText.trim().isNotEmpty(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = com.example.ui.theme.IndigoPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.12f),
                            disabledContentColor = Color.White.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Kirim",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AwakeMemberStatusRow(status: com.example.data.model.AwakeStatus, isMe: Boolean) {
    val durationText = remember(status.timestamp) {
        val diff = System.currentTimeMillis() - status.timestamp
        val minutes = diff / (60 * 1000L)
        when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "${minutes}m lalu"
            else -> {
                val hours = minutes / 60
                if (hours < 24) "${hours}j lalu" else "1h+"
            }
        }
    }
    
    val nickname = status.nickname.ifBlank { "Anonim" }
    
    val bgColor = remember(nickname) {
        val colors = listOf(
            Color(0xFFE8EAF6), Color(0xFFE1F5FE), Color(0xFFE8F5E9),
            Color(0xFFFFF3E0), Color(0xFFFFEBEE), Color(0xFFF3E5F5)
        )
        colors[Math.abs(nickname.hashCode()) % colors.size]
    }
    
    val textColor = remember(nickname) {
        val colors = listOf(
            Color(0xFF3F51B5), Color(0xFF0288D1), Color(0xFF2E7D32),
            Color(0xFFE65100), Color(0xFFC2185B), Color(0xFF8E24AA)
        )
        colors[Math.abs(nickname.hashCode()) % colors.size]
    }

    val initials = nickname.take(2).uppercase()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(
                        width = 2.dp,
                        color = if (status.isAwake) Color(0xFF4CAF50) else Color(0xFFE57373),
                        shape = CircleShape
                    )
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Emoji overlay badge
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (status.isAwake) Color(0xFF4CAF50) else Color(0xFFE57373))
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (status.isAwake) "☀️" else "💤",
                    fontSize = 9.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = if (isMe) "Anda" else nickname,
            color = TextLight,
            fontSize = 11.sp,
            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = durationText,
            color = TextMuted,
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
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
fun RingingOverlay(
    title: String,
    isGroup: Boolean,
    groupCode: String?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
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

    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val threshold = -300f // 300px threshold for swipe up

    val swipeSnoozeText = when (Locale.getDefault().language) {
        "zh" -> "向上滑动延迟10分钟 🛌"
        "id" -> "Geser ke Atas untuk Tunda 10 Menit 🛌"
        "es" -> "Desliza hacia arriba para posponer 10 min 🛌"
        "pt" -> "Deslize para cima para adiar 10 min 🛌"
        "fr" -> "Glissez vers le haut pour répéter 10 min 🛌"
        "de" -> "Nach oben wischen für Schlummern 10 Min 🛌"
        "ru" -> "Проведите вверх, чтобы отложить на 10 мин 🛌"
        "ja" -> "上にスワイプして10分スヌーズ 🛌"
        "ar" -> "اسحب لأعلى للتأجيل 10 دقائق 🛌"
        else -> "Swipe Up to Snooze 10 Mins 🛌"
    }

    val dismissText = when (Locale.getDefault().language) {
        "zh" -> "关闭闹钟 🌸"
        "id" -> "MATIKAN SEKARANG 🌸"
        "es" -> "APAGAR AHORA 🌸"
        "pt" -> "DESLIGAR AGORA 🌸"
        "fr" -> "ÉTEINDRE MAINTENANT 🌸"
        "de" -> "JETZT AUSSCHALTEN 🌸"
        "ru" -> "ВЫКЛЮЧИТЬ СЕЙЧАС 🌸"
        "ja" -> "今すぐ停止 🌸"
        "ar" -> "إيقاف الآن 🌸"
        else -> "DISMISS ALARM 🌸"
    }

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
                text = "bangunn sayanggg Miaw~ ✨",
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

        Spacer(modifier = Modifier.height(48.dp))

        // Premium Swipe Up to Snooze Area
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(100.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF23113D).copy(alpha = 0.6f))
                .border(1.dp, Color(0xFFFF529D).copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val infiniteChevron = rememberInfiniteTransition()
            val chevronAlpha by infiniteChevron.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color(0xFFFF529D).copy(alpha = chevronAlpha),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = swipeSnoozeText,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Draggable sliding handle
            Box(
                modifier = Modifier
                    .offset(y = (dragOffset.value / LocalDensity.current.density).dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF529D), Color(0xFFC51162))
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (dragOffset.value < threshold) {
                                    onSnooze()
                                } else {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = (dragOffset.value + dragAmount.y).coerceAtMost(0f)
                                    dragOffset.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (dragOffset.value < threshold) "LEPAS UNTUK TUNDA! 🛌" else "TARIK KE ATAS 🛌",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(56.dp)
        ) {
            Text(
                text = dismissText,
                color = Color(0xFF311B92),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
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

data class StarParticle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val sizeDp: Float,
    val speedY: Float,
    val windX: Float,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val baseAlpha: Float,
    val twinkleFreq: Float,
    val color: Color
)

@Composable
fun SakuraFallingCanvas(modifier: Modifier = Modifier) {
    val petalCount = 18 // Subtle density to remain elegant and beautiful
    val starCount = 12

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

    // Pre-allocate stars for stardust sparkle
    val stars = remember {
        val random = java.util.Random(2026)
        val colors = listOf(
            Color(0xFFFFF9C4), // Soft warm yellow star
            Color(0xFFE1BEE7), // Dreamy lavender star
            Color(0xFFF8BBD0), // Soft pink star
            Color.White        // Shimmering white star
        )
        List(starCount) {
            StarParticle(
                initialXRatio = random.nextFloat(),
                initialYRatio = random.nextFloat(),
                sizeDp = 4f + random.nextFloat() * 6f, // 4dp to 10dp
                speedY = 0.08f + random.nextFloat() * 0.1f,
                windX = 0.02f + random.nextFloat() * 0.04f,
                initialRotation = random.nextFloat() * 360f,
                rotationSpeed = 20f + random.nextFloat() * 40f,
                baseAlpha = 0.4f + random.nextFloat() * 0.5f,
                twinkleFreq = 1.5f + random.nextFloat() * 2.5f,
                color = colors[random.nextInt(colors.size)]
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

    // A normalized sparkling 4-pointed star path
    val baseStarPath = remember {
        Path().apply {
            moveTo(0f, -0.5f)
            quadraticTo(0f, 0f, 0.5f, 0f)
            quadraticTo(0f, 0f, 0f, 0.5f)
            quadraticTo(0f, 0f, -0.5f, 0f)
            quadraticTo(0f, 0f, 0f, -0.5f)
            close()
        }
    }

    val petalColor = Color(0xFFFFB7C5) // Soft, authentic Sakura pink

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width > 0 && height > 0) {
            // 1. Draw drifting shimmering stars
            stars.forEach { star ->
                val rawY = (star.initialYRatio + (progress * star.speedY)) % 1.0f
                val paddingY = 30f
                val currentY = rawY * (height + paddingY * 2) - paddingY

                val rawX = (star.initialXRatio + (progress * star.windX)) % 1.0f
                val currentX = (rawX * width) % width

                val currentRotation = star.initialRotation + (progress * star.rotationSpeed)
                val starSizePx = star.sizeDp.dp.toPx()

                // Calculate animated twinkle alpha
                val twinkle = 0.3f + 0.7f * kotlin.math.abs(
                    kotlin.math.sin(progress * star.twinkleFreq * 2 * kotlin.math.PI.toFloat())
                )
                val finalAlpha = star.baseAlpha * twinkle

                rotate(currentRotation, pivot = Offset(currentX, currentY)) {
                    translate(left = currentX, top = currentY) {
                        drawScopeScale(scaleX = starSizePx, scaleY = starSizePx, pivot = Offset.Zero) {
                            drawPath(
                                path = baseStarPath,
                                color = star.color.copy(alpha = finalAlpha)
                            )
                        }
                    }
                }
            }

            // 2. Draw falling sakura petals
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
    }
}

@Composable
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
                        text = stringResource(id = R.string.settings_title),
                        color = TextLight,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.settings_desc),
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
                        label = { Text(stringResource(id = R.string.settings_user_name_label)) },
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
                        text = stringResource(id = R.string.settings_theme_title),
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            "light" to stringResource(id = R.string.settings_theme_light),
                            "dark" to stringResource(id = R.string.settings_theme_dark)
                        )
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

            // Language Selection Section
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
                        text = stringResource(id = R.string.settings_language_label),
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.settings_language_desc),
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    
                    val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
                    val currentLangCode = if (currentLocales.isEmpty) "system" else currentLocales.get(0)?.language ?: "system"
                    
                    val languages = listOf(
                        "system" to stringResource(id = R.string.settings_follow_system) + " 🌐",
                        "id" to "Bahasa Indonesia 🇮🇩",
                        "en" to "English 🇬🇧",
                        "es" to "Español 🇪🇸",
                        "pt" to "Português 🇵🇹",
                        "fr" to "Français 🇫🇷",
                        "de" to "Deutsch 🇩🇪",
                        "ru" to "Русский 🇷🇺",
                        "ar" to "العربية 🇸🇦",
                        "ja" to "日本語 🇯🇵",
                        "zh" to "简体中文 🇨🇳"
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    val currentLabel = languages.find { it.first == currentLangCode }?.second ?: (stringResource(id = R.string.settings_follow_system) + " 🌐")
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLabel,
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            languages.forEach { (code, label) ->
                                val isSelected = currentLangCode == code
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) IndigoPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            expanded = false
                                            val localeList = if (code == "system") {
                                                androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                                            } else {
                                                androidx.core.os.LocaleListCompat.forLanguageTags(code)
                                            }
                                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
                                            
                                            val displayLang = if (code == "system") "System" else label
                                            val toastMsg = context.getString(R.string.toast_language_changed, displayLang)
                                            android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) IndigoLight else TextLight,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = IndigoLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val aodPrefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
            var aodEnabled by remember { mutableStateOf(aodPrefs.getBoolean("aod_enabled", false)) }

            // AOD card has been removed per user request

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
                        text = stringResource(id = R.string.settings_alarm_title),
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
                        Text(text = stringResource(id = R.string.settings_force_volume), color = TextLight, fontSize = 13.sp)
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
                            Text(text = stringResource(id = R.string.settings_auto_speaker), color = TextLight, fontSize = 13.sp)
                            Text(
                                text = stringResource(id = R.string.settings_auto_speaker_desc),
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
                        text = stringResource(id = R.string.settings_backup_title),
                        color = IndigoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.settings_backup_desc),
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
                            Text(stringResource(id = R.string.settings_restore_btn), color = TextLight, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val df = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
                                createDocLauncher.launch("Sakura_Backup_${df.format(java.util.Date())}.json")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text(stringResource(id = R.string.settings_backup_btn), color = Color.White, fontSize = 11.sp)
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
                        text = stringResource(id = R.string.settings_support_title),
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
                            Text(stringResource(id = R.string.settings_feedback_label), color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(id = R.string.settings_feedback_desc), color = TextMuted, fontSize = 10.sp)
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
fun FileSharingScreenContent(viewModel: AlarmViewModel) {
    val context = LocalContext.current
    val localFiles by viewModel.localFiles.collectAsState()
    val showSurveyPrompt by viewModel.showSurveyPrompt.collectAsState()
    
    var uploadingState by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf("") }
    var uploadedPin by remember { mutableStateOf<String?>(null) }
    var showUploadSuccessDialog by remember { mutableStateOf(false) }
    
    var rxPinInput by remember { mutableStateOf("") }
    var downloadingState by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf("") }
    var showRxDialog by remember { mutableStateOf(false) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size > 5) {
                android.widget.Toast.makeText(context, "Maksimal memilih 5 berkas sekaligus! Silakan coba lagi.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                uploadingState = true
                uploadProgress = "Mengecek berkas..."
                if (uris.size == 1) {
                    viewModel.uploadSharedFile(
                        uri = uris[0],
                        onProgress = { progress -> uploadProgress = progress },
                        onResult = { result ->
                            uploadingState = false
                            if (result.isSuccess) {
                                uploadedPin = result.getOrNull()
                                showUploadSuccessDialog = true
                            } else {
                                val errMsg = result.exceptionOrNull()?.message ?: "Gagal mengunggah berkas"
                                android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                } else {
                    viewModel.uploadMultipleSharedFiles(
                        uris = uris,
                        onProgress = { progress -> uploadProgress = progress },
                        onResult = { result ->
                            uploadingState = false
                            if (result.isSuccess) {
                                uploadedPin = result.getOrNull()
                                showUploadSuccessDialog = true
                            } else {
                                val errMsg = result.exceptionOrNull()?.message ?: "Gagal mengunggah berkas"
                                android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Survey dialog
    if (showSurveyPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSurveyPrompt(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌸 Berikan Pendapatmu!", fontWeight = FontWeight.Bold, color = PinkAccent)
                }
            },
            text = {
                Text(
                    "Hai! Kamu telah mencoba fitur Kirim & Terima Berkas di Alarm Sync. " +
                    "Kami sangat ingin mendengar pendapatmu agar bisa terus meningkatkan fitur ini.\n\n" +
                    "Apakah kamu ingin fitur ini tetap gratis (disertai dukungan suka rela) atau berbayar?",
                    color = TextLight,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissSurveyPrompt(true)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://faizinuha.github.io/AlarmGrubing/Takesurvey.html"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Tidak dapat membuka link", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkAccent)
                ) {
                    Text("Mulai Survei ✨", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSurveyPrompt(true) }) {
                    Text("Nanti Saja", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Upload success dialog
    if (showUploadSuccessDialog && uploadedPin != null) {
        AlertDialog(
            onDismissRequest = { showUploadSuccessDialog = false },
            title = {
                Text("📤 Berkas Berhasil Dikirim!", fontWeight = FontWeight.ExtraBold, color = IndigoPrimary)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Bagikan kode PIN 6-digit di bawah ini kepada penerima:", color = TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(IndigoPrimary.copy(alpha = 0.1f))
                            .border(androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uploadedPin!!,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = IndigoPrimary,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kode PIN ini akan aktif pada cloud dan dapat diunduh siapa saja secara instan.", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("PIN Berkas", uploadedPin)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Kode PIN disalin! 📋", android.widget.Toast.LENGTH_SHORT).show()
                        showUploadSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Copy PIN & Tutup", color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Receive Dialog
    if (showRxDialog) {
        AlertDialog(
            onDismissRequest = { showRxDialog = false },
            title = {
                Text("📥 Terima Berkas", fontWeight = FontWeight.Bold, color = PinkAccent)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Masukkan PIN 6-digit yang kamu terima dari temanmu:", color = TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rxPinInput,
                        onValueChange = {
                            if (it.length <= 6) rxPinInput = it.filter { c -> c.isDigit() }
                        },
                        label = { Text("Kode PIN 6-Digit") },
                        placeholder = { Text("Contoh: 189345") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkAccent,
                            unfocusedBorderColor = SurfaceDarkElevated,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedLabelColor = PinkAccent,
                            unfocusedLabelColor = TextMuted
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rxPinInput.length != 6) {
                            android.widget.Toast.makeText(context, "PIN harus berupa 6-digit angka", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showRxDialog = false
                        downloadingState = true
                        downloadProgress = "Menghubungkan ke server..."
                        viewModel.downloadSharedFile(
                            code = rxPinInput,
                            onProgress = { progress -> downloadProgress = progress },
                            onResult = { result ->
                                downloadingState = false
                                if (result.isSuccess) {
                                    val downloadedFile = result.getOrNull()
                                    android.widget.Toast.makeText(context, "Berkas ${downloadedFile?.name} berhasil disimpan! 📁", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    val errMsg = result.exceptionOrNull()?.message ?: "Gagal mengunduh berkas"
                                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkAccent)
                ) {
                    Text("Unduh Berkas", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRxDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Loading overlay for uploading or downloading
    if (uploadingState || downloadingState) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PinkAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (uploadingState) "Mengirim Berkas..." else "Menerima Berkas...",
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uploadingState) uploadProgress else downloadProgress,
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fitur Transfer & Berbagi Berkas 📁",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bagikan pengingat, musik, rekaman suara, foto, atau dokumen belajar tanpa batas secara asimetris dengan keamanan cloud instan.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // KIRIM BUTTON CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filePickerLauncher.launch("*/*") },
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PinkAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Kirim Berkas",
                                tint = PinkAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Kirim Berkas", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Dapatkan PIN 6 digit", color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }

                // TERIMA BUTTON CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            rxPinInput = ""
                            showRxDialog = true
                        },
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Terima Berkas",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Terima Berkas", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Masukkan PIN teman", color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Berkas Saya 📂",
                    fontWeight = FontWeight.Bold,
                    color = TextLight,
                    fontSize = 15.sp
                )
                if (localFiles.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.clearAllLocalFiles()
                            android.widget.Toast.makeText(context, "Daftar riwayat dibersihkan!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Hapus Riwayat", color = PinkAccent, fontSize = 11.sp)
                    }
                }
            }
        }

        if (localFiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("¯\\_(ツ)_/¯", fontSize = 28.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada berkas tersimpan", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Kirim atau terima berkas di atas untuk memulainya.", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(localFiles) { file ->
                LocalFileItemCard(
                    file = file,
                    onShare = {
                        shareFile(context, file)
                    },
                    onDelete = {
                        viewModel.deleteLocalFile(file)
                        android.widget.Toast.makeText(context, "Berkas berhasil dihapus!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onKirimUlang = {
                        uploadingState = true
                        uploadProgress = "Mengirim ulang berkas..."
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        viewModel.uploadSharedFile(
                            uri = uri,
                            onProgress = { progress -> uploadProgress = progress },
                            onResult = { result ->
                                uploadingState = false
                                if (result.isSuccess) {
                                    uploadedPin = result.getOrNull()
                                    showUploadSuccessDialog = true
                                } else {
                                    val errMsg = result.exceptionOrNull()?.message ?: "Gagal mengunggah berkas"
                                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun LocalFileItemCard(
    file: File,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onKirimUlang: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sizeFormatted = remember(file) {
        val len = file.length()
        if (len < 1024) {
            "$len B"
        } else if (len < 1024 * 1024) {
            String.format(Locale.getDefault(), "%.1f KB", len / 1024f)
        } else {
            String.format(Locale.getDefault(), "%.1f MB", len / (1024f * 1024f))
        }
    }

    val fileExt = remember(file) {
        file.name.substringAfterLast('.', "").lowercase()
    }

    val iconRes = when (fileExt) {
        "mp3", "wav", "m4a", "ogg" -> Icons.Default.PlayArrow
        "jpg", "jpeg", "png", "gif", "webp" -> Icons.Default.Edit
        "pdf", "doc", "docx", "txt", "xlsx" -> Icons.Default.List
        else -> Icons.Default.Menu
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDarkElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconRes,
                    contentDescription = "Format $fileExt",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ukuran: $sizeFormatted • Tipe: ${fileExt.uppercase()}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onKirimUlang() }) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Kirim Ulang",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { onShare() }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan",
                        tint = PinkAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Berkas?", color = TextLight) },
            text = { Text("Apakah kamu yakin ingin menghapus berkas '${file.name}' dari memori HP-mu?", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Ya, Hapus", color = Color.Red, fontWeight = FontWeight.Bold)
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

private fun shareFile(context: Context, file: File, mimeType: String = "*/*") {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Berkas 💌"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Gagal membagikan berkas: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}



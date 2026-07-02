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
import com.example.ui.MainScreenContent
import com.example.ui.RingingOverlay
import com.example.ui.SettingsScreen

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

        // Start AOD service if enabled
        val aodPrefs = getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        if (aodPrefs.getBoolean("aod_enabled", false)) {
            val serviceIntent = Intent(this, com.example.alarm.AodService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

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


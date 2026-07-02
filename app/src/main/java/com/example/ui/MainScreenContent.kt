package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.R
import com.example.data.api.GithubUpdateChecker
import com.example.data.helper.NetworkConnectionHelper
import com.example.data.model.Alarm
import com.example.data.model.Note
import com.example.ui.AodSettingsScreen
import com.example.ui.CameraQrScannerDialog
import com.example.ui.ShowGroupQrDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import java.io.File
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
                .padding(6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                title = "⏰ Pribadi",
                isActive = activeTab == 0,
                onClick = { activeTab = 0 }
            )
            TabButton(
                title = "👥 Grup",
                isActive = activeTab == 1,
                onClick = { 
                    activeTab = 1 
                    viewModel.forceSyncGroupAndCouple()
                }
            )
            TabButton(
                title = "📝 Catatan",
                isActive = activeTab == 2,
                onClick = { activeTab = 2 }
            )
            TabButton(
                title = "📁 Berbagi",
                isActive = activeTab == 3,
                onClick = { activeTab = 3 }
            )
            TabButton(
                title = "📱 AOD",
                isActive = activeTab == 4,
                onClick = { activeTab = 4 }
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
            .padding(vertical = 10.dp, horizontal = 16.dp),
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


package com.example.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import com.example.R
import com.example.data.model.Alarm
import com.example.data.model.MemberData
import com.example.ui.AlarmViewModel
import com.example.ui.SyncStatus
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
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

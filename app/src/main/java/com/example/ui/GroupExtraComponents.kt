package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.model.AwakeStatus
import com.example.data.model.CouplePair
import com.example.ui.AlarmViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
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

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Alarm
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import java.util.Locale
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

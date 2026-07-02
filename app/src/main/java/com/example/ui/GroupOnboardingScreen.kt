package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AlarmViewModel
import com.example.ui.SyncStatus
import com.example.ui.CameraQrScannerDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
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

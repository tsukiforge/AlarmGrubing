package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ui.AlarmViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PinkAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import java.io.File
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



package com.example.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bottom sheet untuk memilih opsi "Set as Wallpaper".
 *
 * Mendukung:
 * - Wallpaper gambar statis (Home / Lock / Keduanya)
 * - Live wallpaper video (via system wallpaper picker → VideoLiveWallpaperService)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetWallpaperBottomSheet(
    context: Context,
    imageUri: Uri? = null,
    videoUri: Uri? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var isSettingWallpaper by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val setStaticWallpaper = { flag: Int ->
        isSettingWallpaper = true
        resultMessage = null
        scope.launch {
            val cachedUri = withContext(Dispatchers.IO) {
                try {
                    val cacheFile = File(context.cacheDir, "wallpaper_temp_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(imageUri!!)?.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (cacheFile.exists()) Uri.fromFile(cacheFile) else imageUri
                } catch (e: Exception) {
                    android.util.Log.w("SetWallpaper", "Gagal copy ke cache: ${e.message}")
                    imageUri // fallback ke URI asli
                }
            }
            val result = WallpaperHelper.setWallpaperFromUri(context, cachedUri!!, flag)
            isSettingWallpaper = false
            if (cachedUri.scheme == "file") {
                try { File(cachedUri.path!!).delete() } catch (_: Exception) {}
            }
            resultMessage = when (result) {
                is WallpaperHelper.WallpaperResult.Success -> "✅ Wallpaper berhasil diterapkan!"
                is WallpaperHelper.WallpaperResult.Error -> "❌ ${result.message}"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set Sebagai Wallpaper",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Pilih posisi wallpaper yang ingin diterapkan",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (imageUri != null) {
                // Home Screen
                Button(
                    onClick = { setStaticWallpaper(WallpaperManager.FLAG_SYSTEM) },
                    enabled = !isSettingWallpaper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🏠 Layar Utama (Home Screen)", color = Color.White, fontSize = 14.sp)
                }

                // Lock Screen
                Button(
                    onClick = { setStaticWallpaper(WallpaperManager.FLAG_LOCK) },
                    enabled = !isSettingWallpaper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔒 Layar Kunci (Lock Screen)", color = Color.White, fontSize = 14.sp)
                }

                // Both
                Button(
                    onClick = { setStaticWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) },
                    enabled = !isSettingWallpaper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSettingWallpaper) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("📱 Keduanya (Home & Lock Screen)", color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (videoUri != null) {
                var isVideoMuted by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bisukan Audio Video", color = Color.White, fontSize = 14.sp)
                        Text("Matikan suara untuk live wallpaper", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isVideoMuted,
                        onCheckedChange = { isVideoMuted = it }
                    )
                }

                Button(
                    onClick = {
                        isSettingWallpaper = true
                        scope.launch(Dispatchers.IO) {
                            var finalVideoUri = videoUri
                            try {
                                context.contentResolver.takePersistableUriPermission(
                                    videoUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) {
                                android.util.Log.w("SetWallpaper", "Tidak bisa takePersistable untuk video URI, menyalin ke lokal...")
                                // Salin ke internal storage agar bisa diakses
                                try {
                                    val localFile = File(context.filesDir, "shared_wallpaper_video.mp4")
                                    context.contentResolver.openInputStream(videoUri)?.use { input ->
                                        localFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    if (localFile.exists()) {
                                        finalVideoUri = Uri.fromFile(localFile)
                                    }
                                } catch (copyEx: Exception) {
                                    android.util.Log.e("SetWallpaper", "Gagal menyalin video: ${copyEx.message}")
                                }
                            }

                            withContext(Dispatchers.Main) {
                                val prefs = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("video_uri", finalVideoUri.toString())
                                    .putBoolean("video_muted", isVideoMuted)
                                    .apply()

                                isSettingWallpaper = false
                                WallpaperHelper.openVideoLiveWallpaperPicker(context)
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isSettingWallpaper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSettingWallpaper) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("🎬 Set Live Wallpaper Video (Pilih di sistem)", color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: Pemutar video akan disesuaikan sistem.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            resultMessage?.let { msg ->
                Text(
                    text = msg,
                    color = if (msg.startsWith("✅")) Color(0xFF81C784) else Color(0xFFE57373),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                Text("Tutup", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

package com.example.ui

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
import kotlinx.coroutines.launch

/**
 * Bottom sheet untuk memilih opsi "Set as Wallpaper".
 *
 * Mendukung:
 * - Wallpaper gambar statis (via WallpaperManager.setStream)
 * - Live wallpaper video (via system wallpaper picker → VideoLiveWallpaperService)
 *
 * @param context          Context aktif
 * @param imageUri         URI gambar statis yang akan di-set (null jika hanya ingin video)
 * @param videoUri         URI video yang akan di-set sebagai live wallpaper (null jika hanya gambar)
 * @param onDismiss        Callback saat bottom sheet ditutup
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
                text = "Pilih jenis wallpaper yang ingin diterapkan",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // ── Opsi 1: Wallpaper Gambar Statis ──────────────────────────────────
            if (imageUri != null) {
                Button(
                    onClick = {
                        isSettingWallpaper = true
                        resultMessage = null
                        scope.launch {
                            val result = WallpaperHelper.setWallpaperFromUri(context, imageUri)
                            isSettingWallpaper = false
                            resultMessage = when (result) {
                                is WallpaperHelper.WallpaperResult.Success ->
                                    "✅ Wallpaper gambar berhasil diterapkan!"
                                is WallpaperHelper.WallpaperResult.Error ->
                                    "❌ ${result.message}"
                            }
                        }
                    },
                    enabled = !isSettingWallpaper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSettingWallpaper) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "🖼️  Set sebagai Wallpaper Gambar",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── Opsi 2: Live Wallpaper Video ─────────────────────────────────────
            if (videoUri != null) {
                Button(
                    onClick = {
                        // Simpan URI video ke prefs supaya VideoLiveWallpaperService bisa membacanya
                        val prefs = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("video_uri", videoUri.toString()).apply()

                        // Minta persistent URI permission
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                videoUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("SetWallpaper", "Tidak bisa takePersistable untuk video URI: ${e.message}")
                        }

                        // Buka system live wallpaper picker
                        WallpaperHelper.openVideoLiveWallpaperPicker(context)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🎬  Set sebagai Live Wallpaper Video",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Video akan di-loop terus. Playback otomatis berhenti saat baterai ≤ 15%.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Pesan hasil
            resultMessage?.let { msg ->
                Text(
                    text = msg,
                    color = if (msg.startsWith("✅")) Color(0xFF81C784) else Color(0xFFE57373),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Tombol batal
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Batal", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

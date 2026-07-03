package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.alarm.AodService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream


@Composable
fun AodSettingsScreen(context: Context) {
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
    var selectedTemplateIndex by remember { mutableStateOf(prefs.getInt("selected_template", 0)) }
    var customImageUri by remember { mutableStateOf<String?>(prefs.getString("custom_image_uri", null)) }
    var useCustomImage by remember { mutableStateOf(prefs.getBoolean("use_custom_image", false)) }
    var aodEnabled by remember { mutableStateOf(prefs.getBoolean("aod_enabled", false)) }
    var showMotivation by remember { mutableStateOf(prefs.getBoolean("show_motivation", true)) }
    var useAnimatedAod by remember { mutableStateOf(prefs.getBoolean("use_animated_aod", false)) }
    var selectedAnimationIndex by remember { mutableStateOf(prefs.getInt("selected_animation", 0)) }

    val templates = listOf(
        R.drawable.aod_template_1_1782867396832,
        R.drawable.aod_template_2_1782867409346,
        R.drawable.aod_template_3_1782867422789
    )
    
    val animationTemplates = listOf(
        R.raw.aod_anim_1,
        R.raw.aod_anim_2,
        R.raw.aod_anim_3,
        R.raw.aod_anim_4
    )
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (e: Exception) {}
            customImageUri = uri.toString()
            useCustomImage = true
            useAnimatedAod = false
            prefs.edit()
                .putString("custom_image_uri", customImageUri)
                .putBoolean("use_custom_image", true)
                .putBoolean("use_animated_aod", false)
                .apply()
        }
    }

    // Launcher untuk memilih video (live wallpaper)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            val wallpaperPrefs = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)
            wallpaperPrefs.edit().putString("video_uri", uri.toString()).apply()
            WallpaperHelper.openVideoLiveWallpaperPicker(context)
        }
    }

    // State untuk bottom sheet "Set as Wallpaper"
    var wallpaperSheetImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showWallpaperSheet by remember { mutableStateOf(false) }

    // Function to start or stop service
    val toggleAodService = { enabled: Boolean ->
        aodEnabled = enabled
        prefs.edit().putBoolean("aod_enabled", enabled).apply()
        val serviceIntent = Intent(context, AodService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(serviceIntent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Smaller, cleaner header
        Text(
            text = "Fitur Layar Standby (AOD)",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Switch to enable/disable Always-on Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            border = BoxDefaults.CardBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aktifkan Standby AOD",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "AOD otomatis aktif saat layar mati",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = aodEnabled,
                    onCheckedChange = { toggleAodService(it) }
                )
            }
        }

        // Motivation Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tampilkan Kata Motivasi",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Kutipan berganti otomatis setiap 5 menit",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = showMotivation,
                    onCheckedChange = {
                        showMotivation = it
                        prefs.edit().putBoolean("show_motivation", it).apply()
                    }
                )
            }
        }

        // Custom gallery image button
        Button(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Pilih Wallpaper dari Galeri HP 🖼️", color = Color.White, fontSize = 13.sp)
        }

        // Static templates section
        Text(
            text = "Wallpaper Statis (Bawaan)",
            color = Color.LightGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        // Custom robust Grid representation using Rows (No nested scrolling conflicts!)
        val allItems = mutableListOf<@Composable () -> Unit>()
        
        if (customImageUri != null) {
            allItems.add {
                val isSelected = useCustomImage && !useAnimatedAod
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFFE91E63) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            useCustomImage = true
                            useAnimatedAod = false
                            prefs.edit()
                                .putBoolean("use_custom_image", true)
                                .putBoolean("use_animated_aod", false)
                                .apply()
                        }
                ) {
                    AsyncImage(
                        model = customImageUri,
                        contentDescription = "Custom Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Text(
                            text = "Kustom",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .background(Color(0xFFE91E63), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        templates.forEachIndexed { index, resId ->
            allItems.add {
                val isSelected = !useCustomImage && !useAnimatedAod && selectedTemplateIndex == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFFE91E63) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            useCustomImage = false
                            useAnimatedAod = false
                            selectedTemplateIndex = index
                            prefs.edit()
                                .putInt("selected_template", index)
                                .putBoolean("use_custom_image", false)
                                .putBoolean("use_animated_aod", false)
                                .apply()
                        }
                ) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "Template ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Text(
                            text = "Tema ${index + 1}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .background(Color(0xFFE91E63), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Animated templates section
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Wallpaper Animasi (AOD Bergerak)",
            color = Color.LightGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        animationTemplates.forEachIndexed { index, resId ->
            allItems.add {
                val isSelected = useAnimatedAod && selectedAnimationIndex == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            useAnimatedAod = true
                            useCustomImage = false
                            selectedAnimationIndex = index
                            prefs.edit()
                                .putBoolean("use_animated_aod", true)
                                .putBoolean("use_custom_image", false)
                                .putInt("selected_animation", index)
                                .apply()
                        }
                ) {
                    VideoThumbnailImage(
                        rawResId = resId,
                        context = context,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Text(
                            text = "Animasi ${index + 1}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Render in elegant 3-column rows
        val chunked = allItems.chunked(3)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { itemComposable ->
                    Box(modifier = Modifier.weight(1f)) {
                        itemComposable()
                    }
                }
                // Fill up remaining space if row is not full
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Seksi: Set as Wallpaper ──────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Set sebagai Wallpaper HP",
            color = Color.LightGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 10.dp)
        )

        // Tombol set wallpaper gambar — aktif jika ada custom image URI
        Button(
            onClick = {
                val uriStr = customImageUri
                if (uriStr != null) {
                    wallpaperSheetImageUri = android.net.Uri.parse(uriStr)
                    showWallpaperSheet = true
                }
            },
            enabled = customImageUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(bottom = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "🖼️  Set Gambar Kustom sebagai Wallpaper",
                color = Color.White,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tombol set live wallpaper video
        Button(
            onClick = { videoPickerLauncher.launch("video/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "🎬  Pilih Video untuk Live Wallpaper",
                color = Color.White,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Video akan di-loop terus-menerus. Otomatis pause saat baterai ≤ 15%.",
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Bottom sheet untuk konfirmasi & set wallpaper gambar
    if (showWallpaperSheet) {
        SetWallpaperBottomSheet(
            context = context,
            imageUri = wallpaperSheetImageUri,
            videoUri = null,
            onDismiss = { showWallpaperSheet = false }
        )
    }
}

private object BoxDefaults {
    val CardBorder = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
}

@Composable
fun VideoThumbnailImage(
    rawResId: Int,
    context: Context,
    modifier: Modifier = Modifier
) {
    val cacheDir = remember { File(context.filesDir, "aod_thumbnails").also { it.mkdirs() } }
    val cacheFile = remember(rawResId) { File(cacheDir, "thumb_${rawResId}.png") }

    val bitmap by produceState<android.graphics.Bitmap?>(null, rawResId) {
        value = withContext(Dispatchers.IO) {
            try {
                // Coba load dari cache dulu
                if (cacheFile.exists()) {
                    android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
                } else {
                    // Extract first frame dari video & simpan ke cache
                    val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val frame = retriever.frameAtTime
                    retriever.release()

                    if (frame != null) {
                        // Simpan ke cache untuk下次
                        try {
                            FileOutputStream(cacheFile).use { out ->
                                frame.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("AodSettings", "Gagal simpan cache thumbnail: ${e.message}")
                        }
                    }
                    frame
                }
            } catch (e: Exception) {
                android.util.Log.w("AodSettings", "Gagal ambil thumbnail video: ${e.message}")
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Thumbnail Video AOD",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White.copy(alpha = 0.4f),
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * Hapus semua cache thumbnail AOD untuk mengosongkan storage.
 * Panggil ini jika user ingin membersihkan cache.
 */
fun clearAodThumbnailCache(context: Context) {
    try {
        val cacheDir = File(context.filesDir, "aod_thumbnails")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
            cacheDir.delete()
            android.util.Log.d("AodSettings", "Cache thumbnail AOD dibersihkan")
        }
    } catch (e: Exception) {
        android.util.Log.w("AodSettings", "Gagal bersihkan cache: ${e.message}")
    }
}

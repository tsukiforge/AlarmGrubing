package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun AodSettingsScreen(context: Context) {
    var wallpaperSheetImageUri by remember { mutableStateOf<Uri?>(null) }
    var wallpaperSheetVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showWallpaperSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        WallpaperHelper.ensurePreviousWallpaperSaved(context)
    }

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
            wallpaperSheetImageUri = uri
            wallpaperSheetVideoUri = null
            showWallpaperSheet = true
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            wallpaperSheetVideoUri = uri
            wallpaperSheetImageUri = null
            showWallpaperSheet = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wallpaper & Layar Kunci",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Atur gambar atau video sebagai wallpaper HP Anda.",
            color = Color.LightGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Custom gallery buttons side by side (symmetrical and smaller)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("🖼️ Galeri", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("🎬 Live Video", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Static templates section
        Text(
            text = "Gambar Bawaan",
            color = Color.LightGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        val imageItems = mutableListOf<@Composable () -> Unit>()

        // 1. ADD RESTORE WALLPAPER OPTION AT INDEX 0 (NUMBER 1)
        imageItems.add {
            val restoreFile = File(context.filesDir, "previous_wallpaper.jpg")
            val restoreUri = Uri.fromFile(restoreFile)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFFFF4081), RoundedCornerShape(10.dp)) // Highlight border
                    .clickable {
                        if (restoreFile.exists()) {
                            wallpaperSheetImageUri = restoreUri
                            wallpaperSheetVideoUri = null
                            showWallpaperSheet = true
                        }
                    }
            ) {
                if (restoreFile.exists()) {
                    AsyncImage(
                        model = restoreFile,
                        contentDescription = "Wallpaper Restre",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Default", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                
                // Overlay text badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Restre 🔄",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. ADD STANDARD TEMPLATES
        templates.forEachIndexed { index, resId ->
            imageItems.add {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .clickable {
                            wallpaperSheetImageUri = Uri.parse("android.resource://${context.packageName}/$resId")
                            wallpaperSheetVideoUri = null
                            showWallpaperSheet = true
                        }
                ) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "Gambar ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Render image rows
        imageItems.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { itemComposable -> Box(modifier = Modifier.weight(1f)) { itemComposable() } }
                if (rowItems.size < 3) repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated templates section
        Text(
            text = "Video Bawaan (Live Wallpaper)",
            color = Color.LightGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        val videoItems = mutableListOf<@Composable () -> Unit>()
        animationTemplates.forEachIndexed { index, resId ->
            videoItems.add {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .clickable {
                            wallpaperSheetVideoUri = Uri.parse("android.resource://${context.packageName}/$resId")
                            wallpaperSheetImageUri = null
                            showWallpaperSheet = true
                        }
                ) {
                    VideoThumbnailImage(
                        rawResId = resId,
                        context = context,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Render video rows
        videoItems.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { itemComposable -> Box(modifier = Modifier.weight(1f)) { itemComposable() } }
                if (rowItems.size < 3) repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showWallpaperSheet) {
        SetWallpaperBottomSheet(
            context = context,
            imageUri = wallpaperSheetImageUri,
            videoUri = wallpaperSheetVideoUri,
            onDismiss = { showWallpaperSheet = false }
        )
    }
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
                if (cacheFile.exists()) {
                    android.graphics.BitmapFactory.decodeFile(cacheFile.absolutePath)
                } else {
                    val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val frame = retriever.frameAtTime
                    retriever.release()

                    if (frame != null) {
                        try {
                            FileOutputStream(cacheFile).use { out ->
                                frame.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("WallpaperSettings", "Gagal simpan cache thumbnail: ${e.message}")
                        }
                    }
                    frame
                }
            } catch (e: Exception) {
                android.util.Log.w("WallpaperSettings", "Gagal ambil thumbnail video: ${e.message}")
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Thumbnail Video",
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

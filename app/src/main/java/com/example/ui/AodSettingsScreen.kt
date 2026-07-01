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

@Composable
fun AodSettingsScreen(context: Context) {
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }
    var selectedTemplateIndex by remember { mutableStateOf(prefs.getInt("selected_template", 0)) }
    var customImageUri by remember { mutableStateOf<String?>(prefs.getString("custom_image_uri", null)) }
    var useCustomImage by remember { mutableStateOf(prefs.getBoolean("use_custom_image", false)) }
    var aodEnabled by remember { mutableStateOf(prefs.getBoolean("aod_enabled", false)) }
    var showMotivation by remember { mutableStateOf(prefs.getBoolean("show_motivation", true)) }

    val templates = listOf(
        R.drawable.aod_template_1_1782867396832,
        R.drawable.aod_template_2_1782867409346,
        R.drawable.aod_template_3_1782867422789,
        R.drawable.img_aod_miku_ripped_1782906384401,
        R.drawable.img_aod_yandere_cleaver_1782906410094,
        R.drawable.img_aod_miku_blue_1782906427863,
        R.drawable.img_aod_purple_eye_1782906447488,
        R.drawable.img_cat_sleeping_1782296435114,
        R.drawable.img_cat_yawn_1782296451744
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
            prefs.edit()
                .putString("custom_image_uri", customImageUri)
                .putBoolean("use_custom_image", true)
                .apply()
        }
    }

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

        // Template gallery title
        Text(
            text = "Pilih Wallpaper & Desain AOD",
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
                val isSelected = useCustomImage
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
                            prefs.edit().putBoolean("use_custom_image", true).apply()
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
                val isSelected = !useCustomImage && selectedTemplateIndex == index
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
                            selectedTemplateIndex = index
                            prefs.edit().putInt("selected_template", index).putBoolean("use_custom_image", false).apply()
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
    }
}

private object BoxDefaults {
    val CardBorder = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
}

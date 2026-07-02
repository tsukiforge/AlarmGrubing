package com.example.ui

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CatNightBrown
import com.example.ui.theme.CatNightAccent
import com.example.ui.theme.CatNightSurface
import com.example.ui.theme.CatNightBg
import com.example.ui.theme.CatPink
import com.example.ui.theme.CatMidnight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    initialType: FeedbackType = FeedbackType.GENERAL,
    onDismiss: () -> Unit,
    onSubmit: (FeedbackData) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember { FeedbackRepository() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CatNightSurface,
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CatNightBrown.copy(alpha = 0.5f)) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Crossfade(targetState = isSubmitted, label = "FeedbackState") { submitted ->
                if (submitted) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_cat_happy),
                            contentDescription = "Success Masukan",
                            modifier = Modifier
                                .size(140.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Masukan Terkirim! 🐱❤",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = CatNightAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Terima kasih banyak atas feedback Anda! Kontribusi Anda sangat membantu kami dalam menyempurnakan aplikasi Alarm Sync ini biar makin gemas dan stabil! Paw-fect!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CatNightBrown),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Sama-Sama! 🐶", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Form View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cat_ears),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kirim Masukan 🐱",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            )
                        }
                        
                        Text(
                            text = "Umpan balik Anda membantu meningkatkan performa alarm grup.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        // 1. Selector FeedbackType Chips
                        Text(
                            text = "Pilih Kategori",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CatNightAccent,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            val types = listOf(
                                FeedbackType.BUG to "🐛 Bug",
                                FeedbackType.SUGGESTION to "💡 Saran",
                                FeedbackType.PRAISE to "⭐ Pujian",
                                FeedbackType.GENERAL to "❓ Lainnya"
                            )
                            types.forEach { (type, label) ->
                                val selected = selectedType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedType = type },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CatNightBrown,
                                        selectedLabelColor = Color.White,
                                        containerColor = CatNightBg,
                                        labelColor = Color.LightGray
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = CatNightBrown.copy(alpha = 0.5f),
                                        selectedBorderColor = CatNightAccent
                                    )
                                )
                            }
                        }

                        // 2. Star Rating Selector (Paw styled details)
                        Text(
                            text = "Beri Rating Aplikasi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CatNightAccent,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                        ) {
                            for (i in 1..5) {
                                val active = i <= rating
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { rating = i }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_cat_paw),
                                        contentDescription = "$i Cakar",
                                        tint = if (active) CatPink else Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = i.toString(),
                                        fontSize = 10.sp,
                                        color = if (active) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 3. Multi-line Feedback message
                        Text(
                            text = "Pesan Anda",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CatNightAccent,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = message,
                            onValueChange = { if (it.length <= 500) message = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("Tulis laporan bug atau saran Anda di sini...", fontSize = 12.sp, color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CatNightBrown,
                                unfocusedBorderColor = CatNightBg,
                                focusedContainerColor = CatNightBg,
                                unfocusedContainerColor = CatNightBg,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${message.length}/500",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        // 4. Contact Email
                        Text(
                            text = "Alamat Email (Opsional)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CatNightAccent,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            placeholder = { Text("contoh@email.com", fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CatNightBrown,
                                unfocusedBorderColor = CatNightBg,
                                focusedContainerColor = CatNightBg,
                                unfocusedContainerColor = CatNightBg,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (submitError != null) {
                            Text(
                                text = "Gagal mengirim: $submitError",
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // 5. Submit Button
                        Button(
                            onClick = {
                                if (message.trim().isEmpty()) {
                                    android.widget.Toast.makeText(context, "Pesan tidak boleh kosong!", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    isSubmitting = true
                                    submitError = null
                                    
                                    val feedbackData = FeedbackData(
                                        type = selectedType,
                                        rating = rating,
                                        message = message.trim(),
                                        email = email.trim().ifEmpty { null },
                                        appVersion = try {
                                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                                        } catch (e: Exception) { "1.0" },
                                        deviceModel = Build.MODEL,
                                        androidVersion = Build.VERSION.SDK_INT
                                    )
                                    
                                    val result = repository.submitFeedback(feedbackData)
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        isSubmitted = true
                                        onSubmit(feedbackData)
                                    } else {
                                        submitError = result.exceptionOrNull()?.message ?: "Terjadi kesalahan koneksi"
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CatNightAccent,
                                disabledContainerColor = CatNightAccent.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = CatMidnight, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mengirim Masukan...", color = CatMidnight, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("💡 KIRIM MASUKAN KAT", color = CatMidnight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

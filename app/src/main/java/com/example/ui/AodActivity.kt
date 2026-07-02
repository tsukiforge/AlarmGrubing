package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.alarm.AodService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class AodActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AodActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "AodActivity onCreate")

        // Tampil di atas lock screen dan nyalakan layar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Kecerahan sangat rendah untuk hemat baterai (AOD style)
        window.attributes = window.attributes.also { it.screenBrightness = 0.05f }

        // Cegah back button mengembalikan ke lock screen sebelum waktunya
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Double-tap adalah cara exit — back button diabaikan
            }
        })

        val prefs = getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        val selectedTemplateIndex = prefs.getInt("selected_template", 0)
        val useCustomImage = prefs.getBoolean("use_custom_image", false)
        val rawCustomImageUri = prefs.getString("custom_image_uri", null)
        val showMotivation = prefs.getBoolean("show_motivation", true)
        val useAnimatedAod = prefs.getBoolean("use_animated_aod", false)
        val selectedAnimationIndex = prefs.getInt("selected_animation", 0)

        // Validasi URI sebelum diteruskan ke Compose
        // FIX: Cek apakah content URI masih bisa dibaca (persistent permission mungkin expired)
        val validatedCustomImageUri: String? = if (useCustomImage && rawCustomImageUri != null) {
            try {
                val uri = Uri.parse(rawCustomImageUri)
                // Test baca 1 byte untuk konfirmasi permission masih valid
                contentResolver.openInputStream(uri)?.use { _ -> }
                Log.d(TAG, "Custom AOD image URI valid: $rawCustomImageUri")
                rawCustomImageUri
            } catch (e: SecurityException) {
                Log.w(TAG, "Custom AOD image URI permission expired — fallback ke template. URI: $rawCustomImageUri")
                // Bersihkan URI yang tidak bisa diakses dari prefs
                prefs.edit()
                    .remove("custom_image_uri")
                    .putBoolean("use_custom_image", false)
                    .apply()
                null
            } catch (e: Exception) {
                Log.w(TAG, "Custom AOD image URI tidak bisa dibaca: ${e.message}")
                null
            }
        } else null

        setContent {
            AodScreen(
                templateIndex = selectedTemplateIndex,
                useCustomImage = useCustomImage && validatedCustomImageUri != null,
                customImageUri = validatedCustomImageUri,
                showMotivation = showMotivation,
                useAnimatedAod = useAnimatedAod,
                animationIndex = selectedAnimationIndex,
                onExit = { dismissAod() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AodActivity onDestroy — memberi tahu service untuk cancel notifikasi")
        dismissAodNotification()
    }

    private fun dismissAod() {
        Log.d(TAG, "AOD di-dismiss oleh user (double-tap)")
        dismissAodNotification()
        finish()
    }

    private fun dismissAodNotification() {
        // Beritahu AodService untuk cancel trigger notification
        try {
            val dismissIntent = Intent(this, AodService::class.java).apply {
                action = "ACTION_DISMISS_AOD_NOTIF"
            }
            startService(dismissIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Tidak bisa mengirim dismiss intent ke AodService: ${e.message}")
        }
    }
}

val motivations = listOf(
    "Teruslah melangkah, walau perlahan.",
    "Hari ini adalah awal yang baru.",
    "Jangan lupa tersenyum hari ini 😊",
    "Fokus pada hal-hal baik.",
    "Setiap detik sangat berharga.",
    "Istirahatlah jika lelah, jangan menyerah."
)

@Composable
fun AodScreen(
    templateIndex: Int,
    useCustomImage: Boolean,
    customImageUri: String?,
    showMotivation: Boolean,
    useAnimatedAod: Boolean,
    animationIndex: Int,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    // Template bawaan aplikasi (static)
    val templates = listOf(
        R.drawable.aod_template_1_1782867396832,
        R.drawable.aod_template_2_1782867409346,
        R.drawable.aod_template_3_1782867422789
    )

    // Animasi AOD (video bergerak)
    val animationResources = listOf(
        R.raw.aod_anim_1,
        R.raw.aod_anim_2,
        R.raw.aod_anim_3,
        R.raw.aod_anim_4
    )

    val template = templates.getOrNull(templateIndex) ?: templates[0]

    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }

    // Anti Burn-in — geser konten setiap 60 detik
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var motivationText by remember { mutableStateOf(motivations.random()) }

    // FIX: Fallback state apabila custom image gagal load
    var customImageLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        var secondsCount = 0
        while (true) {
            val now = Date()
            timeText = timeFormat.format(now)
            dateText = dateFormat.format(now)

            if (secondsCount % 60 == 0) {
                offsetX = Random.nextInt(-20, 20).toFloat()
                offsetY = Random.nextInt(-30, 30).toFloat()
            }
            if (secondsCount % 300 == 0) {
                motivationText = motivations.random()
            }

            delay(1000)
            secondsCount++
        }
    }

    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = tween(1000), label = "offsetX")
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, animationSpec = tween(1000), label = "offsetY")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onExit() })
            }
    ) {
        // Layer background — urutan prioritas: animated > custom image > built-in template
        when {
            useAnimatedAod && animationIndex in animationResources.indices -> {
                val videoResId = animationResources[animationIndex]
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val uri = Uri.parse("android.resource://${ctx.packageName}/${videoResId}")
                            setVideoURI(uri)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                mp.setVolume(0f, 0f)
                                start()
                            }
                            setOnErrorListener { _, what, extra ->
                                android.util.Log.e("AodScreen", "VideoView error: what=$what extra=$extra")
                                false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize().alpha(0.25f)
                )
            }

            useCustomImage && customImageUri != null && !customImageLoadFailed -> {
                // FIX: Gunakan ImageRequest dengan listener error eksplisit
                // Ini memastikan fallback ke template jika URI tidak bisa dibaca
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(Uri.parse(customImageUri))
                        .crossfade(true)
                        .listener(
                            onError = { _, result ->
                                android.util.Log.e(
                                    "AodScreen",
                                    "Gagal load custom AOD image: ${result.throwable.message}. URI: $customImageUri"
                                )
                                customImageLoadFailed = true
                            },
                            onSuccess = { _, _ ->
                                android.util.Log.d("AodScreen", "Custom AOD image berhasil dimuat")
                            }
                        )
                        .build(),
                    contentDescription = "Custom AOD Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.25f)
                )
            }

            else -> {
                // Template bawaan (fallback jika custom image gagal atau tidak di-set)
                if (customImageLoadFailed && useCustomImage) {
                    android.util.Log.w("AodScreen", "Custom image gagal load — menampilkan template default")
                }
                Image(
                    painter = painterResource(id = template),
                    contentDescription = "AOD Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.25f)
                )
            }
        }

        // Overlay jam & motivasi
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .offset(x = animatedOffsetX.dp, y = animatedOffsetY.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeText,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateText,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            if (showMotivation) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = motivationText,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        }

        Text(
            text = "Ketuk dua kali untuk keluar",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
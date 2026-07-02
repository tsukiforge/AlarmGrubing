package com.example.ui

import android.content.Context
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class AodActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show on top of keyguard / lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        // Setup Window for AOD
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Very low brightness for AOD
        val params = window.attributes
        params.screenBrightness = 0.05f
        window.attributes = params
        
        val prefs = getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        val selectedTemplateIndex = prefs.getInt("selected_template", 0)
        val useCustomImage = prefs.getBoolean("use_custom_image", false)
        val customImageUri = prefs.getString("custom_image_uri", null)
        val showMotivation = prefs.getBoolean("show_motivation", true)
        val useAnimatedAod = prefs.getBoolean("use_animated_aod", false)
        val selectedAnimationIndex = prefs.getInt("selected_animation", 0)

        setContent {
            AodScreen(
                templateIndex = selectedTemplateIndex,
                useCustomImage = useCustomImage,
                customImageUri = customImageUri,
                showMotivation = showMotivation,
                useAnimatedAod = useAnimatedAod,
                animationIndex = selectedAnimationIndex,
                onExit = { finish() }
            )
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
    
    // Anti Burn-in (Kemacetan Layar) mechanism
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var motivationText by remember { mutableStateOf(motivations.random()) }
    
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        var secondsCount = 0
        while (true) {
            val now = Date()
            timeText = timeFormat.format(now)
            dateText = dateFormat.format(now)
            
            // Shift position every 60 seconds to prevent burn-in
            if (secondsCount % 60 == 0) {
                offsetX = Random.nextInt(-20, 20).toFloat()
                offsetY = Random.nextInt(-30, 30).toFloat()
            }
            
            // Change motivation every 5 minutes
            if (secondsCount % 300 == 0) {
                motivationText = motivations.random()
            }
            
            delay(1000)
            secondsCount++
        }
    }
    
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = tween(1000))
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, animationSpec = tween(1000))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onExit() }
                )
            }
    ) {
        if (useAnimatedAod && animationIndex in animationResources.indices) {
            // AOD dengan video animasi bergerak
            val videoResId = animationResources[animationIndex]
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val uri = Uri.parse("android.resource://${ctx.packageName}/${videoResId}")
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f) // Senyap
                            start()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.25f)
            )
        } else if (useCustomImage && customImageUri != null) {
            AsyncImage(
                model = customImageUri,
                contentDescription = "Custom AOD Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.25f // Kept low to save battery
            )
        } else {
            Image(
                painter = painterResource(id = template),
                contentDescription = "AOD Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.25f
            )
        }
        
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

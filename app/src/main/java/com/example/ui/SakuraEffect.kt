package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.*
fun SakuraFallingCanvas(modifier: Modifier = Modifier) {
    val petalCount = 18 // Subtle density to remain elegant and beautiful
    val starCount = 12

    // Pre-allocate the petals to avoid allocations on frames
    val petals = remember {
        val random = java.util.Random(1337)
        List(petalCount) {
            SakuraPetal(
                initialXRatio = random.nextFloat(),
                initialYRatio = random.nextFloat(),
                sizeDp = 6f + random.nextFloat() * 10f, // 6dp to 16dp
                speedY = 0.12f + random.nextFloat() * 0.12f,
                windX = 0.04f + random.nextFloat() * 0.06f,
                swayAmplitude = 12f + random.nextFloat() * 20f,
                swayFrequency = 1f + random.nextFloat() * 1.2f,
                initialRotation = random.nextFloat() * 360f,
                rotationSpeed = 30f + random.nextFloat() * 60f,
                alpha = 0.35f + random.nextFloat() * 0.45f
            )
        }
    }

    // Pre-allocate stars for stardust sparkle
    val stars = remember {
        val random = java.util.Random(2026)
        val colors = listOf(
            Color(0xFFFFF9C4), // Soft warm yellow star
            Color(0xFFE1BEE7), // Dreamy lavender star
            Color(0xFFF8BBD0), // Soft pink star
            Color.White        // Shimmering white star
        )
        List(starCount) {
            StarParticle(
                initialXRatio = random.nextFloat(),
                initialYRatio = random.nextFloat(),
                sizeDp = 4f + random.nextFloat() * 6f, // 4dp to 10dp
                speedY = 0.08f + random.nextFloat() * 0.1f,
                windX = 0.02f + random.nextFloat() * 0.04f,
                initialRotation = random.nextFloat() * 360f,
                rotationSpeed = 20f + random.nextFloat() * 40f,
                baseAlpha = 0.4f + random.nextFloat() * 0.5f,
                twinkleFreq = 1.5f + random.nextFloat() * 2.5f,
                color = colors[random.nextInt(colors.size)]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sakura_infinite")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sakura_progress"
    )

    // A normalized notched petal path (width ~1f, height ~1f)
    val basePetalPath = remember {
        Path().apply {
            moveTo(0f, 0.4f)
            cubicTo(-0.4f, 0.2f, -0.5f, -0.2f, -0.2f, -0.4f)
            lineTo(0f, -0.2f)
            lineTo(0.2f, -0.4f)
            cubicTo(0.5f, -0.2f, 0.4f, 0.2f, 0f, 0.4f)
            close()
        }
    }

    // A normalized sparkling 4-pointed star path
    val baseStarPath = remember {
        Path().apply {
            moveTo(0f, -0.5f)
            quadraticTo(0f, 0f, 0.5f, 0f)
            quadraticTo(0f, 0f, 0f, 0.5f)
            quadraticTo(0f, 0f, -0.5f, 0f)
            quadraticTo(0f, 0f, 0f, -0.5f)
            close()
        }
    }

    val petalColor = Color(0xFFFFB7C5) // Soft, authentic Sakura pink

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width > 0 && height > 0) {
            // 1. Draw drifting shimmering stars
            stars.forEach { star ->
                val rawY = (star.initialYRatio + (progress * star.speedY)) % 1.0f
                val paddingY = 30f
                val currentY = rawY * (height + paddingY * 2) - paddingY

                val rawX = (star.initialXRatio + (progress * star.windX)) % 1.0f
                val currentX = (rawX * width) % width

                val currentRotation = star.initialRotation + (progress * star.rotationSpeed)
                val starSizePx = star.sizeDp.dp.toPx()

                // Calculate animated twinkle alpha
                val twinkle = 0.3f + 0.7f * kotlin.math.abs(
                    kotlin.math.sin(progress * star.twinkleFreq * 2 * kotlin.math.PI.toFloat())
                )
                val finalAlpha = star.baseAlpha * twinkle

                rotate(currentRotation, pivot = Offset(currentX, currentY)) {
                    translate(left = currentX, top = currentY) {
                        drawScopeScale(scaleX = starSizePx, scaleY = starSizePx, pivot = Offset.Zero) {
                            drawPath(
                                path = baseStarPath,
                                color = star.color.copy(alpha = finalAlpha)
                            )
                        }
                    }
                }
            }

            // 2. Draw falling sakura petals
            petals.forEach { petal ->
                // Calculate dynamic coordinates with screen wrapping
                val rawY = (petal.initialYRatio + (progress * petal.speedY)) % 1.0f
                val paddingY = 40f
                val currentY = rawY * (height + paddingY * 2) - paddingY

                // Swaying horizontal motion
                val sway = kotlin.math.sin(progress * petal.swayFrequency * 2 * kotlin.math.PI.toFloat()) * petal.swayAmplitude
                val rawX = (petal.initialXRatio + (progress * petal.windX)) % 1.0f
                val currentX = (rawX * width + sway) % width

                val currentRotation = petal.initialRotation + (progress * petal.rotationSpeed)
                val petalSizePx = petal.sizeDp.dp.toPx()

                // Rotate, translate, scale & draw path efficiently
                rotate(currentRotation, pivot = Offset(currentX, currentY)) {
                    translate(left = currentX, top = currentY) {
                        drawScopeScale(scaleX = petalSizePx, scaleY = petalSizePx, pivot = Offset.Zero) {
                            drawPath(
                                path = basePetalPath,
                                color = petalColor.copy(alpha = petal.alpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SakuraOverlay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

@Composable

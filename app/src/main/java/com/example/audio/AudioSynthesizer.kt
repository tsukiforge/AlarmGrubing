package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

object AudioSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun play(toneType: String) {
        stop()
        isPlaying = true
        job = scope.launch {
            val sampleRate = 44100
            val numSamples = 44100 / 2 // 0.5 sec beep
            val generatedSnd = ByteArray(2 * numSamples)
            
            // Configure track
            val minSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            @Suppress("DEPRECATION")
            val track = AudioTrack(
                AudioManager.STREAM_ALARM,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minSize.coerceAtLeast(generatedSnd.size),
                AudioTrack.MODE_STREAM
            )
            audioTrack = track
            track.play()

            val frequencies = when (toneType) {
                "custom_1" -> doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6 (Arpeggio Chime)
                "custom_2" -> doubleArrayOf(880.0, 880.0, 0.0, 880.0) // Beeps
                "custom_sakura" -> doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.50) // Sakura Pentatonic Sweet Chime
                "custom_anime" -> doubleArrayOf(587.33, 783.99, 880.0, 1174.66, 1318.51) // Shinobi energetic riff
                else -> doubleArrayOf(440.0, 554.37, 659.25, 880.0) // Gentle chord
            }

            var phase = 0.0
            val volume = 0.5

            while (isPlaying) {
                for (freq in frequencies) {
                    if (!isPlaying) break
                    if (freq == 0.0) {
                        val buffer = ShortArray(44100 / 4) // 0.25s silence
                        track.write(buffer, 0, buffer.size)
                        continue
                    }
                    val duration = 0.35 // seconds per tone
                    val count = (sampleRate * duration).toInt()
                    val buffer = ShortArray(count)
                    val freqRad = 2.0 * Math.PI * freq / sampleRate
                    for (i in 0 until count) {
                        val envelope = if (i < 500) {
                            i / 500.0
                        } else if (i > count - 1000) {
                            (count - i) / 1000.0
                        } else {
                            1.0
                        }
                        buffer[i] = (sin(phase) * 32767.0 * volume * envelope).toInt().toShort()
                        phase += freqRad
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }
                    }
                    track.write(buffer, 0, buffer.size)
                }
            }
        }
    }

    fun stop() {
        isPlaying = false
        job?.cancel()
        job = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null
    }
}

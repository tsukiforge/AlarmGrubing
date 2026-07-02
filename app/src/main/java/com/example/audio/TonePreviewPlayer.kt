package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.*
import java.io.File

object TonePreviewPlayer {
    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null
    private var previewJob: Job? = null
    private val previewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Tracks which tone is currently playing as a preview
    var currentlyPlayingTone: String? = null
        private set

    // Simple listener to update Compose UI
    private var onStateChangedListener: (() -> Unit)? = null

    fun setOnStateChangedListener(listener: (() -> Unit)?) {
        onStateChangedListener = listener
    }

    fun isPlaying(tone: String): Boolean {
        return currentlyPlayingTone == tone
    }

    fun play(context: Context, tone: String) {
        // If the same tone is already playing, stop it (toggle action)
        if (currentlyPlayingTone == tone) {
            stop()
            return
        }

        // Stop any currently playing preview
        stop()

        currentlyPlayingTone = tone
        onStateChangedListener?.invoke()

        val appContext = context.applicationContext

        previewJob = previewScope.launch {
            try {
                if (tone.startsWith("custom_")) {
                    // Play using AudioSynthesizer
                    AudioSynthesizer.play(tone)
                } else if (tone.startsWith("local_file:")) {
                    // Play local physical file
                    val fileName = tone.substringAfter("local_file:")
                    val file = File(appContext.filesDir, "custom_sounds/$fileName")
                    if (file.exists()) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(file.absolutePath)
                            isLooping = true
                            prepare()
                            start()
                        }
                    } else {
                        playDefaultSystem(appContext)
                    }
                } else {
                    // Default ringtone
                    playDefaultSystem(appContext)
                }

                // Auto-stop after 5 seconds
                delay(5000L)
                stop()
            } catch (e: CancellationException) {
                // Job cancelled, normal flow
            } catch (e: Exception) {
                e.printStackTrace()
                stop()
            }
        }
    }

    private fun playDefaultSystem(context: Context) {
        try {
            val alert: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, alert)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        previewJob?.cancel()
        previewJob = null

        currentlyPlayingTone = null

        // Stop AudioSynthesizer
        try {
            AudioSynthesizer.stop()
        } catch (e: Exception) {
            // ignore
        }

        // Stop Ringtone
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // ignore
        }
        ringtone = null

        // Stop and release MediaPlayer
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null

        onStateChangedListener?.invoke()
    }

    fun release() {
        stop()
    }
}

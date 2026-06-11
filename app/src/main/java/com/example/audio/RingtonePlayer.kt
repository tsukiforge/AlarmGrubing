package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import java.io.File

object RingtonePlayer {
    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null

    fun playDefault(context: Context) {
        stop()
        try {
            val alert: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context.applicationContext, alert)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playFromFile(context: Context, fileName: String) {
        stop()
        try {
            val file = File(context.filesDir, "custom_sounds/$fileName")
            if (file.exists()) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    isLooping = true
                    prepare()
                    start()
                }
            } else {
                playDefault(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            playDefault(context)
        }
    }

    fun stop() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // ignore
        }
        ringtone = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
    }
}


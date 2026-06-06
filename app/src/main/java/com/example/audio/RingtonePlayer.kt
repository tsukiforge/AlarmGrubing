package com.example.audio

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

object RingtonePlayer {
    private var ringtone: Ringtone? = null

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

    fun stop() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // ignore
        }
        ringtone = null
    }
}

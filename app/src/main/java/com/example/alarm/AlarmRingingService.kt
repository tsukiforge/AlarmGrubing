package com.example.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.audio.AudioSynthesizer
import com.example.audio.RingtonePlayer

class AlarmRingingService : Service() {
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "alarm_ringing_channel"
        const val NOTIFICATION_ID = 9999
        var isRinging = false
        var activeAlarmId: String? = null
        var activeAlarmTitle: String? = null
        var activeAlarmIsGroup: Boolean = false
        var activeAlarmGroupCode: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getStringExtra("ALARM_ID") ?: "unknown"
        val title = intent?.getStringExtra("ALARM_TITLE") ?: "Alarm"
        val tone = intent?.getStringExtra("ALARM_TONE") ?: "default"
        val isGroup = intent?.getBooleanExtra("ALARM_IS_GROUP", false) ?: false
        val groupCode = intent?.getStringExtra("ALARM_GROUP_CODE")

        // 1. Acquire Partial WakeLock to prevent the CPU from sleeping in Doze Mode / Extreme Battery Saver
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlarmSync::AlarmRingingServiceWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
        }
        try {
            // Acquire wake lock with a 10-minute timeout as a safe guard against battery drainage if orphaned
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isRinging = true
        activeAlarmId = alarmId
        activeAlarmTitle = title
        activeAlarmIsGroup = isGroup
        activeAlarmGroupCode = groupCode

        // Trigger Broadcast to UI if active
        val uiUpdateIntent = Intent("ALARM_RINGING_UPDATE").apply {
            putExtra("IS_RINGING", true)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TITLE", title)
            putExtra("ALARM_IS_GROUP", isGroup)
            putExtra("ALARM_GROUP_CODE", groupCode)
        }
        sendBroadcast(uiUpdateIntent)

        // Notification Action "Stop"
        val stopPendingIntent = PendingIntent.getService(
            this,
            123,
            Intent(this, AlarmRingingService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent: open app
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("START_RINGING_UI", true)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TITLE", title)
            putExtra("ALARM_IS_GROUP", isGroup)
            putExtra("ALARM_GROUP_CODE", groupCode)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm Berdering! ⏰")
            .setContentText(if (isGroup) "👥 Grup ($groupCode): $title" else "👤 Pribadi: $title")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Force launch MainActivity to display ringing UI immediately
        try {
            startActivity(contentIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play Tone
        val sharedPrefs = getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)

        val forceFullVolume = sharedPrefs.getBoolean("force_full_volume", false)
        if (forceFullVolume) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            sharedPrefs.edit().putInt("original_alarm_volume", originalVolume).apply()
            am.setStreamVolume(AudioManager.STREAM_ALARM, 
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }

        val autoSpeaker = sharedPrefs.getBoolean("auto_speaker_headset", false)
        if (autoSpeaker) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.isWiredHeadsetOn || am.isBluetoothA2dpOn) {
                am.isSpeakerphoneOn = true
                am.mode = AudioManager.MODE_IN_COMMUNICATION
            }
        }

        if (tone.startsWith("custom_")) {
            AudioSynthesizer.play(tone)
        } else if (tone.startsWith("local_file:")) {
            val fileName = tone.substringAfter("local_file:")
            RingtonePlayer.playFromFile(this, fileName)
        } else {
            RingtonePlayer.playDefault(this)
        }

        // Vibrate
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 1))
        } else {
            vibrator?.vibrate(longArrayOf(0, 500, 500), 1)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun stopAlarm() {
        isRinging = false
        activeAlarmId = null
        activeAlarmTitle = null
        AudioSynthesizer.stop()
        RingtonePlayer.stop()
        try {
            vibrator?.cancel()
        } catch (e: Exception) {}

        val sharedPrefs = getSharedPreferences("alarm_grup_prefs", Context.MODE_PRIVATE)
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        if (sharedPrefs.contains("original_alarm_volume")) {
            val originalVolume = sharedPrefs.getInt("original_alarm_volume", -1)
            if (originalVolume != -1) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            }
            sharedPrefs.edit().remove("original_alarm_volume").apply()
        }

        val autoSpeaker = sharedPrefs.getBoolean("auto_speaker_headset", false)
        if (autoSpeaker) {
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_NORMAL
        }

        // Release Partial WakeLock safely
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null

        val uiUpdateIntent = Intent("ALARM_RINGING_UPDATE").apply {
            putExtra("IS_RINGING", false)
        }
        sendBroadcast(uiUpdateIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Ringing Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for sounding group alarms"
                setSound(null, null)
                enableLights(true)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

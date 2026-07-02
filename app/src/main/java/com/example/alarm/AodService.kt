package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ui.AodActivity

class AodService : Service() {

    companion object {
        private const val TAG = "AodService"
        private const val AOD_CHANNEL_ID = "aod_service_channel"
        private const val AOD_TRIGGER_CHANNEL_ID = "aod_active_channel"
        private const val FOREGROUND_NOTIF_ID = 999
        private const val TRIGGER_NOTIF_ID = 1001

        // Waktu WakeLock cukup panjang agar activity sempat fully drawn (~10 detik)
        private const val WAKE_LOCK_TIMEOUT_MS = 10_000L
    }

    private var wakeLock: PowerManager.WakeLock? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> handleScreenOff(context)
                Intent.ACTION_SCREEN_ON -> {
                    // Layar menyala lagi (misal user tekan power) — lepas WakeLock kalau masih ada
                    releaseWakeLockSafely()
                }
            }
        }
    }

    private fun handleScreenOff(context: Context) {
        val prefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        val aodEnabled = prefs.getBoolean("aod_enabled", false)
        if (!aodEnabled) {
            Log.d(TAG, "AOD tidak aktif, skip.")
            return
        }

        Log.d(TAG, "Layar mati + AOD aktif → meluncurkan AodActivity via fullScreenIntent")

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            // Acquire WakeLock FULL_WAKE_LOCK agar layar menyala kembali
            // FULL_WAKE_LOCK deprecated di API 17 tapi masih functional untuk custom AOD
            @Suppress("DEPRECATION")
            val wl = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "AlarmGrup:AodWakeLock"
            )
            wl.acquire(WAKE_LOCK_TIMEOUT_MS)
            wakeLock = wl
            Log.d(TAG, "WakeLock acquired (${WAKE_LOCK_TIMEOUT_MS}ms)")

            // Buat intent ke AodActivity
            val aodIntent = Intent(context, AodActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                // Pass timestamp agar activity tahu kapan AOD dimulai
                putExtra("aod_trigger_time", System.currentTimeMillis())
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                9999,
                aodIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // FIX UTAMA: Gunakan full-screen notification yang TIDAK di-cancel agar
            // Android dapat menampilkan AodActivity di atas lock screen.
            // Activity akan dismiss notification-nya sendiri lewat ACTION_DISMISS_AOD_NOTIF.
            ensureTriggerChannelExists()

            val triggerNotification = NotificationCompat.Builder(context, AOD_TRIGGER_CHANNEL_ID)
                .setContentTitle("Always-On Display")
                .setContentText("Layar standby aktif")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL) // CATEGORY_CALL paling reliable untuk full-screen
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setSilent(true)
                .setOngoing(true)   // Tidak auto-dismiss — dibiarkan sampai AOD Activity selesai
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(TRIGGER_NOTIF_ID, triggerNotification)

        } catch (e: Exception) {
            Log.e(TAG, "Gagal meluncurkan AOD: ${e.message}", e)
        }
    }

    private fun releaseWakeLockSafely() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
            Log.d(TAG, "WakeLock dilepas")
        } catch (e: Exception) {
            Log.w(TAG, "Gagal melepas WakeLock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    private fun ensureTriggerChannelExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(AOD_TRIGGER_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    AOD_TRIGGER_CHANNEL_ID,
                    "AOD Trigger",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Listen SCREEN_OFF dan SCREEN_ON
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
        Log.d(TAG, "AodService dimulai, menunggu ACTION_SCREEN_OFF")

        // Foreground notification agar service tidak dikill OS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AOD_CHANNEL_ID,
                "AOD Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, AOD_CHANNEL_ID)
            .setContentTitle("Always-On Display (AOD)")
            .setContentText("AOD akan otomatis aktif saat layar mati.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
        startForeground(FOREGROUND_NOTIF_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // AodActivity mengirim intent ini saat di-dismiss supaya notifikasi trigger di-cancel
        if (intent?.action == "ACTION_DISMISS_AOD_NOTIF") {
            Log.d(TAG, "AOD selesai — membatalkan trigger notification")
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(TRIGGER_NOTIF_ID)
            releaseWakeLockSafely()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        releaseWakeLockSafely()
        Log.d(TAG, "AodService dihancurkan")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

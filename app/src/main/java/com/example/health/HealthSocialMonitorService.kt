package com.example.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStats
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ui.HealthTimeChecker
import com.example.ui.isAppCurrentlyLocked
import com.example.ui.loadSchedules
import com.example.R
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * Foreground Service yang memonitor aplikasi foreground menggunakan UsageStatsManager.
 * Berjalan hanya saat ada jadwal Health Social yang aktif (battery-aware).
 */
class HealthSocialMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "health_social_monitor_channel"
        const val NOTIFICATION_ID = 8001
        const val TAG = "HealthMonitorSvc"
        var isRunning = false
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())

        // Mulai monitoring loop
        monitoringJob = scope.launch {
            monitorLoop()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }

    private fun stopMonitoring() {
        isRunning = false
        monitoringJob?.cancel()
        scope.cancel()
    }

    private suspend fun monitorLoop() {
        while (isActive) {
            try {
                val schedule = isAppCurrentlyLocked(this)
                if (schedule != null) {
                    // Ada jadwal aktif — deteksi app foreground
                    val foregroundApp = getCurrentForegroundPackage()
                    if (foregroundApp != null && schedule.lockedApps.contains(foregroundApp)) {
                        // App yang sedang dibuka ada di daftar terkunci
                        HealthSocialOverlayManager.showLockOverlay(
                            this,
                            schedule.name,
                            schedule.icon,
                            foregroundApp
                        )
                    }
                }

                // Cek apakah masih ada jadwal aktif — jika tidak, stop service
                val activeSchedules = loadSchedules(this).filter { it.isActive }
                if (activeSchedules.isEmpty()) {
                    stopMonitoring()
                    stopSelf()
                    return
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Monitoring error: ${e.message}", e)
            }

            // Polling tiap 2 detik untuk respon cepat
            delay(2000L)
        }
    }

    /**
     * Mendapatkan package name dari aplikasi yang sedang di foreground.
     * Menggunakan UsageStatsManager dengan interval waktu pendek (query 1 menit terakhir).
     */
    private fun getCurrentForegroundPackage(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 60_000L // 1 menit terakhir

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            if (stats.isNullOrEmpty()) return null

            // Cari app dengan usage time paling besar (terbaru) yang bukan launcher/system
            val launcherPackage = getLauncherPackageName()
            stats
                .filter { it.packageName != launcherPackage }
                .maxByOrNull { it.lastTimeUsed }
                ?.packageName
        } catch (e: SecurityException) {
            // Permission PACKAGE_USAGE_STATS belum diberikan
            android.util.Log.w(TAG, "UsageStats permission not granted")
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting foreground app: ${e.message}")
            null
        }
    }

    private fun getLauncherPackageName(): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Social Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi monitoring aplikasi untuk fitur Health Social"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Health Social Aktif")
            .setContentText("Monitoring aplikasi sedang berjalan")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}

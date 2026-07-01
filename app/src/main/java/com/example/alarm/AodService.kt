package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ui.AodActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AodService : Service() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                val prefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
                val aodEnabled = prefs.getBoolean("aod_enabled", false)
                if (aodEnabled) {
                    val pendingResult = goAsync()
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            delay(100)
                            
                            // Acquire wake lock to wake screen and keep CPU active
                            val wakeLock = powerManager.newWakeLock(
                                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                                "AlarmGrup:AodWakeLock"
                            )
                            wakeLock.acquire(1500)
                            
                            val aodIntent = Intent(context, AodActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            
                            // Prepare Full Screen Intent via Notification to bypass background start restrictions
                            val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
                                context,
                                9999,
                                aodIntent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                            )
                            
                            // Try starting activity directly first
                            context.startActivity(aodIntent)
                            
                            // Trigger high-priority silent notification with fullScreenIntent to force immediate lockscreen display
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val activeChannelId = "aod_active_channel"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val activeChannel = NotificationChannel(
                                    activeChannelId,
                                    "AOD Active Screen Channel",
                                    NotificationManager.IMPORTANCE_HIGH
                                ).apply {
                                    setSound(null, null)
                                    enableVibration(false)
                                    setShowBadge(false)
                                }
                                notificationManager.createNotificationChannel(activeChannel)
                            }
                            
                            val activeNotification = NotificationCompat.Builder(context, activeChannelId)
                                .setContentTitle("AOD Aktif")
                                .setContentText("Layar AOD sedang berjalan.")
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setCategory(NotificationCompat.CATEGORY_STATUS)
                                .setFullScreenIntent(fullScreenPendingIntent, true)
                                .setSilent(true)
                                .setAutoCancel(true)
                                .build()
                                
                            notificationManager.notify(1001, activeNotification)
                            
                            // Automatically remove the notification after a brief delay
                            delay(1200)
                            notificationManager.cancel(1001)
                            
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
        
        // Use Foreground Service to keep running in background reliably
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "aod_service_channel"
            val channelName = "AOD Background Service"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            
            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Always-On Display (AOD)")
                .setContentText("AOD akan otomatis berjalan saat layar mati.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
            startForeground(999, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

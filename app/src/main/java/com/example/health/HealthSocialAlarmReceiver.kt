package com.example.health

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ui.loadSchedules
import com.example.ui.HealthTimeChecker
import java.util.Calendar

/**
 * BroadcastReceiver untuk menangani alarm start/stop jadwal Health Social.
 * Dipicu oleh AlarmManager saat jadwal mulai/selesai.
 */
class HealthSocialAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "health_social_schedule_channel"
        const val TAG = "HealthAlarmRcvr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra("SCHEDULE_ID") ?: return
        val action = intent.action ?: return

        createNotificationChannel(context)

        when (action) {
            "ACTION_SCHEDULE_START" -> {
                android.util.Log.d(TAG, "Schedule started: $scheduleId")

                // Kirim notifikasi mode dimulai
                showNotification(
                    context,
                    "🎯 Mode ${getScheduleName(context, scheduleId)} Dimulai",
                    "Jadwal ${getScheduleName(context, scheduleId)} telah aktif. Aplikasi terkunci. Tetap fokus! ✨",
                    9001
                )

                // Mulai monitoring service
                HealthSocialAlarmScheduler.startMonitorService(context)
            }

            "ACTION_SCHEDULE_END" -> {
                android.util.Log.d(TAG, "Schedule ended: $scheduleId")

                // Kirim notifikasi mode selesai
                showNotification(
                    context,
                    "✅ Mode ${getScheduleName(context, scheduleId)} Selesai",
                    "Jadwal ${getScheduleName(context, scheduleId)} telah berakhir. Aplikasi tidak lagi dikunci. Semangat! 🎉",
                    9002
                )

                // Cek apakah masih ada jadwal aktif — jika tidak, stop service
                val activeSchedules = loadSchedules(context).filter { it.isActive }
                val nowMinutes = HealthTimeChecker.timeToMinutes(
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(Calendar.getInstance().time)
                )
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayName = when (dayOfWeek) {
                    Calendar.MONDAY -> "Sen"
                    Calendar.TUESDAY -> "Sel"
                    Calendar.WEDNESDAY -> "Rab"
                    Calendar.THURSDAY -> "Kam"
                    Calendar.FRIDAY -> "Jum"
                    Calendar.SATURDAY -> "Sab"
                    Calendar.SUNDAY -> "Min"
                    else -> ""
                }

                val anyStillActive = activeSchedules.any { schedule ->
                    HealthTimeChecker.isScheduleActive(
                        schedule.startTime, schedule.endTime,
                        schedule.days, dayName, nowMinutes
                    )
                }

                if (!anyStillActive) {
                    HealthSocialAlarmScheduler.stopMonitorService(context)
                }
            }
        }
    }

    private fun getScheduleName(context: Context, scheduleId: String): String {
        val schedules = loadSchedules(context)
        return schedules.find { it.id == scheduleId }?.name ?: scheduleId
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Social Schedule",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi jadwal Health Social dimulai/selesai"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}

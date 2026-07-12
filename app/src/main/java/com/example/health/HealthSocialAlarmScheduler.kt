package com.example.health

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ui.HealthSchedule
import com.example.ui.HealthTimeChecker
import com.example.ui.loadSchedules
import com.example.ui.saveSchedule
import java.util.Calendar

/**
 * Mengelola AlarmManager untuk trigger start/stop monitoring service
 * tepat di jam mulai dan selesai jadwal Health Social.
 */
object HealthSocialAlarmScheduler {

    private const val TAG = "HealthAlarmScheduler"

    /**
     * Register semua alarm untuk jadwal yang aktif.
     * Dipanggil saat:
     * - Jadwal diaktifkan/dimatikan
     * - HP restart (dari BootReceiver)
     * - Waktu/jadwal berubah
     */
    fun rescheduleAll(context: Context) {
        // Cancel semua alarm lama
        cancelAllAlarms(context)

        val schedules = loadSchedules(context).filter { it.isActive }
        if (schedules.isEmpty()) {
            // Tidak ada jadwal aktif — stop service
            stopMonitorService(context)
            return
        }

        // Register alarm untuk setiap jadwal aktif
        for (schedule in schedules) {
            scheduleStartAlarm(context, schedule)
            scheduleEndAlarm(context, schedule)
        }

        // Cek apakah saat ini ada jadwal yang aktif — jika ya, start service sekarang
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

        val isAnyActive = schedules.any { schedule ->
            HealthTimeChecker.isScheduleActive(
                schedule.startTime, schedule.endTime,
                schedule.days, dayName, nowMinutes
            )
        }

        if (isAnyActive) {
            startMonitorService(context)
        } else {
            stopMonitorService(context)
        }
    }

    /**
     * Mendaftarkan alarm untuk memulai monitoring saat jadwal mulai.
     */
    private fun scheduleStartAlarm(context: Context, schedule: HealthSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, HealthSocialAlarmReceiver::class.java).apply {
            action = "ACTION_SCHEDULE_START"
            putExtra("SCHEDULE_ID", schedule.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hitung waktu trigger berdasarkan hari dan jam
        scheduleAlarm(context, alarmManager, schedule, schedule.startTime, pendingIntent, "ACTION_SCHEDULE_START")
    }

    /**
     * Mendaftarkan alarm untuk menghentikan monitoring saat jadwal selesai.
     */
    private fun scheduleEndAlarm(context: Context, schedule: HealthSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, HealthSocialAlarmReceiver::class.java).apply {
            action = "ACTION_SCHEDULE_END"
            putExtra("SCHEDULE_ID", schedule.id)
        }

        // Gunakan ID negatif untuk membedakan dengan start alarm
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            -(schedule.id.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(context, alarmManager, schedule, schedule.endTime, pendingIntent, "ACTION_SCHEDULE_END")
    }

    /**
     * Menghitung waktu alarm berikutnya berdasarkan jadwal (hari & jam).
     * Untuk jadwal overnight (end < start), end alarm dijadwalkan ke hari berikutnya.
     */
    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        schedule: HealthSchedule,
        timeStr: String,
        pendingIntent: PendingIntent,
        actionName: String
    ) {
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Jika waktu sudah lewat, tambah 1 hari
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Cari hari berikutnya yang sesuai dengan jadwal (maks 14 hari ke depan)
        var attempts = 0
        while (attempts < 14) {
            val calDay = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (calDay) {
                Calendar.MONDAY -> "Sen"
                Calendar.TUESDAY -> "Sel"
                Calendar.WEDNESDAY -> "Rab"
                Calendar.THURSDAY -> "Kam"
                Calendar.FRIDAY -> "Jum"
                Calendar.SATURDAY -> "Sab"
                Calendar.SUNDAY -> "Min"
                else -> ""
            }
            if (schedule.days.contains(dayName)) {
                break
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            attempts++
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }

        android.util.Log.d(TAG, "Scheduled $actionName for ${schedule.id} at $timeStr on ${calendar.time}")
    }

    private fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Cancel dengan pattern umum — kita cancel semua pending intent
        // Pendekatan simpel: intent filter tidak bisa, jadi kita cancel satuan
        val schedules = loadSchedules(context)
        for (schedule in schedules) {
            val startIntent = Intent(context, HealthSocialAlarmReceiver::class.java).apply {
                action = "ACTION_SCHEDULE_START"
                putExtra("SCHEDULE_ID", schedule.id)
            }
            val startPending = PendingIntent.getBroadcast(
                context,
                schedule.id.hashCode(),
                startIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            startPending?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            val endIntent = Intent(context, HealthSocialAlarmReceiver::class.java).apply {
                action = "ACTION_SCHEDULE_END"
                putExtra("SCHEDULE_ID", schedule.id)
            }
            val endPending = PendingIntent.getBroadcast(
                context,
                -(schedule.id.hashCode()),
                endIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            endPending?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    /**
     * Memulai foreground service monitoring
     */
    fun startMonitorService(context: Context) {
        val intent = Intent(context, HealthSocialMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Menghentikan foreground service monitoring
     */
    fun stopMonitorService(context: Context) {
        val intent = Intent(context, HealthSocialMonitorService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)
    }
}

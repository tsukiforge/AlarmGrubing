package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val alarmDao = db.alarmDao()
                    val enabledAlarms = alarmDao.getEnabledAlarmsSync()
                    enabledAlarms.forEach { alarm ->
                        AlarmScheduler.schedule(context, alarm)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        val alarmId = intent.getStringExtra("ALARM_ID") ?: return
        val title = intent.getStringExtra("ALARM_TITLE") ?: "Alarm"
        val tone = intent.getStringExtra("ALARM_TONE") ?: "default"
        val isGroup = intent.getBooleanExtra("ALARM_IS_GROUP", false)
        val groupCode = intent.getStringExtra("ALARM_GROUP_CODE")

        // 1. Acquire WakeLock to turn on CPU and screen
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AlarmSync::AlarmReceiverWakeLock"
            )
            wakeLock.acquire(10000) // 10 seconds is plenty to start the service
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Start Foreground Ringing Service
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TITLE", title)
            putExtra("ALARM_TONE", tone)
            putExtra("ALARM_IS_GROUP", isGroup)
            putExtra("ALARM_GROUP_CODE", groupCode)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 3. Reschedule repeat alarms or disable single alarm
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val alarmDao = db.alarmDao()
                val alarm = alarmDao.getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.daysOfWeek.isBlank()) {
                        // One-time alarm: disable it in Room
                        val updated = alarm.copy(isEnabled = false)
                        alarmDao.updateAlarm(updated)
                    } else {
                        // Repeating alarm: schedule next wake-up
                        AlarmScheduler.schedule(context, alarm)
                        android.util.Log.d("AlarmReceiver", "Successfully rescheduled repeating alarm: ${alarm.id}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

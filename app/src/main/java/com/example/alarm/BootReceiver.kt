package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Rescheduling all enabled alarms...")
            
            // Resume AOD Service if enabled
            val aodPrefs = context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
            if (aodPrefs.getBoolean("aod_enabled", false)) {
                val serviceIntent = Intent(context, AodService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val alarmDao = db.alarmDao()
                    val enabledAlarms = alarmDao.getEnabledAlarmsSync()
                    
                    var rescheduledCount = 0
                    enabledAlarms.forEach { alarm ->
                        AlarmScheduler.schedule(context, alarm)
                        rescheduledCount++
                    }
                    Log.d("BootReceiver", "Successfully rescheduled $rescheduledCount alarms.")
                    
                    MotivationScheduler.scheduleDailyMotivation(context)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms after boot", e)
                }
            }
        }
    }
}

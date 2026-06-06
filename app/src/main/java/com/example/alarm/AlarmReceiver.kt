package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("ALARM_ID") ?: return
        val title = intent.getStringExtra("ALARM_TITLE") ?: "Alarm"
        val tone = intent.getStringExtra("ALARM_TONE") ?: "default"
        val isGroup = intent.getBooleanExtra("ALARM_IS_GROUP", false)
        val groupCode = intent.getStringExtra("ALARM_GROUP_CODE")

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
    }
}

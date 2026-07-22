package com.example.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.example.ui.HealthSchedule
import com.example.ui.HealthTimeChecker
import com.example.ui.loadSchedules

object AppLockHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val pref = context.packageName + "/" + AppLockAccessibilityService::class.java.canonicalName
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(pref)
    }

    fun getForegroundAppPackage(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 5 // last 5 seconds

        val usageEvents = usm.queryEvents(startTime, endTime)
        var foregroundPackage: String? = null
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPackage = event.packageName
            }
        }
        return foregroundPackage
    }

    fun getCurrentlyLockedAppSchedule(context: Context, targetPackageName: String? = null): Pair<HealthSchedule, String>? {
        val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
        val isLockEnabled = prefs.getBoolean("app_lock_enabled", false)
        if (!isLockEnabled) return null

        val bypassUntil = prefs.getLong("bypass_until", 0L)
        if (System.currentTimeMillis() < bypassUntil) return null

        val schedules = loadSchedules(context)
        val activeSchedules = schedules.filter { it.isActive }
        if (activeSchedules.isEmpty()) return null

        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val dayName = when (dayOfWeek) {
            java.util.Calendar.MONDAY -> "Sen"
            java.util.Calendar.TUESDAY -> "Sel"
            java.util.Calendar.WEDNESDAY -> "Rab"
            java.util.Calendar.THURSDAY -> "Kam"
            java.util.Calendar.FRIDAY -> "Jum"
            java.util.Calendar.SATURDAY -> "Sab"
            java.util.Calendar.SUNDAY -> "Min"
            else -> ""
        }

        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val currentTimeStr = sdf.format(calendar.time)
        val currentMinutes = HealthTimeChecker.timeToMinutes(currentTimeStr)

        val pkgToCheck = targetPackageName ?: getForegroundAppPackage(context) ?: return null
        if (pkgToCheck == context.packageName) return null

        for (schedule in activeSchedules) {
            if (HealthTimeChecker.isScheduleActive(
                    schedule.startTime,
                    schedule.endTime,
                    schedule.days,
                    dayName,
                    currentMinutes
                )
            ) {
                if (schedule.lockedApps.contains(pkgToCheck)) {
                    return Pair(schedule, pkgToCheck)
                }
            }
        }
        return null
    }
}

package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity

class AppLockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        val lockMatch = AppLockHelper.getCurrentlyLockedAppSchedule(applicationContext, packageName)
        if (lockMatch != null) {
            val (schedule, lockedPkg) = lockMatch
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("LOCKED_BY_SCHEDULE_ID", schedule.id)
                putExtra("LOCKED_PACKAGE_NAME", lockedPkg)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}
}

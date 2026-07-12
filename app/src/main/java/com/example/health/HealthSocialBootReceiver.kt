package com.example.health

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver untuk BOOT_COMPLETED — me-reschedule alarm Health Social
 * setelah HP restart agar jadwal tetap berfungsi.
 */
class HealthSocialBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HealthBootRcvr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            Log.d(TAG, "Device rebooted — rescheduling Health Social alarms")

            // Reschedule semua jadwal aktif
            HealthSocialAlarmScheduler.rescheduleAll(context)
        }
    }
}

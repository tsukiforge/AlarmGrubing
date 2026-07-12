package com.example.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.provider.Settings
import com.example.health.HealthSocialOverlayManager

/**
 * Quick Settings Tile: "Kunci Aplikasi Ini"
 *
 * Saat tile di-tap:
 * - Jika user sedang di dalam sebuah aplikasi → aplikasi tsb langsung dikunci
 *   via overlay Health Social (terlepas dari jadwal).
 * - Jika user di home screen → tile tidak melakukan apa-apa (status nonaktif).
 *
 * Membutuhkan permission: PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW.
 */
class AppLockTileService : TileService() {

    companion object {
        private const val TAG = "AppLockTile"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Cek permission
        if (!checkPermissions()) {
            // Permission belum diberikan — arahkan ke settings
            showDialog()
            return
        }

        // Deteksi app foreground
        val foregroundApp = getCurrentForegroundPackage()
        if (foregroundApp == null) {
            // Tidak bisa deteksi app — mungkin di home screen
            showDialog()
            return
        }

        // Cek apakah app adalah launcher/home
        val launcherPackage = getLauncherPackageName()
        if (foregroundApp == launcherPackage) {
            // Di home screen — tile tidak berfungsi
            showDialog()
            return
        }

        // Kunci app ini sekarang
        HealthSocialOverlayManager.showLockOverlay(
            this,
            "Kunci Manual",
            "🔒",
            foregroundApp
        )

        // Update tile state jadi aktif
        qsTile?.let {
            it.state = Tile.STATE_ACTIVE
            it.updateTile()
        }
    }

    private fun updateTileState() {
        qsTile?.let {
            if (!checkPermissions()) {
                it.state = Tile.STATE_UNAVAILABLE
            } else {
                it.state = Tile.STATE_INACTIVE
            }
            it.updateTile()
        }
    }

    private fun checkPermissions(): Boolean {
        // Cek SYSTEM_ALERT_WINDOW
        if (!Settings.canDrawOverlays(this)) return false

        // Cek PACKAGE_USAGE_STATS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            try {
                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 1000 * 10, // 10 detik
                    currentTime
                )
                if (stats.isNullOrEmpty()) {
                    // Bisa jadi permission belum diberikan
                    try {
                        val appUsage = usm.queryUsageStats(
                            UsageStatsManager.INTERVAL_DAILY,
                            currentTime - 60 * 1000,
                            currentTime
                        )
                    } catch (e: Exception) {
                        return false
                    }
                }
            } catch (e: SecurityException) {
                return false
            } catch (e: Exception) {
                return false
            }
        }

        return true
    }

    private fun getCurrentForegroundPackage(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 60_000L

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            if (stats.isNullOrEmpty()) return null

            stats.maxByOrNull { it.lastTimeUsed }?.packageName
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

    private fun showDialog() {
        if (!checkPermissions()) {
            // Arahkan ke settings permission
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
        }
    }
}

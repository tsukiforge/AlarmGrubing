package com.example.health

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.R

/**
 * Mengelola window overlay (TYPE_APPLICATION_OVERLAY) untuk menampilkan
 * prompt PIN saat aplikasi terkunci terdeteksi.
 *
 * Menggunakan pendekatan classic View (bukan Compose) karena overlay
 * perlu berfungsi di atas aplikasi lain, di luar kendali Activity.
 */
object HealthSocialOverlayManager {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private const val TAG = "HealthOverlay"

    /**
     * Menampilkan overlay kunci di atas aplikasi lain.
     * Overlay menampilkan: nama mode aktif, instruksi, tombol buka dengan PIN / bypass 15 menit.
     */
    fun showLockOverlay(
        context: Context,
        scheduleName: String,
        scheduleIcon: String,
        lockedPackageName: String
    ) {
        // Hapus overlay sebelumnya jika ada
        dismissOverlay()

        if (!Settings.canDrawOverlays(context)) {
            // Arahkan user ke settings
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER or Gravity.FILL
            params.alpha = 0.95f

            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.health_social_overlay, null) ?: run {
                // Fallback: gunakan view sederhana jika layout belum tersedia
                fallbackOverlay(context)
                return
            }

            // Isi data
            view.findViewById<TextView>(R.id.overlay_title)?.text = "$scheduleIcon Aplikasi Terkunci"
            val subtitle = view.findViewById<TextView>(R.id.overlay_subtitle)
            subtitle?.text = "Mode: $scheduleName sedang berjalan"

            val description = view.findViewById<TextView>(R.id.overlay_description)
            description?.text = "Aplikasi yang Anda buka sedang dalam daftar blokir. Tetap fokus dengan komitmen Anda! ✨"

            // Tomol Buka dengan PIN
            val pinButton = view.findViewById<Button>(R.id.overlay_pin_button)
            pinButton?.setOnClickListener {
                // Cek apakah PIN aktif
                val prefs = context.getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
                val isPinEnabled = prefs.getBoolean("pin_enabled", false)
                if (isPinEnabled) {
                    // Buka activity untuk memasukkan PIN
                    val intent = Intent(context, HealthSocialOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("ACTION", "VERIFY_PIN")
                    }
                    context.startActivity(intent)
                } else {
                    // PIN tidak aktif — bypass langsung 15 menit
                    com.example.ui.setTemporaryBypass(context, 15)
                    dismissOverlay()
                }
            }

            // Tombol Bypass 15 Menit
            val bypassButton = view.findViewById<Button>(R.id.overlay_bypass_button)
            bypassButton?.setOnClickListener {
                com.example.ui.setTemporaryBypass(context, 15)
                dismissOverlay()
            }

            // Tombol Buka Settings Permission
            val settingsButton = view.findViewById<Button>(R.id.overlay_settings_button)
            settingsButton?.setOnClickListener {
                dismissOverlay()
            }

            wm.addView(view, params)
            overlayView = view
            windowManager = wm

            android.util.Log.d(TAG, "Overlay shown for locked app: $lockedPackageName")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to show overlay: ${e.message}", e)
        }
    }

    /**
     * Fallback jika layout XML belum tersedia — buat view sederhana secara programatis
     */
    private fun fallbackOverlay(context: Context) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            val textView = TextView(context).apply {
                text = "🔒 Aplikasi sedang dikunci oleh Health Social"
                textSize = 18f
                gravity = Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#99000000"))
                setTextColor(android.graphics.Color.WHITE)
                setOnClickListener {
                    dismissOverlay()
                }
            }

            wm.addView(textView, params)
            overlayView = textView
            windowManager = wm
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Fallback overlay failed: ${e.message}")
        }
    }

    fun dismissOverlay() {
        try {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) {
            // View sudah tidak terattach
        }
        overlayView = null
        windowManager = null
    }

    fun isOverlayShowing(): Boolean = overlayView != null
}

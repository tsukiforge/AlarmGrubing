package com.example.health

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ui.SecurePinStorage
import com.example.ui.setTemporaryBypass

/**
 * Activity transparan yang muncul saat overlay PIN diklik.
 * Menampilkan dialog PIN input untuk memverifikasi identitas user.
 */
class HealthSocialOverlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Buat activity di atas lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val action = intent?.getStringExtra("ACTION") ?: ""
        when (action) {
            "VERIFY_PIN" -> showPinVerificationDialog()
            "BYPASS" -> {
                // Bypass langsung 15 menit
                setTemporaryBypass(this, 15)
                HealthSocialOverlayManager.dismissOverlay()
                finish()
            }
            else -> {
                setTemporaryBypass(this, 15)
                HealthSocialOverlayManager.dismissOverlay()
                finish()
            }
        }
    }

    private fun showPinVerificationDialog() {
        val prefs = getSharedPreferences("health_social_prefs", Context.MODE_PRIVATE)
        val encrypted = prefs.getString("pin_code", "") ?: ""
        val actualPin = if (encrypted.isNotEmpty()) {
            SecurePinStorage.decryptPin(encrypted)
        } else ""

        // Jika tidak ada PIN yang disimpan, bypass langsung
        if (actualPin.isEmpty()) {
            setTemporaryBypass(this, 15)
            HealthSocialOverlayManager.dismissOverlay()
            Toast.makeText(this, "PIN belum diatur — akses dibuka 15 menit", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Buat dialog PIN input
        val input = EditText(this).apply {
            hint = "Masukkan PIN 4 digit"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 18f
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(input, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Verifikasi PIN Keamanan")
            .setMessage("Masukkan PIN 4-digit Anda untuk membuka kunci sementara:")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Buka 🔓") { _, _ ->
                val enteredPin = input.text.toString()
                if (enteredPin == actualPin) {
                    setTemporaryBypass(this, 15)
                    HealthSocialOverlayManager.dismissOverlay()
                    Toast.makeText(this, "Akses dibuka 15 menit ✅", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "PIN salah! Silakan coba lagi ❌", Toast.LENGTH_SHORT).show()
                    // Tampilkan dialog lagi
                    showPinVerificationDialog()
                }
            }
            .setNegativeButton("Batal") { _, _ ->
                // Tetap terkunci — dismiss dialog saja
                // Activity tetap terbuka, overlay tetap muncul
            }
            .show()
    }

    override fun onBackPressed() {
        // Back tidak bisa — tetap terkunci
        // Tapi kita bisa close activity
        HealthSocialOverlayManager.dismissOverlay()
        finish()
    }
}

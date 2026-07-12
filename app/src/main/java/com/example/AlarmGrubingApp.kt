package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppLocalesMetadataHolderService
import com.example.health.HealthSocialAlarmScheduler

class AlarmGrubingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedLocale()
        // Inisialisasi Health Social scheduler — register alarm untuk jadwal aktif
        HealthSocialAlarmScheduler.rescheduleAll(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Apply locale as early as possible, before any UI renders
        applySavedLocale()
    }

    private fun applySavedLocale() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("app_language", "system") ?: "system"

        if (savedLanguage != "system") {
            val localeList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.os.LocaleListCompat.forLanguageTags(savedLanguage)
            } else {
                @Suppress("DEPRECATION")
                val locale = when (savedLanguage) {
                    "id" -> java.util.Locale("in", "ID")
                    "en" -> java.util.Locale("en")
                    "es" -> java.util.Locale("es")
                    "fr" -> java.util.Locale("fr")
                    else -> java.util.Locale(savedLanguage)
                }
                @Suppress("DEPRECATION")
                java.util.Locale.setDefault(locale)
                val compatList = androidx.core.os.LocaleListCompat.create(locale)
                compatList
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
}

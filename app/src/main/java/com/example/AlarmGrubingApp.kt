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

        // Default fallback: jika bahasa sistem bukan id/en/ja, paksa ke Indonesia
        val effectiveLanguage = if (savedLanguage == "system") {
            val systemLang = getSystemLocale()
            if (systemLang !in listOf("id", "en", "ja")) "id" else "system"
        } else savedLanguage

        if (effectiveLanguage != "system") {
            val localeList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.os.LocaleListCompat.forLanguageTags(effectiveLanguage)
            } else {
                @Suppress("DEPRECATION")
                val locale = when (effectiveLanguage) {
                    "id" -> java.util.Locale("in", "ID")
                    "en" -> java.util.Locale("en")
                    "ja" -> java.util.Locale("ja")
                    else -> java.util.Locale(effectiveLanguage)
                }
                @Suppress("DEPRECATION")
                java.util.Locale.setDefault(locale)
                val compatList = androidx.core.os.LocaleListCompat.create(locale)
                compatList
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    private fun getSystemLocale(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales.get(0).language
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale.language
        }
    }
}

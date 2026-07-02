package com.example.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * WeatherRepository — Single Source of Truth untuk data cuaca.
 *
 * FIX BUG 3: Sebelumnya WeatherWidgetProvider.kt dan DashboardWidgets.kt masing-masing
 * memanggil fetchWeather() secara independen tanpa berbagi cache. Akibatnya data antara
 * widget dan in-app bisa berbeda karena di-fetch pada waktu berbeda.
 *
 * Fix: Semua consumer (widget + in-app) membaca dari satu cache SharedPreferences yang
 * di-update oleh repository ini. Cache TTL = 30 menit supaya tidak over-fetch.
 */
object WeatherRepository {

    private const val TAG = "WeatherRepository"
    private const val PREFS_NAME = "weather_cache_prefs"
    private const val KEY_TEMP = "cached_temp"
    private const val KEY_CODE = "cached_code"
    private const val KEY_TIMESTAMP = "cached_timestamp"
    private const val KEY_LAT = "cached_lat"
    private const val KEY_LON = "cached_lon"

    /** Cache TTL: 30 menit (dalam milidetik) */
    private const val CACHE_TTL_MS = 30 * 60 * 1000L

    data class WeatherData(
        val temperatureCelsius: Int,
        val weatherCode: Int,
        val fromCache: Boolean = false
    )

    /**
     * Ambil data cuaca. Jika cache masih valid (< 30 menit dan koordinat sama),
     * kembalikan cache. Jika tidak, fetch ulang dari Open-Meteo dan simpan ke cache.
     */
    suspend fun getWeather(context: Context, lat: Double, lon: Double): WeatherData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val cachedTimestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val cachedLat = prefs.getFloat(KEY_LAT, Float.MIN_VALUE).toDouble()
        val cachedLon = prefs.getFloat(KEY_LON, Float.MIN_VALUE).toDouble()
        val cachedTemp = prefs.getInt(KEY_TEMP, Int.MIN_VALUE)
        val cachedCode = prefs.getInt(KEY_CODE, -1)

        val cacheStillValid = (now - cachedTimestamp) < CACHE_TTL_MS
        val sameCoordinates = Math.abs(cachedLat - lat) < 0.1 && Math.abs(cachedLon - lon) < 0.1
        val cacheHasData = cachedTemp != Int.MIN_VALUE && cachedCode >= 0

        if (cacheStillValid && sameCoordinates && cacheHasData) {
            Log.d(TAG, "Cache hit — suhu=$cachedTemp°C code=$cachedCode (${(now - cachedTimestamp) / 1000}s lalu)")
            return WeatherData(
                temperatureCelsius = cachedTemp,
                weatherCode = cachedCode,
                fromCache = true
            )
        }

        Log.d(TAG, "Cache miss / kadaluarsa — fetch ulang dari Open-Meteo lat=$lat lon=$lon")
        return withContext(Dispatchers.IO) {
            val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val current = json.getJSONObject("current_weather")
            val temp = current.getDouble("temperature").toInt()
            val code = current.getInt("weathercode")

            // Simpan ke cache
            prefs.edit()
                .putInt(KEY_TEMP, temp)
                .putInt(KEY_CODE, code)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .putFloat(KEY_LAT, lat.toFloat())
                .putFloat(KEY_LON, lon.toFloat())
                .apply()

            Log.d(TAG, "Fetch berhasil — suhu=$temp°C code=$code → disimpan ke cache")
            WeatherData(temperatureCelsius = temp, weatherCode = code, fromCache = false)
        }
    }

    /** Paksa invalidasi cache (misalnya dipanggil saat user pull-to-refresh di dalam app). */
    fun invalidateCache(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TIMESTAMP)
            .apply()
        Log.d(TAG, "Cache cuaca di-invalidate")
    }

    /** Helper — map WMO weather code ke label deskripsi bahasa Indonesia. */
    fun weatherCodeToDescription(code: Int): String = when {
        code in 50..69 || code in 80..82 || code in 95..99 -> "Hujan"
        code in 45..48 -> "Berkabut"
        code in 1..3 -> "Berawan"
        else -> "Cerah"
    }
}

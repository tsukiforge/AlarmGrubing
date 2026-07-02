package com.example.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FIX BUG 3: WeatherWidgetProvider kini menggunakan WeatherRepository sebagai
 * single source of truth. Data widget dan in-app selalu dari cache yang sama,
 * sehingga tidak bisa berbeda.
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_weather_location, pendingIntent)

            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            views.setTextViewText(R.id.widget_weather_desc, "Memuat...")
            views.setTextViewText(R.id.widget_weather_temp, "--°C")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            if (!hasLocationPermission) {
                // Gunakan koordinat default (Jakarta) jika tidak ada izin lokasi
                CoroutineScope(Dispatchers.IO).launch {
                    fetchAndUpdate(context, appWidgetManager, appWidgetId, -6.2088, 106.8456, isDefault = true)
                }
                return
            }

            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { loc ->
                    val lat = loc?.latitude ?: -6.2088
                    val lon = loc?.longitude ?: 106.8456
                    val isDefault = loc == null
                    CoroutineScope(Dispatchers.IO).launch {
                        fetchAndUpdate(context, appWidgetManager, appWidgetId, lat, lon, isDefault)
                    }
                }.addOnFailureListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        fetchAndUpdate(context, appWidgetManager, appWidgetId, -6.2088, 106.8456, isDefault = true)
                    }
                }
            } catch (e: SecurityException) {
                val views2 = RemoteViews(context.packageName, R.layout.widget_weather)
                views2.setTextViewText(R.id.widget_weather_desc, "Akses lokasi ditolak")
                views2.setTextViewText(R.id.widget_weather_temp, "--°C")
                appWidgetManager.updateAppWidget(appWidgetId, views2)
            }
        }

        private suspend fun fetchAndUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            lat: Double,
            lon: Double,
            isDefault: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            try {
                // FIX: Gunakan WeatherRepository (bukan fetch langsung) untuk single source of truth
                val weather = WeatherRepository.getWeather(context, lat, lon)
                val desc = WeatherRepository.weatherCodeToDescription(weather.weatherCode)
                    .let { if (isDefault) "$it (Default)" else it }

                val bgRes = when {
                    weather.weatherCode in 50..69 ||
                    weather.weatherCode in 80..82 ||
                    weather.weatherCode in 95..99 -> R.drawable.weather_rainy_1782545360489
                    weather.weatherCode in 1..3 ||
                    weather.weatherCode in 45..48 -> R.drawable.weather_cloudy_1782545347634
                    else -> R.drawable.weather_sunny_1782545330091
                }

                views.setImageViewResource(R.id.widget_weather_bg, bgRes)
                views.setTextViewText(R.id.widget_weather_desc, desc)
                views.setTextViewText(R.id.widget_weather_temp, "${weather.temperatureCelsius}°C")
            } catch (e: Exception) {
                android.util.Log.e("WeatherWidget", "Gagal memuat cuaca: ${e.message}", e)
                views.setTextViewText(R.id.widget_weather_desc, "Gagal memuat")
                views.setTextViewText(R.id.widget_weather_temp, "--°C")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

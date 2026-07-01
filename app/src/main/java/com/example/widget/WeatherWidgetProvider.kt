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
import com.example.ui.fetchWeather
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            
            // Intent to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_weather_location, pendingIntent)

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Fetch default location if no permission
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val weatherInfo = fetchWeather(-6.2088, 106.8456)
                        val code = weatherInfo.second
                        val desc = when {
                            code in 50..69 || code in 80..82 || code in 95..99 -> "Hujan"
                            code in 1..3 || code in 45..48 -> if (code in 45..48) "Berkabut" else "Berawan"
                            else -> "Cerah"
                        }
                        val bgRes = when {
                            code in 50..69 || code in 80..82 || code in 95..99 -> R.drawable.weather_rainy_1782545360489
                            code in 1..3 || code in 45..48 -> R.drawable.weather_cloudy_1782545347634
                            else -> R.drawable.weather_sunny_1782545330091
                        }
                        views.setImageViewResource(R.id.widget_weather_bg, bgRes)
                        views.setTextViewText(R.id.widget_weather_desc, "$desc (Izin)")
                        views.setTextViewText(R.id.widget_weather_temp, "${weatherInfo.first}°C")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (e: Exception) {
                        views.setTextViewText(R.id.widget_weather_desc, "Izin lokasi diperlukan")
                        views.setTextViewText(R.id.widget_weather_temp, "--°C")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
                return
            }

            views.setTextViewText(R.id.widget_weather_desc, "Menyinkronkan...")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, com.google.android.gms.tasks.CancellationTokenSource().token).addOnSuccessListener { loc ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val lat = loc?.latitude ?: -6.2088 // Default to Jakarta if null
                            val lon = loc?.longitude ?: 106.8456
                            val weatherInfo = fetchWeather(lat, lon)
                            val temp = "${weatherInfo.first}°C"
                            val code = weatherInfo.second
                            
                            val desc = when {
                                code in 50..69 || code in 80..82 || code in 95..99 -> "Hujan"
                                code in 1..3 || code in 45..48 -> if (code in 45..48) "Berkabut" else "Berawan"
                                else -> "Cerah"
                            }
                            
                            val bgRes = when {
                                code in 50..69 || code in 80..82 || code in 95..99 -> R.drawable.weather_rainy_1782545360489
                                code in 1..3 || code in 45..48 -> R.drawable.weather_cloudy_1782545347634
                                else -> R.drawable.weather_sunny_1782545330091
                            }
                            
                            views.setImageViewResource(R.id.widget_weather_bg, bgRes)
                            views.setTextViewText(R.id.widget_weather_desc, if (loc == null) "$desc (Def)" else desc)
                            views.setTextViewText(R.id.widget_weather_temp, temp)
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        } catch (e: Exception) {
                            views.setTextViewText(R.id.widget_weather_desc, "Gagal memuat cuaca")
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                }.addOnFailureListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val weatherInfo = fetchWeather(-6.2088, 106.8456)
                            val code = weatherInfo.second
                            val desc = when {
                                code in 50..69 || code in 80..82 || code in 95..99 -> "Hujan"
                                code in 1..3 || code in 45..48 -> if (code in 45..48) "Berkabut" else "Berawan"
                                else -> "Cerah"
                            }
                            val bgRes = when {
                                code in 50..69 || code in 80..82 || code in 95..99 -> R.drawable.weather_rainy_1782545360489
                                code in 1..3 || code in 45..48 -> R.drawable.weather_cloudy_1782545347634
                                else -> R.drawable.weather_sunny_1782545330091
                            }
                            views.setImageViewResource(R.id.widget_weather_bg, bgRes)
                            views.setTextViewText(R.id.widget_weather_desc, "$desc (Def)")
                            views.setTextViewText(R.id.widget_weather_temp, "${weatherInfo.first}°C")
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        } catch (e: Exception) {
                            views.setTextViewText(R.id.widget_weather_desc, "Lokasi tidak ditemukan")
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                }
            } catch (e: SecurityException) {
                views.setTextViewText(R.id.widget_weather_desc, "Akses lokasi ditolak")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

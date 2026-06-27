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

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                views.setTextViewText(R.id.widget_weather_desc, "Izin lokasi diperlukan")
                views.setTextViewText(R.id.widget_weather_temp, "--°C")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            views.setTextViewText(R.id.widget_weather_desc, "Menyinkronkan...")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val weatherInfo = fetchWeather(loc.latitude, loc.longitude)
                                val temp = "${weatherInfo.first}°C"
                                val code = weatherInfo.second
                                
                                val desc = when {
                                    code in 50..69 || code in 80..82 || code in 95..99 -> "Hujan"
                                    code in 1..3 || code in 45..48 -> if (code in 45..48) "Berkabut" else "Berawan"
                                    else -> "Cerah"
                                }
                                
                                views.setTextViewText(R.id.widget_weather_desc, desc)
                                views.setTextViewText(R.id.widget_weather_temp, temp)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            } catch (e: Exception) {
                                views.setTextViewText(R.id.widget_weather_desc, "Gagal memuat cuaca")
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        }
                    } else {
                        views.setTextViewText(R.id.widget_weather_desc, "Lokasi tidak ditemukan")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: SecurityException) {
                views.setTextViewText(R.id.widget_weather_desc, "Akses lokasi ditolak")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

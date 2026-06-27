package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import java.util.Calendar

class MotivationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val quotes = listOf(
            "Setiap pagi adalah kesempatan baru untuk menjadi lebih baik.",
            "Bangunlah dengan tekad, tidurlah dengan kepuasan.",
            "Hari ini adalah awal dari sesuatu yang luar biasa.",
            "Jangan tunggu hari yang sempurna, jadikan hari ini sempurna.",
            "Mimpi hanya akan menjadi kenyataan jika kamu bangun untuk mewujudkannya.",
            "Tersenyumlah, karena hari ini penuh dengan peluang baru.",
            "Kunci kesuksesan adalah bangun lebih awal dan bekerja lebih keras."
        )

        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val quote = quotes[currentDay % quotes.size]

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_motivation)
            views.setTextViewText(R.id.widget_quote_text, "\"$quote\"")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.ui.QuickShareActivity

class QuickShareWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_share)
            
            // Intent for Send
            val sendIntent = Intent(context, QuickShareActivity::class.java).apply {
                action = "ACTION_SEND_FILE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val sendPending = PendingIntent.getActivity(
                context, 0, sendIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quick_send, sendPending)

            // Intent for Receive
            val receiveIntent = Intent(context, QuickShareActivity::class.java).apply {
                action = "ACTION_RECEIVE_FILE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val receivePending = PendingIntent.getActivity(
                context, 1, receiveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quick_receive, receivePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

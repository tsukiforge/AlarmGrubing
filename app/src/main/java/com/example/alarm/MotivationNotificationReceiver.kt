package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class MotivationNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("motivation_channel", "Motivasi Harian", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifikasi motivasi setiap pagi jam 6"
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "motivation_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Selamat Pagi! ☀️")
            .setContentText(quote)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}

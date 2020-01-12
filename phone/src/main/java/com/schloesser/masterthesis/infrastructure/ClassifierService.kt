package com.schloesser.masterthesis.infrastructure

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.schloesser.masterthesis.MainActivity
import com.schloesser.masterthesis.R


class ClassifierService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "ai-glasses-notifications"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, getNotification())
    }

    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "AI Glasses",
                NotificationManager.IMPORTANCE_HIGH
            )

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        registerNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Vuzix Blade Service")
            .setContentText("Connecting to Glasses...")
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentIntent(pendingIntent)

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
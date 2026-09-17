package com.example.ghostframe.overlay.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class OverlayNotificationFactory(
    private val context: Context
) {
    fun create(): Notification {
        createChannel()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("GhostFrame overlay is running")
            .setContentText("Open the overlay menu and choose Close to stop.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager =
                context.getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "GhostFrame overlay",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ghostframe_overlay"
    }
}
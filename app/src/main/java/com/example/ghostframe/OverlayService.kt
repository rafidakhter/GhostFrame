package com.example.ghostframe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlay: FrameLayout? = null

    override fun onCreate() {
        super.onCreate()

        // Keep the overlay running with a foreground notification.
        val channelId = "ghostframe_overlay"
        val notifications = getSystemService(NotificationManager::class.java)

        notifications.createNotificationChannel(
            NotificationChannel(
                channelId,
                "GhostFrame overlay",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("GhostFrame overlay is running")
            .setContentText("Tap Close on the overlay to stop.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layer = FrameLayout(this).apply {
            // Only the background is 50% opaque.
            setBackgroundColor(Color.argb(128, 0, 0, 0))
        }

        val closeButton = Button(this).apply {
            text = "Close"
            setTextColor(Color.BLACK)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.WHITE)
            setOnClickListener { stopSelf() }
        }

        val buttonLayout = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        ).apply {
            val margin = (16 * resources.displayMetrics.density).toInt()
            setMargins(margin, margin, margin, margin)
        }

        layer.addView(closeButton, buttonLayout)

        val windowLayout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(layer, windowLayout)
        overlay = layer
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_NOT_STICKY

    override fun onDestroy() {
        overlay?.let { windowManager.removeView(it) }
        overlay = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
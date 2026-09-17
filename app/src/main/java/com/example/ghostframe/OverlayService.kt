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
		private var closeOverlay: Button? = null	
		
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

				// Background window: touches pass through it.
				val layer = FrameLayout(this).apply {
						setBackgroundColor(Color.BLACK)
				}

				val backgroundParams = WindowManager.LayoutParams(
						WindowManager.LayoutParams.MATCH_PARENT,
						WindowManager.LayoutParams.MATCH_PARENT,
						WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
						WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
								WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
						PixelFormat.TRANSLUCENT
				).apply {
						alpha = 0.5f
				}

				windowManager.addView(layer, backgroundParams)
				overlay = layer

				// Separate button window: fully opaque and tappable.
				val closeButton = Button(this).apply {
						text = "Close"
						setTextColor(Color.BLACK)
						backgroundTintList =
								android.content.res.ColorStateList.valueOf(Color.WHITE)
						setOnClickListener { stopSelf() }
				}

				val buttonParams = WindowManager.LayoutParams(
						WindowManager.LayoutParams.WRAP_CONTENT,
						WindowManager.LayoutParams.WRAP_CONTENT,
						WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
						WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
						PixelFormat.TRANSLUCENT
				).apply {
						gravity = Gravity.TOP or Gravity.END
						val margin = (16 * resources.displayMetrics.density).toInt()
						x = margin
						y = margin
				}

				windowManager.addView(closeButton, buttonParams)
				closeOverlay = closeButton
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_NOT_STICKY

		override fun onDestroy() {
				closeOverlay?.let { windowManager.removeView(it) }
				closeOverlay = null

				overlay?.let { windowManager.removeView(it) }
				overlay = null

				stopForeground(STOP_FOREGROUND_REMOVE)
				super.onDestroy()
		}

    override fun onBind(intent: Intent?): IBinder? = null
}
package com.example.ghostframe

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.graphics.drawable.GradientDrawable
import android.widget.PopupMenu
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.ghostframe.overlay.platform.OverlayNotificationFactory
import com.example.ghostframe.overlay.platform.OverlayWindowHost

class OverlayService : Service() {

		private lateinit var windowHost: OverlayWindowHost
    private var overlay: FrameLayout? = null
		private var closeOverlay: Button? = null	
		private var repositioning = false
		
    override fun onCreate() {
			super.onCreate()

			val notificationFactory = OverlayNotificationFactory(this)

			startForeground(
					OverlayNotificationFactory.NOTIFICATION_ID,
					notificationFactory.create()
			)

			if (!Settings.canDrawOverlays(this)) {
					stopSelf()
					return
			}

			windowHost = OverlayWindowHost(
				windowManager = getSystemService(WINDOW_SERVICE) as WindowManager,
				density = resources.displayMetrics.density
			)

			// Background window: touches pass through it.
			val layer = FrameLayout(this).apply {
				setBackgroundColor(Color.TRANSPARENT)				
			}

			overlay = layer

			// Separate button window: fully opaque and tappable.
			val closeButton = Button(this).apply {
				text = "☰"
				textSize = 22f
				contentDescription = "Overlay options"

				setTextColor(Color.BLACK)
				backgroundTintList = null
				background = GradientDrawable().apply {
						shape = GradientDrawable.OVAL
						setColor(Color.WHITE)
				}

				setPadding(0, 0, 0, 0)
				minWidth = 0
				minHeight = 0

				setOnClickListener {
					PopupMenu(this@OverlayService, this).apply {
							menu.add(
									0, 1, 0,
									if (repositioning) "Done repositioning" else "Reposition"
							)
							menu.add(0, 2, 1, "Close")

							setOnMenuItemClickListener { item ->
									when (item.itemId) {
											1 -> {
													setRepositioning(!repositioning)
													true
											}
											2 -> {
													stopSelf()
													true
											}
											else -> false
									}
							}

							show()
					}
				}
			}

			closeOverlay = closeButton
			windowHost.show(layer, closeButton)
    }

		override fun onStartCommand(
				intent: Intent?,
				flags: Int,
				startId: Int
		): Int {
				val photoUri = intent?.data ?: run {
						stopSelf()
						return START_NOT_STICKY
				}
				val layer = overlay ?: return START_NOT_STICKY

				val photo = ImageView(this).apply {
						scaleType = ImageView.ScaleType.FIT_CENTER
						setImageURI(photoUri)
				}

				layer.removeAllViews()
				layer.addView(
						photo,
						FrameLayout.LayoutParams(
								FrameLayout.LayoutParams.MATCH_PARENT,
								FrameLayout.LayoutParams.MATCH_PARENT
						)
				)
				
				enablePhotoGestures(layer, photo)
				return START_NOT_STICKY
		}
		
		private fun setRepositioning(enabled: Boolean) {
			if (!::windowHost.isInitialized) return

			windowHost.setTouchThrough(enabled = !enabled)
			repositioning = enabled
		}

		private fun enablePhotoGestures(
				layer: FrameLayout,
				photo: ImageView
		) {
				val scaleDetector = ScaleGestureDetector(
						this,
						object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
								override fun onScale(
										detector: ScaleGestureDetector
								): Boolean {
										val scale = (photo.scaleX * detector.scaleFactor)
												.coerceIn(0.25f, 4f)

										photo.scaleX = scale
										photo.scaleY = scale
										return true
								}
						}
				)

				var lastX = 0f
				var lastY = 0f
				var dragging = false

				layer.setOnTouchListener { view, event ->
						if (!repositioning) {
								return@setOnTouchListener false
						}

						scaleDetector.onTouchEvent(event)

						when (event.actionMasked) {
								MotionEvent.ACTION_DOWN -> {
										lastX = event.x
										lastY = event.y
										dragging = true
								}

								MotionEvent.ACTION_MOVE -> {
										if (event.pointerCount == 1 &&
												!scaleDetector.isInProgress
										) {
												if (dragging) {
														photo.translationX += event.x - lastX
														photo.translationY += event.y - lastY
												}

												lastX = event.x
												lastY = event.y
												dragging = true
										} else {
												dragging = false
										}
								}

								MotionEvent.ACTION_POINTER_DOWN,
								MotionEvent.ACTION_POINTER_UP -> {
										dragging = false
								}

								MotionEvent.ACTION_UP -> {
										dragging = false
										view.performClick()
								}

								MotionEvent.ACTION_CANCEL -> {
										dragging = false
								}
						}

						true
				}
		}

		override fun onDestroy() {
				if (::windowHost.isInitialized) {
						windowHost.remove()
				}

				closeOverlay = null
				overlay = null

				stopForeground(STOP_FOREGROUND_REMOVE)
				super.onDestroy()
		}

    override fun onBind(intent: Intent?): IBinder? = null
}
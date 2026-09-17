package com.example.ghostframe

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
import android.widget.ImageView
import android.graphics.drawable.GradientDrawable
import android.widget.PopupMenu
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.ghostframe.overlay.platform.OverlayNotificationFactory

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
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

			windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

			// Background window: touches pass through it.
			val layer = FrameLayout(this).apply {
				setBackgroundColor(Color.TRANSPARENT)				
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

			val buttonParams = WindowManager.LayoutParams(
					(56 * resources.displayMetrics.density).toInt(),
					(56 * resources.displayMetrics.density).toInt(),
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
		): Int {
				val photoUri = intent?.data ?: return START_NOT_STICKY
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
				val layer = overlay ?: return
				val params = layer.layoutParams as WindowManager.LayoutParams

				params.flags = if (enabled) {
						// Receive gestures to move and resize the photo.
						params.flags and
								WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
				} else {
						// Send touches to the app underneath again.
						params.flags or
								WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				}

				windowManager.updateViewLayout(layer, params)
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
				closeOverlay?.let { windowManager.removeView(it) }
				closeOverlay = null

				overlay?.let { windowManager.removeView(it) }
				overlay = null

				stopForeground(STOP_FOREGROUND_REMOVE)
				super.onDestroy()
		}

    override fun onBind(intent: Intent?): IBinder? = null
}
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
import com.example.ghostframe.overlay.platform.OverlayNotificationFactory
import com.example.ghostframe.overlay.platform.OverlayWindowHost
import com.example.ghostframe.overlay.platform.PhotoGestureHandler
import com.example.ghostframe.overlay.domain.OverlayState

class OverlayService : Service() {

		private lateinit var windowHost: OverlayWindowHost
    private var overlay: FrameLayout? = null
		private var closeOverlay: Button? = null	
		private var overlayState = OverlayState()
				
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
								if (overlayState.repositioning) {
										"Done repositioning"
								} else {
										"Reposition"
								}
							)
							
							menu.add(0, 2, 1, "Close")

							setOnMenuItemClickListener { item ->
									when (item.itemId) {
											1 -> {
													setRepositioning(!overlayState.repositioning)
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
				
				// Start each newly selected photo at its original position and size.
				overlayState = OverlayState()
				setRepositioning(false)
				renderPhoto(photo)
				
				enablePhotoGestures(layer, photo)
				return START_NOT_STICKY
		}
		
		private fun setRepositioning(enabled: Boolean) {
				if (!::windowHost.isInitialized) return

				windowHost.setTouchThrough(enabled = !enabled)
				overlayState = overlayState.copy(repositioning = enabled)
		}

		private fun enablePhotoGestures(
				layer: FrameLayout,
				photo: ImageView
		) {
				val gestures = PhotoGestureHandler(
						context = this,
						isEnabled = { overlayState.repositioning },
						onDrag = { dx, dy ->
								overlayState = overlayState.dragBy(dx, dy)
								renderPhoto(photo)
						},
						onZoom = { factor ->
								overlayState = overlayState.zoomBy(factor)
								renderPhoto(photo)
						}
				)

				layer.setOnTouchListener(gestures)
		}

		private fun renderPhoto(photo: ImageView) {
				photo.translationX = overlayState.offsetX
				photo.translationY = overlayState.offsetY
				photo.scaleX = overlayState.scale
				photo.scaleY = overlayState.scale
		}

		override fun onDestroy() {
				overlay?.setOnTouchListener(null)

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
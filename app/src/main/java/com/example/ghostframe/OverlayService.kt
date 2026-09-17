package com.example.ghostframe

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.ghostframe.overlay.platform.OverlayNotificationFactory
import com.example.ghostframe.overlay.platform.OverlayWindowHost
import com.example.ghostframe.overlay.platform.PhotoGestureHandler
import com.example.ghostframe.overlay.domain.OverlayState
import com.example.ghostframe.overlay.presentation.OverlayController
import com.example.ghostframe.overlay.platform.OverlayMenuView

class OverlayService : Service() {

		private lateinit var windowHost: OverlayWindowHost
    private var overlay: FrameLayout? = null
		private var overlayMenu: OverlayMenuView? = null
		private var photoView: ImageView? = null
		private val controller = OverlayController(
				onStateChanged = { state -> renderOverlay(state) }
		)
				
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
			val menu = OverlayMenuView(
					context = this,
					isRepositioning = { controller.state.repositioning },
					onToggleRepositioning = {
							controller.setRepositioning(!controller.state.repositioning)
					},
					onClose = { stopSelf() }
			)

			overlayMenu = menu
			windowHost.show(layer, menu.view)
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
				photoView = photo
				controller.reset()
				enablePhotoGestures(layer)
				return START_NOT_STICKY
		}

		private fun enablePhotoGestures(layer: FrameLayout) {
				val gestures = PhotoGestureHandler(
						context = this,
						isEnabled = { controller.state.repositioning },
						onDrag = { dx, dy -> controller.dragBy(dx, dy) },
						onZoom = { factor -> controller.zoomBy(factor) }
				)

				layer.setOnTouchListener(gestures)
		}

		private fun renderOverlay(state: OverlayState) {
			photoView?.let { photo ->
				photo.translationX = state.offsetX
				photo.translationY = state.offsetY
				photo.scaleX = state.scale
				photo.scaleY = state.scale
			}

			if (::windowHost.isInitialized) {
				windowHost.setTouchThrough(enabled = !state.repositioning)
			}
		}

		override fun onDestroy() {
				overlayMenu?.dismiss()
				overlay?.setOnTouchListener(null)

				if (::windowHost.isInitialized) {
						windowHost.remove()
				}

				overlayMenu = null
				photoView = null
				overlay = null

				stopForeground(STOP_FOREGROUND_REMOVE)
				super.onDestroy()
		}

    override fun onBind(intent: Intent?): IBinder? = null
}
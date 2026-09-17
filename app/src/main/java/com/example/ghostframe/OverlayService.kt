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
import android.widget.Toast
import coil3.SingletonImageLoader
import com.example.ghostframe.overlay.data.CoilPhotoLoader
import com.example.ghostframe.overlay.data.PhotoLoad
import com.example.ghostframe.overlay.data.PhotoLoader

class OverlayService : Service() {

		private lateinit var windowHost: OverlayWindowHost
    private var overlay: FrameLayout? = null
		private var overlayMenu: OverlayMenuView? = null
		private var photoView: ImageView? = null
		private val controller = OverlayController(
				onStateChanged = { state -> renderOverlay(state) }
		)
		private var photoLoad: PhotoLoad? = null
	
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

				val layer = overlay ?: run {
						stopSelf()
						return START_NOT_STICKY
				}

				// Cancel the previous request before loading another photo.
				photoLoad?.cancel()
				photoLoad = null
				layer.setOnTouchListener(null)

				val photo = ImageView(this).apply {
						scaleType = ImageView.ScaleType.FIT_CENTER
				}

				layer.removeAllViews()
				layer.addView(
						photo,
						FrameLayout.LayoutParams(
								FrameLayout.LayoutParams.MATCH_PARENT,
								FrameLayout.LayoutParams.MATCH_PARENT
						)
				)

				photoView = photo
				controller.reset()
				enablePhotoGestures(layer)

				val metrics = resources.displayMetrics

				photoLoad = photoLoader.load(
						uri = photoUri,
						width = metrics.widthPixels,
						height = metrics.heightPixels,
						onSuccess = { drawable ->
								if (photoView === photo) {
										photo.setImageDrawable(drawable)
								}
						},
						onError = {
								if (photoView === photo) {
										Toast.makeText(
												this,
												"Could not open this photo. Please choose another.",
												Toast.LENGTH_LONG
										).show()

										stopSelf(startId)
								}
						}
				)

				return START_NOT_STICKY
		}
		
		private val photoLoader: PhotoLoader by lazy {
				CoilPhotoLoader(
						context = applicationContext,
						imageLoader = SingletonImageLoader.get(applicationContext)
				)
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
				photoView = null
				photoLoad?.cancel()
				photoLoad = null

				overlayMenu?.dismiss()
				overlay?.setOnTouchListener(null)

				if (::windowHost.isInitialized) {
						windowHost.remove()
				}

				overlayMenu = null
				overlay = null

				stopForeground(STOP_FOREGROUND_REMOVE)
				super.onDestroy()
		}

    override fun onBind(intent: Intent?): IBinder? = null
}
package com.example.ghostframe

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
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
import com.example.ghostframe.overlay.platform.OverlayPhotoView
import com.example.ghostframe.overlay.platform.OpacitySliderView

class OverlayService : Service() {

    private lateinit var windowHost: OverlayWindowHost
    private var overlay: OverlayPhotoView? = null
    private var photoRequestId = 0
    private var overlayMenu: OverlayMenuView? = null
    private val controller = OverlayController(
        onStateChanged = { state -> renderOverlay(state) }
    )
    private var photoLoad: PhotoLoad? = null
    private var opacitySlider: OpacitySliderView? = null

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

        val layer = OverlayPhotoView(this)
        overlay = layer
        enablePhotoGestures(layer)

        // Separate button window: fully opaque and tappable.
        val menu = OverlayMenuView(
            context = this,
            isRepositioning = { controller.state.repositioning },
            onToggleRepositioning = {
                hideOpacitySlider()
                controller.setRepositioning(
                    !controller.state.repositioning
                )
            },
            onToggleOpacity = { toggleOpacitySlider() },
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

        // Invalidate old callbacks before cancelling the old request.
        val requestId = ++photoRequestId
        photoLoad?.cancel()
        photoLoad = null

        layer.clearPhoto()
        controller.reset()

        val metrics = resources.displayMetrics

        photoLoad = photoLoader.load(
            uri = photoUri,
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            onSuccess = { drawable ->
                if (requestId == photoRequestId && overlay === layer) {
                    layer.showPhoto(drawable)
                }
            },
            onError = {
                if (requestId == photoRequestId && overlay === layer) {
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

    private fun enablePhotoGestures(layer: OverlayPhotoView) {
        val gestures = PhotoGestureHandler(
            context = this,
            isEnabled = { controller.state.repositioning },
            onDrag = { dx, dy -> controller.dragBy(dx, dy) },
            onZoom = { factor -> controller.zoomBy(factor) }
        )

        layer.setOnTouchListener(gestures)
    }

    private fun renderOverlay(state: OverlayState) {
        overlay?.render(state)
        opacitySlider?.render(state.opacity)

        if (::windowHost.isInitialized) {
            windowHost.setTouchThrough(
                enabled = !state.repositioning
            )
            windowHost.setOpacity(state.opacity)
        }
    }

    private fun toggleOpacitySlider() {
        if (opacitySlider != null) {
            windowHost.hideOpacityControl()
            opacitySlider = null
            return
        }

        controller.setRepositioning(false)

        val slider = OpacitySliderView(
            context = this,
            onDismiss = { hideOpacitySlider() },
            onOpacityChanged = { controller.setOpacity(it) }
        )

        slider.render(controller.state.opacity)
        opacitySlider = slider
        windowHost.showOpacityControl(slider)
    }

    private fun hideOpacitySlider() {
        windowHost.hideOpacityControl()
        opacitySlider = null
    }

    override fun onDestroy() {
        photoRequestId++
        photoLoad?.cancel()
        photoLoad = null
        opacitySlider = null
        overlayMenu?.dismiss()
        overlay?.setOnTouchListener(null)
        overlay?.clearPhoto()

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
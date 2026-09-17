package com.example.ghostframe.overlay.platform

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class OverlayWindowHost(
    private val windowManager: WindowManager,
    private val density: Float
) {
    private var background: View? = null
    private var controls: View? = null
    private var backgroundParams: WindowManager.LayoutParams? = null

    fun show(backgroundView: View, controlsView: View) {
        remove()

        val imageParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            alpha = 0.5f
        }

        val controlsParams = WindowManager.LayoutParams(
            dp(56),
            dp(56),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16)
            y = dp(16)
        }

        try {
            windowManager.addView(backgroundView, imageParams)
            background = backgroundView
            backgroundParams = imageParams

            windowManager.addView(controlsView, controlsParams)
            controls = controlsView
        } catch (error: RuntimeException) {
            // Avoid leaving one window behind if setup fails.
            remove()
            throw error
        }
    }

    fun setTouchThrough(enabled: Boolean) {
        val view = background ?: return
        val params = backgroundParams ?: return

        params.flags = if (enabled) {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            params.flags and
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }

        windowManager.updateViewLayout(view, params)
    }

    fun remove() {
        controls?.let { windowManager.removeView(it) }
        controls = null

        background?.let { windowManager.removeView(it) }
        background = null
        backgroundParams = null
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}
package com.example.ghostframe.overlay.platform

import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowInsets
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
    private var opacityControl: View? = null
    private var cropEditor: View? = null
    private val toolWindows = mutableListOf<View>()
    private val retiringTools = mutableListOf<View>()
    private var mainParams: WindowManager.LayoutParams? = null
    private fun controlParams(w: Int, h: Int, right: Int, bottom: Int, left: Boolean = false) =
        WindowManager.LayoutParams(dp(w), dp(h), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM or if (left) Gravity.LEFT else Gravity.RIGHT
            x = dp(right); y = dp(bottom)
        }
    private fun safeSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) {
            val m = windowManager.currentWindowMetrics
            val i = m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            return ((m.bounds.width()-i.left-i.right)/density).toInt() to
                ((m.bounds.height()-i.top-i.bottom)/density).toInt()
        }
        val size = android.graphics.Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(size)
        return (size.x/density).toInt() to (size.y/density).toInt()
    }
    fun showTools(buttons: List<View>, unpin: View) {
        hideTools(false)
        val (w,h) = safeSize()
        val compact = h < 540 || w < 350
        buttons.forEachIndexed { index, button ->
            val right = if(compact) 12 + index * ((w-24)/4) else listOf(160,148,104,40)[index]
            val bottom = if(compact) 96 else listOf(8,76,132,168)[index]
            button.animate().withEndAction(null)
            button.visibility = View.VISIBLE
            windowManager.addView(button, controlParams(64,80,right,bottom))
            toolWindows.add(button)
            button.alpha=0f; button.scaleX=.9f; button.scaleY=.9f
            button.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140).start()
        }
        unpin.animate().withEndAction(null)
        unpin.alpha = 1f; unpin.scaleX = 1f; unpin.scaleY = 1f
        unpin.visibility = View.VISIBLE
        windowManager.addView(unpin,controlParams(104,48,24,24,true))
        toolWindows.add(unpin)
        if (cropEditor != null) toolWindows.forEach { it.visibility = View.INVISIBLE }
    }
    fun hideTools(animate: Boolean) {
        retiringTools.toList().forEach { v ->
            v.animate().cancel()
            if (v.isAttachedToWindow) windowManager.removeView(v)
        }
        retiringTools.clear()
        val old = toolWindows.toList()
        toolWindows.clear()
        old.forEach { v ->
            v.animate().cancel()
            if (animate && v.isAttachedToWindow) {
                // Stop intercepting touches as soon as collapse begins.
                val params = v.layoutParams as WindowManager.LayoutParams
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(v, params)
                retiringTools.add(v)
                v.animate().alpha(0f).scaleX(.92f).scaleY(.92f).setDuration(100).withEndAction {
                    if (v.isAttachedToWindow) windowManager.removeView(v)
                    retiringTools.remove(v)
                }.start()
            } else if (v.isAttachedToWindow) windowManager.removeView(v)
        }
    }

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

        val controlsParams = controlParams(72,72,16,16)
        mainParams = controlsParams

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

        val newFlags = if (enabled) {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            params.flags and
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }

        if (params.flags == newFlags) return

        params.flags = newFlags
        windowManager.updateViewLayout(view, params)
    }

    fun showOpacityControl(view: View) {
        hideOpacityControl()

        val (w,h) = safeSize()
        val params = controlParams(minOf(246,w-48),104,24,
            if(h < 540 || w < 350) 188 else 264, true)

        windowManager.addView(view, params)
        opacityControl = view
    }

    fun hideOpacityControl() {
        opacityControl?.let { windowManager.removeView(it) }
        opacityControl = null
    }

    fun showCropEditor(view: View) {
        hideCropEditor()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        // No layout-under-system-bars flags: the dedicated editor uses the safe window area.
        windowManager.addView(view, params)
        cropEditor = view
        background?.visibility = View.INVISIBLE
        toolWindows.forEach { it.visibility = View.INVISIBLE }
        // Keep the main X above the editor, with room below its Apply/Cancel row.
        view.setPadding(view.paddingLeft,view.paddingTop,view.paddingRight,view.paddingBottom+dp(88))
        controls?.let { windowManager.removeView(it); windowManager.addView(it, mainParams) }
    }

    fun hideCropEditor() {
        cropEditor?.let { windowManager.removeView(it) }
        cropEditor = null
        background?.visibility = View.VISIBLE
        controls?.visibility = View.VISIBLE
        toolWindows.forEach { it.visibility = View.VISIBLE }
    }

    fun remove() {
        hideTools(false)
        hideCropEditor()
        hideOpacityControl()
        controls?.let { windowManager.removeView(it) }
        controls = null

        background?.let { windowManager.removeView(it) }
        background = null
        backgroundParams = null
    }

    fun setOpacity(opacity: Float) {
        if (!opacity.isFinite()) return

        val view = background ?: return
        val params = backgroundParams ?: return
        val newAlpha = opacity.coerceIn(0f, 0.8f)

        if (params.alpha == newAlpha) return

        params.alpha = newAlpha
        windowManager.updateViewLayout(view, params)
    }


    private fun dp(value: Int): Int = (value * density).toInt()
}
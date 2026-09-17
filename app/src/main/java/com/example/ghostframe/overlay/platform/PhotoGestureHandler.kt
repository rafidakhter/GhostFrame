package com.example.ghostframe.overlay.platform

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class PhotoGestureHandler(
    context: Context,
    private val isEnabled: () -> Boolean,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onZoom: (factor: Float) -> Unit
) : View.OnTouchListener {

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(
                detector: ScaleGestureDetector
            ): Boolean {
                onZoom(detector.scaleFactor)
                return true
            }
        }
    )

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (!isEnabled()) {
            dragging = false
            return false
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
                        onDrag(event.x - lastX, event.y - lastY)
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

        return true
    }
}
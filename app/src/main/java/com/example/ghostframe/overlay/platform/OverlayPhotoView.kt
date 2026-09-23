package com.example.ghostframe.overlay.platform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import com.example.ghostframe.overlay.domain.ImageCrop
import com.example.ghostframe.overlay.domain.OverlayState
import kotlin.math.min

/** Keeps the decoded original intact; cropping is just a clip in image space. */
class OverlayPhotoView(context: Context) : View(context) {
    var original: Drawable? = null
        private set
    private var state = OverlayState()
    private val transform = Matrix()

    fun showPhoto(drawable: Drawable) {
        original = drawable
        invalidate()
    }

    fun clearPhoto() {
        original = null
        invalidate()
    }

    fun render(state: OverlayState) {
        this.state = state
        invalidate()
    }

    private fun imageTransform(): Matrix {
        val image = original ?: return transform
        val w = image.intrinsicWidth.toFloat().coerceAtLeast(1f)
        val h = image.intrinsicHeight.toFloat().coerceAtLeast(1f)
        val fit = min(width / w, height / h) * state.scale
        transform.reset()
        transform.postTranslate(-state.crop.centerX * w, -state.crop.centerY * h)
        transform.postScale(fit, fit)
        transform.postRotate(state.rotationDegrees.toFloat())
        transform.postTranslate(width / 2f + state.offsetX, height / 2f + state.offsetY)
        return transform
    }

    /** Move the pivot to the new crop centre without moving any displayed pixels. */
    fun cropCenterShift(draft: ImageCrop): FloatArray {
        val image = original ?: return floatArrayOf(0f, 0f)
        val delta = floatArrayOf(
            (draft.centerX - state.crop.centerX) * image.intrinsicWidth,
            (draft.centerY - state.crop.centerY) * image.intrinsicHeight
        )
        imageTransform().mapVectors(delta)
        return delta
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val image = original ?: return
        val w = image.intrinsicWidth.coerceAtLeast(1)
        val h = image.intrinsicHeight.coerceAtLeast(1)
        canvas.save()
        canvas.concat(imageTransform())
        canvas.clipRect(RectF(state.crop.left * w, state.crop.top * h,
            state.crop.right * w, state.crop.bottom * h))
        image.setBounds(0, 0, w, h)
        image.draw(canvas)
        canvas.restore()
    }
}

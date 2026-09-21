package com.example.ghostframe.overlay.platform

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.ghostframe.overlay.domain.OverlayState

class OverlayPhotoView(context: Context) : FrameLayout(context) {

    private val photo = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)

        addView(
            photo,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }

    fun showPhoto(drawable: Drawable) {
        photo.setImageDrawable(drawable)
    }

    fun clearPhoto() {
        photo.setImageDrawable(null)
    }

    fun render(state: OverlayState) {
        photo.translationX = state.offsetX
        photo.translationY = state.offsetY
        photo.scaleX = state.scale
        photo.scaleY = state.scale
        // The default View pivot is its centre, also the FIT_CENTER photo's centre.
        // Rotate only the image; the parent keeps gesture coordinates screen-aligned.
        photo.rotation = state.rotationDegrees.toFloat()
    }
}

package com.example.ghostframe.overlay.domain

data class OverlayState(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val repositioning: Boolean = false,
    val opacity: Float = 0.5f,
    val rotationDegrees: Int = 0,
    val crop: ImageCrop = ImageCrop(),
    val cropDraft: ImageCrop? = null
) {
    fun dragBy(dx: Float, dy: Float): OverlayState {
        if (!repositioning || cropDraft != null) return this

        return copy(
            offsetX = offsetX + dx,
            offsetY = offsetY + dy
        )
    }

    fun zoomBy(factor: Float): OverlayState {
        if (!repositioning || cropDraft != null || !factor.isFinite() || factor <= 0f) {
            return this
        }

        return copy(
            scale = (scale * factor).coerceIn(0.25f, 4f)
        )
    }
}

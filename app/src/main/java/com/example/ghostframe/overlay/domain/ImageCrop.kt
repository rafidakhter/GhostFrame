package com.example.ghostframe.overlay.domain

/** Coordinates in the EXIF-oriented original image, independent of overlay rotation. */
data class ImageCrop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    init {
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite())
        require(left >= 0f && top >= 0f && right <= 1f && bottom <= 1f)
        require(left < right && top < bottom)
    }
    val centerX get() = (left + right) / 2f
    val centerY get() = (top + bottom) / 2f
}

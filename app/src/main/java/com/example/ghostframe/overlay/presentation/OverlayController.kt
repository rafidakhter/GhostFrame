package com.example.ghostframe.overlay.presentation

import com.example.ghostframe.overlay.domain.OverlayState
import com.example.ghostframe.overlay.domain.ImageCrop

class OverlayController(
    private val onStateChanged: (OverlayState) -> Unit
) {
    var state = OverlayState()
        private set

    fun reset() {
        update(OverlayState())
    }

    fun setRepositioning(enabled: Boolean) {
        update(state.copy(repositioning = enabled))
    }

    fun dragBy(dx: Float, dy: Float) {
        update(state.dragBy(dx, dy))
    }

    fun zoomBy(factor: Float) {
        update(state.zoomBy(factor))
    }

    fun rotateClockwise() {
        update(state.copy(rotationDegrees = (state.rotationDegrees + 90) % 360))
    }

    fun beginCrop() {
        update(state.copy(cropDraft = state.crop))
    }

    fun updateCropDraft(crop: ImageCrop) {
        if (state.cropDraft != null) update(state.copy(cropDraft = crop))
    }

    fun cancelCrop() {
        update(state.copy(cropDraft = null))
    }

    fun applyCrop(centerShiftX: Float, centerShiftY: Float) {
        val draft = state.cropDraft ?: return
        update(state.copy(
            crop = draft, cropDraft = null,
            offsetX = state.offsetX + centerShiftX,
            offsetY = state.offsetY + centerShiftY
        ))
    }

    private fun update(newState: OverlayState) {
        state = newState
        onStateChanged(state)
    }

    fun setOpacity(opacity: Float) {
        if (!opacity.isFinite()) return

        update(
            state.copy(opacity = opacity.coerceIn(0f, 0.8f))
        )
    }
}

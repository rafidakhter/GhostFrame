package com.example.ghostframe.overlay.presentation

import com.example.ghostframe.overlay.domain.OverlayState

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

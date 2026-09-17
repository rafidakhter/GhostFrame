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

    private fun update(newState: OverlayState) {
			state = newState
			onStateChanged(state)
    }
}
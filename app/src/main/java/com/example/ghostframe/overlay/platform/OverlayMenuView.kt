package com.example.ghostframe.overlay.platform

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.PopupMenu

class OverlayMenuView(
	context: Context,
	private val isRepositioning: () -> Boolean,
	private val onToggleRepositioning: () -> Unit,
	private val onClose: () -> Unit
) {
	private var popup: PopupMenu? = null

	val view: Button = Button(context).apply {
			text = "☰"
			textSize = 22f
			contentDescription = "Overlay options"

			setTextColor(Color.BLACK)
			backgroundTintList = null
			background = GradientDrawable().apply {
					shape = GradientDrawable.OVAL
					setColor(Color.WHITE)
			}

			setPadding(0, 0, 0, 0)
			minWidth = 0
			minHeight = 0

			setOnClickListener { showMenu() }
	}

	private fun showMenu() {
			dismiss()

			popup = PopupMenu(view.context, view).apply {
					menu.add(
							0,
							REPOSITION,
							0,
							if (isRepositioning()) "Done repositioning"
							else "Reposition"
					)
					menu.add(0, CLOSE, 1, "Close")

					setOnMenuItemClickListener { item ->
							when (item.itemId) {
									REPOSITION -> {
											onToggleRepositioning()
											true
									}
									CLOSE -> {
											onClose()
											true
									}
									else -> false
							}
					}

					show()
			}
	}

	fun dismiss() {
			popup?.dismiss()
			popup = null
	}

	private companion object {
			const val REPOSITION = 1
			const val CLOSE = 2
	}
}
package com.example.ghostframe.overlay.platform

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.roundToInt

class OpacitySliderView(
    context: Context,
    private val onOpacityChanged: (Float) -> Unit
) : FrameLayout(context) {

    private val label = TextView(context).apply {
        setTextColor(Color.BLACK)
        textSize = 14f
        gravity = Gravity.CENTER
    }

    private val slider = SeekBar(context).apply {
        max = 80
        progress = 50
        contentDescription = "Photo opacity"
    }

    init {
        clipChildren = false
        clipToPadding = false

        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(24).toFloat()
        }

        addView(
            label,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(40),
                Gravity.TOP
            )
        )

				addView(
						slider,
						LayoutParams(
								LayoutParams.MATCH_PARENT,
								dp(48),
								Gravity.BOTTOM
						).apply {
								leftMargin = dp(12)
								rightMargin = dp(12)
						}
				)

        slider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    label.text = "$progress%"

                    if (fromUser) {
                        onOpacityChanged(progress / 100f)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        render(0.5f)
    }

    fun render(opacity: Float) {
        val percentage = (opacity.coerceIn(0f, 0.8f) * 100).roundToInt()
        slider.progress = percentage
        label.text = "$percentage%"
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}
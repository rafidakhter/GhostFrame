package com.example.ghostframe.overlay.platform

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.roundToInt

class OpacitySliderView(
    context: Context,
    private val onDismiss: () -> Unit,
    private val onOpacityChanged: (Float) -> Unit
) : FrameLayout(context) {

    private var dragging = false

    private val label = TextView(context).apply {
        setTextColor(Color.BLACK)
        textSize = 14f
        gravity = Gravity.CENTER
    }

    private val slider = SeekBar(context).apply {
        max = 800
        progress = 500
        contentDescription = "Photo opacity"
    }

    init {
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(24).toFloat()
        }

        addView(
            label,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(48),
                Gravity.TOP
            ).apply {
                rightMargin = dp(88)
            }
        )

        val doneButton = Button(context).apply {
            text = "Done"
            isAllCaps = false
            setOnClickListener { onDismiss() }
        }

        addView(
            doneButton,
            LayoutParams(
                dp(88),
                dp(48),
                Gravity.TOP or Gravity.END
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
                    updateLabel(progress)

                    if (fromUser) {
                        onOpacityChanged(progress / 1000f)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    dragging = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    dragging = false
                }
            }
        )

        render(0.5f)
    }

    fun render(opacity: Float) {
        if (dragging || !opacity.isFinite()) return

        val progress = (opacity.coerceIn(0f, 0.8f) * 1000).roundToInt()

        if (slider.progress != progress) {
            slider.progress = progress
        }

        updateLabel(progress)
    }

    private fun updateLabel(progress: Int) {
        label.text = "Opacity ${(progress / 10f).roundToInt()}%"
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}
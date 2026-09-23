package com.example.ghostframe.overlay.platform

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ghostframe.overlay.domain.ImageCrop
import com.example.ghostframe.ui.theme.GhostPalette as Colors
import kotlin.math.abs
import kotlin.math.min

/** Dedicated editor: its own fitted preview never changes the overlay's zoom or position. */
class CropEditorView(
    context: Context,
    image: Drawable,
    rotation: Int,
    initial: ImageCrop,
    onDraft: (ImageCrop) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) : LinearLayout(context) {
    private val preview = CropSelectionView(context, image, rotation, initial, onDraft)
    private val presetButtons = mutableListOf<Button>()

    init {
        orientation = VERTICAL
        setBackgroundColor(Colors.Canvas)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        addView(TextView(context).apply {
            text = "Crop photo"
            textSize = 22f
            setTextColor(Colors.Text)
        })
        addView(TextView(context).apply {
            text = "Drag the selection or its corners. Swipe for more presets."
            textSize = 13f
            setTextColor(Colors.Secondary)
        })
        addView(preview, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        val row = LinearLayout(context)
        val presets = listOf(
            "Freeform" to null, "Original" to preview.originalRatio,
            "1:1 · Square / Instagram square" to 1f,
            "4:3 · Landscape" to 4f / 3f,
            "2:3 · Portrait" to 2f / 3f,
            "3:2 · Classic landscape" to 3f / 2f,
            "4:5 · Instagram portrait" to 4f / 5f,
            "3:4 · Tall portrait" to 3f / 4f,
            "16:9 · Widescreen" to 16f / 9f,
            "9:16 · Instagram Story" to 9f / 16f
        )
        presets.forEachIndexed { index, (label, ratio) ->
            val button = action(label) {
                preview.selectRatio(ratio)
                selectPreset(index)
            }
            presetButtons.add(button)
            row.addView(button)
        }
        addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = true
            addView(row)
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val actions = LinearLayout(context)
        actions.addView(action("Cancel", onCancel), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(action("Reset") {
            preview.reset()
            selectPreset(0)
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(action("Apply", onApply).apply { style(this, true) },
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(actions)
        selectPreset(0)
    }

    private fun selectPreset(index: Int) {
        presetButtons.forEachIndexed { i, button ->
            button.isSelected = i == index
            style(button, i == index)
        }
    }

    private fun action(label: String, click: () -> Unit) = Button(context).apply {
        text = label
        textSize = 17f
        isAllCaps = false
        minHeight = dp(48)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        style(this, false)
        setOnClickListener { click() }
    }

    private fun style(button: Button, selected: Boolean) {
        button.setTextColor(if (selected) Colors.Canvas else Colors.Text)
        button.backgroundTintList = null
        button.background = RippleDrawable(ColorStateList.valueOf(0x337CF0C4),
            GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (selected) Colors.Mint else Colors.Surface)
                setStroke(dp(1), Colors.Border)
            }, null)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

private class CropSelectionView(
    context: Context,
    private val image: Drawable,
    private val rotation: Int,
    private var draft: ImageCrop,
    private val onDraft: (ImageCrop) -> Unit
) : View(context) {
    private val imageWidth = image.intrinsicWidth.coerceAtLeast(1).toFloat()
    private val imageHeight = image.intrinsicHeight.coerceAtLeast(1).toFloat()
    val originalRatio = if (rotation % 180 == 0) imageWidth / imageHeight else imageHeight / imageWidth
    private var ratio: Float? = null
    private val matrix = Matrix()
    private val inverse = Matrix()
    private val bounds = RectF()
    private val selection = RectF()
    private val start = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var corner = -1
    private var active = false
    private var downX = 0f
    private var downY = 0f
    private val density = resources.displayMetrics.density

    init {
        contentDescription = "Crop selection. Drag inside to move; drag a corner to resize."
        isClickable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val swapped = rotation % 180 != 0
        val rotatedWidth = if (swapped) imageHeight else imageWidth
        val rotatedHeight = if (swapped) imageWidth else imageHeight
        val fit = min((w - 48 * density).coerceAtLeast(1f) / rotatedWidth,
            (h - 48 * density).coerceAtLeast(1f) / rotatedHeight)
        matrix.reset()
        matrix.postTranslate(-imageWidth / 2, -imageHeight / 2)
        matrix.postRotate(rotation.toFloat())
        matrix.postScale(fit, fit)
        matrix.postTranslate(w / 2f, h / 2f)
        matrix.invert(inverse)
        bounds.set(0f, 0f, imageWidth, imageHeight)
        matrix.mapRect(bounds)
        mapDraft()
    }

    private fun mapDraft() {
        selection.set(draft.left * imageWidth, draft.top * imageHeight,
            draft.right * imageWidth, draft.bottom * imageHeight)
        matrix.mapRect(selection)
        invalidate()
    }

    fun reset() {
        ratio = null
        draft = ImageCrop()
        mapDraft()
        onDraft(draft)
    }

    fun selectRatio(value: Float?) {
        ratio = value
        if (value != null && !bounds.isEmpty) {
            // Start each preset at its largest size; switching presets must not
            // repeatedly shrink the draft until its handles become unusable.
            val w = min(bounds.width(), bounds.height() * value)
            val h = w / value
            val left = (selection.centerX() - w / 2).coerceIn(bounds.left, bounds.right - w)
            val top = (selection.centerY() - h / 2).coerceIn(bounds.top, bounds.bottom - h)
            selection.set(left, top, left + w, top + h)
            publish()
        }
    }

    private fun publish() {
        val rect = RectF(selection)
        inverse.mapRect(rect)
        val l = (rect.left / imageWidth).coerceIn(0f, 1f)
        val t = (rect.top / imageHeight).coerceIn(0f, 1f)
        val r = (rect.right / imageWidth).coerceIn(0f, 1f)
        val b = (rect.bottom / imageHeight).coerceIn(0f, 1f)
        if (l < r && t < b) {
            draft = ImageCrop(l, t, r, b)
            onDraft(draft)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.concat(matrix)
        image.setBounds(0, 0, imageWidth.toInt(), imageHeight.toInt())
        image.draw(canvas)
        canvas.restore()
        paint.style = Paint.Style.FILL
        paint.color = 0xAA0E0F12.toInt()
        canvas.drawRect(bounds.left, bounds.top, bounds.right, selection.top, paint)
        canvas.drawRect(bounds.left, selection.bottom, bounds.right, bounds.bottom, paint)
        canvas.drawRect(bounds.left, selection.top, selection.left, selection.bottom, paint)
        canvas.drawRect(selection.right, selection.top, bounds.right, selection.bottom, paint)
        paint.color = Colors.Mint
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2 * density
        canvas.drawRect(selection, paint)
        paint.strokeWidth = density
        paint.alpha = 150
        for (i in 1..2) {
            val x = selection.left + selection.width() * i / 3
            val y = selection.top + selection.height() * i / 3
            canvas.drawLine(x, selection.top, x, selection.bottom, paint)
            canvas.drawLine(selection.left, y, selection.right, y, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
        for (i in 0..3) {
            canvas.drawCircle(if (i % 2 == 0) selection.left else selection.right,
                if (i < 2) selection.top else selection.bottom, 6 * density, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                start.set(selection)
                corner = (0..3).minByOrNull { i ->
                    val x = if (i % 2 == 0) selection.left else selection.right
                    val y = if (i < 2) selection.top else selection.bottom
                    (x - downX) * (x - downX) + (y - downY) * (y - downY)
                } ?: -1
                val cx = if (corner % 2 == 0) selection.left else selection.right
                val cy = if (corner < 2) selection.top else selection.bottom
                if (abs(cx - downX) > 24 * density || abs(cy - downY) > 24 * density) corner = -1
                active = corner >= 0 || selection.contains(downX, downY)
                return active
            }
            MotionEvent.ACTION_MOVE -> if (active && event.pointerCount == 1) {
                if (corner < 0) {
                    selection.set(start)
                    selection.offset((event.x - downX).coerceIn(bounds.left - start.left, bounds.right - start.right),
                        (event.y - downY).coerceIn(bounds.top - start.top, bounds.bottom - start.bottom))
                } else {
                    val left = corner % 2 == 0
                    val top = corner < 2
                    val ax = if (left) start.right else start.left
                    val ay = if (top) start.bottom else start.top
                    val maxW = if (left) ax - bounds.left else bounds.right - ax
                    val maxH = if (top) ay - bounds.top else bounds.bottom - ay
                    var w = (if (left) ax - event.x else event.x - ax).coerceIn(min(24 * density, maxW), maxW)
                    var h = (if (top) ay - event.y else event.y - ay).coerceIn(min(24 * density, maxH), maxH)
                    ratio?.let { r ->
                        w = min(min(maxOf(w, h * r), maxW), maxH * r)
                        h = w / r
                    }
                    selection.set(if (left) ax - w else ax, if (top) ay - h else ay,
                        if (left) ax else ax + w, if (top) ay else ay + h)
                }
                publish()
            }
            MotionEvent.ACTION_UP -> { active = false; performClick() }
            MotionEvent.ACTION_CANCEL -> active = false
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}

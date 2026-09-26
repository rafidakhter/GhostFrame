package com.example.ghostframe.overlay.platform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.example.ghostframe.ui.theme.GhostPalette as Colors

/** Each control gets a small independent window so empty space remains touch-through. */
class OverlayMenuView(
    context: Context,
    private val host: OverlayWindowHost,
    private val onTool: (String) -> Unit,
    private val onDismiss: () -> Unit,
    onUnpin: () -> Unit
) {
    var open = false
        private set
    private var active: String? = null
    val view = ToolButton(context, "Frames", false).apply {
        contentDescription = "Open overlay tools"
        setOnClickListener {
            if (open) dismissTools() else {
                open = true
                render()
                host.showTools(buttons, unpin)
            }
        }
    }
    private val buttons = listOf("Move", "Opacity", "Crop", "Rotate").map { name ->
        ToolButton(context, name, true).apply {
            contentDescription = if (name == "Rotate") "Rotate 90 degrees clockwise" else name
            setOnClickListener { onTool(name) }
        }
    }
    private val unpin = ToolButton(context, "Unpin", false).apply {
        contentDescription = "Unpin photo and stop overlay"
        setOnClickListener { onUnpin() }
    }

    fun relayout() { if (open) host.showTools(buttons, unpin) }
    fun select(tool: String?) { active = tool; render() }
    private fun render() {
        view.icon = if (open) "X" else "Frames"
        view.isSelected = open
        view.contentDescription = if (open) "Close tools and exit editing" else "Open overlay tools"
        view.invalidate()
        buttons.forEach { it.isSelected = it.icon == active; it.invalidate() }
    }
    fun dismissTools() {
        onDismiss()
        open = false
        active = null
        host.hideTools(true)
        render()
    }
    fun dismiss() { host.hideTools(false) }
}

class ToolButton(context: Context, var icon: String, private val label: Boolean) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    init { isClickable = true; isFocusable = true; setLayerType(LAYER_TYPE_SOFTWARE, null) }
    override fun drawableStateChanged() { super.drawableStateChanged(); invalidate() }
    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = if (label) 26 * d else height / 2f
        val radius = (if (label) 24 else 27) * d
        p.style = Paint.Style.FILL
        p.color = if (isSelected || isPressed) Colors.Mint else Colors.Surface
        p.setShadowLayer(3*d,0f,2*d,0x550E0F12)
        if (icon == "Unpin") {
            c.drawRoundRect(1f, 1f, width - 1f, height - 1f, 24*d, 24*d, p)
        } else c.drawCircle(cx, cy, radius, p)
        p.clearShadowLayer()
        p.style = Paint.Style.STROKE; p.strokeWidth = d
        p.color = if (isSelected) Colors.Mint else Colors.Border
        if (icon == "Unpin") c.drawRoundRect(1f, 1f, width-1f, height-1f, 24*d, 24*d, p)
        else c.drawCircle(cx, cy, radius, p)
        c.save()
        c.translate(if (icon == "Unpin") 12*d else cx-12*d, cy-12*d)
        c.scale(d,d)
        p.color = if (isSelected || isPressed) Colors.Canvas else if(icon == "Unpin") Colors.Amber else if(icon == "Frames") Colors.Mint else Colors.Text
        p.strokeWidth = 1.7f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        fun line(a: Float,b: Float,x: Float,y: Float) = c.drawLine(a,b,x,y,p)
        when(icon) {
            "X" -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
            "Frames" -> { c.drawRoundRect(4f,4f,15f,15f,2f,2f,p); c.drawRoundRect(9f,9f,20f,20f,2f,2f,p) }
            "Move" -> {
                line(12f,2f,12f,22f); line(2f,12f,22f,12f)
                for (r in 0..3) { c.save(); c.rotate(r*90f,12f,12f); line(9f,5f,12f,2f); line(12f,2f,15f,5f); c.restore() }
            }
            "Opacity" -> { c.drawCircle(12f,12f,9f,p); p.style=Paint.Style.FILL; c.drawArc(3f,3f,21f,21f,90f,180f,true,p) }
            "Crop" -> { line(6f,2f,6f,18f); line(6f,18f,22f,18f); line(2f,6f,18f,6f); line(18f,6f,18f,22f) }
            "Rotate" -> { c.drawArc(3f,3f,21f,21f,0f,290f,false,p); line(21f,3f,21f,9f); line(21f,9f,15f,9f) }
            "Unpin" -> { c.drawRoundRect(5f,5f,19f,19f,2f,2f,p); line(3f,21f,21f,3f) }
        }
        c.restore()
        p.style=Paint.Style.FILL; p.color=Colors.Text
        p.textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
        p.textAlign = if(icon == "Unpin") Paint.Align.LEFT else Paint.Align.CENTER
        if(label) {
            val baseline = cy+radius+16*d
            val half = p.measureText(icon)/2+4*d
            p.color=Colors.Surface
            c.drawRoundRect(cx-half,baseline+p.fontMetrics.ascent-2*d,cx+half,
                baseline+p.fontMetrics.descent+2*d,4*d,4*d,p)
            p.color=Colors.Text
            c.drawText(icon,cx,baseline,p)
        }
        if(icon == "Unpin") c.drawText("Unpin",42*d,cy+4*d,p)
    }
}

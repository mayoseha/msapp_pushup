package kr.haengwoon.pushup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val MODE_RING = 0
        const val MODE_BAR = 1
    }

    private var count = 0
    private var ghost = 0
    private var goal = 100
    var mode = MODE_RING
        set(value) { field = value; invalidate() }

    private val d = resources.displayMetrics.density
    private val ringStroke = 22f * d
    private val barHeight = 18f * d

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2C"); style = Paint.Style.STROKE
        strokeWidth = ringStroke; strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700"); style = Paint.Style.STROKE
        strokeWidth = ringStroke; strokeCap = Paint.Cap.ROUND
    }
    private val solidTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2C2C2C") }
    private val solidFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0"); strokeWidth = 3f * d; strokeCap = Paint.Cap.ROUND
    }
    private val arc = RectF()
    private val rect = RectF()

    fun setValues(count: Int, ghost: Int, goal: Int) {
        this.count = count
        this.ghost = ghost
        this.goal = if (goal > 0) goal else 1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (mode == MODE_RING) drawRing(canvas) else drawBar(canvas)
    }

    private fun drawRing(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - ringStroke / 2f - 2f * d
        arc.set(cx - r, cy - r, cx + r, cy + r)

        canvas.drawArc(arc, 0f, 360f, false, trackPaint)

        val ratio = min(count.toFloat() / goal, 1f)
        if (ratio > 0f) canvas.drawArc(arc, -90f, 360f * ratio, false, fillPaint)

        if (ghost > 0) {
            val gRatio = min(ghost.toFloat() / goal, 1f)
            val ang = Math.toRadians((-90f + 360f * gRatio).toDouble())
            val inner = r - ringStroke / 2f - 1f * d
            val outer = r + ringStroke / 2f + 1f * d
            canvas.drawLine(
                cx + (cos(ang) * inner).toFloat(), cy + (sin(ang) * inner).toFloat(),
                cx + (cos(ang) * outer).toFloat(), cy + (sin(ang) * outer).toFloat(),
                ghostPaint
            )
        }
    }

    private fun drawBar(canvas: Canvas) {
        val w = width.toFloat()
        val top = height - barHeight - 8f * d
        val bottom = top + barHeight
        val r = barHeight / 2f

        rect.set(0f, top, w, bottom)
        canvas.drawRoundRect(rect, r, r, solidTrack)

        val ratio = min(count.toFloat() / goal, 1f)
        if (ratio > 0f) {
            rect.set(0f, top, w * ratio, bottom)
            canvas.drawRoundRect(rect, r, r, solidFill)
        }

        if (ghost > 0) {
            val gx = w * min(ghost.toFloat() / goal, 1f)
            canvas.drawLine(gx, top - 7f * d, gx, bottom + 7f * d, ghostPaint)
        }
    }
}

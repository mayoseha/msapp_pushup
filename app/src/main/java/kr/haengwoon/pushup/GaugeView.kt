package kr.haengwoon.pushup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class GaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var count = 0
    private var ghost = 0
    private var goal = 100

    private val d = resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2C2C2C") }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF"); strokeWidth = 4f * d; strokeCap = Paint.Cap.ROUND
    }
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5A5A5A") }
    private val rect = RectF()

    fun setValues(count: Int, ghost: Int, goal: Int) {
        this.count = count
        this.ghost = ghost
        this.goal = if (goal > 0) goal else 1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f

        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, trackPaint)

        // 체크포인트 눈금 (25% / 50% / 75%)
        for (p in listOf(0.25f, 0.5f, 0.75f)) {
            val x = w * p
            canvas.drawRect(x - 1.5f * d, h * 0.25f, x + 1.5f * d, h * 0.75f, checkPaint)
        }

        val ratio = min(count.toFloat() / goal, 1f)
        if (ratio > 0f) {
            rect.set(0f, 0f, w * ratio, h)
            canvas.drawRoundRect(rect, r, r, fillPaint)
        }

        if (ghost > 0) {
            val gx = w * min(ghost.toFloat() / goal, 1f)
            canvas.drawLine(gx, -8f * d, gx, h + 8f * d, ghostPaint)
        }
    }
}

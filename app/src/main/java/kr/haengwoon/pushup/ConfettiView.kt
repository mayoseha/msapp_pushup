package kr.haengwoon.pushup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var rot: Float, var vr: Float, val size: Float, val color: Int
    ) { var life = 1f }

    private val colors = intArrayOf(
        Color.parseColor("#EF9F27"), Color.parseColor("#1D9E75"),
        Color.parseColor("#D85A30"), Color.parseColor("#7F77DD"),
        Color.parseColor("#D4537E"), Color.parseColor("#378ADD")
    )

    private val parts = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun burst() {
        parts.clear()
        val cx = width / 2f
        val cy = height * 0.4f
        repeat(110) { i ->
            val a = Random.nextFloat() * 6.283f
            val sp = 6f + Random.nextFloat() * 24f
            parts.add(
                Particle(
                    cx, cy,
                    Math.cos(a.toDouble()).toFloat() * sp,
                    Math.sin(a.toDouble()).toFloat() * sp - 12f,
                    Random.nextFloat() * 6f,
                    (Random.nextFloat() - 0.5f) * 0.4f,
                    10f + Random.nextFloat() * 12f,
                    colors[i % colors.size]
                )
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (parts.isEmpty()) return
        var alive = 0
        for (p in parts) {
            if (p.life <= 0f) continue
            p.vy += 0.9f
            p.x += p.vx
            p.y += p.vy
            p.rot += p.vr
            p.life -= 0.011f
            if (p.y > height + 40) { p.life = 0f; continue }
            alive++
            paint.color = p.color
            paint.alpha = (255 * p.life.coerceIn(0f, 1f)).toInt()
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(Math.toDegrees(p.rot.toDouble()).toFloat())
            canvas.drawRect(-p.size / 2f, -p.size / 2f, p.size / 2f, p.size * 0.8f, paint)
            canvas.restore()
        }
        if (alive > 0) postInvalidateOnAnimation() else parts.clear()
    }
}

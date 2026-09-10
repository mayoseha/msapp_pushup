package kr.haengwoon.pushup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/** 통계 꺾은선 그래프. 축 없이 선과 면, 라벨만으로 그린다. */
class StatsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<Pair<String, Int>> = emptyList()
    private val d = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#FFD700")
        strokeWidth = 2.5f * d; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") }
    private val dotCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#121212") }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A8A8A"); textSize = 10f * d; textAlign = Paint.Align.CENTER
    }
    private val maxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6A6A6A"); textSize = 10f * d
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#242424"); strokeWidth = 1f
    }

    fun setData(list: List<Pair<String, Int>>) {
        data = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) return

        val padL = 8f * d
        val padR = 8f * d
        val padT = 14f * d
        val padB = 22f * d
        val w = width - padL - padR
        val h = height - padT - padB
        if (w <= 0 || h <= 0) return

        val maxV = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

        // 옅은 가로 눈금 3줄
        for (i in 0..2) {
            val y = padT + h * i / 2f
            canvas.drawLine(padL, y, padL + w, y, gridPaint)
        }
        canvas.drawText("$maxV", padL, padT - 4f * d, maxPaint)

        val n = data.size
        fun px(i: Int) = padL + if (n == 1) w / 2f else w * i / (n - 1f)
        fun py(v: Int) = padT + h - h * v / maxV.toFloat()

        // 면 채우기
        val area = Path()
        area.moveTo(px(0), padT + h)
        for (i in data.indices) area.lineTo(px(i), py(data[i].second))
        area.lineTo(px(n - 1), padT + h)
        area.close()
        fillPaint.shader = LinearGradient(
            0f, padT, 0f, padT + h,
            Color.parseColor("#55FFD700"), Color.parseColor("#00FFD700"),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(area, fillPaint)

        // 선
        val line = Path()
        for (i in data.indices) {
            if (i == 0) line.moveTo(px(i), py(data[i].second))
            else line.lineTo(px(i), py(data[i].second))
        }
        canvas.drawPath(line, linePaint)

        // 점 — 값이 0보다 큰 곳만
        for (i in data.indices) {
            if (data[i].second <= 0) continue
            canvas.drawCircle(px(i), py(data[i].second), 4f * d, dotPaint)
            canvas.drawCircle(px(i), py(data[i].second), 1.8f * d, dotCore)
        }

        // 라벨 — 개수가 많으면 건너뛰며 표시
        val step = if (n > 8) 2 else 1
        for (i in data.indices step step) {
            canvas.drawText(data[i].first, px(i), height - 6f * d, labelPaint)
        }
    }
}

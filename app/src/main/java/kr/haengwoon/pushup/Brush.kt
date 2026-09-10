package kr.haengwoon.pushup

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.sin

/**
 * 붓글씨 흉내용 그리기 도구.
 * 폰트가 아니라 곡선 위를 따라가며 굵기가 변하는 점을 찍어 획을 만든다.
 * 가운데가 굵고 양 끝이 가늘어져 붓으로 쓴 느낌이 난다.
 */
object Brush {

    /** 3차 베지어 곡선 한 획 */
    fun stroke(
        c: Canvas, paint: Paint,
        x0: Float, y0: Float, cx1: Float, cy1: Float,
        cx2: Float, cy2: Float, x1: Float, y1: Float,
        wStart: Float, wEnd: Float, belly: Float = 1f
    ) {
        val n = 90
        for (i in 0..n) {
            val t = i / n.toFloat()
            val m = 1f - t
            val x = m * m * m * x0 + 3 * m * m * t * cx1 + 3 * m * t * t * cx2 + t * t * t * x1
            val y = m * m * m * y0 + 3 * m * m * t * cy1 + 3 * m * t * t * cy2 + t * t * t * y1
            val base = wStart + (wEnd - wStart) * t
            val w = base * (0.30f + 0.70f * belly * sin(PI * t).toFloat())
            c.drawCircle(x, y, (w / 2f).coerceAtLeast(0.6f), paint)
        }
    }

    /**
     * 'Pushup' 을 필기체로 그린다.
     * ox, oy 는 글자 왼쪽 아래 기준점. scale 로 크기를 맞춘다.
     */
    fun pushup(c: Canvas, ox: Float, oy: Float, scale: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        fun s(
            x0: Float, y0: Float, cx1: Float, cy1: Float,
            cx2: Float, cy2: Float, x1: Float, y1: Float,
            w0: Float, w1: Float, belly: Float = 1f
        ) = stroke(
            c, p,
            ox + x0 * scale, oy + y0 * scale,
            ox + cx1 * scale, oy + cy1 * scale,
            ox + cx2 * scale, oy + cy2 * scale,
            ox + x1 * scale, oy + y1 * scale,
            w0 * scale, w1 * scale, belly
        )

        // P — 세로 기둥과 둥근 배
        s(14f, -104f, 4f, -60f, 2f, -20f, 6f, 26f, 5f, 3f, 0.8f)
        s(14f, -104f, 46f, -118f, 62f, -84f, 40f, -62f, 4f, 6f)
        s(40f, -62f, 30f, -52f, 20f, -50f, 10f, -52f, 6f, 3f)

        // u
        s(64f, -54f, 60f, -20f, 62f, -4f, 78f, -4f, 3f, 5f)
        s(78f, -4f, 92f, -6f, 96f, -30f, 98f, -54f, 5f, 3f)
        s(98f, -54f, 98f, -30f, 98f, -12f, 104f, -4f, 3f, 4f)

        // s
        s(126f, -46f, 120f, -56f, 106f, -50f, 112f, -36f, 3f, 5f)
        s(112f, -36f, 120f, -22f, 134f, -22f, 126f, -8f, 5f, 4f)
        s(126f, -8f, 120f, -1f, 110f, -2f, 106f, -8f, 4f, 3f)

        // h
        s(146f, -110f, 140f, -60f, 138f, -26f, 140f, -4f, 5f, 3f, 0.9f)
        s(140f, -40f, 152f, -58f, 172f, -56f, 172f, -34f, 3f, 4f)
        s(172f, -34f, 172f, -20f, 170f, -10f, 176f, -4f, 4f, 4f)

        // u
        s(196f, -54f, 192f, -20f, 194f, -4f, 210f, -4f, 3f, 5f)
        s(210f, -4f, 224f, -6f, 228f, -30f, 230f, -54f, 5f, 3f)
        s(230f, -54f, 230f, -30f, 230f, -12f, 236f, -4f, 3f, 4f)

        // p — 아래로 길게 빠지는 획
        s(256f, -52f, 250f, -10f, 248f, 18f, 244f, 44f, 5f, 2f, 0.9f)
        s(256f, -44f, 274f, -60f, 296f, -46f, 288f, -26f, 3f, 5f)
        s(288f, -26f, 280f, -8f, 262f, -8f, 252f, -18f, 5f, 3f)

        // 마무리 흘림
        s(288f, -30f, 320f, -18f, 348f, -36f, 372f, -58f, 3f, 1f, 0.7f)
    }
}

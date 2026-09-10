package kr.haengwoon.pushup

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 안드로이드 15부터는 앱이 화면 끝까지 그려지므로,
 * 폰 하단의 제스처 막대나 내비게이션 버튼과 겹치지 않도록 여백을 직접 넣어야 한다.
 */
object Insets {
    fun applyBottom(v: View) {
        val padL = v.paddingLeft
        val padT = v.paddingTop
        val padR = v.paddingRight
        val padB = v.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(padL + bars.left, padT + bars.top, padR + bars.right, padB + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(v)
    }
}

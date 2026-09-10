package kr.haengwoon.pushup

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class CalendarActivity : AppCompatActivity() {

    private lateinit var grid: GridLayout
    private lateinit var tvMonth: TextView
    private val cal: Calendar = Calendar.getInstance()
    private lateinit var stats: StatsView
    private lateinit var tvStatTitle: TextView
    private lateinit var tvStatSum: TextView
    private lateinit var periodButtons: List<Button>
    private var period = Records.Period.DAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        grid = findViewById(R.id.grid)
        tvMonth = findViewById(R.id.tvMonth)
        stats = findViewById(R.id.stats)
        tvStatTitle = findViewById(R.id.tvStatTitle)
        tvStatSum = findViewById(R.id.tvStatSum)

        // 폰 하단 제스처 막대와 버튼이 겹치지 않게 여백을 준다
        Insets.applyBottom(findViewById(R.id.root))

        periodButtons = listOf(
            findViewById(R.id.btnDay), findViewById(R.id.btnWeek),
            findViewById(R.id.btnMonth), findViewById(R.id.btnYear)
        )
        val periods = listOf(
            Records.Period.DAY, Records.Period.WEEK,
            Records.Period.MONTH, Records.Period.YEAR
        )
        periodButtons.forEachIndexed { i, b ->
            b.setOnClickListener { period = periods[i]; drawStats() }
        }

        findViewById<Button>(R.id.btnPrev).setOnClickListener {
            cal.add(Calendar.MONTH, -1); draw()
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            cal.add(Calendar.MONTH, 1); draw()
        }
        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }

        cal.set(Calendar.DAY_OF_MONTH, 1)
        draw()
        drawStats()
    }

    private fun draw() {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        tvMonth.text = "${year}년 ${month}월"

        grid.removeAllViews()
        grid.columnCount = 7

        for (w in listOf("일", "월", "화", "수", "목", "금", "토")) {
            grid.addView(headerCell(w))
        }

        val first = cal.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)
        val blanks = first.get(Calendar.DAY_OF_WEEK) - 1
        repeat(blanks) { grid.addView(emptyCell()) }

        val last = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (d in 1..last) {
            val date = String.format("%04d-%02d-%02d", year, month, d)
            grid.addView(dayCell(d, date))
        }
    }

    private fun drawStats() {
        val data = Records.series(this, period)
        stats.setData(data)
        tvStatSum.text = "합계 ${data.sumOf { it.second }}개"
        tvStatTitle.text = when (period) {
            Records.Period.DAY -> "최근 14일"
            Records.Period.WEEK -> "최근 8주"
            Records.Period.MONTH -> "최근 12개월"
            Records.Period.YEAR -> "최근 5년"
        }
        val idx = listOf(
            Records.Period.DAY, Records.Period.WEEK,
            Records.Period.MONTH, Records.Period.YEAR
        ).indexOf(period)
        periodButtons.forEachIndexed { i, b ->
            b.setTextColor(
                if (i == idx) Color.parseColor("#FFD700") else Color.parseColor("#8A8A8A")
            )
        }
    }

    private fun cellParams(): GridLayout.LayoutParams {
        val p = GridLayout.LayoutParams()
        p.width = 0
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        p.setMargins(3, 3, 3, 3)
        return p
    }

    private fun headerCell(t: String): View {
        val tv = TextView(this)
        tv.text = t
        tv.gravity = Gravity.CENTER
        tv.setTextColor(Color.parseColor("#9A9A9A"))
        tv.textSize = 15f
        tv.setPadding(0, 12, 0, 12)
        tv.layoutParams = cellParams()
        return tv
    }

    private fun emptyCell(): View {
        val v = View(this)
        v.layoutParams = cellParams()
        return v
    }

    private fun dayCell(d: Int, date: String): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setPadding(0, 16, 0, 16)
        box.layoutParams = cellParams()

        val rec = Records.get(this, date)
        val done = rec != null && rec.second > 0 && rec.first >= rec.second

        box.setBackgroundColor(
            when {
                done -> Color.parseColor("#4A3F00")
                rec != null -> Color.parseColor("#242424")
                else -> Color.TRANSPARENT
            }
        )

        val num = TextView(this)
        num.text = d.toString()
        num.gravity = Gravity.CENTER
        num.textSize = 17f
        num.setTextColor(if (done) Color.parseColor("#FFD700") else Color.WHITE)
        box.addView(num)

        val sub = TextView(this)
        sub.gravity = Gravity.CENTER
        sub.textSize = 11f
        sub.setTextColor(Color.parseColor("#9A9A9A"))
        sub.text = if (rec != null) "${rec.first}/${rec.second}" else ""
        box.addView(sub)

        val lv = TextView(this)
        lv.gravity = Gravity.CENTER
        lv.textSize = 10f
        lv.setTextColor(Color.parseColor("#7A7A7A"))
        lv.text = if (rec != null) Records.LEVELS_SHORT.getOrElse(rec.third) { "" } else ""
        box.addView(lv)

        box.setOnClickListener { showDay(date, rec) }
        return box
    }

    private fun showDay(date: String, rec: Triple<Int, Int, Int>?) {
        val msg = if (rec == null) {
            "$date\n\n기록이 없습니다."
        } else {
            val lvl = Records.LEVELS.getOrElse(rec.third) { "-" }
            val state = if (rec.second > 0 && rec.first >= rec.second) "목표 달성" else "미달성"
            "$date\n\n개수  ${rec.first}개\n목표  ${rec.second}개\n난이도  $lvl\n결과  $state"
        }

        val b = AlertDialog.Builder(this).setMessage(msg).setPositiveButton("닫기", null)

        val cert = Records.getCert(this, date)
        if (cert != null && java.io.File(cert).exists()) {
            b.setNeutralButton("수료증 보기") { _, _ ->
                startActivity(android.content.Intent(this, CertificateActivity::class.java).apply {
                    putExtra(CertificateActivity.EXTRA_DATE, date)
                    putExtra(CertificateActivity.EXTRA_LEVEL, rec?.third ?: 0)
                    putExtra(CertificateActivity.EXTRA_VIEW_ONLY, true)
                })
            }
        }
        b.show()
    }
}

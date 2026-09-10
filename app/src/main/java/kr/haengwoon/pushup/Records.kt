package kr.haengwoon.pushup

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 날짜별 기록 저장소.
 * 폰 내부에만 저장되며 외부로 전송되지 않는다.
 *
 * 저장 형태: {"2026-09-10": {"c":45, "g":100, "d":1}, ...}
 *   c = 그날 총 개수, g = 그날 목표, d = 난이도 번호
 */
object Records {

    val LEVELS = listOf(
        "초급 · 무릎 대고",
        "중급 · 기본 자세",
        "고급 · 다리 올리고",
        "최고급 · 파이크",
        "마스터 · 아처"
    )

    val LEVELS_SHORT = listOf("초급", "중급", "고급", "최고급", "마스터")

    /** 한 난이도를 수료하는 데 필요한 목표 달성 일수 */
    const val DAYS_TO_CERTIFY = 20

    private const val KEY = "records"

    fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Calendar.getInstance().time)

    private fun load(ctx: Context): JSONObject {
        val p = ctx.getSharedPreferences("pushup", Context.MODE_PRIVATE)
        return try {
            JSONObject(p.getString(KEY, "{}") ?: "{}")
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun save(ctx: Context, obj: JSONObject) {
        ctx.getSharedPreferences("pushup", Context.MODE_PRIVATE)
            .edit().putString(KEY, obj.toString()).apply()
    }

    /** 오늘 기록에 1회 더한다 */
    fun addOne(ctx: Context, goal: Int, level: Int) {
        val all = load(ctx)
        val d = today()
        val day = all.optJSONObject(d) ?: JSONObject()
        day.put("c", day.optInt("c", 0) + 1)
        day.put("g", goal)
        day.put("d", level)
        all.put(d, day)
        save(ctx, all)
    }

    fun get(ctx: Context, date: String): Triple<Int, Int, Int>? {
        val day = load(ctx).optJSONObject(date) ?: return null
        return Triple(day.optInt("c", 0), day.optInt("g", 0), day.optInt("d", 0))
    }

    fun todayCount(ctx: Context): Int = get(ctx, today())?.first ?: 0

    /** 그날 발급된 수료증 이미지 경로 */
    fun setCert(ctx: Context, date: String, path: String) {
        val all = load(ctx)
        val day = all.optJSONObject(date) ?: JSONObject()
        day.put("cert", path)
        all.put(date, day)
        save(ctx, all)
    }

    fun getCert(ctx: Context, date: String): String? {
        val day = load(ctx).optJSONObject(date) ?: return null
        val p = day.optString("cert", "")
        return if (p.isEmpty()) null else p
    }

    /** 오늘 이전에 기록이 있는 가장 최근 날의 총개수 (고스트 기준) */
    fun previousDayCount(ctx: Context): Int {
        val all = load(ctx)
        val t = today()
        var best: String? = null
        for (k in all.keys()) {
            if (k >= t) continue
            if (best == null || k > best!!) best = k
        }
        val b = best ?: return 0
        return all.optJSONObject(b)?.optInt("c", 0) ?: 0
    }

    /** 오늘 개수를 0으로 되돌린다 (목표와 난이도는 유지) */
    fun resetToday(ctx: Context) {
        val all = load(ctx)
        val day = all.optJSONObject(today()) ?: return
        day.put("c", 0)
        all.put(today(), day)
        save(ctx, all)
    }

    /** 특정 난이도로 목표를 달성한 날짜 수 */
    fun certifiedDays(ctx: Context, level: Int): Int {
        val all = load(ctx)
        var n = 0
        for (k in all.keys()) {
            val day = all.optJSONObject(k) ?: continue
            if (day.optInt("d", -1) == level &&
                day.optInt("g", 0) > 0 &&
                day.optInt("c", 0) >= day.optInt("g", 0)
            ) n++
        }
        return n
    }
}

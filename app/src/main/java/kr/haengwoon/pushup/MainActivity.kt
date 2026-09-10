package kr.haengwoon.pushup

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var playlist: List<Int> = emptyList()
    private var trackIndex = 0
    private val baseVolume = 0.7f
    private val fadeHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private var goal = 100
    private var level = 1
    private var musicOn = true

    private var lastRepTime = 0L

    private lateinit var tvGoal: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvCount: TextView
    private lateinit var tvOfGoal: TextView
    private lateinit var tvPct: TextView
    private lateinit var tvGhost: TextView
    private lateinit var btnMusic: Button
    private lateinit var gauge: GaugeView
    private lateinit var confetti: ConfettiView

    private val lines = listOf(
        "좋습니다. 그대로 갑니다.",
        "잘하고 있습니다.",
        "호흡 놓치지 마십시오.",
        "자세 무너지지 않게.",
        "여기서 한 개 더.",
        "충분히 하고 계십니다."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tvGoal = findViewById(R.id.tvGoal)
        tvLevel = findViewById(R.id.tvLevel)
        tvCount = findViewById(R.id.tvCount)
        tvOfGoal = findViewById(R.id.tvOfGoal)
        tvPct = findViewById(R.id.tvPct)
        tvGhost = findViewById(R.id.tvGhost)
        btnMusic = findViewById(R.id.btnMusic)
        gauge = findViewById(R.id.gauge)
        confetti = findViewById(R.id.confetti)

        prefs = getSharedPreferences("pushup", Context.MODE_PRIVATE)
        goal = prefs.getInt("goal", 100)
        level = prefs.getInt("level", 1)
        musicOn = prefs.getBoolean("music", true)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = duck(true)
                    override fun onDone(id: String?) = duck(false)
                    @Deprecated("deprecated")
                    override fun onError(id: String?) = duck(false)
                })
            }
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener { confirmReset() }
        findViewById<Button>(R.id.btnGoal).setOnClickListener { askGoal() }
        findViewById<Button>(R.id.btnLevel).setOnClickListener { askLevel() }
        findViewById<Button>(R.id.btnCalendar).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        // 임시 확인용: 달력 버튼을 길게 누르면 수료증 화면이 바로 열린다.
        // 정식 출시 전에 이 블록을 삭제할 것.
        findViewById<Button>(R.id.btnCalendar).setOnLongClickListener {
            startActivity(Intent(this, CertificateActivity::class.java).apply {
                putExtra(CertificateActivity.EXTRA_LEVEL, level)
                putExtra(CertificateActivity.EXTRA_DATE, Records.today())
            })
            true
        }
        btnMusic.setOnClickListener { toggleMusic() }

        findViewById<android.view.View>(R.id.touchArea).setOnTouchListener { v, e ->
            if (e.action == android.view.MotionEvent.ACTION_UP) {
                v.performClick()
                addRep()
            }
            true
        }

        render()
    }

    // ---------- 배경음악 ----------

    /** res/raw 에 있는 bgm1, bgm2, bgm3 ... 를 찾아 목록을 만든다 */
    private fun buildPlaylist(): List<Int> {
        val list = mutableListOf<Int>()
        var i = 1
        while (true) {
            val id = resources.getIdentifier("bgm$i", "raw", packageName)
            if (id == 0) break
            list.add(id)
            i++
        }
        // 이름 없이 bgm.mp3 만 넣은 경우도 받아준다
        if (list.isEmpty()) {
            val single = resources.getIdentifier("bgm", "raw", packageName)
            if (single != 0) list.add(single)
        }
        return list
    }

    private fun startMusic() {
        if (!musicOn || player != null) return
        if (playlist.isEmpty()) {
            playlist = buildPlaylist().shuffled()   // 켤 때마다 순서를 섞는다
            trackIndex = 0
        }
        if (playlist.isEmpty()) return              // 음원이 하나도 없으면 조용히 넘어간다
        playTrack()
    }

    private fun playTrack() {
        val id = playlist.getOrNull(trackIndex) ?: return
        player = MediaPlayer.create(this, id)?.apply {
            isLooping = false
            setVolume(0f, 0f)                       // 무음으로 시작해 서서히 올린다
            setOnCompletionListener { nextTrack() }
            start()
        }
        fadeIn()
    }

    /** 곡이 바뀌는 순간 음성 카운트를 덮지 않도록 1초에 걸쳐 볼륨을 올린다 */
    private fun fadeIn() {
        val steps = 10
        for (i in 1..steps) {
            fadeHandler.postDelayed({
                val v = baseVolume * i / steps
                try { player?.setVolume(v, v) } catch (e: Exception) { }
            }, (i * 100).toLong())
        }
    }

    private fun nextTrack() {
        player?.release()
        player = null
        trackIndex++
        if (trackIndex >= playlist.size) {
            playlist = playlist.shuffled()          // 한 바퀴 돌면 순서를 다시 섞는다
            trackIndex = 0
        }
        if (musicOn) playTrack()
    }

    private fun stopMusic() {
        fadeHandler.removeCallbacksAndMessages(null)
        player?.let {
            it.setOnCompletionListener(null)
            if (it.isPlaying) it.stop()
            it.release()
        }
        player = null
    }

    private fun duck(quiet: Boolean) {
        val v = if (quiet) 0.15f else baseVolume
        try { player?.setVolume(v, v) } catch (e: Exception) { }
    }

    // ---------- 수명주기 ----------

    override fun onResume() {
        super.onResume()
        startMusic()
        render()
    }

    override fun onPause() {
        super.onPause()
        stopMusic()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        stopMusic()
        super.onDestroy()
    }

    // ---------- 카운트 ----------

    private fun addRep() {
        // 손이 미끄러져 두 번 잡히는 것을 막는다
        val now = System.currentTimeMillis()
        if (now - lastRepTime < 250) return
        lastRepTime = now

        Records.addOne(this, goal, level)
        val today = Records.todayCount(this)
        render()

        val doneKey = "done_" + Records.today()
        val already = prefs.getBoolean(doneKey, false)

        when {
            today >= goal && !already -> {
                prefs.edit().putBoolean(doneKey, true).apply()
                speak("오늘 목표 달성입니다. 대단합니다.")
                confetti.burst()
                checkCertificate()
            }
            today % 10 == 0 -> {
                val line = lines[(today / 10) % lines.size]
                val left = goal - today
                speak(if (left > 0) "$line ${left}개 남았습니다." else "$line 목표를 넘었습니다.")
                if (isCheckpoint(today)) confetti.burst(small = true)
            }
            else -> speak(today.toString())
        }
    }

    private fun isCheckpoint(c: Int): Boolean {
        for (p in listOf(0.25f, 0.5f, 0.75f)) {
            val mark = Math.ceil((goal * p).toDouble()).toInt()
            if (c >= mark && c - 10 < mark) return true
        }
        return false
    }

    private fun checkCertificate() {
        val days = Records.certifiedDays(this, level)
        if (days == Records.DAYS_TO_CERTIFY) {
            AlertDialog.Builder(this)
                .setTitle("수료")
                .setMessage(
                    "${Records.LEVELS.getOrElse(level) { "" }}\n\n" +
                    "목표 달성 ${Records.DAYS_TO_CERTIFY}일을 채우셨습니다.\n" +
                    "수료증을 만들 수 있습니다."
                )
                .setPositiveButton("수료증 만들기") { _, _ ->
                    startActivity(Intent(this, CertificateActivity::class.java).apply {
                        putExtra(CertificateActivity.EXTRA_LEVEL, level)
                        putExtra(CertificateActivity.EXTRA_DATE, Records.today())
                    })
                }
                .setNegativeButton("나중에", null)
                .show()
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rep")
    }

    // ---------- 화면 ----------

    private fun render() {
        val today = Records.todayCount(this)
        val ghost = Records.previousDayCount(this)

        tvGoal.text = "오늘 목표 ${goal}개"
        tvLevel.text = Records.LEVELS.getOrElse(level) { "" }
        tvCount.text = today.toString()
        tvOfGoal.text = "/ $goal"
        tvPct.text = "${(today.toFloat() / goal * 100).roundToInt()}%"
        tvGhost.text = if (ghost > 0) "지난 운동일 ${ghost}개" else "지난 기록 없음"

        // 음악은 글자 대신 색으로 켜짐/꺼짐을 표시한다
        btnMusic.setTextColor(
            if (musicOn) android.graphics.Color.parseColor("#FFD700")
            else android.graphics.Color.parseColor("#FFB0B0B0")
        )

        gauge.setValues(today, ghost, goal)
    }

    // ---------- 버튼 ----------

    private fun toggleMusic() {
        musicOn = !musicOn
        prefs.edit().putBoolean("music", musicOn).apply()
        if (musicOn) {
            startMusic()
        } else {
            stopMusic()
            playlist = emptyList()
        }
        render()
    }

    private fun askLevel() {
        val items = Records.LEVELS.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("난이도")
            .setSingleChoiceItems(items, level) { dlg, which ->
                level = which
                prefs.edit().putInt("level", level).apply()
                render()
                dlg.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setMessage("오늘 개수를 0으로 되돌립니다.\n지난 날짜 기록은 그대로 남습니다.")
            .setPositiveButton("초기화") { _, _ ->
                Records.resetToday(this)
                prefs.edit().putBoolean("done_" + Records.today(), false).apply()
                render()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun askGoal() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(goal.toString())
        }
        AlertDialog.Builder(this)
            .setTitle("오늘 목표 개수")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val v = input.text.toString().toIntOrNull()
                if (v != null && v > 0) {
                    goal = v
                    prefs.edit().putInt("goal", goal).apply()
                    render()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}

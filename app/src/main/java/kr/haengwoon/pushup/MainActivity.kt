package kr.haengwoon.pushup

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var prefs: SharedPreferences
    private lateinit var sm: SensorManager
    private var proximity: Sensor? = null
    private var tts: TextToSpeech? = null
    private var vibrator: Vibrator? = null

    private var count = 0
    private var ghost = 0
    private var goal = 100
    private var gaugeMode = GaugeView.MODE_RING
    private var touchBackup = false

    private var covered = false
    private var threshold = 5f
    private var lastRepTime = 0L

    private lateinit var tvGoal: TextView
    private lateinit var tvCount: TextView
    private lateinit var tvOfGoal: TextView
    private lateinit var tvPct: TextView
    private lateinit var tvGhost: TextView
    private lateinit var btnMode: Button
    private lateinit var btnTouch: Button
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
        tvCount = findViewById(R.id.tvCount)
        tvOfGoal = findViewById(R.id.tvOfGoal)
        tvPct = findViewById(R.id.tvPct)
        tvGhost = findViewById(R.id.tvGhost)
        btnMode = findViewById(R.id.btnMode)
        btnTouch = findViewById(R.id.btnTouch)
        gauge = findViewById(R.id.gauge)
        confetti = findViewById(R.id.confetti)

        prefs = getSharedPreferences("pushup", Context.MODE_PRIVATE)
        count = prefs.getInt("count", 0)
        ghost = prefs.getInt("ghost", 0)
        goal = prefs.getInt("goal", 100)
        gaugeMode = prefs.getInt("mode", GaugeView.MODE_RING)
        touchBackup = prefs.getBoolean("touch", false)

        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximity?.let { threshold = min(it.maximumRange, 5f) }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.KOREAN
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener { confirmReset() }
        findViewById<Button>(R.id.btnGoal).setOnClickListener { askGoal() }
        btnMode.setOnClickListener { toggleMode() }
        btnTouch.setOnClickListener { toggleTouch() }

        findViewById<android.view.View>(R.id.touchArea).setOnClickListener {
            if (touchBackup) addRep(fromSensor = false)
        }

        gauge.mode = gaugeMode
        render()
    }

    override fun onResume() {
        super.onResume()
        proximity?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        val d = event?.values?.getOrNull(0) ?: return
        if (!covered && d < threshold) {
            covered = true
        } else if (covered && d >= threshold) {
            covered = false
            addRep(fromSensor = true)
        }
    }

    private fun addRep(fromSensor: Boolean) {
        val now = System.currentTimeMillis()
        if (!fromSensor && now - lastRepTime < 800) return
        lastRepTime = now

        count++
        prefs.edit().putInt("count", count).apply()
        render()
        vibrate(50)

        when {
            count == goal -> {
                speak("목표 달성입니다. 대단합니다.")
                vibrate(500)
                confetti.burst()
            }
            count % 10 == 0 -> {
                val line = lines[(count / 10) % lines.size]
                val left = goal - count
                speak(if (left > 0) "$line ${left}개 남았습니다." else "$line 목표를 넘었습니다.")
            }
            else -> speak(count.toString())
        }
    }

    private fun vibrate(ms: Long) {
        vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rep")
    }

    private fun render() {
        tvGoal.text = "목표 ${goal}개"
        tvCount.text = count.toString()
        tvOfGoal.text = "/ $goal"
        tvPct.text = "${(count.toFloat() / goal * 100).roundToInt()}%"
        tvGhost.text = if (ghost > 0) "직전 ${ghost}개" else "직전 기록 없음"
        btnMode.text = if (gaugeMode == GaugeView.MODE_RING) "원형" else "직선"
        btnTouch.text = if (touchBackup) "터치 켬" else "터치 끔"
        gauge.setValues(count, ghost, goal)
    }

    private fun toggleMode() {
        gaugeMode = if (gaugeMode == GaugeView.MODE_RING) GaugeView.MODE_BAR else GaugeView.MODE_RING
        prefs.edit().putInt("mode", gaugeMode).apply()
        gauge.mode = gaugeMode
        render()
    }

    private fun toggleTouch() {
        touchBackup = !touchBackup
        prefs.edit().putBoolean("touch", touchBackup).apply()
        render()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setMessage("지금 개수를 직전 기록으로 넘기고 0부터 시작합니다.")
            .setPositiveButton("초기화") { _, _ ->
                ghost = count
                count = 0
                prefs.edit().putInt("ghost", ghost).putInt("count", 0).apply()
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
            .setTitle("목표 개수")
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

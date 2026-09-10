package kr.haengwoon.pushup

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * A4 비율(1240 x 1754, 150dpi) 수료증 이미지를 만든다.
 * 사진은 기기 안에서만 처리되며 외부로 전송되지 않는다.
 */
class CertificateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEVEL = "level"
        const val EXTRA_DATE = "date"
        const val EXTRA_VIEW_ONLY = "viewOnly"
        const val EXTRA_REPS = "reps"
        private const val W = 1240
        private const val H = 1754
    }

    private var level = 0
    private lateinit var date: String
    private var photo: Bitmap? = null
    private var name: String = ""
    private var reps: Int = 0
    private var current: Bitmap? = null

    private lateinit var preview: ImageView

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photo = loadScaled(uri, 700)
            rebuild()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate)

        level = intent.getIntExtra(EXTRA_LEVEL, 0)
        date = intent.getStringExtra(EXTRA_DATE) ?: Records.today()
        val viewOnly = intent.getBooleanExtra(EXTRA_VIEW_ONLY, false)
        reps = intent.getIntExtra(EXTRA_REPS, 0)

        preview = findViewById(R.id.preview)
        val btnPhoto = findViewById<Button>(R.id.btnPhoto)

        Insets.applyBottom(findViewById(R.id.root))

        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            current?.let {
                saveToFile(it)
                Toast.makeText(this, "저장했습니다. 달력에서 다시 볼 수 있습니다", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnShare).setOnClickListener { share() }
        btnPhoto.setOnClickListener {
            picker.launch(PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build())
        }

        name = getSharedPreferences("pushup", MODE_PRIVATE).getString("name", "") ?: ""

        findViewById<Button>(R.id.btnName).setOnClickListener { askName() }

        val saved = Records.getCert(this, date)
        if (viewOnly && saved != null && File(saved).exists()) {
            btnPhoto.isEnabled = false
            findViewById<Button>(R.id.btnName).isEnabled = false
            findViewById<Button>(R.id.btnSave).isEnabled = false
            current = BitmapFactory.decodeFile(saved)
            preview.setImageBitmap(current)
        } else {
            rebuild()
        }
    }

    // ---------- 이미지 만들기 ----------

    private fun rebuild() {
        val bmp = draw()
        current = bmp
        preview.setImageBitmap(bmp)
        // 저장은 "저장" 버튼을 눌렀을 때만 한다
    }

    private fun draw(): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val gold = Color.parseColor("#A8862B")
        val goldPale = Color.parseColor("#D9BE72")
        val wine = Color.parseColor("#7B2230")
        val ink = Color.parseColor("#2A2318")
        val grey = Color.parseColor("#6E675B")

        c.drawColor(Color.parseColor("#FAF5E4"))

        val serif = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val serifThin = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        val mx = W / 2f

        fun text(t: String, y: Float, size: Float, col: Int, tf: Typeface, sp: Float = 0f) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = col; textSize = size; typeface = tf
                textAlign = Paint.Align.CENTER; letterSpacing = sp
            }
            c.drawText(t, mx, y, p)
        }

        // ================= 테두리 =================
        val bandOuter = 46f
        val bandInner = 150f

        // 바깥 굵은 금띠
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = goldPale; strokeWidth = 26f
        }
        c.drawRect(bandOuter + 13f, bandOuter + 13f, W - bandOuter - 13f, H - bandOuter - 13f, band)

        // 레이스 물결 — 네 변을 따라 반원을 촘촘히 찍는다
        val lace = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = gold; strokeWidth = 3f
        }
        val step = 34f
        val rr = 21f
        var x = bandOuter + 40f
        while (x < W - bandOuter - 40f) {
            c.drawArc(x - rr, bandInner - 34f - rr, x + rr, bandInner - 34f + rr, 0f, 180f, false, lace)
            c.drawArc(x - rr, H - bandInner + 34f - rr, x + rr, H - bandInner + 34f + rr, 180f, 180f, false, lace)
            x += step
        }
        var y = bandInner - 34f
        while (y < H - bandInner + 34f) {
            c.drawArc(bandInner - 34f - rr, y - rr, bandInner - 34f + rr, y + rr, 90f, 180f, false, lace)
            c.drawArc(W - bandInner + 34f - rr, y - rr, W - bandInner + 34f + rr, y + rr, 270f, 180f, false, lace)
            y += step
        }

        // 안쪽 이중 실선
        val thin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = gold; strokeWidth = 4f
        }
        c.drawRect(bandInner, bandInner, W - bandInner, H - bandInner, thin)
        thin.strokeWidth = 1.4f
        c.drawRect(bandInner + 14f, bandInner + 14f, W - bandInner - 14f, H - bandInner - 14f, thin)

        // 코너 로제트
        fun rosette(cx2: Float, cy2: Float) {
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; color = gold; strokeWidth = 2.5f
            }
            c.drawCircle(cx2, cy2, 40f, ring)
            c.drawCircle(cx2, cy2, 27f, ring)
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0)
                c.drawLine(
                    cx2 + (Math.cos(a) * 27).toFloat(), cy2 + (Math.sin(a) * 27).toFloat(),
                    cx2 + (Math.cos(a) * 40).toFloat(), cy2 + (Math.sin(a) * 40).toFloat(), ring
                )
            }
            c.drawCircle(cx2, cy2, 11f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = wine })
        }
        rosette(bandInner, bandInner)
        rosette(W - bandInner, bandInner)
        rosette(bandInner, H - bandInner)
        rosette(W - bandInner, H - bandInner)

        // ================= 본문 =================

        // 붓글씨 워드마크
        Brush.pushup(c, 330f, 350f, 1.35f, wine)

        text("A T H L E T I C   A C H I E V E M E N T", 400f, 24f, gold, serifThin, 0.16f)

        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; strokeWidth = 2f }
        c.drawLine(300f, 452f, 940f, 452f, rule)

        text("THIS CERTIFICATE RECOGNIZES THAT", 516f, 27f, ink, serifThin, 0.12f)
        text(if (name.isBlank()) "ATHLETE" else name, 596f, 62f, ink, serif, 0.04f)
        text("HAS SUCCESSFULLY COMPLETED", 660f, 27f, ink, serifThin, 0.12f)
        text("PUSH-UP LEVEL (${level + 1})", 730f, 58f, ink, serif, 0.03f)
        text(englishLevel(level), 776f, 26f, grey, serifThin, 0.1f)

        // 사진
        val cy = 990f
        val r = 155f
        val ph = photo
        if (ph != null) {
            val square = centerCrop(ph, (r * 2).toInt())
            val shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val m = Matrix()
            m.setTranslate(mx - r, cy - r)
            shader.setLocalMatrix(m)
            c.drawCircle(mx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        } else {
            c.drawCircle(mx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EFE7CE")
            })
            text("PHOTO", cy + 10f, 26f, grey, serifThin, 0.2f)
        }
        c.drawCircle(mx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = gold; strokeWidth = 7f
        })
        c.drawCircle(mx, cy, r + 15f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = goldPale; strokeWidth = 3f
        })

        text("$reps  PUSH-UPS  IN  ONE  UNBROKEN  SET", 1230f, 26f, ink, serifThin, 0.12f)
        text("Date awarded   " + prettyDate(), 1288f, 26f, grey, serifThin, 0.06f)

        // 서명 두 곳
        val sigL = 350f
        val sigR = W - 350f
        Brush.pushup(c, sigL - 118f, 1440f, 0.62f, ink)
        Brush.pushup(c, sigR - 118f, 1440f, 0.62f, ink)
        val line2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = grey; strokeWidth = 1.6f }
        c.drawLine(sigL - 150f, 1462f, sigL + 150f, 1462f, line2)
        c.drawLine(sigR - 150f, 1462f, sigR + 150f, 1462f, line2)
        val lab = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grey; textSize = 22f; typeface = serifThin
            textAlign = Paint.Align.CENTER; letterSpacing = 0.08f
        }
        c.drawText("HEAD OF TRAINING", sigL, 1496f, lab)
        c.drawText("PROGRAM DIRECTOR", sigR, 1496f, lab)

        // 가운데 인장
        val sy = 1420f
        c.drawCircle(mx, sy, 88f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F3E7C2")
        })
        val sealRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = gold; strokeWidth = 4f
        }
        c.drawCircle(mx, sy, 88f, sealRing)
        for (i in 0 until 36) {
            val a = Math.toRadians(i * 10.0)
            c.drawLine(
                mx + (Math.cos(a) * 88).toFloat(), sy + (Math.sin(a) * 88).toFloat(),
                mx + (Math.cos(a) * 99).toFloat(), sy + (Math.sin(a) * 99).toFloat(), sealRing
            )
        }
        c.drawCircle(mx, sy, 62f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = wine })
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldPale; textAlign = Paint.Align.CENTER; typeface = serif; textSize = 30f
        }
        c.drawText("LV.${level + 1}", mx, sy + 10f, sp)
        sp.textSize = 17f
        sp.typeface = serifThin
        c.drawText("CERTIFIED", mx, sy + 38f, sp)

        text("No. " + date.replace("-", "") + "-" + (level + 1), 1608f, 21f, grey, serifThin, 0.1f)

        return bmp
    }

    private fun englishLevel(l: Int): String = when (l) {
        0 -> "K N E E   P U S H - U P"
        1 -> "S T A N D A R D   F O R M"
        2 -> "F E E T   E L E V A T E D"
        3 -> "P I K E   P U S H - U P"
        4 -> "A R C H E R   P U S H - U P"
        else -> ""
    }

    private fun prettyDate(): String {
        val m = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return try {
            val p = date.split("-")
            "${p[2]} ${m[p[1].toInt() - 1]} ${p[0]}"
        } catch (e: Exception) { date }
    }

    private fun askName() {
        val input = android.widget.EditText(this).apply { setText(name) }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Name on certificate")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                name = input.text.toString().trim()
                getSharedPreferences("pushup", MODE_PRIVATE).edit().putString("name", name).apply()
                rebuild()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun centerCrop(src: Bitmap, size: Int): Bitmap {
        val s = min(src.width, src.height)
        val x = (src.width - s) / 2
        val y = (src.height - s) / 2
        val sq = Bitmap.createBitmap(src, x, y, s, s)
        return Bitmap.createScaledBitmap(sq, size, size, true)
    }

    private fun loadScaled(uri: Uri, target: Int): Bitmap? {
        return try {
            val opt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opt) }
            val big = max(opt.outWidth, opt.outHeight)
            val opt2 = BitmapFactory.Options().apply {
                inSampleSize = max(1, big / target)
            }
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opt2) }
        } catch (e: Exception) {
            Toast.makeText(this, "사진을 불러오지 못했습니다", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // ---------- 저장 / 내보내기 ----------

    private fun file(): File {
        val dir = File(filesDir, "certs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "cert_${date}_$level.png")
    }

    private fun saveToFile(bmp: Bitmap) {
        try {
            val f = file()
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Records.setCert(this, date, f.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "저장하지 못했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun share() {
        val f = file()
        if (!f.exists()) {
            current?.let { saveToFile(it) }
        }
        if (!f.exists()) {
            Toast.makeText(this, "이미지가 아직 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(i, "수료증 내보내기"))
    }
}

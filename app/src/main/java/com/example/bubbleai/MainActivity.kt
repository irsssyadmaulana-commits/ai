package com.example.bubbleai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var keyInput: EditText
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(fill: Int, r: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(r).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun tv(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.parseColor("#0B0B14")
        window.navigationBarColor = Color.parseColor("#1A1033")
        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(64), dp(24), dp(32))
        }
        val logo = tv("N", 44f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#7C5CFF"), Color.parseColor("#3DA9FC"))).apply { shape = GradientDrawable.OVAL }
            elevation = dp(12).toFloat()
        }
        col.addView(logo, LinearLayout.LayoutParams(dp(104), dp(104)))
        col.addView(tv("NotMe AI", 32f, Color.WHITE, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(20), 0, 0) })
        col.addView(tv("Asisten layar mengambang", 15f, Color.parseColor("#9AA0B4")).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(32)) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#151522"), 24, Color.parseColor("#2A2A40"))
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        card.addView(tv("API KEY ANTHROPIC", 11f, Color.parseColor("#7C5CFF"), true))
        keyInput = EditText(this).apply {
            hint = "sk-ant-..."; setHintTextColor(Color.parseColor("#5A5F75")); setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("key", "")); isSingleLine = true
            background = rounded(Color.parseColor("#0F0F1A"), 14, Color.parseColor("#2A2A40"))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        card.addView(keyInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10); bottomMargin = dp(18) })
        val btn = Button(this).apply {
            text = "Mulai"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false; stateListAnimator = null
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#7C5CFF"), Color.parseColor("#3DA9FC"))).apply { cornerRadius = dp(16).toFloat() }
            setOnClickListener { start() }
        }
        card.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, dp(54)))
        col.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        col.addView(tv("1  Izinkan tampil di atas aplikasi lain\n2  Izinkan perekaman layar\n3  Ketuk gelembung N untuk bertanya", 13f, Color.parseColor("#9AA0B4")).apply {
            setLineSpacing(0f, 1.35f); setPadding(dp(8), dp(28), dp(8), 0)
        })
        col.addView(tv("Hanya menampilkan jawaban. Tidak untuk ujian atau tes berpengawas.", 11f, Color.parseColor("#5A5F75")).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(28), 0, 0)
        })

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0B0B14"), Color.parseColor("#1A1033")))
            addView(col)
        })
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
    }

    private fun start() {
        val key = keyInput.text.toString().trim()
        if (key.isEmpty()) { Toast.makeText(this, "Masukkan API key dulu", Toast.LENGTH_SHORT).show(); return }
        getSharedPreferences("p", MODE_PRIVATE).edit().putString("key", key).apply()
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan 'tampil di atas aplikasi lain', lalu tekan Mulai lagi", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val mpm = getSystemService(MediaProjectionManager::class.java)
        startActivityForResult(mpm.createScreenCaptureIntent(), 1)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 1 && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, OverlayService::class.java).putExtra("code", res).putExtra("data", data))
            moveTaskToBack(true)
        }
    }
}

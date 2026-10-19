package com.example.bubbleai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.graphics.Typeface
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import android.graphics.drawable.Drawable
import android.widget.ScrollView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private var bubble: TextView? = null
    private var panel: View? = null
    private var busy = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i == null || projection != null) return START_NOT_STICKY
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("c", "NotMe AI", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "c").setContentTitle("NotMe AI aktif")
            .setSmallIcon(android.R.drawable.ic_menu_view).build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val mpm = getSystemService(MediaProjectionManager::class.java)
        @Suppress("DEPRECATION")
        projection = mpm.getMediaProjection(i.getIntExtra("code", 0), i.getParcelableExtra("data")!!)
        projection!!.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, main)

        val dm = resources.displayMetrics
        reader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2)
        vd = projection!!.createVirtualDisplay("cap", dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, null)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addBubble()
        return START_NOT_STICKY
    }

    private fun lp(w: Int, h: Int, focus: Boolean = false) = WindowManager.LayoutParams(w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        if (focus) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun addBubble() {
        val size = dp(58)
        val p = lp(size, size).apply { x = dp(12); y = dp(220) }
        val v = TextView(this).apply {
            text = "N"; setTextColor(Color.WHITE); gravity = Gravity.CENTER; textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#7C5CFF"), Color.parseColor("#3DA9FC"))).apply {
                shape = GradientDrawable.OVAL; setStroke(dp(2), Color.parseColor("#55FFFFFF"))
            }
            alpha = 0.95f
        }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt(); val dy = (e.rawY - sy).toInt()
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) moved = true
                    p.x = ox + dx; p.y = oy + dy; wm.updateViewLayout(v, p)
                }
                MotionEvent.ACTION_UP -> if (!moved) onTap()
            }
            true
        }
        bubble = v; wm.addView(v, p)
    }

    private fun onTap() {
        if (busy) return
        busy = true
        panel?.let { wm.removeView(it); panel = null }
        bubble?.visibility = View.INVISIBLE          // sembunyikan agar tidak ikut tertangkap
        main.postDelayed({
            val b64 = grab()
            bubble?.visibility = View.VISIBLE
            if (b64 == null) { show("Gagal mengambil layar, coba lagi."); return@postDelayed }
            show("", loading = true)
            val key = getSharedPreferences("p", MODE_PRIVATE).getString("key", "") ?: ""
            thread {
                val ans = try { ClaudeClient.ask(key, b64) } catch (e: Exception) { "Gagal: ${e.message}" }
                main.post { show(ans) }
            }
        }, 250)
    }

    private fun grab(): String? {
        val img = reader?.acquireLatestImage() ?: return null
        try {
            val pl = img.planes[0]
            val w = pl.rowStride / pl.pixelStride
            val full = Bitmap.createBitmap(w, img.height, Bitmap.Config.ARGB_8888)
            full.copyPixelsFromBuffer(pl.buffer)
            val crop = Bitmap.createBitmap(full, 0, 0, img.width, img.height)
            val s = 1000f / crop.width
            val small = Bitmap.createScaledBitmap(crop, 1000, (crop.height * s).toInt(), true)
            val out = ByteArrayOutputStream()
            small.compress(Bitmap.CompressFormat.JPEG, 80, out)
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } finally { img.close() }
    }

    private fun show(text: String, loading: Boolean = false) {
        busy = loading
        panel?.let { wm.removeView(it) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F2121220")); setStroke(dp(1), Color.parseColor("#2A2A40"))
                val r = dp(28).toFloat(); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }
        root.addView(View(this).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor("#3A3A55")); cornerRadius = dp(3).toFloat() }
        }, LinearLayout.LayoutParams(dp(40), dp(5)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(10) })

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            this.text = "NotMe AI"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f))
        if (!loading) head.addView(TextView(this).apply {
            this.text = "Salin"; textSize = 13f; setTextColor(Color.parseColor("#3DA9FC")); setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("NotMe AI", text))
                Toast.makeText(context, "Tersalin", Toast.LENGTH_SHORT).show()
            }
        })
        head.addView(TextView(this).apply {
            this.text = "\u2715"; textSize = 16f; setTextColor(Color.parseColor("#9AA0B4")); setPadding(dp(12), dp(8), dp(4), dp(8))
            setOnClickListener { panel?.let { wm.removeView(it) }; panel = null; busy = false }
        })
        root.addView(head)

        if (loading) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(18), 0, dp(8)) }
            row.addView(ProgressBar(this).apply { isIndeterminate = true }, LinearLayout.LayoutParams(dp(28), dp(28)))
            row.addView(TextView(this).apply { this.text = "Menganalisis layar..."; textSize = 15f; setTextColor(Color.parseColor("#C8CCE0")); setPadding(dp(14), 0, 0, 0) })
            root.addView(row)
        } else {
            val tv = TextView(this).apply {
                this.text = text; textSize = 15f; setTextColor(Color.parseColor("#E8E8F0")); setLineSpacing(0f, 1.3f)
                setTextIsSelectable(true); setPadding(0, dp(10), 0, 0)
            }
            root.addView(ScrollView(this).apply { addView(tv) }, LinearLayout.LayoutParams(MATCH, 0, 1f))
        }
        val h = if (loading) WindowManager.LayoutParams.WRAP_CONTENT else (resources.displayMetrics.heightPixels * 0.45).toInt()
        val p = lp(MATCH, h).apply { gravity = Gravity.BOTTOM; x = 0 }
        panel = root; wm.addView(root, p)
    }

    private val MATCH = WindowManager.LayoutParams.MATCH_PARENT

    override fun onDestroy() {
        panel?.let { wm.removeView(it) }; bubble?.let { wm.removeView(it) }
        vd?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
}

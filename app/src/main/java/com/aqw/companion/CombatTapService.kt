package com.aqw.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** User-initiated taps only. Does not detect bosses, cooldowns, HP or deaths. */
class CombatTapService : AccessibilityService() {
    companion object {
        @Volatile var instance: CombatTapService? = null
        const val PREFS = "combat_tap"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var overlay: LinearLayout? = null
    private lateinit var wm: WindowManager
    private var index = 0
    private var sequence = listOf(2, 3, 4, 5)
    private var intervalMs = 1200L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopLoop() }
    override fun onDestroy() {
        stopLoop()
        overlay?.let { runCatching { wm.removeView(it) } }
        overlay = null
        instance = null
        super.onDestroy()
    }
    fun stopLoop() { running = false; handler.removeCallbacksAndMessages(null); updateLabel() }
    fun startLoop() {
        stopLoop()
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        sequence = prefs.getString("sequence", "2,3,4,5").orEmpty()
            .split(',', ' ', '-', '>').mapNotNull { it.trim().toIntOrNull()?.takeIf { n -> n in 1..5 } }
        if (sequence.isEmpty()) return
        intervalMs = prefs.getLong("interval", 1200L).coerceIn(350L, 10000L)
        index = 0
        running = true
        updateLabel()
        handler.post(loop)
    }
    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            val n = sequence[index % sequence.size]
            index++
            val metrics = resources.displayMetrics
            val xPercent = prefs.getInt("x$n", 50).coerceIn(1, 99)
            val yPercent = prefs.getInt("y", 85).coerceIn(1, 99)
            val x = metrics.widthPixels * xPercent / 100f
            val y = metrics.heightPixels * yPercent / 100f
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 55)).build()
            dispatchGesture(gesture, null, null)
            handler.postDelayed(this, intervalMs)
        }
    }
    private var state: TextView? = null
    private fun updateLabel() { state?.text = if (running) "Auto: ATIVO" else "Auto: PARADO" }
    private fun showOverlay() {
        if (overlay != null) return
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(0xDD20252E.toInt())
        }
        state = TextView(this).apply { setTextColor(-1); textSize = 12f }
        layout.addView(state)
        layout.addView(Button(this).apply {
            text = "Iniciar"
            setOnClickListener { startLoop() }
        })
        layout.addView(Button(this).apply {
            text = "PARAR"
            setOnClickListener { stopLoop() }
        })
        val params = WindowManager.LayoutParams(
            170, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; y = 170 }
        wm.addView(layout, params)
        overlay = layout
        updateLabel()
    }
}

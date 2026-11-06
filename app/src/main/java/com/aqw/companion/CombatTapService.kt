package com.aqw.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/** User-initiated taps only. Does not detect bosses, cooldowns, HP or deaths. */
class CombatTapService : AccessibilityService() {
    companion object {
        @Volatile var instance: CombatTapService? = null
        const val PREFS = "combat_tap"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var index = 0
    private var sequence = listOf(2, 3, 4, 5)
    private var intervalMs = 1200L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Controls are hosted by the existing BubbleService overlay.
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopLoop() }
    override fun onDestroy() {
        stopLoop()
        instance = null
        super.onDestroy()
    }
    fun isLoopRunning(): Boolean = running
    fun stopLoop() { running = false; handler.removeCallbacksAndMessages(null);  }
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
            val size = Point()
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(size)
            val xPercent = prefs.getInt("x$n", 36 + n * 4).coerceIn(1, 99)
            val yPercent = prefs.getInt("y", 94).coerceIn(1, 99)
            val x = size.x * xPercent / 100f
            val y = size.y * yPercent / 100f
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 55)).build()
            dispatchGesture(gesture, null, null)
            handler.postDelayed(this, intervalMs)
        }
    }
}

package com.aqw.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/** Repeats user-selected skill button taps; never moves the character or targets enemies. */
class CombatTapService : AccessibilityService() {
    companion object {
        @Volatile var instance: CombatTapService? = null
        const val PREFS = "combat_tap"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var index = 0
    private var sequence = listOf(2, 3, 4, 5)
    private var intervalMs = 250L
    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            val n = sequence[index % sequence.size]
            val size = Point()
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(size)
            val xPercent = prefs.getInt("x$n", 36 + n * 4).coerceIn(1, 99)
            val yPercent = prefs.getInt("y", 94).coerceIn(1, 99)
            val path = Path().apply {
                moveTo(size.x * xPercent / 100f, size.y * yPercent / 100f)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 55)).build()
            val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (!running) return
                    index++
                    scheduleNextTap()
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    stopLoop()
                }
            }, handler)
            if (!accepted) stopLoop()
        }
    }

    private fun scheduleNextTap() {
        if (running) handler.postDelayed(loop, intervalMs)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopLoop() }
    override fun onDestroy() {
        stopLoop()
        instance = null
        super.onDestroy()
    }
    fun isLoopRunning(): Boolean = running
    fun stopLoop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }
    fun startLoop(mode: String = "farm") {
        stopLoop()
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val key = if (mode == "boss") "boss_sequence" else "farm_sequence"
        sequence = prefs.getString(key, if (mode == "boss") "4,3,2,5" else prefs.getString("sequence", "4,3,2,5")).orEmpty()
            .split(',', ' ', '-', '>').mapNotNull {
                it.trim().toIntOrNull()?.takeIf { n -> n in 1..5 }
            }
        if (sequence.isEmpty()) return
        intervalMs = prefs.getLong("interval", 250L).let { if (it == 1200L) 250L else it }.coerceIn(120L, 10000L)
        index = 0
        running = true
        handler.post(loop)
    }
}

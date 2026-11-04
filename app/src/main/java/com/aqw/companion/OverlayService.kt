package com.aqw.companion

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (panel == null) showPanel()
        return START_NOT_STICKY
    }

    private fun showPanel() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            setBackgroundColor(0xEE20242A.toInt())
        }
        val status = TextView(this).apply {
            text = "AQW Companion • pronto"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
        }
        box.addView(status)
        box.addView(Button(this).apply {
            text = "Scanner visual (em breve)"
            setOnClickListener { status.text = "OCR ainda não implementado nesta versão." }
        })
        box.addView(Button(this).apply {
            text = "Fechar painel"
            setOnClickListener { stopSelf() }
        })
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 24; y = 120 }
        windowManager.addView(box, params)
        panel = box
    }

    override fun onDestroy() {
        panel?.let { windowManager.removeView(it) }
        panel = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

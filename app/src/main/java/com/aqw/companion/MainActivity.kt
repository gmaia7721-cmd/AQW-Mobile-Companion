package com.aqw.companion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        layout.addView(TextView(this).apply {
            text = "AQW Mobile Companion\n\nPainel visual auxiliar. Não automatiza combate."
            textSize = 20f
        })
        layout.addView(Button(this).apply {
            text = "Ativar painel flutuante"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                    finish()
                }
            }
        })
        setContentView(layout)
    }
}

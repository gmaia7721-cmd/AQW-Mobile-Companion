package com.aqw.companion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    companion object {
        private const val REQUEST_OVERLAY = 100
        private const val REQUEST_CAPTURE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        layout.addView(TextView(this).apply {
            text = "AQW Mobile Companion\n\nPrepare a captura antes de entrar no servidor. Depois, mantenha o jogo aberto e use o painel flutuante para identificar os monstros."
            textSize = 18f
        })
        layout.addView(Button(this).apply {
            text = "Preparar para jogar"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                        REQUEST_OVERLAY
                    )
                } else {
                    requestScreenCapture()
                }
            }
        })
        setContentView(layout)
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) requestScreenCapture()
        } else if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_CAPTURE
                    putExtra(OverlayService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(OverlayService.EXTRA_RESULT_DATA, data)
                }
                startForegroundService(serviceIntent)
            }
            finish()
        }
    }
}

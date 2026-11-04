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
import android.app.AlertDialog
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    companion object {
        private const val REQUEST_OVERLAY = 100
        private const val REQUEST_CAPTURE = 101
        private const val RELEASE_API = "https://api.github.com/repos/gmaia7721-cmd/AQW-Mobile-Companion/releases/latest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        layout.addView(TextView(this).apply {
            text = "AQW Mobile Companion\\n\\nPrepare a captura antes de entrar no servidor. Depois, mantenha o jogo aberto e use o painel flutuante para identificar os monstros.\\n\\nVersão instalada: ${installedVersionName()}"
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
        MonsterCatalogUpdater.loadCached(this)
        checkForUpdates()
    }

    private fun installedVersionName(): String {
        @Suppress("DEPRECATION")
        return packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0"
    }

    private fun checkForUpdates() {
        Thread {
            var latestVersion: String? = null
            var releaseUrl: String? = null
            try {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "AQW-Mobile-Companion")
                }
                try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val release = JSONObject(body)
                        if (!release.optBoolean("draft", true) && !release.optBoolean("prerelease", true)) {
                            latestVersion = release.optString("tag_name").removePrefix("v")
                            releaseUrl = release.optString("html_url")
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                // Sem internet ou API indisponível: o app continua funcionando normalmente.
            }

            val remote = latestVersion
            val url = releaseUrl
            if (!remote.isNullOrBlank() && !url.isNullOrBlank() && isNewerVersion(remote, installedVersionName())) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        AlertDialog.Builder(this)
                            .setTitle("Nova atualização disponível!")
                            .setMessage("Versão instalada: ${installedVersionName()}\\nNova versão: $remote\\n\\nDeseja abrir a página para baixar a atualização?")
                            .setNegativeButton("Depois", null)
                            .setPositiveButton("Baixar atualização") { _, _ ->
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                            .show()
                    }
                }
            }
        }.start()
    }

    private fun isNewerVersion(remote: String, installed: String): Boolean {
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val installedParts = installed.split(".").map { it.toIntOrNull() ?: 0 }
        val size = maxOf(remoteParts.size, installedParts.size)
        for (i in 0 until size) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = installedParts.getOrElse(i) { 0 }
            if (r != l) return r > l
        }
        return false
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

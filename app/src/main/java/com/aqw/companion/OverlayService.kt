package com.aqw.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OverlayService : Service() {
    companion object {
        const val ACTION_CAPTURE = "com.aqw.companion.ACTION_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "aqw_scanner"
        private const val NOTIFICATION_ID = 42
    }

    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var statusView: TextView? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var processing = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CAPTURE) {
            startForegroundCompat()
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_RESULT_DATA)
            }
            if (data != null) beginCapture(resultCode, data)
            else updateStatus("Não foi possível iniciar a captura.")
        }
        return START_NOT_STICKY
    }

    private fun showPanel() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            setBackgroundColor(0xEE20242A.toInt())
        }
        statusView = TextView(this).apply {
            text = "AQW Companion • pronto"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
        }
        box.addView(statusView)
        box.addView(Button(this).apply {
            text = "IDENTIFICAR MONSTROS"
            setOnClickListener {
                updateStatus("Autorize a captura de tela na próxima janela.")
                val scanIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_SCAN
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(scanIntent)
            }
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

    private fun startForegroundCompat() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Scanner AQW", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setContentTitle("AQW Mobile Companion")
            .setContentText("Analisando a tela do jogo")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginCapture(resultCode: Int, data: Intent) {
        stopProjection()
        try {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, data)
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                if (processing) return@setOnImageAvailableListener
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                processing = true
                analyzeImage(image)
            }, mainHandler)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AQW-Monster-Scanner",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                mainHandler
            )
            updateStatus("Lendo a tela… aguarde.")
        } catch (e: Exception) {
            updateStatus("Falha ao capturar: ${e.localizedMessage ?: "erro desconhecido"}")
            stopProjection()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun analyzeImage(image: Image) {
        var bitmap: Bitmap? = null
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val paddedWidth = image.width + rowPadding / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            buffer.rewind()
            padded.copyPixelsFromBuffer(buffer)
            bitmap = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
            padded.recycle()
        } catch (e: Exception) {
            updateStatus("Não foi possível ler a imagem da tela.")
            image.close()
            processing = false
            stopProjection()
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        image.close()

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                val lines = result.textBlocks.flatMap { block -> block.lines.map { it.text.trim() } }
                    .filter { it.length >= 3 }
                    .distinct()
                val output = if (lines.isEmpty()) {
                    "Nenhum nome/texto reconhecido. Tente deixar os monstros maiores na tela."
                } else {
                    "Textos encontrados (confira os nomes):\n" + lines.take(12).joinToString("\n")
                }
                updateStatus(output)
            }
            .addOnFailureListener {
                updateStatus("Erro no reconhecimento de texto. Tente novamente.")
            }
            .addOnCompleteListener {
                bitmap.recycle()
                recognizer.close()
                processing = false
                stopProjection()
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
    }

    private fun updateStatus(message: String) {
        mainHandler.post { statusView?.text = message }
    }

    private fun stopProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        processing = false
    }

    override fun onDestroy() {
        stopProjection()
        panel?.let { windowManager.removeView(it) }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

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
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var processing = false
    @Volatile private var scanRequested = false

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
            else updateStatus("Não foi possível iniciar a captura. Abra o app e autorize novamente.")
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
            text = "AQW Companion • aguardando preparação"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
        }
        box.addView(statusView)
        box.addView(Button(this).apply {
            text = "IDENTIFICAR MONSTROS"
            setOnClickListener {
                if (mediaProjection == null || imageReader == null) {
                    updateStatus("Captura não preparada. Abra o Companion e toque em Preparar para jogar antes de entrar no servidor.")
                } else if (processing || scanRequested) {
                    updateStatus("A análise anterior ainda está em andamento.")
                } else {
                    scanRequested = true
                    updateStatus("Capturando a tela do jogo…")
                }
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
            .setContentText("Captura preparada para identificar monstros")
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
        if (mediaProjection != null) {
            updateStatus("Captura já está preparada. Volte ao jogo e toque em Identificar Monstros.")
            return
        }
        try {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, data)
            mediaProjection = projection

            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    virtualDisplay?.release()
                    virtualDisplay = null
                    imageReader?.close()
                    imageReader = null
                    mediaProjection = null
                    projectionCallback = null
                    scanRequested = false
                    processing = false
                    updateStatus("Captura encerrada pelo Android. Abra o Companion e prepare novamente.")
                }
            }
            projection.registerCallback(projectionCallback!!, mainHandler)

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
            imageReader = reader
            reader.setOnImageAvailableListener({ source ->
                val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                if (!scanRequested || processing) {
                    image.close()
                    return@setOnImageAvailableListener
                }
                scanRequested = false
                processing = true
                analyzeImage(image)
            }, mainHandler)

            virtualDisplay = projection.createVirtualDisplay(
                "AQW-Monster-Scanner",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )
            updateStatus("Captura preparada! Agora entre no servidor. Não será necessário autorizar novamente nesta sessão.")
        } catch (e: Exception) {
            updateStatus("Falha ao preparar captura: ${e.localizedMessage ?: "erro desconhecido"}")
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
                    "Nenhum texto reconhecido. O scanner ainda precisa da base de nomes dos monstros."
                } else {
                    "Textos encontrados na tela:\n" + lines.take(12).joinToString("\n")
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
            }
    }

    private fun updateStatus(message: String) {
        mainHandler.post { statusView?.text = message }
    }

    private fun stopProjection() {
        val projection = mediaProjection
        val callback = projectionCallback
        mediaProjection = null
        projectionCallback = null
        if (projection != null && callback != null) {
            try { projection.unregisterCallback(callback) } catch (_: Exception) {}
        }
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        scanRequested = false
        processing = false
        try { projection?.stop() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopProjection()
        panel?.let { windowManager.removeView(it) }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

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
        MonsterCatalogUpdater.loadCached(this)
        MonsterCatalogUpdater.refresh(this) { count, updated ->
            if (updated) {
                updateStatus("Base do Wiki atualizada: $count nomes. Pode buscar monstros.")
            } else {
                updateStatus("Base local disponível: $count nomes. Não foi possível atualizar o Wiki agora.")
            }
        }
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

    private var panelParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private var statusMessage = "AQW Companion • captura aguardando"

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showPanel() {
        val params = WindowManager.LayoutParams(
            dp(56),
            dp(56),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(120)
        }
        panelParams = params
        panel = LinearLayout(this)
        renderPanel()
        windowManager.addView(panel, params)
    }

    private fun renderPanel() {
        val box = panel ?: return
        box.removeAllViews()
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(10), dp(8), dp(10), dp(8))
        box.setBackgroundColor(0xEE20242A.toInt())

        if (!expanded) {
            val bubble = Button(this).apply {
                text = "AQW"
                textSize = 13f
                setPadding(dp(4), 0, dp(4), 0)
                setOnClickListener {
                    expanded = true
                    resizePanel()
                    renderPanel()
                }
            }
            box.addView(bubble, LinearLayout.LayoutParams(dp(56), dp(40)))
        } else {
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            header.addView(TextView(this).apply {
                text = "AQW • MONSTROS"
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
            }, LinearLayout.LayoutParams(0, dp(36), 1f))
            header.addView(Button(this).apply {
                text = "—"
                textSize = 16f
                setPadding(0, 0, 0, 0)
                setOnClickListener {
                    expanded = false
                    resizePanel()
                    renderPanel()
                }
            }, LinearLayout.LayoutParams(dp(42), dp(36)))
            box.addView(header)

            statusView = TextView(this).apply {
                text = statusMessage
                textSize = 12f
                maxLines = 14
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(dp(2), dp(4), dp(2), dp(6))
            }
            box.addView(statusView)

            box.addView(Button(this).apply {
                text = "BUSCAR MONSTROS"
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
        }
    }

    private fun resizePanel() {
        val params = panelParams ?: return
        params.width = if (expanded) dp(240) else dp(76)
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        panel?.let { windowManager.updateViewLayout(it, params) }
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

        val sourceBitmap = bitmap ?: run {
            processing = false
            return
        }
        // Enlarge small floating monster labels before OCR. A 1.5x scale
        // improves character detail without the memory cost of doubling
        // the full captured screen.
        val ocrBitmap = Bitmap.createScaledBitmap(
            sourceBitmap,
            (sourceBitmap.width * 1.5f).toInt().coerceAtLeast(sourceBitmap.width),
            (sourceBitmap.height * 1.5f).toInt().coerceAtLeast(sourceBitmap.height),
            true
        )
        val redOcrBitmap = createRedTextBitmap(ocrBitmap)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val recognizedLines = linkedSetOf<String>()
        var successfulPasses = 0

        // First read the full screen, then scan four overlapping regions.
        // This gives small/distant monster labels a larger effective OCR area.
        data class ScanRegion(val left: Int, val top: Int, val width: Int, val height: Int)
        val regions = mutableListOf(
            ScanRegion(0, 0, ocrBitmap.width, ocrBitmap.height)
        )
        val overlapX = (ocrBitmap.width * 0.12f).toInt()
        val overlapY = (ocrBitmap.height * 0.12f).toInt()
        val halfW = ocrBitmap.width / 2
        val halfH = ocrBitmap.height / 2
        for (row in 0..1) {
            for (col in 0..1) {
                val left = (col * halfW - if (col == 1) overlapX else 0).coerceAtLeast(0)
                val top = (row * halfH - if (row == 1) overlapY else 0).coerceAtLeast(0)
                val right = ((col + 1) * halfW + if (col == 0) overlapX else 0).coerceAtMost(ocrBitmap.width)
                val bottom = ((row + 1) * halfH + if (row == 0) overlapY else 0).coerceAtMost(ocrBitmap.height)
                regions.add(ScanRegion(left, top, right - left, bottom - top))
            }
        }

        fun finishAll() {
            val monsters = MonsterCatalog.findMatches(recognizedLines.toList())
            val lines = recognizedLines.toList()
            val output = when {
                monsters.isNotEmpty() ->
                    "MONSTROS IDENTIFICADOS (${monsters.size}):\\n" +
                        monsters.take(8).joinToString("\\n") +
                        if (monsters.size > 8) "\\n+${monsters.size - 8} outros" else ""
                lines.isEmpty() ->
                    "Nenhum texto reconhecido. Verifique se os nomes estão visíveis."
                else ->
                    "Nenhum monstro confirmado. OCR leu ${lines.size} linhas:\\n" +
                        lines.take(14).joinToString("\\n") +
                        if (lines.size > 14) "\\n… e mais ${lines.size - 14} linhas" else ""
            }
            if (successfulPasses == 0) updateStatus("Erro no reconhecimento de texto. Tente novamente.")
            else updateStatus(output)
            ocrBitmap.recycle()
            redOcrBitmap.recycle()
            sourceBitmap.recycle()
            recognizer.close()
            processing = false
        }

        fun addResult(result: com.google.mlkit.vision.text.Text) {
            successfulPasses++
            result.textBlocks.flatMap { block -> block.lines.map { it.text.trim() } }
                .filter { it.length >= 3 }.forEach { recognizedLines.add(it) }
        }

        // Process regions one at a time to limit memory use on the phone.
        fun scanRegion(index: Int) {
            if (index >= regions.size) {
                finishAll()
                return
            }
            val region = regions[index]
            val crop = Bitmap.createBitmap(
                ocrBitmap, region.left, region.top, region.width, region.height
            )
            val redCrop = createRedTextBitmap(crop)
            recognizer.process(InputImage.fromBitmap(crop, 0))
                .addOnSuccessListener { result -> addResult(result) }
                .addOnCompleteListener {
                    recognizer.process(InputImage.fromBitmap(redCrop, 0))
                        .addOnSuccessListener { result -> addResult(result) }
                        .addOnCompleteListener {
                            crop.recycle()
                            redCrop.recycle()
                            scanRegion(index + 1)
                        }
                }
        }
        scanRegion(0)
    }

    /**
     * Creates a black-on-white mask of red-dominant pixels. The ordinary OCR
     * pass still runs, so labels in other colors remain detectable.
     */
    private fun createRedTextBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val row = IntArray(source.width)
        val mask = IntArray(source.width)
        for (y in 0 until source.height) {
            source.getPixels(row, 0, source.width, 0, y, source.width, 1)
            for (x in row.indices) {
                val pixel = row[x]
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val isRedText = red >= 75 && red - green >= 25 && red - blue >= 25 &&
                    red * 100 >= green * 118 && red * 100 >= blue * 118
                mask[x] = if (isRedText) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
            output.setPixels(mask, 0, source.width, 0, y, source.width, 1)
        }
        return output
    }

    private fun updateStatus(message: String) {
        statusMessage = message
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

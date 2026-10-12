package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.R
import com.example.model.BossCalculationEngine
import com.example.model.BossDefaultRates
import com.example.model.CutPiece
import com.example.model.GlassCutResult
import com.example.ui.BossAppState
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object ChartPrintHelper {
    private var cachedLogoBase64: String? = null
    private var cachedCustomLogoPath: String? = null

    private var cachedBgBase64: String? = null
    private var cachedCustomBgPath: String? = null

    @SuppressLint("StaticFieldLeak")
    private var activePrintWebView: WebView? = null

    fun invalidateLogoCache() {
        cachedLogoBase64 = null
        cachedCustomLogoPath = null
    }

    fun invalidateBackgroundCache() {
        cachedBgBase64 = null
        cachedCustomBgPath = null
    }

    fun getTransparentLogoBitmap(
        context: Context,
        targetSize: Int = 180,
        customLogoPath: String? = null,
        isVipOwner: Boolean = false
    ): Bitmap? {
        return try {
            val size = targetSize.coerceAtLeast(64)

            if (customLogoPath != null) {
                val file = java.io.File(customLogoPath)
                if (file.exists() && file.length() > 0) {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    var sample = 1
                    while (boundsOptions.outWidth / (sample * 2) >= size && boundsOptions.outHeight / (sample * 2) >= size) {
                        sample *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sample
                    }
                    val customBm = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    if (customBm != null) {
                        val scale = Math.min(size.toFloat() / customBm.width, size.toFloat() / customBm.height)
                        val targetW = (customBm.width * scale).toInt().coerceAtLeast(1)
                        val targetH = (customBm.height * scale).toInt().coerceAtLeast(1)
                        return Bitmap.createScaledBitmap(customBm, targetW, targetH, true)
                    }
                }
            }

            val rawLogo = BitmapFactory.decodeResource(context.resources, R.drawable.ic_boss_logo_clean)
            if (rawLogo != null) {
                val scaled = Bitmap.createScaledBitmap(rawLogo, size, size, true)
                val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(output)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                val radius = size / 2f
                canvas.drawCircle(radius, radius, radius * 0.98f, paint)
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(scaled, 0f, 0f, paint)
                return output
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun createHighResRoundLogoBitmap(
        context: Context,
        drawableResId: Int = R.drawable.ic_boss_logo_clean,
        targetSize: Int = 1024
    ): Bitmap? {
        return try {
            val rawLogo = BitmapFactory.decodeResource(context.resources, drawableResId) ?: return null
            val scaled = Bitmap.createScaledBitmap(rawLogo, targetSize, targetSize, true)
            val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val radius = targetSize / 2f
            canvas.drawCircle(radius, radius, radius - 2f, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            output
        } catch (e: Exception) {
            null
        }
    }

    fun saveRoundLogoToGalleryAndDownloads(
        context: Context,
        drawableResId: Int = R.drawable.ic_boss_logo_clean,
        fileName: String = "Boss_B_Logo_Round.png"
    ): Boolean {
        return try {
            val roundBitmap = createHighResRoundLogoBitmap(context, drawableResId, 1024) ?: return false
            var savedSuccess = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val imageValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/BossAluminium"
                    )
                }
                val imageUri = resolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    imageValues
                )
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        roundBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    savedSuccess = true
                }

                val downloadValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                }
                val downloadUri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    downloadValues
                )
                if (downloadUri != null) {
                    resolver.openOutputStream(downloadUri)?.use { out ->
                        roundBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    savedSuccess = true
                }
            }

            val fallbackDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            val fallbackFile = File(fallbackDir, fileName)
            FileOutputStream(fallbackFile).use { out ->
                roundBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            savedSuccess = true

            Toast.makeText(
                context,
                "Round 'B' Logo (PNG) saved to Gallery & Downloads ($fileName)!",
                Toast.LENGTH_LONG
            ).show()
            savedSuccess
        } catch (e: Exception) {
            Toast.makeText(context, "Could not save logo: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareRoundLogoPng(
        context: Context,
        drawableResId: Int = R.drawable.ic_boss_logo_clean,
        fileName: String = "Boss_B_Logo_Round.png"
    ) {
        try {
            val roundBitmap = createHighResRoundLogoBitmap(context, drawableResId, 1024) ?: return
            val imageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            if (!imageDir.exists()) imageDir.mkdirs()
            val outFile = File(imageDir, fileName)
            FileOutputStream(outFile).use { out ->
                roundBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Boss Round 'B' Logo (PNG)")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Save / Share Round 'B' Logo"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing round logo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getLogoBase64(context: Context, customLogoPath: String? = null, isVipOwner: Boolean = false): String {
        if (cachedLogoBase64 != null && cachedCustomLogoPath == customLogoPath) {
            return cachedLogoBase64!!
        }
        return try {
            val transparentBitmap = getTransparentLogoBitmap(context, 160, customLogoPath, isVipOwner)
            if (transparentBitmap != null) {
                val outputStream = ByteArrayOutputStream()
                transparentBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                cachedLogoBase64 = encoded
                cachedCustomLogoPath = customLogoPath
                encoded
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getQuotationBackgroundBitmap(context: Context, customBgPath: String? = null): Bitmap? {
        return try {
            if (!customBgPath.isNullOrBlank()) {
                val file = java.io.File(customBgPath)
                if (file.exists()) {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    val maxTargetDim = 1200
                    var sample = 1
                    while (boundsOptions.outWidth / (sample * 2) >= maxTargetDim || boundsOptions.outHeight / (sample * 2) >= maxTargetDim) {
                        sample *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val customBm = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    if (customBm != null) return customBm
                }
            }
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_boss_quotation_crisp)
            if (drawable != null) {
                val bmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, 595, 842)
                drawable.draw(canvas)
                return bmp
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, newAttributes != oldAttributes)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                if (destination == null) {
                    callback?.onWriteFailed("Print destination descriptor is null")
                    return
                }
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    Log.e("ChartPrintHelper", "Error writing PDF to print spooler: ${e.message}")
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    fun printHtml(context: Context, html: String, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

        mainHandler.post {
            try {
                val webView = try {
                    WebView(context)
                } catch (e: Throwable) {
                    Toast.makeText(context, "System print engine unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@post
                }
                activePrintWebView = webView
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                webView.settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val targetView = view ?: activePrintWebView ?: return
                        try {
                            val rawAdapter = targetView.createPrintDocumentAdapter(jobName)
                            val printAdapter = object : PrintDocumentAdapter() {
                                override fun onStart() {
                                    try { rawAdapter.onStart() } catch (_: Exception) {}
                                }
                                override fun onLayout(
                                    oldAttributes: PrintAttributes?,
                                    newAttributes: PrintAttributes?,
                                    cancellationSignal: CancellationSignal?,
                                    callback: LayoutResultCallback?,
                                    extras: Bundle?
                                ) {
                                    rawAdapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
                                }
                                override fun onWrite(
                                    pages: Array<out PageRange>?,
                                    destination: ParcelFileDescriptor?,
                                    cancellationSignal: CancellationSignal?,
                                    callback: WriteResultCallback?
                                ) {
                                    rawAdapter.onWrite(pages, destination, cancellationSignal, callback)
                                }
                                override fun onFinish() {
                                    try { rawAdapter.onFinish() } catch (_: Exception) {}
                                    try { targetView.destroy() } catch (_: Exception) {}
                                    if (activePrintWebView === targetView) activePrintWebView = null
                                }
                            }
                            val printAttributes = PrintAttributes.Builder()
                                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build()
                            printManager.print(jobName, printAdapter, printAttributes)
                        } catch (_: Exception) {}
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        try { view?.destroy() } catch (_: Exception) {}
                        if (activePrintWebView === view) activePrintWebView = null
                        return true
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "UTF-8", null)
            } catch (_: Exception) {}
        }
    }

    fun printLengthChart(
        context: Context,
        state: BossAppState,
        sectionMap: Map<String, List<CutPiece>>
    ) {
        val jobName = "Boss_LengthChart_${state.customerName.ifBlank { "Job" }}_${System.currentTimeMillis() % 10000}"
        val pdfFile = try {
            ChartPdfGenerator.generateLengthChartPdf(context, state, sectionMap)
        } catch (e: Throwable) {
            null
        }
        if (pdfFile != null && pdfFile.exists()) {
            printPdfFile(context, pdfFile, jobName)
        }
    }

    fun printCuttingChart(
        context: Context,
        state: BossAppState,
        sectionMap: Map<String, List<CutPiece>>,
        glassCuts: List<GlassCutResult>
    ) {
        val jobName = "Boss_CuttingChart_${state.customerName.ifBlank { "Job" }}_${System.currentTimeMillis() % 10000}"
        val pdfFile = try {
            ChartPdfGenerator.generateCuttingChartPdf(context, state, sectionMap, glassCuts)
        } catch (e: Throwable) {
            null
        }
        if (pdfFile != null && pdfFile.exists()) {
            printPdfFile(context, pdfFile, jobName)
        }
    }

    fun printGlassChart(
        context: Context,
        state: BossAppState,
        cuts: List<GlassCutResult>,
        totalSqft: Double
    ) {
        val jobName = "Boss_GlassChart_${state.customerName.ifBlank { "Job" }}_${System.currentTimeMillis() % 10000}"
        val pdfFile = try {
            ChartPdfGenerator.generateGlassChartPdf(context, state, cuts, totalSqft)
        } catch (e: Throwable) {
            null
        }
        if (pdfFile != null && pdfFile.exists()) {
            printPdfFile(context, pdfFile, jobName)
        }
    }

    fun printQuotationBill(
        context: Context,
        state: BossAppState,
        quote: com.example.model.QuotationResult
    ) {
        val jobName = "Boss_Quotation_${state.customerName.ifBlank { "Job" }}_${System.currentTimeMillis() % 10000}"
        val pdfFile = try {
            QuotationPdfGenerator.generatePdfFile(context, quote, state)
        } catch (e: Throwable) {
            null
        }
        if (pdfFile != null && pdfFile.exists()) {
            printPdfFile(context, pdfFile, jobName)
        }
    }
}

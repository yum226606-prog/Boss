package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.model.BossDefaultRates
import com.example.model.ScannedRateRow
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

data class DetectedRateItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val section: String,
    val originalText: String,
    val gauge: String,
    val gaugeSource: String,
    val category: String,
    val isCustomSection: Boolean,
    val rates: Map<String, Double>,
    val detectedColors: List<String> = rates.keys.toList()
)

data class RateDetectionResult(
    val engine: String,
    val items: List<DetectedRateItem>,
    val categoriesFound: List<String>,
    val rawText: String = "",
    val error: String? = null
)

object RateListDetector {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1500): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var bytes: ByteArray? = null

            try {
                contentResolver.openInputStream(uri)?.use { bytes = it.readBytes() }
            } catch (_: Throwable) {}

            if (bytes == null || bytes!!.isEmpty()) {
                try {
                    contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        FileInputStream(pfd.fileDescriptor).use { fis ->
                            bytes = fis.readBytes()
                        }
                    }
                } catch (_: Throwable) {}
            }

            if (bytes == null || bytes!!.isEmpty()) {
                try {
                    val tempFile = File(context.cacheDir, "temp_ocr_${System.currentTimeMillis()}.img")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        bytes = tempFile.readBytes()
                        tempFile.delete()
                    }
                } catch (_: Throwable) {}
            }

            val rawBytes = bytes ?: return@withContext null
            if (rawBytes.isEmpty()) return@withContext null

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            var sampleSize = 1
            if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap = try {
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            } catch (_: OutOfMemoryError) {
                val lowMemOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize * 2
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, lowMemOptions)
            } ?: return@withContext null

            try {
                ByteArrayInputStream(rawBytes).use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    }
                    if (!matrix.isIdentity) {
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        if (rotated != bitmap) {
                            bitmap.recycle()
                            bitmap = rotated
                        }
                    }
                }
            } catch (_: Throwable) {}

            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun renderPdfPages(context: Context, uri: Uri, maxPages: Int = 6): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        val contentResolver = context.contentResolver
        val pfd = try {
            contentResolver.openFileDescriptor(uri, "r")
        } catch (_: Exception) {
            return@withContext emptyList()
        } ?: return@withContext emptyList()

        var renderer: PdfRenderer? = null
        try {
            renderer = PdfRenderer(pfd)
            val pagesToProcess = minOf(renderer.pageCount, maxPages)
            for (i in 0 until pagesToProcess) {
                val page = renderer.openPage(i)
                try {
                    val scale = 2
                    val width = page.width * scale
                    val height = page.height * scale
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bmp)
                } finally {
                    page.close()
                }
            }
        } catch (_: Exception) {
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd.close() } catch (_: Exception) {}
        }
        bitmaps
    }

    suspend fun detectRatesFromFile(
        context: Context,
        uri: Uri,
        customApiKey: String? = null
    ): RateDetectionResult = withContext(Dispatchers.IO) {
        try {
            val mimeType = try { context.contentResolver.getType(uri)?.lowercase() ?: "" } catch (_: Throwable) { "" }
            val uriStr = uri.toString().lowercase()
            val isPdf = mimeType == "application/pdf" || uriStr.endsWith(".pdf")

            val bitmaps: List<Bitmap> = try {
                if (isPdf) {
                    renderPdfPages(context, uri, maxPages = 4)
                } else {
                    val bmp = loadBitmapFromUri(context, uri, maxDimension = 1500)
                    if (bmp != null) listOf(bmp) else emptyList()
                }
            } catch (_: Throwable) {
                emptyList()
            }

            if (bitmaps.isEmpty()) {
                return@withContext RateDetectionResult(
                    engine = "None",
                    items = emptyList(),
                    categoriesFound = emptyList(),
                    error = "Could not open or decode the image/PDF file."
                )
            }

            val effectiveApiKey = when {
                !customApiKey.isNullOrBlank() -> customApiKey.trim()
                BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY") -> BuildConfig.GEMINI_API_KEY.trim()
                else -> ""
            }

            if (effectiveApiKey.isNotBlank()) {
                try {
                    val geminiResult = detectRatesWithGemini(bitmaps.first(), effectiveApiKey)
                    if (geminiResult.items.isNotEmpty()) {
                        bitmaps.forEach { if (!it.isRecycled) it.recycle() }
                        return@withContext geminiResult
                    }
                } catch (_: Throwable) {}
            }

            val mlResult = try {
                detectRatesWithMlKit(bitmaps)
            } catch (t: Throwable) {
                RateDetectionResult(
                    engine = "ML Kit Error",
                    items = emptyList(),
                    categoriesFound = emptyList(),
                    error = "OCR Engine encountered an error: ${t.localizedMessage ?: "Unknown error"}."
                )
            }

            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
            mlResult
        } catch (t: Throwable) {
            RateDetectionResult(
                engine = "Error",
                items = emptyList(),
                categoriesFound = emptyList(),
                error = "Failed to process file: ${t.localizedMessage ?: "Memory or decoding error"}"
            )
        }
    }

    suspend fun detectRatesWithGemini(bitmap: Bitmap, apiKey: String): RateDetectionResult = withContext(Dispatchers.IO) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
You are an expert Pakistani Aluminium price list reader and OCR table parser.
Extract ALL aluminium sections, their categories, gauges, and rates from this image.
Output the result strictly as a clean JSON array matching this schema:
[
  {
    "category": "CURTAIN WALL SECTIONS",
    "section_name": "D.10",
    "gauge": "1.2mm",
    "rates": {
      "DULL": 514,
      "H23_PC_RAL": 516,
      "SAHARA_BROWN": 552,
      "BLACK_MULTI": 578,
      "WOOD_COAT": 645
    }
  }
]
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("Gemini API Error: HTTP ${response.code} - $responseBody")
            }

            val respObj = JSONObject(responseBody)
            val candidates = respObj.optJSONArray("candidates") ?: throw Exception("No response candidates")
            val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: throw Exception("Empty content")
            val parts = content.optJSONArray("parts") ?: throw Exception("Empty parts")
            val rawJsonText = parts.optJSONObject(0)?.optString("text") ?: throw Exception("Empty text part")

            parseDetectedRatesJson(rawJsonText, "Gemini Multimodal AI")
        }
    }

    fun parseDetectedRatesJson(jsonText: String, engine: String): RateDetectionResult {
        val cleanJson = jsonText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val itemsArray = try {
            if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else {
                val root = JSONObject(cleanJson)
                root.optJSONArray("items")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("sections")
                    ?: JSONArray()
            }
        } catch (_: Exception) {
            JSONArray()
        }

        val detected = mutableListOf<DetectedRateItem>()
        val categories = mutableSetOf<String>()

        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.optJSONObject(i) ?: continue
            val secRaw = itemObj.optString("section_name", "").ifBlank {
                itemObj.optString("section", "")
            }.trim()
            if (secRaw.isBlank()) continue

            val normalizedSec = normalizeSectionCode(secRaw)
            val orig = itemObj.optString("originalText", secRaw)
            val rawGauge = itemObj.optString("gauge", "1.2mm")
            var gauge = normalizeGauge(rawGauge)
            if (gauge.isBlank()) gauge = "1.2"

            val gaugeSource = if (rawGauge.contains("1.2") && !orig.contains("1.2")) "DEFAULT_1_2" else "EXPLICIT_ROW"
            val cat = itemObj.optString("category", "GENERAL SECTIONS").trim().ifBlank { "GENERAL SECTIONS" }
            categories.add(cat)

            val isCustom = !BossDefaultRates.DEFAULT_SECTIONS.contains(normalizedSec)
            val ratesObj = itemObj.optJSONObject("rates") ?: JSONObject()
            val ratesMap = mutableMapOf<String, Double>()

            val keys = ratesObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val r = ratesObj.optDouble(k, 0.0)
                if (r > 0) {
                    val standardColor = mapColorHeader(k)
                    ratesMap[standardColor] = r
                }
            }

            if (ratesMap.isNotEmpty()) {
                detected.add(
                    DetectedRateItem(
                        section = normalizedSec,
                        originalText = orig,
                        gauge = gauge,
                        gaugeSource = gaugeSource,
                        category = cat,
                        isCustomSection = isCustom,
                        rates = ratesMap,
                        detectedColors = ratesMap.keys.toList()
                    )
                )
            }
        }

        return RateDetectionResult(
            engine = engine,
            items = detected,
            categoriesFound = categories.toList(),
            rawText = cleanJson,
            error = if (detected.isEmpty()) "No rates found in the AI response." else null
        )
    }

    suspend fun detectRatesWithMlKit(bitmaps: List<Bitmap>): RateDetectionResult = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val allItems = mutableListOf<DetectedRateItem>()
        val allCategories = mutableSetOf<String>()
        val fullTextBuilder = StringBuilder()

        for ((index, bitmap) in bitmaps.withIndex()) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.processAwait(inputImage)
                fullTextBuilder.append("\n# --- Page ${index + 1} ---\n")
                fullTextBuilder.append(visionText.text)

                var pageItems = parseFromRawTextLines(visionText.text)
                if (pageItems.isEmpty() && bitmap.width > 50 && bitmap.height > 50) {
                    try {
                        val inverted = createInvertedBitmap(bitmap)
                        val invVision = recognizer.processAwait(InputImage.fromBitmap(inverted, 0))
                        val invItems = parseFromRawTextLines(invVision.text)
                        if (invItems.isNotEmpty()) {
                            pageItems = invItems
                        }
                        if (!inverted.isRecycled) inverted.recycle()
                    } catch (_: Throwable) {}
                }
                allItems.addAll(pageItems)
                pageItems.forEach { allCategories.add(it.category) }
            } catch (_: Exception) {}
        }

        val mergedItems = mergeDuplicateDetectedItems(allItems)
        RateDetectionResult(
            engine = "On-Device ML Kit OCR",
            items = mergedItems,
            categoriesFound = allCategories.toList(),
            rawText = fullTextBuilder.toString(),
            error = if (mergedItems.isEmpty()) "No rates found in the image/screenshot." else null
        )
    }

    private suspend fun com.google.mlkit.vision.text.TextRecognizer.processAwait(image: InputImage): Text =
        suspendCancellableCoroutine { cont ->
            process(image)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }

    private fun createInvertedBitmap(src: Bitmap): Bitmap {
        val inverted = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(inverted)
        val paint = Paint()
        val matrix = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return inverted
    }

    private fun mergeDuplicateDetectedItems(items: List<DetectedRateItem>): List<DetectedRateItem> {
        val map = linkedMapOf<String, DetectedRateItem>()
        for (item in items) {
            val key = "${item.section.uppercase()}_${item.gauge}"
            val existing = map[key]
            if (existing == null) {
                map[key] = item
            } else {
                val combinedRates = existing.rates.toMutableMap()
                for ((col, rate) in item.rates) {
                    if (rate > 0) combinedRates[col] = rate
                }
                map[key] = existing.copy(
                    rates = combinedRates,
                    detectedColors = combinedRates.keys.toList()
                )
            }
        }
        return map.values.toList()
    }

    fun parseFromRawTextLines(text: String): List<DetectedRateItem> {
        val items = mutableListOf<DetectedRateItem>()
        val lines = text.lines()
        var currentCategory = "GENERAL"
        var currentGauge = "1.2"
        var pageColor: String? = null
        val defaultColors = listOf("DULL", "SAHARA/BROWN", "H23/PC-RAL", "BLACK/MULTI", "WOOD COAT")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            val upper = line.uppercase()

            if (isCategoryHeading(upper)) {
                currentCategory = cleanCategoryHeading(line)
                currentGauge = extractHeadingGauge(upper)
                pageColor = extractHeadingColor(upper)
                continue
            }

            val secName = detectSectionName(line) ?: continue
            val explicitGauge = extractGaugeFromText(line)
            val gauge = explicitGauge ?: currentGauge
            val numbers = extractPriceNumbersFromLine(line)
            if (numbers.isEmpty()) continue

            val lineColors = extractInlineColors(line)
            val ratesMap = mutableMapOf<String, Double>()

            if (lineColors.isNotEmpty() && lineColors.size == numbers.size) {
                for (i in lineColors.indices) {
                    ratesMap[lineColors[i]] = numbers[i]
                }
            } else if (pageColor != null && numbers.size == 1) {
                ratesMap[pageColor] = numbers.first()
            } else {
                for (i in numbers.indices) {
                    val colorKey = when {
                        i < lineColors.size -> lineColors[i]
                        i < defaultColors.size -> defaultColors[i]
                        else -> "COLOR_${i + 1}"
                    }
                    ratesMap[colorKey] = numbers[i]
                }
            }

            if (ratesMap.isNotEmpty()) {
                val isCustom = !BossDefaultRates.DEFAULT_SECTIONS.contains(secName)
                items.add(
                    DetectedRateItem(
                        section = secName,
                        originalText = line,
                        gauge = gauge,
                        gaugeSource = if (explicitGauge != null) "EXPLICIT_ROW" else "HEADING_GAUGE",
                        category = currentCategory,
                        isCustomSection = isCustom,
                        rates = ratesMap,
                        detectedColors = ratesMap.keys.toList()
                    )
                )
            }
        }
        return items
    }

    private fun extractPriceNumbersFromLine(line: String): List<Double> {
        val tokens = line.split(Regex("[\\s,;|\t/:-]+"))
        val prices = mutableListOf<Double>()
        for (token in tokens) {
            val clean = token.replace("RS", "", ignoreCase = true)
                .replace("PKR", "", ignoreCase = true)
                .replace("=", "")
                .trim()
            val d = clean.toDoubleOrNull()
            if (d != null && d in 100.0..99999.0 && !token.contains("MM", ignoreCase = true)) {
                prices.add(d)
            }
        }
        return prices
    }

    private fun extractInlineColors(line: String): List<String> {
        val upper = line.uppercase()
        val colors = mutableListOf<String>()
        val colorKeywords = listOf(
            "DULL" to "DULL",
            "SILVER" to "DULL",
            "MILL" to "DULL",
            "SAHARA" to "SAHARA/BROWN",
            "BROWN" to "SAHARA/BROWN",
            "BRONZE" to "SAHARA/BROWN",
            "CHAMPAGNE" to "SAHARA/BROWN",
            "H23" to "H23/PC-RAL",
            "H-23" to "H23/PC-RAL",
            "RAL" to "H23/PC-RAL",
            "PC-RAL" to "H23/PC-RAL",
            "WHITE" to "H23/PC-RAL",
            "POWDER" to "H23/PC-RAL",
            "BLACK" to "BLACK/MULTI",
            "BLAK" to "BLACK/MULTI",
            "MULTI" to "BLACK/MULTI",
            "MATT BLACK" to "BLACK/MULTI",
            "WOOD" to "WOOD COAT",
            "WOODEN" to "WOOD COAT",
            "GOLD" to "GOLD",
            "GREY" to "GREY"
        )
        for ((keyword, standard) in colorKeywords) {
            if (upper.contains(keyword) && !colors.contains(standard)) {
                colors.add(standard)
            }
        }
        return colors
    }

    fun detectSectionName(raw: String): String? {
        val upper = raw.uppercase().trim()
        if (upper.isBlank()) return null
        val condensed = upper.replace(".", "").replace(" ", "").replace("-", "")

        val dcMatch = Regex("""\b(?:SEC(?:TION)?[:\s-]*)?DC[-.]?\s*([2-3]\d[A-Z]?)\b""").find(upper)
        if (dcMatch != null) return "DC${dcMatch.groupValues[1]}"

        val dMatch = Regex("""\b(?:SEC(?:TION)?[:\s-]*)?D[-.]?\s*([1-6]\d[A-Z]?)\b""").find(upper)
        if (dMatch != null) return "D${dMatch.groupValues[1]}"

        val mMatch = Regex("""\b(?:SEC(?:TION)?[:\s-]*)?M[-.]?\s*([2-3]\d[A-Z]?)\b""").find(upper)
        if (mMatch != null) return "M${mMatch.groupValues[1]}"

        val cwMatch = Regex("""\b(?:SEC(?:TION)?[:\s-]*)?CW[-.]?\s*(\d+[A-Z]?)\b""").find(upper)
        if (cwMatch != null) {
            val num = cwMatch.groupValues[1]
            return if (num.length == 1) "CW0$num" else "CW$num"
        }

        if (upper.contains("PIPE") || upper.contains("TUBE")) {
            val pipeDigits = upper.replace(Regex("""[^0-9*xX/.-]"""), "")
            when {
                pipeDigits.contains("1123") -> return "PIPE1123"
                pipeDigits.contains("1-1/2") || pipeDigits.contains("11/2") || pipeDigits.contains("1.5") || pipeDigits.contains("112") -> return "PIPE112"
                pipeDigits.contains("1*1") || pipeDigits.contains("1X1") || pipeDigits == "11" || pipeDigits == "1" -> return "PIPE1"
                pipeDigits.contains("1*2") || pipeDigits.contains("1X2") || pipeDigits.contains("2*1") || pipeDigits == "12" -> return "PIPE12"
                pipeDigits.contains("2*2") || pipeDigits.contains("2X2") || pipeDigits == "22" -> return "PIPE22"
                pipeDigits.contains("1*3") || pipeDigits.contains("1X3") || pipeDigits == "13" -> return "PIPE13"
                else -> return "PIPE12"
            }
        }

        if (upper.contains("ANGLE")) {
            return if (upper.contains("112") || upper.contains("1.5") || upper.contains("1-1/2")) "ANGLE112" else "ANGLE1"
        }

        if (upper.contains("PATTI") || upper.contains("PLATE")) {
            return if (upper.contains("6")) "PATTI6" else "PATTI412"
        }

        if (Regex("""\bL[-.]?\s*39\b""").containsMatchIn(upper)) return "L39"
        if (Regex("""\bZ[-.]?\s*1\b""").containsMatchIn(upper)) return "Z1"
        if (Regex("""\bZ[-.]?\s*2\b""").containsMatchIn(upper)) return "Z2"

        val sortedDefaultSections = BossDefaultRates.DEFAULT_SECTIONS.sortedByDescending { it.length }
        for (sec in sortedDefaultSections) {
            if (condensed == sec) return sec
            val regex = Regex("""(?:\b|[^A-Z0-9])""" + Regex.escape(sec) + """(?:\b|[^A-Z0-9]|$)""")
            if (regex.containsMatchIn(upper)) return sec
        }

        val genericMatch = Regex("""\b([A-Z]{1,4}[-.]?[0-9]{1,4}[A-Z]?)\b""").find(upper)
        if (genericMatch != null) {
            val code = genericMatch.groupValues[1].replace(".", "").replace("-", "").trim()
            if (code.length >= 2 && !code.all { it.isDigit() }) {
                return code
            }
        }

        return null
    }

    private fun extractHeadingColor(text: String): String? {
        val u = text.uppercase()
        return when {
            u.contains("DULL") -> "DULL"
            u.contains("SAHARA") || u.contains("BROWN") || u.contains("BRONZE") || u.contains("CHAMPAGNE") -> "SAHARA/BROWN"
            u.contains("RAL") || u.contains("H23") || u.contains("H-23") || u.contains("POWDER") || u.contains("WHITE") -> "H23/PC-RAL"
            u.contains("BLACK") || u.contains("BLAK") -> "BLACK/MULTI"
            u.contains("WOOD") -> "WOOD COAT"
            u.contains("GOLD") -> "GOLD"
            else -> null
        }
    }

    private fun extractHeadingGauge(upper: String): String {
        return when {
            upper.contains("2MM") || upper.contains("2.0MM") || upper.contains("2.0 MM") || upper.contains("2 MM") -> "2"
            upper.contains("1.6MM") || upper.contains("1.6 MM") -> "1.6"
            upper.contains("1.4MM") || upper.contains("1.4 MM") -> "1.4"
            upper.contains("2.5MM") || upper.contains("2.5 MM") -> "2.5"
            upper.contains("3MM") || upper.contains("3.0MM") || upper.contains("3.0 MM") -> "3"
            else -> "1.2"
        }
    }

    private fun isCategoryHeading(text: String): Boolean {
        val t = text.uppercase()
        return t.contains("SLIDING SEC") ||
                t.contains("CURTAIN WALL") ||
                t.contains("KITCHEN SEC") ||
                t.contains("RECT.PIPE") ||
                t.contains("RECT PIPE") ||
                t.contains("RECTANGULAR PIPE") ||
                t.contains("SQUARE PIPE") ||
                t.contains("ANGLE") ||
                t.contains("PATTI") ||
                t.contains("NORMAL SLIDING") ||
                t.contains("2MM SLIDING") ||
                t.contains("1.6MM SLIDING") ||
                t.contains("PRICE LIST") ||
                t.contains("RATE LIST") ||
                t.contains("ALUMINIUM SECTIONS")
    }

    private fun cleanCategoryHeading(text: String): String {
        return text.replace(Regex("(?i)[#*_=~-]"), "").trim()
    }

    fun mapColorHeader(raw: String): String {
        val r = raw.trim().uppercase().replace(".", "").replace(" ", "").replace("-", "").replace("_", "").replace("/", "")
        return when {
            r.contains("DULL") || r == "SILVER" || r.contains("MILL") || r.contains("PLAIN") || r.contains("ANODIZED") || r.contains("NATURAL") -> "DULL"
            r.contains("H23PCRAL") || r.contains("H23RAL") || r.contains("H23") || r.contains("RALPC") ||
                    r.contains("RAL") || r.contains("PCRAL") || r.contains("POWDER") || r.contains("WHITE") -> "H23/PC-RAL"
            r.contains("SAHARABROWN") || r.contains("SHARA") || r.contains("SAHARA") ||
                    r.contains("BRO") || r.contains("BROWN") || r.contains("BRONZE") || r.contains("CHAMPAGNE") -> "SAHARA/BROWN"
            r.contains("BLACKMULTI") || r.contains("BLAK") || r.contains("BLACK") || r.contains("MULTI") -> "BLACK/MULTI"
            r.contains("WOOD") || r.contains("WOODEN") || r.contains("WOODCOAT") -> "WOOD COAT"
            r.contains("GOLD") || r.contains("GOLDEN") -> "GOLD"
            r.contains("GREY") || r.contains("GRAY") -> "GREY"
            else -> raw.trim().uppercase()
        }
    }

    fun extractGaugeFromText(text: String): String? {
        val upper = text.uppercase()
        val mmMatch = Regex("""(?:^|\s|\(|\b)(\d+(?:\.\d+)?)\s*(?:MM|M|GAUGE)\b""").find(upper)
        if (mmMatch != null) {
            return normalizeGauge(mmMatch.groupValues[1])
        }
        val decMatch = Regex("""\b(1\.[2-9]|2\.[0-9]|3\.[0-9])\b""").find(upper)
        if (decMatch != null) {
            return normalizeGauge(decMatch.groupValues[1])
        }
        return null
    }

    fun normalizeGauge(g: String): String {
        val trimmed = g.trim().uppercase().replace("MM", "").replace("M", "").trim()
        val d = trimmed.toDoubleOrNull() ?: return "1.2"
        return when {
            abs(d - 1.2) < 0.05 -> "1.2"
            abs(d - 1.4) < 0.05 -> "1.4"
            abs(d - 1.6) < 0.05 -> "1.6"
            abs(d - 2.0) < 0.05 -> "2"
            abs(d - 2.5) < 0.05 -> "2.5"
            abs(d - 3.0) < 0.05 -> "3"
            abs(d - 3.2) < 0.05 -> "3.2"
            abs(d - 3.5) < 0.05 -> "3.5"
            abs(d - 4.0) < 0.05 -> "4"
            abs(d - 5.0) < 0.05 -> "5"
            abs(d - 6.0) < 0.05 -> "6"
            else -> if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
        }
    }

    fun normalizeSectionCode(raw: String): String {
        val detected = detectSectionName(raw)
        if (detected != null) return detected

        val clean = raw.uppercase()
            .replace(Regex("""\(\s*\d+(?:\.\d+)?\s*MM\s*\)"""), "")
            .replace(Regex("""\b\d+(?:\.\d+)?\s*MM\b"""), "")
            .replace(Regex("""\(.*?\)"""), "")
            .trim()

        val condensed = clean.replace(".", "").replace(" ", "").replace("-", "")
        val sortedKnown = BossDefaultRates.DEFAULT_SECTIONS.sortedByDescending { it.length }
        for (sec in sortedKnown) {
            if (condensed.startsWith(sec) || clean.contains(sec)) {
                return sec
            }
        }

        val safeCode = clean.replace(Regex("""[^A-Z0-9*xX/_-]"""), "").trim()
        return if (safeCode.isNotBlank()) safeCode else condensed.ifBlank { raw.trim().uppercase() }
    }

    fun convertDetectedItemsToScannedRows(items: List<DetectedRateItem>): List<ScannedRateRow> {
        val result = mutableListOf<ScannedRateRow>()
        items.forEach { item ->
            val cat = item.category.ifBlank { "ALUMINIUM SECTIONS" }
            val sec = item.section
            val g = item.gauge.ifBlank { "1.2" }
            item.rates.forEach { (col, rate) ->
                if (rate > 0.0) {
                    result.add(
                        ScannedRateRow(
                            sectionTitle = cat,
                            sectionCode = sec,
                            gauge = g,
                            color = col,
                            rate = rate
                        )
                    )
                }
            }
        }
        return result
    }

    fun parseRawTextToScannedRows(rawText: String): List<ScannedRateRow> {
        if (rawText.isBlank()) return emptyList()
        val result = mutableListOf<ScannedRateRow>()
        val lines = rawText.lines()

        // Check if user pasted direct 5-column lines: [Title] | [Code] | [Gauge] | [Color] | [Rate]
        var parsedAsFiveCol = false
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (line.contains("|")) {
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 5) {
                    val rate = parts[4].replace("Rs", "", ignoreCase = true).replace(",", "").trim().toDoubleOrNull()
                    if (rate != null && rate > 0.0) {
                        result.add(
                            ScannedRateRow(
                                sectionTitle = parts[0].ifBlank { "ALUMINIUM SECTIONS" },
                                sectionCode = normalizeSectionCode(parts[1]),
                                gauge = normalizeGauge(parts[2]),
                                color = mapColorHeader(parts[3]),
                                rate = rate
                            )
                        )
                        parsedAsFiveCol = true
                    }
                }
            }
        }

        if (parsedAsFiveCol && result.isNotEmpty()) {
            return result
        }

        // Otherwise parse using regular regex/NLP line parser
        val items = parseFromRawTextLines(rawText)
        return convertDetectedItemsToScannedRows(items)
    }

    fun formatScannedRowsToFiveColumnText(rows: List<ScannedRateRow>): String {
        return rows.joinToString("\n") { row ->
            "${row.sectionTitle} | ${row.sectionCode} | ${row.gauge} | ${row.color} | ${row.rate.toLong()}"
        }
    }

    suspend fun detectScannedRowsFromMultipleUris(
        context: Context,
        uris: List<Uri>,
        rawTextInput: String = ""
    ): List<ScannedRateRow> = withContext(Dispatchers.IO) {
        val allRows = mutableListOf<ScannedRateRow>()

        // 1. Process all selected photos/PDFs
        for (uri in uris) {
            try {
                val res = detectRatesFromFile(context, uri)
                if (res.items.isNotEmpty()) {
                    allRows.addAll(convertDetectedItemsToScannedRows(res.items))
                }
            } catch (_: Throwable) {
                // Ignore individual file parse errors and continue
            }
        }

        // 2. Process pasted raw text / sample sheet
        if (rawTextInput.isNotBlank()) {
            allRows.addAll(parseRawTextToScannedRows(rawTextInput))
        }

        // 3. Deduplicate rows: unique by (gauge, color, sectionCode)
        val deduplicated = mutableMapOf<String, ScannedRateRow>()
        allRows.forEach { r ->
            val key = "${normalizeGauge(r.gauge)}|${mapColorHeader(r.color)}|${normalizeSectionCode(r.sectionCode)}"
            deduplicated[key] = r
        }

        return@withContext deduplicated.values.toList()
    }
}


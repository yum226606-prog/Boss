package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.BossCalculationEngine
import com.example.model.BossDefaultRates
import com.example.model.CutPiece
import com.example.model.GlassCutResult
import com.example.ui.BossAppState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChartPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val MAX_Y = 785f

    private fun getShopTitle(state: BossAppState): String {
        return if (state.userProfile.isVipOwner) {
            state.companyName.ifBlank { "BOSS ALUMINIUM & GLASS" }
        } else {
            if (state.companyName.isNotBlank() && state.companyName != "BOSS ALUMINIUM & GLASS") state.companyName
            else state.userProfile.shopAddress.ifBlank { state.userProfile.name.ifBlank { "YOUR SHOP / BUSINESS NAME" } }
        }
    }

    fun generateCuttingChartPdf(
        context: Context,
        state: BossAppState,
        sectionMap: Map<String, List<CutPiece>>,
        glassCuts: List<GlassCutResult>
    ): File? {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.parseColor("#CBD5E1")
        }

        val logoBitmap = try {
            ChartPrintHelper.getTransparentLogoBitmap(context, 350, state.customLogoPath, state.userProfile.isVipOwner)
        } catch (e: Exception) {
            null
        }

        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        val refCode = "CUT-${System.currentTimeMillis() % 100000}"

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 48
        }

        fun drawWatermark(c: Canvas) {
            if (logoBitmap != null) {
                try {
                    val wSize = 340
                    val scaledW = Bitmap.createScaledBitmap(logoBitmap, wSize, wSize, true)
                    val left = (PAGE_WIDTH - wSize) / 2f
                    val top = (PAGE_HEIGHT - wSize) / 2f
                    c.drawBitmap(scaledW, left, top, watermarkPaint)
                } catch (e: Exception) {}
            }
        }

        fun drawHeader(c: Canvas) {
            drawWatermark(c)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#D9A441")
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, paint)

            if (logoBitmap != null) {
                try {
                    val scaled = Bitmap.createScaledBitmap(logoBitmap, 38, 38, true)
                    c.drawBitmap(scaled, MARGIN_LEFT, 18f, paint)
                } catch (e: Exception) {
                    paint.color = Color.parseColor("#D9A441")
                    c.drawCircle(MARGIN_LEFT + 19f, 37f, 18f, paint)
                }
            } else {
                paint.color = Color.parseColor("#D9A441")
                c.drawCircle(MARGIN_LEFT + 19f, 37f, 18f, paint)
            }

            boldPaint.textSize = 13.5f
            boldPaint.color = Color.parseColor("#0F172A")
            val compName = getShopTitle(state)
            c.drawText(compName, MARGIN_LEFT + 46f, 30f, boldPaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#B45309")
            c.drawText("Workshop Cutting Chart & Optimization Plan", MARGIN_LEFT + 46f, 41f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#64748B")
            val cAddr = state.companyAddress.ifBlank { "Main Market, Pakistan" }
            val cPhone = state.companyPhone.ifBlank { "+92 300 1234567" }
            val cNtn = state.companyNtn.ifBlank { "1234567-8" }
            val compDetails = buildList {
                if (cPhone.isNotBlank()) add("Phone: $cPhone")
                if (cAddr.isNotBlank()) add("Factory: $cAddr")
                if (cNtn.isNotBlank()) add("NTN: $cNtn")
            }.joinToString(" | ")
            c.drawText(compDetails, MARGIN_LEFT + 46f, 51f, textPaint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FEF3C7")
            val badgeRect = RectF(440f, 18f, MARGIN_RIGHT, 38f)
            c.drawRoundRect(badgeRect, 4f, 4f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#D97706")
            c.drawRoundRect(badgeRect, 4f, 4f, paint)

            boldPaint.textSize = 9.5f
            boldPaint.color = Color.parseColor("#92400E")
            c.drawText("CUTTING CHART", 450f, 31f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#475569")
            c.drawText("Date: $currentDate", 420f, 48f, textPaint)
            c.drawText("Ref: $refCode", 420f, 58f, textPaint)

            strokePaint.color = Color.parseColor("#CBD5E1")
            c.drawLine(MARGIN_LEFT, 66f, MARGIN_RIGHT, 66f, strokePaint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            val infoBox = RectF(MARGIN_LEFT, 72f, MARGIN_RIGHT, 102f)
            c.drawRoundRect(infoBox, 4f, 4f, paint)
            strokePaint.color = Color.parseColor("#E2E8F0")
            c.drawRoundRect(infoBox, 4f, 4f, strokePaint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.parseColor("#1E293B")
            val custStr = state.customerName.ifBlank { "Workshop Job" }
            val phoneStr = if (state.customerPhone.isNotBlank()) " (${state.customerPhone})" else ""
            c.drawText("Customer: $custStr$phoneStr", MARGIN_LEFT + 8f, 85f, boldPaint)

            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#475569")
            c.drawText("Spec: ${state.ctxGauge}mm | ${state.ctxColor} | Frame: ${state.ctxFrameType}", MARGIN_LEFT + 8f, 96f, textPaint)

            val totalSecCount = sectionMap.size
            val totalBarsCount = sectionMap.values.sumOf { pieces ->
                BossCalculationEngine.packSection(pieces, BossCalculationEngine.barLengthsFor(pieces.firstOrNull()?.section ?: "")).bins.size
            }
            c.drawText("Sections: $totalSecCount | Total Bars: $totalBarsCount", 380f, 85f, textPaint)
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            strokePaint.color = Color.parseColor("#E2E8F0")
            c.drawLine(MARGIN_LEFT, 804f, MARGIN_RIGHT, 804f, strokePaint)
            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#94A3B8")
            c.drawText("Generated by Boss Aluminium & Glass App - Authorized Workshop Plan", MARGIN_LEFT, 818f, textPaint)
            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#475569")
            c.drawText("Page $pNum", MARGIN_RIGHT - 35f, 818f, boldPaint)
        }

        drawHeader(canvas)
        var curY = 112f

        fun checkNewPage(neededSpace: Float) {
            if (curY + neededSpace > MAX_Y) {
                drawFooter(canvas, pageNumber)
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                curY = 112f
            }
        }

        sectionMap.forEach { (section, pieces) ->
            val lens = BossCalculationEngine.barLengthsFor(section)
            val packed = BossCalculationEngine.packSection(pieces, lens)
            val label = BossDefaultRates.SECTION_LABELS[section]
            val sectionHeader = if (!label.isNullOrBlank() && !label.equals(section, ignoreCase = true)) {
                "$section - $label"
            } else {
                section
            }

            checkNewPage(45f)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#1E293B")
            val secBanner = RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f)
            canvas.drawRoundRect(secBanner, 3f, 3f, paint)

            boldPaint.textSize = 9.5f
            boldPaint.color = Color.WHITE
            canvas.drawText(sectionHeader, MARGIN_LEFT + 8f, curY + 13f, boldPaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#FDE68A")
            canvas.drawText("${packed.bins.size} Bar(s)", MARGIN_RIGHT - 65f, curY + 13f, boldPaint)

            curY += 22f

            packed.bins.forEachIndexed { barIdx, bar ->
                val barTitle = if (bar.isCustom) "${bar.lenFt} FT Offcut" else "${bar.lenFt} FT Stock Bar"
                val waste = BossCalculationEngine.toDotSutar(bar.remainInches.coerceAtLeast(0.0))
                val pieceRows = bar.pieces.withIndex().chunked(2)
                val estimatedHeight = 22f + (pieceRows.size * 15f) + 6f

                checkNewPage(estimatedHeight)

                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F1F5F9")
                val barHeader = RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 15f)
                canvas.drawRect(barHeader, paint)
                strokePaint.color = Color.parseColor("#CBD5E1")
                canvas.drawRect(barHeader, strokePaint)

                boldPaint.textSize = 8.5f
                boldPaint.color = Color.parseColor("#0F172A")
                canvas.drawText("Bar #${barIdx + 1}: $barTitle", MARGIN_LEFT + 6f, curY + 11f, boldPaint)

                boldPaint.textSize = 8f
                boldPaint.color = Color.parseColor("#B45309")
                canvas.drawText("Waste/Offcut: $waste\"", MARGIN_RIGHT - 110f, curY + 11f, boldPaint)

                curY += 15f

                val midX = (MARGIN_LEFT + MARGIN_RIGHT) / 2f

                pieceRows.forEachIndexed { rIdx, pair ->
                    val isAlt = rIdx % 2 == 1
                    if (isAlt) {
                        paint.style = Paint.Style.FILL
                        paint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 15f, paint)
                    }

                    pair.forEachIndexed { colIdx, indexedPiece ->
                        val pIdx = indexedPiece.index
                        val p = indexedPiece.value
                        val colStart = if (colIdx == 0) MARGIN_LEFT else midX
                        val colEnd = if (colIdx == 0) midX else MARGIN_RIGHT
                        val sizeStr = "${BossCalculationEngine.toDotSutar(p.length)}\""

                        boldPaint.textSize = 7.8f
                        boldPaint.color = Color.parseColor("#0F172A")
                        canvas.drawText("#${pIdx + 1} ${p.source}", colStart + 6f, curY + 10.5f, boldPaint)

                        textPaint.textSize = 7.2f
                        textPaint.color = Color.parseColor("#475569")
                        canvas.drawText(p.label, colStart + 92f, curY + 10.5f, textPaint)

                        boldPaint.textSize = 8.5f
                        boldPaint.color = Color.parseColor("#B45309")
                        canvas.drawText(sizeStr, colEnd - 46f, curY + 10.5f, boldPaint)
                    }

                    strokePaint.color = Color.parseColor("#CBD5E1")
                    canvas.drawLine(midX, curY, midX, curY + 15f, strokePaint)
                    strokePaint.color = Color.parseColor("#E2E8F0")
                    canvas.drawLine(MARGIN_LEFT, curY + 15f, MARGIN_RIGHT, curY + 15f, strokePaint)
                    curY += 15f
                }

                curY += 6f
            }
            curY += 6f
        }

        if (glassCuts.isNotEmpty()) {
            checkNewPage(50f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0F766E")
            val glassBanner = RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f)
            canvas.drawRoundRect(glassBanner, 3f, 3f, paint)

            boldPaint.textSize = 9.5f
            boldPaint.color = Color.WHITE
            canvas.drawText("GLASS CUTTING SIZES (${glassCuts.size} cuts)", MARGIN_LEFT + 8f, curY + 13f, boldPaint)
            curY += 22f

            glassCuts.forEachIndexed { idx, g ->
                checkNewPage(16f)
                val isAlt = idx % 2 == 1
                if (isAlt) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#F0FDFA")
                    canvas.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 15f, paint)
                }

                boldPaint.textSize = 8f
                boldPaint.color = Color.parseColor("#134E4A")
                canvas.drawText(g.frameLabel, MARGIN_LEFT + 8f, curY + 11f, boldPaint)

                textPaint.textSize = 7.5f
                textPaint.color = Color.parseColor("#334155")
                canvas.drawText("${g.glassGauge}mm ${g.glassColor}", MARGIN_LEFT + 150f, curY + 11f, textPaint)

                boldPaint.textSize = 8.5f
                boldPaint.color = Color.parseColor("#0F766E")
                val cutDim = "${BossCalculationEngine.toDotSutar(g.gWpanel)}\" x ${BossCalculationEngine.toDotSutar(g.gH)}\""
                canvas.drawText(cutDim, MARGIN_RIGHT - 140f, curY + 11f, boldPaint)

                textPaint.textSize = 7.5f
                canvas.drawText("(${g.pieces} pcs, ${String.format(Locale.ENGLISH, "%.1f", g.sqft)} sqft)", MARGIN_RIGHT - 75f, curY + 11f, textPaint)

                strokePaint.color = Color.parseColor("#E2E8F0")
                canvas.drawLine(MARGIN_LEFT, curY + 15f, MARGIN_RIGHT, curY + 15f, strokePaint)
                curY += 15f
            }
        }

        drawFooter(canvas, pageNumber)
        pdfDoc.finishPage(page)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanName = state.customerName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Workshop" }
        val fileName = "Boss_CuttingChart_${cleanName}_$timeStamp.pdf"

        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            file
        } catch (e: Exception) {
            pdfDoc.close()
            null
        }
    }

    fun generateLengthChartPdf(
        context: Context,
        state: BossAppState,
        sectionMap: Map<String, List<CutPiece>>
    ): File? {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.parseColor("#CBD5E1")
        }

        val logoBitmap = try {
            ChartPrintHelper.getTransparentLogoBitmap(context, 350, state.customLogoPath, state.userProfile.isVipOwner)
        } catch (e: Exception) {
            null
        }

        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        val refCode = "LEN-${System.currentTimeMillis() % 100000}"

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 48
        }

        fun drawWatermark(c: Canvas) {
            if (logoBitmap != null) {
                try {
                    val wSize = 340
                    val scaledW = Bitmap.createScaledBitmap(logoBitmap, wSize, wSize, true)
                    val left = (PAGE_WIDTH - wSize) / 2f
                    val top = (PAGE_HEIGHT - wSize) / 2f
                    c.drawBitmap(scaledW, left, top, watermarkPaint)
                } catch (e: Exception) {}
            }
        }

        fun drawHeader(c: Canvas) {
            drawWatermark(c)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#D9A441")
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, paint)

            if (logoBitmap != null) {
                try {
                    val scaled = Bitmap.createScaledBitmap(logoBitmap, 38, 38, true)
                    c.drawBitmap(scaled, MARGIN_LEFT, 18f, paint)
                } catch (e: Exception) {
                    paint.color = Color.parseColor("#D9A441")
                    c.drawCircle(MARGIN_LEFT + 19f, 37f, 18f, paint)
                }
            } else {
                paint.color = Color.parseColor("#D9A441")
                c.drawCircle(MARGIN_LEFT + 19f, 37f, 18f, paint)
            }

            boldPaint.textSize = 13.5f
            boldPaint.color = Color.parseColor("#0F172A")
            val compName2 = getShopTitle(state)
            c.drawText(compName2, MARGIN_LEFT + 46f, 30f, boldPaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#B45309")
            c.drawText("Bar Length Stock & Order Tally Report", MARGIN_LEFT + 46f, 41f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#64748B")
            val cAddr2 = state.companyAddress.ifBlank { "Main Market, Pakistan" }
            val cPhone2 = state.companyPhone.ifBlank { "+92 300 1234567" }
            val cNtn2 = state.companyNtn.ifBlank { "1234567-8" }
            val compDetails2 = buildList {
                if (cPhone2.isNotBlank()) add("Phone: $cPhone2")
                if (cAddr2.isNotBlank()) add("Factory: $cAddr2")
                if (cNtn2.isNotBlank()) add("NTN: $cNtn2")
            }.joinToString(" | ")
            c.drawText(compDetails2, MARGIN_LEFT + 46f, 51f, textPaint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FEF3C7")
            val badgeRect = RectF(440f, 18f, MARGIN_RIGHT, 38f)
            c.drawRoundRect(badgeRect, 4f, 4f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#D97706")
            c.drawRoundRect(badgeRect, 4f, 4f, paint)

            boldPaint.textSize = 9.5f
            boldPaint.color = Color.parseColor("#92400E")
            c.drawText("LENGTH CHART", 450f, 31f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#475569")
            c.drawText("Date: $currentDate", 420f, 48f, textPaint)
            c.drawText("Ref: $refCode", 420f, 58f, textPaint)

            strokePaint.color = Color.parseColor("#CBD5E1")
            c.drawLine(MARGIN_LEFT, 66f, MARGIN_RIGHT, 66f, strokePaint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            val infoBox = RectF(MARGIN_LEFT, 72f, MARGIN_RIGHT, 102f)
            c.drawRoundRect(infoBox, 4f, 4f, paint)
            strokePaint.color = Color.parseColor("#E2E8F0")
            c.drawRoundRect(infoBox, 4f, 4f, strokePaint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.parseColor("#1E293B")
            val custStr = state.customerName.ifBlank { "Standard Order" }
            val phoneStr = if (state.customerPhone.isNotBlank()) " (${state.customerPhone})" else ""
            c.drawText("Customer: $custStr$phoneStr", MARGIN_LEFT + 8f, 85f, boldPaint)

            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#475569")
            c.drawText("Spec: ${state.ctxGauge}mm | ${state.ctxColor} | Frame: ${state.ctxFrameType}", MARGIN_LEFT + 8f, 96f, textPaint)
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            strokePaint.color = Color.parseColor("#E2E8F0")
            c.drawLine(MARGIN_LEFT, 804f, MARGIN_RIGHT, 804f, strokePaint)
            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#94A3B8")
            c.drawText("Generated by Boss Aluminium & Glass App - Stock & Material Order Sheet", MARGIN_LEFT, 818f, textPaint)
            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#475569")
            c.drawText("Page $pNum", MARGIN_RIGHT - 35f, 818f, boldPaint)
        }

        drawHeader(canvas)
        var curY = 112f

        fun checkNewPage(neededSpace: Float) {
            if (curY + neededSpace > MAX_Y) {
                drawFooter(canvas, pageNumber)
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                curY = 112f
            }
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E293B")
        val thRect = RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f)
        canvas.drawRoundRect(thRect, 3f, 3f, paint)

        boldPaint.textSize = 8.5f
        boldPaint.color = Color.WHITE
        canvas.drawText("Section & Profile", MARGIN_LEFT + 8f, curY + 12f, boldPaint)
        canvas.drawText("Required Stock Lengths", MARGIN_LEFT + 180f, curY + 12f, boldPaint)
        canvas.drawText("Bars", MARGIN_RIGHT - 90f, curY + 12f, boldPaint)
        canvas.drawText("Total Feet", MARGIN_RIGHT - 45f, curY + 12f, boldPaint)
        curY += 20f

        var grandTotalBars = 0
        var grandTotalFeet = 0

        sectionMap.forEach { (section, pieces) ->
            val lens = BossCalculationEngine.barLengthsFor(section)
            val packed = BossCalculationEngine.packSection(pieces, lens)
            val label = BossDefaultRates.SECTION_LABELS[section]
            val sectionHeader = if (!label.isNullOrBlank() && !label.equals(section, ignoreCase = true)) {
                "$section - $label"
            } else {
                section
            }

            var secBars = 0
            var secFeet = 0
            val tallyStrings = packed.tally.entries.sortedByDescending { it.key }.map { (lenFt, tallyInfo) ->
                secBars += tallyInfo.count
                secFeet += lenFt * tallyInfo.count
                "${lenFt}FT x ${tallyInfo.count}${if (tallyInfo.isCustom) " (offcut)" else ""}"
            }.joinToString(", ")

            grandTotalBars += secBars
            grandTotalFeet += secFeet

            checkNewPage(18f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 17f, paint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(sectionHeader, MARGIN_LEFT + 8f, curY + 12f, boldPaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#B45309")
            canvas.drawText(tallyStrings, MARGIN_LEFT + 180f, curY + 12f, boldPaint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.parseColor("#1E293B")
            canvas.drawText(secBars.toString(), MARGIN_RIGHT - 80f, curY + 12f, boldPaint)
            canvas.drawText("${secFeet} ft", MARGIN_RIGHT - 45f, curY + 12f, boldPaint)

            strokePaint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(MARGIN_LEFT, curY + 17f, MARGIN_RIGHT, curY + 17f, strokePaint)
            curY += 18f
        }

        checkNewPage(24f)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FEF3C7")
        val totRect = RectF(MARGIN_LEFT, curY + 2f, MARGIN_RIGHT, curY + 22f)
        canvas.drawRoundRect(totRect, 4f, 4f, paint)
        strokePaint.color = Color.parseColor("#D97706")
        canvas.drawRoundRect(totRect, 4f, 4f, strokePaint)

        boldPaint.textSize = 9.5f
        boldPaint.color = Color.parseColor("#92400E")
        canvas.drawText("GRAND TOTAL MATERIAL REQUIRED:", MARGIN_LEFT + 10f, curY + 16f, boldPaint)
        canvas.drawText("$grandTotalBars BARS", MARGIN_RIGHT - 120f, curY + 16f, boldPaint)
        canvas.drawText("${grandTotalFeet} RUNNING FT", MARGIN_RIGHT - 75f, curY + 16f, boldPaint)

        curY += 30f

        sectionMap.forEach { (section, pieces) ->
            val lens = BossCalculationEngine.barLengthsFor(section)
            val packed = BossCalculationEngine.packSection(pieces, lens)
            val label = BossDefaultRates.SECTION_LABELS[section]
            val sectionHeader = if (!label.isNullOrBlank() && !label.equals(section, ignoreCase = true)) {
                "$section - $label"
            } else {
                section
            }

            checkNewPage(35f)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#334155")
            val barSec = RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 16f)
            canvas.drawRoundRect(barSec, 3f, 3f, paint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.WHITE
            canvas.drawText("Profile: $sectionHeader", MARGIN_LEFT + 8f, curY + 11f, boldPaint)
            curY += 18f

            packed.bins.forEachIndexed { bIdx, b ->
                checkNewPage(14f)
                val waste = BossCalculationEngine.toDotSutar(b.remainInches.coerceAtLeast(0.0))
                val cutsSummary = b.pieces.joinToString(", ") { "${BossCalculationEngine.toDotSutar(it.length)}\" (${it.source})" }
                textPaint.textSize = 7.5f
                textPaint.color = Color.parseColor("#1E293B")
                canvas.drawText("Bar #${bIdx + 1} (${b.lenFt}FT): $cutsSummary", MARGIN_LEFT + 12f, curY + 10f, textPaint)

                boldPaint.textSize = 7.5f
                boldPaint.color = Color.parseColor("#B45309")
                canvas.drawText("Waste: $waste\"", MARGIN_RIGHT - 70f, curY + 10f, boldPaint)
                curY += 13f
            }
            curY += 6f
        }

        drawFooter(canvas, pageNumber)
        pdfDoc.finishPage(page)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanName = state.customerName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Order" }
        val fileName = "Boss_LengthChart_${cleanName}_$timeStamp.pdf"

        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            file
        } catch (e: Exception) {
            pdfDoc.close()
            null
        }
    }

    fun generateGlassChartPdf(
        context: Context,
        state: BossAppState,
        cuts: List<GlassCutResult>,
        totalSqft: Double
    ): File? {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = Color.parseColor("#CBD5E1")
        }

        val logoBitmap = try {
            ChartPrintHelper.getTransparentLogoBitmap(context, 350, state.customLogoPath, state.userProfile.isVipOwner)
        } catch (e: Exception) {
            null
        }

        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        val refCode = "GLS-${System.currentTimeMillis() % 100000}"

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 48
        }

        fun drawWatermark(c: Canvas) {
            if (logoBitmap != null) {
                try {
                    val wSize = 340
                    val scaledW = Bitmap.createScaledBitmap(logoBitmap, wSize, wSize, true)
                    val left = (PAGE_WIDTH - wSize) / 2f
                    val top = (PAGE_HEIGHT - wSize) / 2f
                    c.drawBitmap(scaledW, left, top, watermarkPaint)
                } catch (e: Exception) {}
            }
        }

        fun drawHeader(c: Canvas) {
            drawWatermark(c)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#D9A441")
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, paint)

            if (logoBitmap != null) {
                try {
                    val scaled = Bitmap.createScaledBitmap(logoBitmap, 38, 38, true)
                    c.drawBitmap(scaled, MARGIN_LEFT, 18f, paint)
                } catch (e: Exception) {
                    paint.color = Color.parseColor("#D9A441")
                    c.drawCircle(MARGIN_LEFT + 19f, 37f, 18f, paint)
                }
            } else {
                paint.color = Color.parseColor("#D9A441")
                c.drawCircle(MARGIN_LEFT + 19f, 37f, 18f, paint)
            }

            boldPaint.textSize = 13.5f
            boldPaint.color = Color.parseColor("#0F172A")
            val compName3 = getShopTitle(state)
            c.drawText(compName3, MARGIN_LEFT + 46f, 30f, boldPaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#0F766E")
            c.drawText("Architectural Glass Cutting & Billed Area Report", MARGIN_LEFT + 46f, 41f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#64748B")
            val cAddr3 = state.companyAddress.ifBlank { "Main Market, Pakistan" }
            val cPhone3 = state.companyPhone.ifBlank { "+92 300 1234567" }
            val cNtn3 = state.companyNtn.ifBlank { "1234567-8" }
            val compDetails3 = buildList {
                if (cPhone3.isNotBlank()) add("Phone: $cPhone3")
                if (cAddr3.isNotBlank()) add("Factory: $cAddr3")
                if (cNtn3.isNotBlank()) add("NTN: $cNtn3")
            }.joinToString(" | ")
            c.drawText(compDetails3, MARGIN_LEFT + 46f, 51f, textPaint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#CCFBF1")
            val badgeRect = RectF(440f, 18f, MARGIN_RIGHT, 38f)
            c.drawRoundRect(badgeRect, 4f, 4f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#0D9488")
            c.drawRoundRect(badgeRect, 4f, 4f, paint)

            boldPaint.textSize = 9.5f
            boldPaint.color = Color.parseColor("#115E59")
            c.drawText("GLASS CHART", 450f, 31f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#475569")
            c.drawText("Date: $currentDate", 420f, 48f, textPaint)
            c.drawText("Ref: $refCode", 420f, 58f, textPaint)

            strokePaint.color = Color.parseColor("#CBD5E1")
            c.drawLine(MARGIN_LEFT, 66f, MARGIN_RIGHT, 66f, strokePaint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F8FAFC")
            val infoBox = RectF(MARGIN_LEFT, 72f, MARGIN_RIGHT, 102f)
            c.drawRoundRect(infoBox, 4f, 4f, paint)
            strokePaint.color = Color.parseColor("#E2E8F0")
            c.drawRoundRect(infoBox, 4f, 4f, strokePaint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.parseColor("#1E293B")
            val custStr = state.customerName.ifBlank { "Client Order" }
            val phoneStr = if (state.customerPhone.isNotBlank()) " (${state.customerPhone})" else ""
            c.drawText("Customer: $custStr$phoneStr", MARGIN_LEFT + 8f, 85f, boldPaint)

            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#475569")
            c.drawText("Default Glass: ${state.ctxGlassGauge}mm | ${state.ctxGlassColor}", MARGIN_LEFT + 8f, 96f, textPaint)
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            strokePaint.color = Color.parseColor("#E2E8F0")
            c.drawLine(MARGIN_LEFT, 804f, MARGIN_RIGHT, 804f, strokePaint)
            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#94A3B8")
            c.drawText("Generated by Boss Aluminium & Glass App - Glass Procurement & Cutting Sheet", MARGIN_LEFT, 818f, textPaint)
            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#475569")
            c.drawText("Page $pNum", MARGIN_RIGHT - 35f, 818f, boldPaint)
        }

        drawHeader(canvas)
        var curY = 112f

        fun checkNewPage(neededSpace: Float) {
            if (curY + neededSpace > MAX_Y) {
                drawFooter(canvas, pageNumber)
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                curY = 112f
            }
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F766E")
        val thRect = RectF(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 18f)
        canvas.drawRoundRect(thRect, 3f, 3f, paint)

        boldPaint.textSize = 8f
        boldPaint.color = Color.WHITE
        canvas.drawText("Item / Tag", MARGIN_LEFT + 8f, curY + 12f, boldPaint)
        canvas.drawText("Glass Specification", MARGIN_LEFT + 110f, curY + 12f, boldPaint)
        canvas.drawText("Actual Cut (W x H)", MARGIN_LEFT + 220f, curY + 12f, boldPaint)
        canvas.drawText("Billed (Ft)", MARGIN_LEFT + 340f, curY + 12f, boldPaint)
        canvas.drawText("Pcs", MARGIN_RIGHT - 80f, curY + 12f, boldPaint)
        canvas.drawText("Sq Ft", MARGIN_RIGHT - 45f, curY + 12f, boldPaint)
        curY += 20f

        var totalPieces = 0

        cuts.forEachIndexed { idx, c ->
            checkNewPage(18f)
            totalPieces += c.pieces
            val isAlt = idx % 2 == 1
            if (isAlt) {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F0FDFA")
                canvas.drawRect(MARGIN_LEFT, curY, MARGIN_RIGHT, curY + 17f, paint)
            }

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#134E4A")
            canvas.drawText(c.frameLabel, MARGIN_LEFT + 8f, curY + 12f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#334155")
            canvas.drawText("${c.glassGauge}mm ${c.glassColor}", MARGIN_LEFT + 110f, curY + 12f, textPaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#0F766E")
            val cutDim = "${BossCalculationEngine.toDotSutar(c.gWpanel)}\" x ${BossCalculationEngine.toDotSutar(c.gH)}\""
            canvas.drawText(cutDim, MARGIN_LEFT + 220f, curY + 12f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#475569")
            val billedDim = "${String.format(Locale.ENGLISH, "%.2f", c.billedWFt)}' x ${String.format(Locale.ENGLISH, "%.2f", c.billedHFt)}'"
            canvas.drawText(billedDim, MARGIN_LEFT + 340f, curY + 12f, textPaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(c.pieces.toString(), MARGIN_RIGHT - 75f, curY + 12f, boldPaint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.parseColor("#0F766E")
            canvas.drawText(String.format(Locale.ENGLISH, "%.2f", c.sqft), MARGIN_RIGHT - 45f, curY + 12f, boldPaint)

            strokePaint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(MARGIN_LEFT, curY + 17f, MARGIN_RIGHT, curY + 17f, strokePaint)
            curY += 18f
        }

        checkNewPage(44f)
        val totalActualSqft = cuts.sumOf { (it.gWpanel * it.gH / 144.0) * it.pieces }
        val totalGlassCost = cuts.sumOf { c ->
            val r = state.glassRates[c.glassGauge]?.get(c.glassColor) ?: if (c.glassGauge == "8") 390.0 else 230.0
            c.sqft * r
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#CCFBF1")
        val totRect = RectF(MARGIN_LEFT, curY + 4f, MARGIN_RIGHT, curY + 40f)
        canvas.drawRoundRect(totRect, 4f, 4f, paint)
        strokePaint.color = Color.parseColor("#0D9488")
        canvas.drawRoundRect(totRect, 4f, 4f, strokePaint)

        boldPaint.textSize = 9.5f
        boldPaint.color = Color.parseColor("#115E59")
        canvas.drawText("TOTAL GLASS SUMMARY (${cuts.size} Frames):", MARGIN_LEFT + 10f, curY + 18f, boldPaint)
        canvas.drawText("Pieces: $totalPieces Pcs", MARGIN_LEFT + 220f, curY + 18f, boldPaint)
        canvas.drawText("Billed: ${String.format(Locale.ENGLISH, "%.2f", totalSqft)} Sq.Ft", MARGIN_RIGHT - 135f, curY + 18f, boldPaint)

        textPaint.textSize = 8.5f
        textPaint.color = Color.parseColor("#134E4A")
        canvas.drawText("Actual Cut Area: ${String.format(Locale.ENGLISH, "%.2f", totalActualSqft)} Sq.Ft", MARGIN_LEFT + 10f, curY + 33f, textPaint)
        boldPaint.textSize = 9f
        boldPaint.color = Color.parseColor("#0F766E")
        canvas.drawText("Estimated Glass Amount: Rs. ${String.format(Locale.ENGLISH, "%,.0f", totalGlassCost)}", MARGIN_RIGHT - 200f, curY + 33f, boldPaint)

        drawFooter(canvas, pageNumber)
        pdfDoc.finishPage(page)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanName = state.customerName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Client" }
        val fileName = "Boss_GlassChart_${cleanName}_$timeStamp.pdf"

        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            file
        } catch (e: Exception) {
            pdfDoc.close()
            null
        }
    }

    fun saveAndOpenCuttingChartPdf(
        context: Context,
        state: BossAppState,
        sectionMap: Map<String, List<CutPiece>>,
        glassCuts: List<GlassCutResult>
    ) {
        val file = generateCuttingChartPdf(context, state, sectionMap, glassCuts)
        openPdfFile(context, file, "Cutting Chart")
    }

    fun saveAndOpenLengthChartPdf(
        context: Context,
        state: BossAppState,
        sectionMap: Map<String, List<CutPiece>>
    ) {
        val file = generateLengthChartPdf(context, state, sectionMap)
        openPdfFile(context, file, "Length Chart")
    }

    fun saveAndOpenGlassChartPdf(
        context: Context,
        state: BossAppState,
        cuts: List<GlassCutResult>,
        totalSqft: Double
    ) {
        val file = generateGlassChartPdf(context, state, cuts, totalSqft)
        openPdfFile(context, file, "Glass Chart")
    }

    fun shareGlassChartPdf(context: Context, state: BossAppState, cuts: List<GlassCutResult>, totalSqft: Double) {
        val file = generateGlassChartPdf(context, state, cuts, totalSqft)
        sharePdfFile(context, file, "Glass Cutting Chart - Boss Aluminium")
    }

    fun sharePdfFile(context: Context, file: File?, title: String) {
        if (file != null && file.exists() && file.length() > 0) {
            try {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    clipData = ClipData.newRawUri(title, contentUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share $title PDF"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Error creating $title PDF for sharing.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPdfFile(context: Context, file: File?, title: String) {
        if (file != null && file.exists() && file.length() > 0) {
            val sizeKb = file.length() / 1024
            Toast.makeText(context, "$title PDF Saved (${sizeKb} KB): ${file.name}", Toast.LENGTH_LONG).show()
            try {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, "Open $title PDF"))
            } catch (e: Exception) {
                Toast.makeText(context, "Saved to Documents: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Error creating $title PDF. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}

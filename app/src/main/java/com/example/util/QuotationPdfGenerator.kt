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
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.BossDefaultRates
import com.example.model.QuotationResult
import com.example.ui.BossAppState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QuotationPdfGenerator {

    fun generatePdfFile(context: Context, quote: QuotationResult, state: BossAppState): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

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
            color = Color.parseColor("#E5E7EB")
        }

        val logoBitmap = try {
            ChartPrintHelper.getTransparentLogoBitmap(context, 350, state.customLogoPath, state.userProfile.isVipOwner)
        } catch (e: Exception) {
            null
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val currentDate = dateFormat.format(Date())
        val refCode = (System.currentTimeMillis() % 100000).toString().padStart(5, '0')

        val totalItems = quote.aluminiumRows.size + quote.glassLines.size
        val needsTwoPages = totalItems > 14
        val totalPages = if (needsTwoPages) 2 else 1

        var currentPageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(c: Canvas, pageNum: Int) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#D9A441")
            c.drawRect(0f, 0f, pageWidth.toFloat(), 5f, paint)

            if (logoBitmap != null) {
                try {
                    val scale = Math.min(44f / logoBitmap.width, 42f / logoBitmap.height)
                    val w = (logoBitmap.width * scale).toInt().coerceAtLeast(1)
                    val h = (logoBitmap.height * scale).toInt().coerceAtLeast(1)
                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, w, h, true)
                    c.drawBitmap(scaledLogo, 36f, 24f, paint)
                } catch (e: Exception) {
                    paint.color = Color.parseColor("#D9A441")
                    c.drawCircle(57f, 45f, 21f, paint)
                }
            } else {
                paint.color = Color.parseColor("#D9A441")
                c.drawCircle(57f, 45f, 21f, paint)
                boldPaint.textSize = 14f
                boldPaint.color = Color.parseColor("#120E05")
                c.drawText("B", 52f, 50f, boldPaint)
            }

            boldPaint.textSize = 14.5f
            boldPaint.color = Color.parseColor("#111827")
            val isOwner = state.userProfile.isVipOwner
            val compName = if (isOwner) state.companyName.ifBlank { "BOSS ALUMINIUM & GLASS" } else {
                if (state.companyName.isNotBlank() && state.companyName != "BOSS ALUMINIUM & GLASS") state.companyName
                else state.userProfile.shopAddress.ifBlank { state.userProfile.name.ifBlank { "YOUR SHOP / BUSINESS NAME" } }
            }
            c.drawText(compName, 88f, 38f, boldPaint)

            boldPaint.textSize = 8.5f
            boldPaint.color = Color.parseColor("#B45309")
            val compTagline = if (isOwner) state.companyTagline.ifBlank { "Premium Architectural & Commercial Glazing Solutions" } else state.companyTagline.ifBlank { "Aluminium & Glass Fabrication Works" }
            c.drawText(compTagline, 88f, 49f, boldPaint)

            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#4B5563")
            val compAddr = if (isOwner) state.companyAddress.ifBlank { "Main Market, Pakistan" } else state.companyAddress.ifBlank { state.userProfile.shopAddress.ifBlank { "Shop Address" } }
            val compPhone = if (isOwner) state.companyPhone.ifBlank { "+92 310 5524225" } else state.companyPhone.ifBlank { state.userProfile.phone.ifBlank { "+92 3XX XXXXXXX" } }
            val compNtn = state.companyNtn.ifBlank { if (isOwner) "1234567-8" else "" }
            val compDetails = buildList {
                if (compPhone.isNotBlank()) add("Phone: $compPhone")
                if (compAddr.isNotBlank()) add("Factory: $compAddr")
                if (compNtn.isNotBlank()) add("NTN: $compNtn")
            }.joinToString(" | ")
            c.drawText(compDetails, 88f, 60f, textPaint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FEF3C7")
            val badgeRect = RectF(445f, 22f, 559f, 42f)
            c.drawRoundRect(badgeRect, 4f, 4f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#D97706")
            c.drawRoundRect(badgeRect, 4f, 4f, paint)

            boldPaint.textSize = 10.5f
            boldPaint.color = Color.parseColor("#92400E")
            c.drawText("QUOTATION", 468f, 36f, boldPaint)

            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#374151")
            c.drawText("Date: $currentDate", 450f, 52f, textPaint)
            textPaint.color = Color.parseColor("#6B7280")
            c.drawText("Quote #: BOSS-$refCode", 450f, 63f, textPaint)

            strokePaint.color = Color.parseColor("#E5E7EB")
            c.drawLine(36f, 72f, 559f, 72f, strokePaint)
        }

        fun drawFooter(c: Canvas, pageNum: Int) {
            strokePaint.color = Color.parseColor("#E5E7EB")
            c.drawLine(36f, 804f, 559f, 804f, strokePaint)
            textPaint.textSize = 7.5f
            textPaint.color = Color.parseColor("#9CA3AF")
            c.drawText("Generated by Boss Aluminium & Glass App - Valid for 15 Days", 36f, 818f, textPaint)
            c.drawText("Page $pageNum of $totalPages", 505f, 818f, textPaint)
        }

        val bgBitmap = try {
            if (state.letterheadMode) ChartPrintHelper.getQuotationBackgroundBitmap(context, state.customBackgroundPath) else null
        } catch (e: Exception) {
            null
        }

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 38
        }

        fun drawWatermark(c: Canvas) {
            if (bgBitmap != null) {
                try {
                    val scaledBg = Bitmap.createScaledBitmap(bgBitmap, pageWidth, pageHeight, true)
                    c.drawBitmap(scaledBg, 0f, 0f, null)
                } catch (e: Exception) {}
            }
            if (logoBitmap != null) {
                try {
                    val maxW = 280f
                    val scale = Math.min(maxW / logoBitmap.width, maxW / logoBitmap.height)
                    val w = (logoBitmap.width * scale).toInt().coerceAtLeast(1)
                    val h = (logoBitmap.height * scale).toInt().coerceAtLeast(1)
                    val scaledW = Bitmap.createScaledBitmap(logoBitmap, w, h, true)
                    val left = (pageWidth - w) / 2f
                    val top = (pageHeight - h) / 2f
                    c.drawBitmap(scaledW, left, top, watermarkPaint)
                } catch (e: Exception) {}
            }
        }

        drawWatermark(canvas)
        drawHeader(canvas, 1)

        val custRect = RectF(36f, 76f, 559f, 108f)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1ECE1")
        canvas.drawRoundRect(custRect, 5f, 5f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.parseColor("#C89D28")
        canvas.drawRoundRect(custRect, 5f, 5f, paint)

        boldPaint.textSize = 10.5f
        boldPaint.color = Color.parseColor("#0F172A")
        val clientName = state.customerName.ifBlank { "Valued Client" }
        canvas.drawText("Customer: $clientName", 46f, 90f, boldPaint)

        textPaint.textSize = 9f
        textPaint.color = Color.parseColor("#334155")
        val phoneStr = if (state.customerPhone.isNotBlank()) "Phone: ${state.customerPhone}" else "Phone: N/A"
        canvas.drawText(phoneStr, 46f, 102f, textPaint)

        boldPaint.textSize = 9.5f
        boldPaint.color = Color.parseColor("#92400E")
        val colorGaugeStr = "Color: ${state.ctxColor} | Gauge: ${state.ctxGauge}mm"
        canvas.drawText(colorGaugeStr, 330f, 90f, boldPaint)

        textPaint.textSize = 9f
        textPaint.color = Color.parseColor("#334155")
        val frameStr = if (state.ctxFrameType == "F") "Frame: DC30F / DC26F (Full)" else "Frame: DC30C / DC26C"
        canvas.drawText(frameStr, 330f, 102f, textPaint)

        var curY = 114f

        fun drawTableHeader(c: Canvas) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0F172A")
            c.drawRoundRect(RectF(36f, curY, 559f, curY + 16f), 3f, 3f, paint)

            boldPaint.textSize = 9f
            boldPaint.color = Color.parseColor("#F5B82E")
            c.drawText("S#", 42f, curY + 11.5f, boldPaint)
            c.drawText("SECTION / ITEM DESCRIPTION", 68f, curY + 11.5f, boldPaint)
            c.drawText("GAUGE", 265f, curY + 11.5f, boldPaint)
            c.drawText("BARS", 320f, curY + 11.5f, boldPaint)
            c.drawText("FEET", 370f, curY + 11.5f, boldPaint)
            c.drawText("RATE/FT", 425f, curY + 11.5f, boldPaint)
            c.drawText("AMOUNT (RS)", 488f, curY + 11.5f, boldPaint)

            curY += 17f
        }

        drawTableHeader(canvas)

        val rowHeight = 14.5f
        val alumRows = quote.aluminiumRows

        for (i in alumRows.indices) {
            val r = alumRows[i]
            if (curY > 740f && currentPageNum == 1 && needsTwoPages) {
                drawFooter(canvas, currentPageNum)
                pdfDocument.finishPage(page)
                currentPageNum = 2
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawWatermark(canvas)
                drawHeader(canvas, currentPageNum)
                curY = 82f
                drawTableHeader(canvas)
            }

            strokePaint.color = Color.parseColor("#E5E7EB")
            strokePaint.strokeWidth = 0.5f
            canvas.drawLine(36f, curY + rowHeight, 559f, curY + rowHeight, strokePaint)

            val sectionLabel = BossDefaultRates.SECTION_LABELS[r.section]
            val displaySec = if (!sectionLabel.isNullOrBlank() && !sectionLabel.equals(r.section, ignoreCase = true)) {
                "${r.section} - $sectionLabel"
            } else {
                r.section
            }
            textPaint.textSize = 9.5f
            textPaint.color = Color.parseColor("#1F2937")
            canvas.drawText("${i + 1}", 42f, curY + 12.5f, textPaint)
            boldPaint.textSize = 9.5f
            boldPaint.color = Color.parseColor("#111827")
            canvas.drawText(displaySec, 68f, curY + 12.5f, boldPaint)
            canvas.drawText("${r.gauge}m", 265f, curY + 12.5f, textPaint)
            canvas.drawText("${r.bars}", 325f, curY + 12.5f, textPaint)
            canvas.drawText("${r.totalFt}", 375f, curY + 12.5f, textPaint)
            canvas.drawText("Rs ${r.ratePerFt.toInt()}", 425f, curY + 12.5f, textPaint)

            boldPaint.textSize = 10f
            boldPaint.color = Color.parseColor("#111827")
            canvas.drawText("Rs ${r.cost.toInt()}", 488f, curY + 12.5f, boldPaint)

            curY += rowHeight
        }

        curY += 3f
        strokePaint.color = Color.parseColor("#9CA3AF")
        strokePaint.strokeWidth = 0.8f
        canvas.drawLine(36f, curY, 559f, curY, strokePaint)
        canvas.drawLine(36f, curY + 18f, 559f, curY + 18f, strokePaint)

        boldPaint.textSize = 10f
        boldPaint.color = Color.parseColor("#111827")
        canvas.drawText("Aluminium Gross Subtotal:", 180f, curY + 13f, boldPaint)
        canvas.drawText("Rs. ${quote.alumTotal.toInt()}", 488f, curY + 13f, boldPaint)
        curY += 20f

        if (state.remoteLayoutConfig.showDiscountRow && quote.discountPct > 0.0) {
            boldPaint.textSize = 9.5f
            boldPaint.color = Color.parseColor("#047857")
            canvas.drawText("Aluminium Discount (-${quote.discountPct}%):", 180f, curY + 12f, boldPaint)
            canvas.drawText("- Rs. ${quote.discountAmt.toInt()}", 485f, curY + 12f, boldPaint)
            curY += 16f

            strokePaint.color = Color.parseColor("#D97706")
            strokePaint.strokeWidth = 0.8f
            canvas.drawLine(36f, curY, 559f, curY, strokePaint)
            canvas.drawLine(36f, curY + 16f, 559f, curY + 16f, strokePaint)

            boldPaint.textSize = 10f
            boldPaint.color = Color.parseColor("#92400E")
            canvas.drawText("Net Aluminium Total:", 180f, curY + 12f, boldPaint)
            canvas.drawText("Rs. ${quote.netAlumTotal.toInt()}", 488f, curY + 12f, boldPaint)
            curY += 20f
        }

        if (quote.markupPct > 0) {
            strokePaint.color = Color.parseColor("#D97706")
            strokePaint.strokeWidth = 0.8f
            canvas.drawLine(36f, curY, 559f, curY, strokePaint)
            canvas.drawLine(36f, curY + 16f, 559f, curY + 16f, strokePaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#92400E")
            canvas.drawText("Fabrication Markup (+${quote.markupPct}%):", 180f, curY + 11f, boldPaint)
            canvas.drawText("Rs. ${quote.alumWithMarkup.toInt()}", 495f, curY + 11f, boldPaint)
            curY += 20f
        }

        if (quote.glassLines.isNotEmpty()) {
            curY += 6f
            strokePaint.color = Color.parseColor("#374151")
            strokePaint.strokeWidth = 1f
            canvas.drawLine(36f, curY, 559f, curY, strokePaint)
            canvas.drawLine(36f, curY + 16f, 559f, curY + 16f, strokePaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#1F2937")
            canvas.drawText("GLASS SPECIFICATIONS & GLAZING", 42f, curY + 11f, boldPaint)
            canvas.drawText("SQ.FT", 340f, curY + 11f, boldPaint)
            canvas.drawText("RATE", 430f, curY + 11f, boldPaint)
            canvas.drawText("AMOUNT (RS)", 495f, curY + 11f, boldPaint)
            curY += 16f

            quote.glassLines.forEach { gl ->
                strokePaint.color = Color.parseColor("#E5E7EB")
                strokePaint.strokeWidth = 0.5f
                canvas.drawLine(36f, curY + 15f, 559f, curY + 15f, strokePaint)

                textPaint.textSize = 7.8f
                textPaint.color = Color.parseColor("#374151")
                canvas.drawText(gl.description, 42f, curY + 10.5f, textPaint)
                canvas.drawText("${gl.sqft.toInt()} sft", 340f, curY + 10.5f, textPaint)
                canvas.drawText("Rs ${gl.rate.toInt()}", 430f, curY + 10.5f, textPaint)

                boldPaint.textSize = 8f
                boldPaint.color = Color.parseColor("#111827")
                canvas.drawText("Rs ${gl.total.toInt()}", 495f, curY + 10.5f, boldPaint)

                curY += 15f
            }
            curY += 3f
            strokePaint.color = Color.parseColor("#9CA3AF")
            strokePaint.strokeWidth = 0.8f
            canvas.drawLine(36f, curY, 559f, curY, strokePaint)
            canvas.drawLine(36f, curY + 16f, 559f, curY + 16f, strokePaint)

            boldPaint.textSize = 8f
            boldPaint.color = Color.parseColor("#111827")
            canvas.drawText("Glass Subtotal:", 240f, curY + 11f, boldPaint)
            canvas.drawText("Rs. ${quote.glassTotal.toInt()}", 495f, curY + 11f, boldPaint)
            curY += 20f
        }

        curY += 6f
        val summaryRect = RectF(36f, curY, 559f, curY + 70f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.parseColor("#D1D5DB")
        canvas.drawRoundRect(summaryRect, 6f, 6f, paint)

        textPaint.textSize = 9.5f
        textPaint.color = Color.parseColor("#1F2937")
        var summaryRowOffset = 18f
        if (state.remoteLayoutConfig.showHardwareRow) {
            val hwLabel = if (quote.totalUnits > 0) "Hardware (${quote.totalUnits} Units × Rs ${state.hardwareRatePerUnit.toInt()}):" else "Hardware & Accessories:"
            canvas.drawText(hwLabel, 48f, curY + summaryRowOffset, textPaint)
            canvas.drawText("Rs. ${quote.hardwareTotal.toInt()}", 205f, curY + summaryRowOffset, boldPaint)
            summaryRowOffset += 17f
        }
        if (state.remoteLayoutConfig.showLabourRow) {
            canvas.drawText("Labour & Fabrication:", 48f, curY + summaryRowOffset, textPaint)
            canvas.drawText("Rs. ${quote.labourCost.toInt()}", 205f, curY + summaryRowOffset, boldPaint)
            summaryRowOffset += 17f
        }
        if (state.remoteLayoutConfig.showTransportRow) {
            canvas.drawText("Transport & Rent:", 48f, curY + summaryRowOffset, textPaint)
            canvas.drawText("Rs. ${quote.transportRent.toInt()}", 205f, curY + summaryRowOffset, boldPaint)
        }

        val grandTotalRect = RectF(310f, curY + 10f, 545f, curY + 58f)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(28, 254, 243, 199)
        canvas.drawRoundRect(grandTotalRect, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = Color.parseColor("#D97706")
        canvas.drawRoundRect(grandTotalRect, 6f, 6f, paint)

        boldPaint.textSize = 10f
        boldPaint.color = Color.parseColor("#92400E")
        canvas.drawText("NET ESTIMATED TOTAL", 325f, curY + 28f, boldPaint)

        boldPaint.textSize = 16f
        boldPaint.color = Color.parseColor("#78350F")
        canvas.drawText("Rs. ${quote.grandTotal.toInt()}", 325f, curY + 48f, boldPaint)

        curY += 86f

        val sigY = curY.coerceAtMost(740f)
        strokePaint.color = Color.parseColor("#9CA3AF")
        strokePaint.strokeWidth = 0.8f
        canvas.drawLine(48f, sigY + 28f, 190f, sigY + 28f, strokePaint)
        textPaint.textSize = 7.5f
        textPaint.color = Color.parseColor("#6B7280")
        canvas.drawText("Authorized Signature", 68f, sigY + 38f, textPaint)

        canvas.drawLine(405f, sigY + 28f, 545f, sigY + 28f, strokePaint)
        canvas.drawText("Customer Acceptance", 425f, sigY + 38f, textPaint)

        drawFooter(canvas, currentPageNum)
        pdfDocument.finishPage(page)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeName = state.customerName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Client" }
        val fileName = "Boss_Quotation_${safeName}_$timeStamp.pdf"

        return try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    fun saveAndOpenPdf(context: Context, quote: QuotationResult, state: BossAppState) {
        val file = generatePdfFile(context, quote, state)
        if (file != null && file.exists() && file.length() > 0) {
            val fileSizeKb = file.length() / 1024
            Toast.makeText(context, "Quotation PDF Saved (${fileSizeKb} KB): ${file.name}", Toast.LENGTH_LONG).show()
            try {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, "Open Quotation PDF"))
            } catch (e: Exception) {
                Toast.makeText(context, "Saved to Documents: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Error saving PDF. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, quote: QuotationResult, state: BossAppState) {
        val file = generatePdfFile(context, quote, state)
        if (file != null && file.exists() && file.length() > 0) {
            try {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Quotation: ${state.customerName.ifBlank { "Boss Aluminium & Glass" }}")
                    clipData = ClipData.newRawUri("Quotation PDF", contentUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Quotation PDF"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Could not generate PDF for sharing.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareImage(context: Context, quote: QuotationResult, state: BossAppState) {
        val file = generateQuotationImageFile(context, quote, state)
        if (file != null && file.exists() && file.length() > 0) {
            try {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Quotation: ${state.customerName.ifBlank { "Boss Aluminium & Glass" }}")
                    clipData = ClipData.newRawUri("Quotation Image", contentUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Quotation Picture"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing Picture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Could not generate image for sharing.", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateQuotationImageFile(context: Context, quote: QuotationResult, state: BossAppState): File? {
        val pdfFile = generatePdfFile(context, quote, state) ?: return null
        val cleanName = state.customerName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Client" }
        return renderPdfToImageFile(context, pdfFile, "Quotation_$cleanName")
    }

    fun renderPdfToImageFile(context: Context, pdfFile: File, baseName: String): File? {
        return try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                renderer.close()
                pfd.close()
                return null
            }

            val scale = 2f
            val bitmaps = mutableListOf<Bitmap>()
            var totalHeight = 0
            var maxWidth = 0

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()
                val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bm)
                canvas.drawColor(Color.WHITE)
                page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bm)
                totalHeight += h
                if (w > maxWidth) maxWidth = w
            }
            renderer.close()
            pfd.close()

            val combined = if (bitmaps.size == 1) {
                bitmaps[0]
            } else {
                val stitched = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(stitched)
                canvas.drawColor(Color.WHITE)
                var currentY = 0f
                for (bm in bitmaps) {
                    canvas.drawBitmap(bm, 0f, currentY, null)
                    currentY += bm.height
                }
                stitched
            }

            val cleanName = baseName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val imageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
            if (!imageDir.exists()) imageDir.mkdirs()
            val imageFile = File(imageDir, "${cleanName}_${System.currentTimeMillis() % 10000}.jpg")
            FileOutputStream(imageFile).use { out ->
                combined.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }
            imageFile
        } catch (e: Exception) {
            null
        }
    }
}

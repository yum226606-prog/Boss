package com.example.model

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong

object BossCalculationEngine {
    const val ELEVEN_FT_IN = 132.0 // 11 ft
    const val BAR_EXTRA_IN = 1.0 // 1 inch extra stock margin

    fun toDotSutar(decIn: Double): String {
        val neg = decIn < 0
        val v = abs(decIn)
        val sutar = (v * 8.0).roundToLong()
        val inch = sutar / 8
        val rem = sutar % 8
        val s = "$inch.$rem"
        return if (neg) "-$s" else s
    }

    fun parseInSutar(raw: String): Double {
        val str = raw.trim()
        if (str.isEmpty()) return 0.0
        val sign = if (str.startsWith("-")) -1.0 else 1.0
        val body = str.removePrefix("-").removePrefix("+")
        if (body.matches(Regex("""^\d+(?:\.\d+)?$"""))) {
            val parts = body.split(".")
            val inch = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val sutRaw = parts.getOrNull(1)?.take(2) ?: "0"
            val sutar = sutRaw.toDoubleOrNull() ?: 0.0
            return sign * (inch + sutar / 8.0)
        }
        return str.toDoubleOrNull() ?: 0.0
    }

    fun billFeet(decIn: Double): Double {
        if (decIn.isNaN() || decIn <= 0.0) return 0.0
        val eighths = (decIn * 8.0).roundToLong()
        val rem = eighths % 48
        val rounded = if (rem == 0L) eighths else (eighths - rem + 48)
        return rounded / 96.0
    }

    fun barLengthsFor(section: String): List<Int> {
        val u = section.uppercase()
        val isF = Regex("""(?:^|[^A-Z0-9])(?:M26F|M30F|DC26F|DC30F|D42F|D51F|D54F|E60F|EC26F|EC30F|DG26F|CW1F)""").containsMatchIn(u)
                || u.contains("M26F") || u.contains("M30F") || u.contains("DC26F") || u.contains("DC30F")
        if (isF) return listOf(15, 17, 19)
        if (u.startsWith("ANGLE") || u.startsWith("PIPE") || u.startsWith("U") || u.startsWith("PATTI") || u.startsWith("LOOM") || u.startsWith("OVALTUBE")) {
            return listOf(12)
        }
        return listOf(14, 16, 18)
    }

    fun pricingGaugeForSection(section: String, fallbackGauge: String): String {
        val k = section.uppercase().replace(Regex("""\s+"""), "")
        if (k == "D29" || k == "D41" || k == "D31") return "1.2"
        return fallbackGauge.ifBlank { "1.2" }
    }

    fun formatDim(inches: Double, unit: String = "INCH_SUTAR"): String {
        if (inches.isNaN() || inches.isInfinite()) return "0"
        return when (unit) {
            "CM" -> String.format(java.util.Locale.US, "%.1f cm", inches * 2.54)
            "MM" -> String.format(java.util.Locale.US, "%.0f mm", inches * 25.4)
            else -> "${toDotSutar(inches)}\""
        }
    }

    fun formatDimValueOnly(inches: Double, unit: String = "INCH_SUTAR"): String {
        if (inches.isNaN() || inches.isInfinite() || inches <= 0.0) return ""
        return when (unit) {
            "CM" -> String.format(java.util.Locale.US, "%.1f", inches * 2.54)
            "MM" -> String.format(java.util.Locale.US, "%.0f", inches * 25.4)
            else -> toDotSutar(inches)
        }
    }

    fun parseDimToInches(raw: String, unit: String = "INCH_SUTAR"): Double {
        val str = raw.trim()
        if (str.isEmpty()) return 0.0
        return when (unit) {
            "CM" -> {
                val v = str.replace("cm", "").trim().toDoubleOrNull() ?: 0.0
                v / 2.54
            }
            "MM" -> {
                val v = str.replace("mm", "").trim().toDoubleOrNull() ?: 0.0
                v / 25.4
            }
            else -> parseInSutar(raw)
        }
    }

    fun unitLabel(unit: String = "INCH_SUTAR"): String {
        return when (unit) {
            "CM" -> "cm"
            "MM" -> "mm"
            else -> "in.sut"
        }
    }

    fun packSection(pieceList: List<CutPiece>, lengthsFt: List<Int>): PackedResult {
        val pool = mutableListOf<CutPiece>()
        pieceList.forEach { p ->
            for (i in 0 until p.qty) {
                pool.add(p.copy(qty = 1))
            }
        }
        pool.sortByDescending { it.length }

        val lens = lengthsFt.sorted()
        val maxIn = if (lens.isNotEmpty()) (lens.maxOrNull() ?: 0) * 12.0 + BAR_EXTRA_IN else 0.0

        val bins = mutableListOf<PackedBar>()
        val unfit = mutableListOf<CutPiece>()

        while (pool.isNotEmpty()) {
            val item = pool.removeAt(0)
            val len = item.length

            if (len > maxIn + 1e-6) {
                unfit.add(item)
                continue
            }

            var bestIdx = -1
            var bestRemain = Double.MAX_VALUE
            for (i in bins.indices) {
                val b = bins[i]
                if (b.remainInches >= len - 1e-6 && (b.remainInches - len) < bestRemain) {
                    bestIdx = i
                    bestRemain = b.remainInches - len
                }
            }

            if (bestIdx >= 0) {
                val b = bins[bestIdx]
                bins[bestIdx] = b.copy(
                    remainInches = b.remainInches - len,
                    pieces = b.pieces + item
                )
                continue
            }

            var bestSl = lens.firstOrNull() ?: 14
            var bestUtil = -1.0
            val bestFit = mutableListOf<CutPiece>()

            for (L in lens) {
                val cap = L * 12.0 + BAR_EXTRA_IN
                if (cap < len - 1e-6) continue
                var rem = cap - len
                var used = len
                val fit = mutableListOf<CutPiece>()

                for (p in pool) {
                    if (p.length <= rem + 1e-6) {
                        rem -= p.length
                        used += p.length
                        fit.add(p)
                    }
                }
                val util = used / cap
                if (util > bestUtil + 1e-9 || (abs(util - bestUtil) <= 1e-9 && fit.size > bestFit.size)) {
                    bestUtil = util
                    bestSl = L
                    bestFit.clear()
                    bestFit.addAll(fit)
                }
            }

            val piecesInBin = mutableListOf(item)
            var rem = bestSl * 12.0 + BAR_EXTRA_IN - len
            bestFit.forEach { p ->
                rem -= p.length
                piecesInBin.add(p)
                pool.remove(p)
            }
            bins.add(PackedBar(lenFt = bestSl, isCustom = false, remainInches = rem, pieces = piecesInBin))
        }

        val finalBins = bins.map { b ->
            val content = b.pieces.sumOf { it.length }
            if (content <= ELEVEN_FT_IN) {
                val customLenFt = ceil(content / 12.0).toInt().coerceAtLeast(1)
                b.copy(
                    lenFt = customLenFt,
                    isCustom = true,
                    remainInches = customLenFt * 12.0 - content
                )
            } else {
                b
            }
        }

        val tally = mutableMapOf<Int, TallyInfo>()
        finalBins.forEach { b ->
            val cur = tally[b.lenFt]
            if (cur == null) {
                tally[b.lenFt] = TallyInfo(count = 1, isCustom = b.isCustom)
            } else {
                tally[b.lenFt] = cur.copy(count = cur.count + 1, isCustom = cur.isCustom || b.isCustom)
            }
        }

        return PackedResult(tally = tally, unfit = unfit, bins = finalBins)
    }

    fun resolveDoorFixOuterSection(itemOuter: String, ctxFrameType: String?): String {
        val clean = itemOuter.trim().uppercase()
        if (ctxFrameType.equals("F", ignoreCase = true)) {
            return when (clean) {
                "D54A", "D54", "D54F" -> "D54F"
                "D51A", "D51", "D51F" -> "D51F"
                "D45A", "D45", "D45F" -> "D45F"
                "D42", "D42F" -> "D42F"
                else -> if (clean.endsWith("F")) clean else "${clean}F"
            }
        }
        return clean
    }

    fun normalizeDoorFixBaseOuter(selectedOuter: String, currentSavedBase: String = "D54A"): String {
        val clean = selectedOuter.trim().uppercase()
        return when (clean) {
            "D54F" -> if (currentSavedBase.uppercase() == "D54") "D54" else "D54A"
            "D51F" -> if (currentSavedBase.uppercase() == "D51") "D51" else "D51A"
            "D45F" -> if (currentSavedBase.uppercase() == "D45") "D45" else "D45A"
            "D42F" -> "D42"
            else -> clean
        }
    }

    fun computeSlidingWindowPieces(
        w: WindowItem,
        overrideFrameType: String? = null,
        formula: WindowFormulaSettings = WindowFormulaSettings(),
        fFormula: FSectionFormula = FSectionFormula(),
        itemNumber: Int = 1
    ): List<CutPiece> {
        val ft = (overrideFrameType ?: (if (w.frameType.isNotBlank()) w.frameType else "C")).trim()
        val isCustom = ft != "C" && ft != "F" && ft.isNotBlank()
        val isF = if (isCustom) ft.uppercase().endsWith("F") else ft.uppercase() == "F"
        val c = if (isF) "F" else "C"
        val h = w.height
        val width = w.width
        val q = w.qty.coerceAtLeast(1)
        val pieces = mutableListOf<CutPiece>()
        val src = "Window #$itemNumber"
        val g = w.sectionGauge

        val isPair = ft.contains("/")
        val topSec = when {
            isPair -> ft.split("/")[0].trim()
            isCustom -> ft
            else -> "DC30$c"
        }
        val botSec = when {
            isPair -> {
                val parts = ft.split("/")
                if (parts.size > 1 && parts[1].trim().isNotBlank()) parts[1].trim() else parts[0].trim()
            }
            isCustom -> ft
            else -> "DC26$c"
        }

        if (isF && fFormula.enabled) {
            val hLen = fFormula.calculateHeight(h)
            val hQty = fFormula.qtyH
            pieces.add(CutPiece(topSec, hLen, hQty * q, src, "Frame side F (Height)", gauge = g))

            val wLen = fFormula.calculateWidth(width)
            val wTopQty = (fFormula.qtyW / 2).coerceAtLeast(1)
            val wBotQty = (fFormula.qtyW - wTopQty).coerceAtLeast(1)
            pieces.add(CutPiece(topSec, wLen, wTopQty * q, src, "Frame top F (Width)", gauge = g))
            pieces.add(CutPiece(botSec, wLen, wBotQty * q, src, "Frame bottom F (Width)", gauge = g))
        } else {
            pieces.add(CutPiece(topSec, h, 2 * q, src, "Frame side (Height)", gauge = g))
            pieces.add(CutPiece(topSec, width, 1 * q, src, "Frame top (Width)", gauge = g))
            pieces.add(CutPiece(botSec, width, 1 * q, src, "Frame bottom (Width)", gauge = g))
        }

        val panels = w.panelCount.coerceIn(2, 4)

        val sashH = max(0.0, h - formula.m23HeightDeduction.totalInches)
        val m24Deduction = when (panels) {
            3 -> formula.m24WidthDeduction3.totalInches
            4 -> formula.m24WidthDeduction4.totalInches
            else -> formula.m24WidthDeduction.totalInches
        }
        val sashW = max(0.0, (width - m24Deduction) / panels.toDouble())

        val m23Apply = formula.m23ApplyTo
        val m23Pcs = when (panels) {
            3 -> if (formula.m23Qty3 > 0) formula.m23Qty3 else 2
            4 -> if (formula.m23Qty4 > 0) formula.m23Qty4 else (if (formula.m28Qty > 0) 4 else 8)
            else -> formula.m23Qty.coerceAtLeast(0)
        }
        if (m23Pcs > 0) {
            if (m23Apply == "H" || m23Apply == "both") {
                pieces.add(CutPiece("M23", sashH, m23Pcs * q, src, "Sash height (M23)", gauge = g))
            }
            if (m23Apply == "W" || m23Apply == "both") {
                pieces.add(CutPiece("M23", sashW, m23Pcs * q, src, "Sash width (M23)", gauge = g))
            }
        }

        val m28Apply = formula.m28ApplyTo
        val m28Pcs = when (panels) {
            3 -> if (formula.m28Qty3 > 0) formula.m28Qty3 else 4
            4 -> if (formula.m28Qty4 > 0) formula.m28Qty4 else (if (formula.m28Qty > 0) 4 else 0)
            else -> formula.m28Qty.coerceAtLeast(0)
        }
        if (m28Pcs > 0) {
            if (m28Apply == "H" || m28Apply == "both") {
                pieces.add(CutPiece("M28", sashH, m28Pcs * q, src, "Sash height (M28)", gauge = g))
            }
            if (m28Apply == "W" || m28Apply == "both") {
                pieces.add(CutPiece("M28", sashW, m28Pcs * q, src, "Sash width (M28)", gauge = g))
            }
        }

        val m24Pcs = w.m24Qty.coerceAtLeast(0)
        val m24Apply = formula.m24ApplyTo

        if (panels == 3) {
            val sashW_center = max(0.0, (width - formula.m24WidthCenterDeduction3.totalInches) / 2.0)
            val sashW_sides = max(0.0, (width - formula.m24WidthSidesDeduction3.totalInches) / 4.0)

            if (m24Pcs > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    val centerPcs = if (m24Pcs == 6) 2 else (m24Pcs / 3).coerceAtLeast(1)
                    val sidesPcs = if (m24Pcs == 6) 4 else max(0, m24Pcs - centerPcs)
                    if (centerPcs > 0) {
                        pieces.add(CutPiece("M24", sashW_center, centerPcs * q, src, "M24 Center width", gauge = g))
                    }
                    if (sidesPcs > 0) {
                        pieces.add(CutPiece("M24", sashW_sides, sidesPcs * q, src, "M24 Sides width", gauge = g))
                    }
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    val hPcs = if (m24Pcs <= 6) 6 else m24Pcs
                    pieces.add(CutPiece("M24", sashH, hPcs * q, src, "Sash height (M24)", gauge = g))
                }
            }
            if (w.m24aQty > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    val centerPcsA = if (w.m24aQty == 6) 2 else (w.m24aQty / 3).coerceAtLeast(1)
                    val sidesPcsA = if (w.m24aQty == 6) 4 else max(0, w.m24aQty - centerPcsA)
                    if (centerPcsA > 0) {
                        pieces.add(CutPiece("M24A", sashW_center, centerPcsA * q, src, "M24A Center width", gauge = g))
                    }
                    if (sidesPcsA > 0) {
                        pieces.add(CutPiece("M24A", sashW_sides, sidesPcsA * q, src, "M24A Sides width", gauge = g))
                    }
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    val hPcsA = if (w.m24aQty <= 6) 6 else w.m24aQty
                    pieces.add(CutPiece("M24A", sashH, hPcsA * q, src, "Sash height (M24A)", gauge = g))
                }
            }
        } else {
            val sashW_eff = sashW
            val wLabel = "M24 width"
            if (m24Pcs > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    pieces.add(CutPiece("M24", sashW_eff, m24Pcs * q, src, wLabel, gauge = g))
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    pieces.add(CutPiece("M24", sashH, m24Pcs * q, src, "Sash height (M24)", gauge = g))
                }
            }
            if (w.m24aQty > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    pieces.add(CutPiece("M24A", sashW_eff, w.m24aQty * q, src, wLabel.replace("M24", "M24A"), gauge = g))
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    pieces.add(CutPiece("M24A", sashH, w.m24aQty * q, src, "Sash height (M24A)", gauge = g))
                }
            }
        }

        val d29H = max(0.0, h - formula.getD29HeightDeduction(panels).totalInches)
        val jaliH = if (formula.getD29HeightDeduction(panels).totalInches > 0.0) d29H else sashH
        val m24ForJali = when (panels) {
            3 -> max(0.0, (width - formula.m24WidthSidesDeduction3.totalInches) / 4.0)
            4 -> max(0.0, (width - formula.m24WidthDeduction4.totalInches) / 4.0)
            else -> max(0.0, (width - formula.m24WidthDeduction.totalInches) / 2.0)
        }
        val d29ExtraW = formula.getD29WidthOverlap(panels).totalInches
        var jaliW = max(0.0, m24ForJali + d29ExtraW)

        if (w.lockType.equals("PUSH_HANDLE", ignoreCase = true) || w.lockType.equals("PUSH", ignoreCase = true)) {
            val phDed = formula.d29PushHandleDeduction.totalInches
            jaliW = max(0.0, jaliW - phDed)
        }

        val d29HPcs = w.d29HeightQty.coerceAtLeast(0)
        val d29WPcs = w.d29WidthQty.coerceAtLeast(0)

        val jaliWLabel = if (w.lockType.equals("PUSH_HANDLE", ignoreCase = true) || w.lockType.equals("PUSH", ignoreCase = true)) {
            "Jali width (D29) [Push Handle]"
        } else {
            "Jali width (D29)"
        }

        if (d29HPcs > 0) {
            pieces.add(CutPiece("D29", jaliH, d29HPcs * q, src, "Jali height (D29)", gauge = "1.2"))
        }
        if (d29WPcs > 0) {
            pieces.add(CutPiece("D29", jaliW, d29WPcs * q, src, jaliWLabel, gauge = "1.2"))
        }

        if (panels == 4 && formula.d31HeightDeduction4.totalInches > 0.0) {
            val d31H = max(0.0, h - formula.d31HeightDeduction4.totalInches)
            pieces.add(CutPiece("D31", d31H, 1 * q, src, "D31 Height (4-Palay)", gauge = "1.2"))
        }

        w.extras.forEach { ex ->
            if (ex.lengthFt > 0 && ex.qty > 0) {
                pieces.add(CutPiece(ex.section, ex.lengthFt * 12.0, ex.qty * q, src, "Extra piece", gauge = g))
            }
        }
        return pieces
    }

    fun computeSlidingDoorPieces(
        it: SlidingDoorItem,
        formula: SlidingDoorFormulaSettings = SlidingDoorFormulaSettings(),
        fFormula: FSectionFormula = FSectionFormula(),
        itemNumber: Int = 1
    ): List<CutPiece> {
        val rawOuter = it.outer.trim()
        val isCustom = rawOuter.isNotBlank() && !rawOuter.equals("NORMAL", ignoreCase = true) && !rawOuter.equals("F", ignoreCase = true) && !rawOuter.equals("C", ignoreCase = true)
        val isF = if (isCustom) rawOuter.uppercase().endsWith("F") else rawOuter.equals("F", ignoreCase = true)
        val isPair = rawOuter.contains("/")
        val m30 = when {
            isPair -> rawOuter.split("/")[0].trim()
            isCustom -> rawOuter
            isF -> "M30F"
            else -> "M30"
        }
        val m26 = when {
            isPair -> {
                val parts = rawOuter.split("/")
                if (parts.size > 1 && parts[1].trim().isNotBlank()) parts[1].trim() else parts[0].trim()
            }
            isCustom -> rawOuter
            isF -> "M26F"
            else -> "M26"
        }
        val h = it.height
        val width = it.width
        val q = it.qty.coerceAtLeast(1)
        val pieces = mutableListOf<CutPiece>()
        val src = "Sliding Door #$itemNumber"
        val g = it.sectionGauge

        if (isF && fFormula.enabled) {
            val hLen = fFormula.calculateHeight(h)
            val hQty = fFormula.qtyH
            pieces.add(CutPiece(m30, hLen, hQty * q, src, "$m30 - Side height F", gauge = g))

            val wLen = fFormula.calculateWidth(width)
            val wTopQty = (fFormula.qtyW / 2).coerceAtLeast(1)
            val wBotQty = (fFormula.qtyW - wTopQty).coerceAtLeast(1)
            pieces.add(CutPiece(m30, wLen, wTopQty * q, src, "$m30 - Top width F", gauge = g))
            pieces.add(CutPiece(m26, wLen, wBotQty * q, src, "$m26 - Bottom width F", gauge = g))
        } else {
            pieces.add(CutPiece(m30, h, 2 * q, src, "$m30 - Side height", gauge = g))
            pieces.add(CutPiece(m30, width, 1 * q, src, "$m30 - Top width", gauge = g))
            pieces.add(CutPiece(m26, width, 1 * q, src, "$m26 - Bottom width", gauge = g))
        }

        val panels = it.panelCount.coerceIn(2, 4)

        val sashH = max(0.0, h - formula.m23HeightDeduction.totalInches)
        val m24Deduction = when (panels) {
            3 -> formula.m24WidthDeduction3.totalInches
            4 -> formula.m24WidthDeduction4.totalInches
            else -> formula.m24WidthDeduction.totalInches
        }
        val sashW = max(0.0, (width - m24Deduction) / panels.toDouble())

        val m23Apply = formula.m23ApplyTo
        val m23Pcs = when (panels) {
            3 -> if (formula.m23Qty3 > 0) formula.m23Qty3 else 2
            4 -> if (formula.m23Qty4 > 0) formula.m23Qty4 else (if (formula.m28Qty > 0) 4 else 8)
            else -> formula.m23Qty.coerceAtLeast(0)
        }
        if (m23Pcs > 0) {
            if (m23Apply == "H" || m23Apply == "both") {
                pieces.add(CutPiece("M23", sashH, m23Pcs * q, src, "Sash height (M23)", gauge = g))
            }
            if (m23Apply == "W" || m23Apply == "both") {
                pieces.add(CutPiece("M23", sashW, m23Pcs * q, src, "Sash width (M23)", gauge = g))
            }
        }

        val m28Apply = formula.m28ApplyTo
        val m28Pcs = when (panels) {
            3 -> if (formula.m28Qty3 > 0) formula.m28Qty3 else 4
            4 -> if (formula.m28Qty4 > 0) formula.m28Qty4 else (if (formula.m28Qty > 0) 4 else 0)
            else -> formula.m28Qty.coerceAtLeast(0)
        }
        if (m28Pcs > 0) {
            if (m28Apply == "H" || m28Apply == "both") {
                pieces.add(CutPiece("M28", sashH, m28Pcs * q, src, "Sash height (M28)", gauge = g))
            }
            if (m28Apply == "W" || m28Apply == "both") {
                pieces.add(CutPiece("M28", sashW, m28Pcs * q, src, "Sash width (M28)", gauge = g))
            }
        }

        val m24Pcs = it.m24Qty.coerceAtLeast(0)
        val m24Apply = formula.m24ApplyTo

        if (panels == 3) {
            val sashW_center = max(0.0, (width - formula.m24WidthCenterDeduction3.totalInches) / 2.0)
            val sashW_sides = max(0.0, (width - formula.m24WidthSidesDeduction3.totalInches) / 4.0)

            if (m24Pcs > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    val centerPcs = if (m24Pcs == 6) 2 else (m24Pcs / 3).coerceAtLeast(1)
                    val sidesPcs = if (m24Pcs == 6) 4 else max(0, m24Pcs - centerPcs)
                    if (centerPcs > 0) {
                        pieces.add(CutPiece("M24", sashW_center, centerPcs * q, src, "M24 Center width", gauge = g))
                    }
                    if (sidesPcs > 0) {
                        pieces.add(CutPiece("M24", sashW_sides, sidesPcs * q, src, "M24 Sides width", gauge = g))
                    }
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    val hPcs = if (m24Pcs <= 6) 6 else m24Pcs
                    pieces.add(CutPiece("M24", sashH, hPcs * q, src, "Sash height (M24)", gauge = g))
                }
            }
            if (it.m24aQty > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    val centerPcsA = if (it.m24aQty == 6) 2 else (it.m24aQty / 3).coerceAtLeast(1)
                    val sidesPcsA = if (it.m24aQty == 6) 4 else max(0, it.m24aQty - centerPcsA)
                    if (centerPcsA > 0) {
                        pieces.add(CutPiece("M24A", sashW_center, centerPcsA * q, src, "M24A Center width", gauge = g))
                    }
                    if (sidesPcsA > 0) {
                        pieces.add(CutPiece("M24A", sashW_sides, sidesPcsA * q, src, "M24A Sides width", gauge = g))
                    }
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    val hPcsA = if (it.m24aQty <= 6) 6 else it.m24aQty
                    pieces.add(CutPiece("M24A", sashH, hPcsA * q, src, "M24A height", gauge = g))
                }
            }
        } else {
            val sashW_eff = sashW
            val wLabel = "M24 width"
            if (m24Pcs > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    pieces.add(CutPiece("M24", sashW_eff, m24Pcs * q, src, wLabel, gauge = g))
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    pieces.add(CutPiece("M24", sashH, m24Pcs * q, src, "M24 height", gauge = g))
                }
            }
            if (it.m24aQty > 0) {
                if (m24Apply == "W" || m24Apply == "both") {
                    pieces.add(CutPiece("M24A", sashW_eff, it.m24aQty * q, src, wLabel.replace("M24", "M24A"), gauge = g))
                }
                if (m24Apply == "H" || m24Apply == "both") {
                    pieces.add(CutPiece("M24A", sashH, it.m24aQty * q, src, "M24A height", gauge = g))
                }
            }
        }

        val d29H = max(0.0, h - formula.getD29HeightDeduction(panels).totalInches)
        val jaliH = if (formula.getD29HeightDeduction(panels).totalInches > 0.0) d29H else sashH
        val m24ForJali = when (panels) {
            3 -> max(0.0, (width - formula.m24WidthSidesDeduction3.totalInches) / 4.0)
            4 -> max(0.0, (width - formula.m24WidthDeduction4.totalInches) / 4.0)
            else -> max(0.0, (width - formula.m24WidthDeduction.totalInches) / 2.0)
        }
        val d29ExtraW = formula.getD29WidthOverlap(panels).totalInches
        var jaliW = max(0.0, m24ForJali + d29ExtraW)

        if (it.lockType.equals("PUSH_HANDLE", ignoreCase = true) || it.lockType.equals("PUSH", ignoreCase = true)) {
            val phDed = formula.d29PushHandleDeduction.totalInches
            jaliW = max(0.0, jaliW - phDed)
        }

        val d29HPcs = it.d29HeightQty.coerceAtLeast(0)
        val d29WPcs = it.d29WidthQty.coerceAtLeast(0)
        val jaliWLabel = if (it.lockType.equals("PUSH_HANDLE", ignoreCase = true) || it.lockType.equals("PUSH", ignoreCase = true)) {
            "Jali width (D29) [Push Handle]"
        } else {
            "Jali width (D29)"
        }

        if (d29HPcs > 0) {
            pieces.add(CutPiece("D29", jaliH, d29HPcs * q, src, "Jali height (D29)", gauge = "1.2"))
        }
        if (d29WPcs > 0) {
            pieces.add(CutPiece("D29", jaliW, d29WPcs * q, src, jaliWLabel, gauge = "1.2"))
        }

        if (panels == 4 && formula.d31HeightDeduction4.totalInches > 0.0) {
            val d31H = max(0.0, h - formula.d31HeightDeduction4.totalInches)
            pieces.add(CutPiece("D31", d31H, 1 * q, src, "D31 Height (4-Palay)", gauge = "1.2"))
        }

        it.extras.forEach { ex ->
            if (ex.lengthFt > 0 && ex.qty > 0) {
                pieces.add(CutPiece(ex.section, ex.lengthFt * 12.0, ex.qty * q, src, "Extra piece", gauge = g))
            }
        }
        return pieces
    }

    fun computeOpenablePieces(
        width: Double,
        height: Double,
        qty: Int,
        outer: String,
        kind: String, // "door", "fix", "openwin"
        categorySettings: FormulaCategorySettings,
        extras: List<ExtraPiece>,
        gauge: String = "1.6",
        fFormula: FSectionFormula = FSectionFormula(),
        d59Qty: Int = 0,
        d59aQty: Int = 0,
        d52Qty: Int = 0,
        itemNumber: Int = 1
    ): List<CutPiece> {
        val q = qty.coerceAtLeast(1)
        val pieces = mutableListOf<CutPiece>()
        val src = when (kind) {
            "door" -> "Door #$itemNumber"
            "fix" -> "Fix Frame #$itemNumber"
            else -> "Openable Win #$itemNumber"
        }

        val isOuterF = outer.endsWith("F", ignoreCase = true) || outer.contains("F")

        val hLen = if (isOuterF && fFormula.enabled) fFormula.calculateHeight(height) else height
        val hQty = if (isOuterF && fFormula.enabled) fFormula.qtyH else 2
        pieces.add(CutPiece(outer, hLen, hQty * q, src, "Outer frame height", gauge = gauge))

        val defWQty = if (kind == "door") 1 else 2
        val wQty = if (isOuterF && fFormula.enabled) fFormula.qtyW else defWQty
        val wLen = if (isOuterF && fFormula.enabled) fFormula.calculateWidth(width) else width
        pieces.add(CutPiece(outer, wLen, wQty * q, src, "Outer frame width", gauge = gauge))

        fun addAcc(key: String, sec: String) {
            val accCfg = categorySettings.acc[key] ?: return
            val f = categorySettings.formulas[key] ?: DeductionPair()
            val qH = if (accCfg.applyTo == "H") accCfg.qty else (if (accCfg.qtyH > 0) accCfg.qtyH else accCfg.qty)
            val qW = if (accCfg.applyTo == "W") accCfg.qty else (if (accCfg.qtyW > 0) accCfg.qtyW else accCfg.qty)

            if (accCfg.applyTo == "H" || accCfg.applyTo == "both") {
                val isSecF = sec.endsWith("F", ignoreCase = true) || sec.contains("F")
                val lenH = if (isSecF && fFormula.enabled) {
                    fFormula.calculateHeight(height)
                } else {
                    max(0.0, height - f.h.totalInches)
                }
                if (qH > 0 && lenH > 0) {
                    pieces.add(CutPiece(sec, lenH, qH * q, src, "$sec height", gauge = gauge))
                }
            }
            if (accCfg.applyTo == "W" || accCfg.applyTo == "both") {
                val isSecF = sec.endsWith("F", ignoreCase = true) || sec.contains("F")
                val lenW = if (isSecF && fFormula.enabled) {
                    fFormula.calculateWidth(width)
                } else {
                    max(0.0, width - f.w.totalInches)
                }
                if (qW > 0 && lenW > 0) {
                    pieces.add(CutPiece(sec, lenW, qW * q, src, "$sec width", gauge = gauge))
                }
            }
        }

        categorySettings.acc.forEach { (key, accCfg) ->
            if (accCfg.enabled) {
                if (key == "D52" && (kind == "fix" || d52Qty > 0)) return@forEach
                if (key == "D59" && (d59Qty > 0 || d59aQty > 0)) return@forEach
                if (key == "D50" && kind == "fix") return@forEach
                if (key == "D46" && kind != "door") return@forEach

                val targetSec = if (accCfg.which != "none" && accCfg.which.isNotBlank()) accCfg.which else key
                addAcc(key, targetSec)
            }
        }

        if (d59Qty > 0) {
            val f = categorySettings.formulas["D59"] ?: DeductionPair()
            val len = if (f.w.totalInches > 0) max(0.0, width - f.w.totalInches) else width
            pieces.add(CutPiece("D59", len, d59Qty * q, src, "D59 width", gauge = gauge))
        }
        if (d59aQty > 0) {
            val f = categorySettings.formulas["D59"] ?: DeductionPair()
            val len = if (f.w.totalInches > 0) max(0.0, width - f.w.totalInches) else width
            pieces.add(CutPiece("D59A", len, d59aQty * q, src, "D59A width", gauge = gauge))
        }
        if (d52Qty > 0) {
            val f = categorySettings.formulas["D52"] ?: DeductionPair()
            val len = if (f.w.totalInches > 0) max(0.0, width - f.w.totalInches) else width
            pieces.add(CutPiece("D52", len, d52Qty * q, src, "D52 width", gauge = gauge))
        }

        extras.forEach { ex ->
            if (ex.lengthFt > 0 && ex.qty > 0) {
                pieces.add(CutPiece(ex.section, ex.lengthFt * 12.0, ex.qty * q, src, "Extra piece", gauge = gauge))
            }
        }
        return pieces
    }
}

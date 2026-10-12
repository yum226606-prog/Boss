package com.example.model

data class WindowItem(
    val id: String,
    val width: Double = 36.0,
    val height: Double = 48.0,
    val qty: Int = 1,
    val frameType: String = "C", // "C" or "F" or custom
    val sectionGauge: String = "1.6",
    val glassGauge: String = "5",
    val glassColor: String = "CLEAR",
    val glassEnabled: Boolean = true,
    val m24Qty: Int = 4,
    val m24aQty: Int = 0,
    val d29HeightQty: Int = 2,
    val d29WidthQty: Int = 2,
    val extras: List<ExtraPiece> = emptyList(),
    val outerSection: String = "",
    val panelCount: Int = 2, // 2 = 2 Palay, 3 = 3 Palay, 4 = 4 Palay
    val lockType: String = "LATCH" // "LATCH" or "PUSH_HANDLE"
)

data class DoorItem(
    val id: String,
    val width: Double = 36.0,
    val height: Double = 84.0,
    val qty: Int = 1,
    val outer: String = "D54A",
    val sectionGauge: String = "1.6",
    val glassGauge: String = "5",
    val glassColor: String = "CLEAR",
    val glassEnabled: Boolean = true,
    val d59Qty: Int = 0,
    val d59aQty: Int = 0,
    val d52Qty: Int = 0,
    val extras: List<ExtraPiece> = emptyList()
)

data class FixFrameItem(
    val id: String,
    val width: Double = 36.0,
    val height: Double = 48.0,
    val qty: Int = 1,
    val outer: String = "D54A",
    val sectionGauge: String = "1.6",
    val glassGauge: String = "5",
    val glassColor: String = "CLEAR",
    val glassEnabled: Boolean = true,
    val d59Qty: Int = 0,
    val d59aQty: Int = 0,
    val d52Qty: Int = 0,
    val extras: List<ExtraPiece> = emptyList()
)

data class OpenableWindowItem(
    val id: String,
    val width: Double = 36.0,
    val height: Double = 48.0,
    val qty: Int = 1,
    val outer: String = "D54A",
    val sectionGauge: String = "1.6",
    val glassGauge: String = "5",
    val glassColor: String = "CLEAR",
    val glassEnabled: Boolean = true,
    val d59Qty: Int = 0,
    val d59aQty: Int = 0,
    val d52Qty: Int = 0,
    val extras: List<ExtraPiece> = emptyList()
)

data class SlidingDoorItem(
    val id: String,
    val width: Double = 48.0,
    val height: Double = 84.0,
    val qty: Int = 1,
    val outer: String = "NORMAL", // "NORMAL" (M26+M30) or "F" (M26F+M30F) or custom pair
    val m24Qty: Int = 4,
    val m24aQty: Int = 0,
    val d29HeightQty: Int = 2,
    val d29WidthQty: Int = 2,
    val sectionGauge: String = "1.6",
    val glassGauge: String = "5",
    val glassColor: String = "CLEAR",
    val glassEnabled: Boolean = true,
    val extras: List<ExtraPiece> = emptyList(),
    val panelCount: Int = 2, // 2 = 2 Palay, 3 = 3 Palay, 4 = 4 Palay
    val lockType: String = "LATCH" // "LATCH" or "PUSH_HANDLE"
)

data class ExtraPiece(
    val section: String = "D54A",
    val lengthFt: Double = 16.0,
    val qty: Int = 1,
    val id: String = java.util.UUID.randomUUID().toString(),
    val gauge: String = "1.6"
)

data class DeductionDim(
    val inch: Double = 0.0,
    val sutar: Double = 0.0
) {
    val totalInches: Double
        get() = inch + sutar / 8.0
}

data class DeductionPair(
    val h: DeductionDim = DeductionDim(),
    val w: DeductionDim = DeductionDim()
)

data class AccessoryConfig(
    val enabled: Boolean = true,
    val which: String = "none",
    val applyTo: String = "both", // "H", "W", "both"
    val qty: Int = 2,
    val qtyH: Int = 2,
    val qtyW: Int = 1
)

data class FormulaCategorySettings(
    val formulas: Map<String, DeductionPair> = defaultFormulas(),
    val acc: Map<String, AccessoryConfig> = defaultAccessories()
) {
    companion object {
        fun defaultFormulas(): Map<String, DeductionPair> = mapOf(
            "D41" to DeductionPair(),
            "D59" to DeductionPair(),
            "D52" to DeductionPair(),
            "D50" to DeductionPair(),
            "D46" to DeductionPair()
        )

        fun defaultAccessories(isOpenableWindow: Boolean = false): Map<String, AccessoryConfig> = mapOf(
            "D41" to AccessoryConfig(enabled = true, which = "D41", applyTo = "both", qty = 2),
            "D59" to AccessoryConfig(enabled = false, which = "D59A", applyTo = "both", qty = 2),
            "D52" to AccessoryConfig(enabled = false, which = "D52", applyTo = "W", qty = 2),
            "D50" to AccessoryConfig(enabled = true, which = "D50", applyTo = "both", qty = 2, qtyH = 2, qtyW = if (isOpenableWindow) 2 else 1),
            "D46" to AccessoryConfig(enabled = true, which = "D46", applyTo = "W", qty = 1, qtyW = 1)
        )

        fun defaultOpenableWindowSettings(): FormulaCategorySettings = FormulaCategorySettings(
            formulas = defaultFormulas(),
            acc = defaultAccessories(isOpenableWindow = true)
        )
    }
}

data class WindowFormulaSettings(
    val m23HeightDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24Panels: Int = 2,
    val d29HeightDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthOverlap: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29Panels: Int = 2,
    val m23ApplyTo: String = "H",
    val m23Qty: Int = 2,
    val m28ApplyTo: String = "H",
    val m28Qty: Int = 2,
    val m24ApplyTo: String = "W",
    val m24Qty: Int = 2,
    val d29ApplyTo: String = "both",
    val d29HeightQty: Int = 2,
    val d29WidthQty: Int = 2,
    // 3-Palay Configurations
    val m24WidthCenterDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthSidesDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthOverlap3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m23Qty3: Int = 2,
    val m28Qty3: Int = 4,
    val m24Qty3: Int = 6,
    val d29WidthDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthOverlap3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29Panels3: Int = 3,
    val d29HeightQty3: Int = 4,
    val d29WidthQty3: Int = 4,
    val d29HeightDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    // 4-Palay Configurations
    val m24WidthDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m23Qty4: Int = 4,
    val m28Qty4: Int = 4,
    val m24Qty4: Int = 8,
    val d29WidthDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthOverlap4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29Panels4: Int = 4,
    val d29HeightQty4: Int = 4,
    val d29WidthQty4: Int = 4,
    val d29HeightDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    // Push handle deduction
    val d29PushHandleDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d31HeightDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0)
) {
    fun getD29WidthDeduction(panels: Int): DeductionDim = when (panels) {
        3 -> d29WidthDeduction3
        4 -> d29WidthDeduction4
        else -> d29WidthDeduction
    }
    fun getD29WidthOverlap(panels: Int): DeductionDim = when (panels) {
        3 -> d29WidthOverlap3
        4 -> d29WidthOverlap4
        else -> d29WidthOverlap
    }
    fun getD29Panels(panels: Int): Int = when (panels) {
        3 -> d29Panels3
        4 -> d29Panels4
        else -> d29Panels
    }
    fun getD29HeightQty(panels: Int): Int = when (panels) {
        3 -> d29HeightQty3
        4 -> d29HeightQty4
        else -> d29HeightQty
    }
    fun getD29WidthQty(panels: Int): Int = when (panels) {
        3 -> d29WidthQty3
        4 -> d29WidthQty4
        else -> d29WidthQty
    }
    fun getD29HeightDeduction(panels: Int): DeductionDim = when (panels) {
        3 -> d29HeightDeduction3
        4 -> d29HeightDeduction4
        else -> d29HeightDeduction
    }
}

data class SlidingDoorFormulaSettings(
    val m23HeightDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24Panels: Int = 2,
    val m23ApplyTo: String = "H",
    val m23Qty: Int = 2,
    val m28ApplyTo: String = "H",
    val m28Qty: Int = 2,
    val m24ApplyTo: String = "W",
    val m24Qty: Int = 2,
    val d29HeightDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthOverlap: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29Panels: Int = 2,
    val d29ApplyTo: String = "BOTH",
    val d29HeightQty: Int = 2,
    val d29WidthQty: Int = 2,
    // 3-Palay Configurations
    val m24WidthCenterDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthSidesDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m24WidthOverlap3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m23Qty3: Int = 2,
    val m28Qty3: Int = 4,
    val m24Qty3: Int = 6,
    val d29WidthDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthOverlap3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29Panels3: Int = 3,
    val d29HeightQty3: Int = 4,
    val d29WidthQty3: Int = 4,
    val d29HeightDeduction3: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    // 4-Palay Configurations
    val m24WidthDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val m23Qty4: Int = 4,
    val m28Qty4: Int = 4,
    val m24Qty4: Int = 8,
    val d29WidthDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29WidthOverlap4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29Panels4: Int = 4,
    val d29HeightQty4: Int = 4,
    val d29WidthQty4: Int = 4,
    val d29HeightDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d29PushHandleDeduction: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val d31HeightDeduction4: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0)
) {
    fun getD29WidthDeduction(panels: Int): DeductionDim = when (panels) {
        3 -> d29WidthDeduction3
        4 -> d29WidthDeduction4
        else -> d29WidthDeduction
    }
    fun getD29WidthOverlap(panels: Int): DeductionDim = when (panels) {
        3 -> d29WidthOverlap3
        4 -> d29WidthOverlap4
        else -> d29WidthOverlap
    }
    fun getD29HeightDeduction(panels: Int): DeductionDim = when (panels) {
        3 -> d29HeightDeduction3
        4 -> d29HeightDeduction4
        else -> d29HeightDeduction
    }
}

data class GlassFormula(
    val marginH: Double = 0.0,
    val marginW: Double = 0.0,
    val panels: Int = 1,
    val marginHSutar: Double = 0.0,
    val marginWSutar: Double = 0.0,
    // 3-Palay Configurations
    val marginH3: Double = 0.0,
    val marginH3Sutar: Double = 0.0,
    val marginW3Center: Double = 0.0,
    val marginW3CenterSutar: Double = 0.0,
    val marginW3Sides: Double = 0.0,
    val marginW3SidesSutar: Double = 0.0,
    val marginW3: Double = 0.0,
    val marginW3Sutar: Double = 0.0,
    val overlapW3: Double = 0.0,
    val overlapW3Sutar: Double = 0.0,
    // 4-Palay Configurations
    val marginH4: Double = 0.0,
    val marginH4Sutar: Double = 0.0,
    val marginW4: Double = 0.0,
    val marginW4Sutar: Double = 0.0
) {
    fun getEffectiveMarginH(palay: Int = 2): Double = when (palay) {
        3 -> marginH3 + marginH3Sutar / 8.0
        4 -> marginH4 + marginH4Sutar / 8.0
        else -> marginH + marginHSutar / 8.0
    }

    fun getEffectiveMarginW(palay: Int = 2): Double = when (palay) {
        3 -> marginW3 + marginW3Sutar / 8.0
        4 -> marginW4 + marginW4Sutar / 8.0
        else -> marginW + marginWSutar / 8.0
    }

    fun getEffectiveMarginW3Center(): Double =
        if (marginW3Center > 0 || marginW3CenterSutar > 0) marginW3Center + marginW3CenterSutar / 8.0
        else marginW3 + marginW3Sutar / 8.0

    fun getEffectiveMarginW3Sides(): Double =
        if (marginW3Sides > 0 || marginW3SidesSutar > 0) marginW3Sides + marginW3SidesSutar / 8.0
        else marginW3 + marginW3Sutar / 8.0

    fun getEffectiveOverlapW3(): Double = overlapW3 + overlapW3Sutar / 8.0
}

data class GlassPair(
    val gauge: String,
    val color: String
)

data class FSectionFormula(
    val enabled: Boolean = true,
    val applyTo: String = "both",
    val heightDim: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val widthDim: DeductionDim = DeductionDim(inch = 0.0, sutar = 0.0),
    val heightIsAdd: Boolean = true,
    val widthIsAdd: Boolean = true,
    val qtyH: Int = 2,
    val qtyW: Int = 2
) {
    fun calculateHeight(frameHeight: Double): Double {
        val offset = if (heightIsAdd) heightDim.totalInches else -heightDim.totalInches
        return maxOf(0.0, frameHeight + offset)
    }

    fun calculateWidth(frameWidth: Double): Double {
        val offset = if (widthIsAdd) widthDim.totalInches else -widthDim.totalInches
        return maxOf(0.0, frameWidth + offset)
    }
}

data class CutPiece(
    val section: String,
    val length: Double,
    val qty: Int = 1,
    val source: String,
    val label: String,
    val gauge: String = "1.2"
)

data class PackedBar(
    val lenFt: Int,
    val isCustom: Boolean,
    val remainInches: Double,
    val pieces: List<CutPiece>
)

data class TallyInfo(
    val count: Int,
    val isCustom: Boolean
)

data class PackedResult(
    val tally: Map<Int, TallyInfo>,
    val unfit: List<CutPiece>,
    val bins: List<PackedBar>
)

data class GlassCutResult(
    val frameLabel: String,
    val frameDim: String,
    val glassGauge: String,
    val glassColor: String,
    val gH: Double,
    val gWpanel: Double,
    val billedHFt: Double,
    val billedWFt: Double,
    val pieces: Int,
    val sqft: Double
)

data class AluminiumCostRow(
    val section: String,
    val gauge: String,
    val bars: Int,
    val totalFt: Int,
    val ratePerFt: Double,
    val cost: Double,
    val tallySummary: String = ""
)

data class CustomBillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val amount: Double
)

data class GlassCostLine(
    val description: String,
    val sqft: Double,
    val rate: Double,
    val total: Double
)

data class QuotationResult(
    val aluminiumRows: List<AluminiumCostRow>,
    val alumTotal: Double,
    val discountPct: Double = 0.0,
    val discountAmt: Double = 0.0,
    val netAlumTotal: Double = alumTotal,
    val alumWithMarkup: Double,
    val markupPct: Double,
    val totalUnits: Int,
    val hardwareTotal: Double,
    val glassTotal: Double,
    val glassLines: List<GlassCostLine>,
    val transportRent: Double,
    val labourCost: Double,
    val grandTotal: Double
)

data class SavedQuotation(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val label: String,
    val customerName: String,
    val customerPhone: String,
    val gauge: String,
    val color: String,
    val frameType: String,
    val grandTotal: Double,
    val alumTotal: Double,
    val hardwareTotal: Double,
    val glassTotal: Double,
    val transportRent: Double,
    val labourCost: Double,
    val totalUnits: Int,
    val itemsSummary: String,
    val windowsJson: String = "",
    val doorsJson: String = "",
    val fixframesJson: String = "",
    val openableWindowsJson: String = "",
    val slidingDoorsJson: String = "",
    val projectExtrasJson: String = ""
)

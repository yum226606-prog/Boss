package com.example.model

import java.util.UUID

data class DynamicMasterSection(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "Windows",
    val code: String,
    val title: String,
    val defaultGauge: String = "1.6",
    val defaultColor: String = "DULL",
    val defaultRate: Double = 650.0,
    val weightPerFoot: Double = 0.45,
    val formulaWidthDeduction: Double = 0.0,
    val formulaHeightDeduction: Double = 0.0,
    val glassMarginW: Double = 0.0,
    val glassMarginH: Double = 0.0,
    val isCuttingEnabled: Boolean = true,
    val isGlassEnabled: Boolean = true,
    val accessoriesRequired: List<String> = emptyList(),
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class DragDropBlueprint(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val widthFt: Double = 4.0,
    val heightFt: Double = 4.0,
    val panels: Int = 2,
    val hasBottomFixed: Boolean = false,
    val category: String = "Sliding Window"
)

data class RemoteLayoutConfig(
    val enabledTabs: Set<String> = emptySet(),
    val tabCustomTitles: Map<String, String> = emptyMap(),
    val showMasterGaugeSelector: Boolean = true,
    val showMasterColorSelector: Boolean = true,
    val showMasterFrameSelector: Boolean = true,
    val showMasterGlassSelector: Boolean = true,
    val showThemeStudioButton: Boolean = true,
    val showDoneQuotationButton: Boolean = true,
    val showCompareCFButton: Boolean = true,
    val showDiscountRow: Boolean = true,
    val showHardwareRow: Boolean = true,
    val showTransportRow: Boolean = true,
    val showLabourRow: Boolean = true
) {
    private fun aliasesForTab(enumName: String, defaultTitle: String): List<String> {
        val base = mutableListOf(enumName.uppercase(), defaultTitle.uppercase())
        when (enumName.uppercase()) {
            "WINDOWS" -> base.addAll(listOf("WIN", "WINDOW", "SLIDING_WINDOWS", "SLIDING WINDOW", "SLIDING WINDOWS"))
            "DOORS" -> base.addAll(listOf("DOOR", "DOORS_AND_FRAMES", "DOORS & FRAMES", "DOORS_FRAMES", "DOOR & FRAME"))
            "EXTRA_PIECES" -> base.addAll(listOf("EXTRA", "EXTRAS", "EXTRA PIECES", "PROJECT_EXTRAS"))
            "CUTTING_CHART" -> base.addAll(listOf("CUTTING", "CUTTING CHART", "CUT_CHART"))
            "LENGTH_CHART" -> base.addAll(listOf("LENGTH", "LENGTH CHART", "BAR_CHART"))
            "GLASS_CHART" -> base.addAll(listOf("GLASS", "GLASS CHART"))
            "BILLING" -> base.addAll(listOf("QUOTATION", "QUOTE", "QUOTATION CHART", "QUOTATION_CHART", "BILL"))
            "HISTORY" -> base.addAll(listOf("SAVED", "SAVED_QUOTATIONS", "HISTORY"))
            "RATES_AND_SETTINGS" -> base.addAll(listOf("SETTINGS", "ACCOUNT", "ACCOUNT & LICENSE", "RATES", "RATES & SETTINGS", "PROFILE"))
        }
        return base.distinct()
    }

    fun isClientTabEnabled(enumName: String, defaultTitle: String): Boolean {
        if (enabledTabs.isEmpty()) return true
        val normEnabled = enabledTabs.map { it.trim().uppercase() }.toSet()
        val aliases = aliasesForTab(enumName, defaultTitle)
        return aliases.any { normEnabled.contains(it) }
    }

    fun getTabTitle(enumName: String, defaultTitle: String): String {
        if (tabCustomTitles.isEmpty()) return defaultTitle
        val aliases = aliasesForTab(enumName, defaultTitle)
        for ((k, v) in tabCustomTitles) {
            if (v.isNotBlank() && aliases.contains(k.trim().uppercase())) {
                return v.trim()
            }
        }
        return defaultTitle
    }
}

data class MasterEngineCloudConfig(
    val version: Long = System.currentTimeMillis(),
    val publishedBy: String = "Syed Saboor Nawab (Master)",
    val customSections: List<DynamicMasterSection> = emptyList(),
    val hiddenSections: Set<String> = emptySet(),
    val deletedSections: Set<String> = emptySet(),
    val availableGauges: List<String>? = null,
    val availableColors: List<String>? = null,
    val availableGlassGauges: List<String>? = null,
    val availableGlassColors5mm: List<String>? = null,
    val availableGlassColors8mm: List<String>? = null,
    val layoutPreset: String? = null,
    val layoutTabPosition: String? = null,
    val layoutButtonStyle: String? = null,
    val layoutFontWeightMode: String? = null,
    val remoteLayoutConfig: RemoteLayoutConfig = RemoteLayoutConfig(),
    val rateOverrides: Map<String, Double> = emptyMap(),
    val rates: Map<String, Map<String, Map<String, Double>>>? = null,
    val glassRates: Map<String, Map<String, Double>>? = null,
    val formulaAdjustments: Map<String, Double> = emptyMap(),
    val winFormulas: WindowFormulaSettings? = null,
    val slidingDoorFormulas: SlidingDoorFormulaSettings? = null,
    val doorFormulas: FormulaCategorySettings? = null,
    val fixFormulas: FormulaCategorySettings? = null,
    val openableWinFormulas: FormulaCategorySettings? = null,
    val fSectionFormula: FSectionFormula? = null,
    val glassFormulas: Map<String, GlassFormula>? = null,
    val dragDropBlueprints: List<DragDropBlueprint> = emptyList(),
    val companyName: String? = null,
    val companyTagline: String? = null,
    val companyPhone: String? = null,
    val companyAddress: String? = null,
    val quotationTerms: String? = null,
    val mainLogoBase64: String = "",
    val quotationLetterheadBase64: String = "",
    val clearRateListCommand: Boolean = false,
    val clearRateListTimestamp: Long = 0L,
    val activeBanner: AppOfferBanner? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

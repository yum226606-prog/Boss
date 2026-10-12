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

data class MasterEngineCloudConfig(
    val version: Long = System.currentTimeMillis(),
    val publishedBy: String = "Syed Saboor Nawab (Master)",
    val customSections: List<DynamicMasterSection> = emptyList(),
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
    val mainLogoBase64: String = "",
    val quotationLetterheadBase64: String = "",
    val clearRateListCommand: Boolean = false,
    val clearRateListTimestamp: Long = 0L,
    val activeBanner: AppOfferBanner? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

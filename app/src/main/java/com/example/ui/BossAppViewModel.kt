package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.ui.theme.ThemeConfigEngine
import com.example.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToLong

enum class ClientTab(val title: String) {
    WINDOWS("Windows"),
    DOORS("Doors & Frames"),
    EXTRA_PIECES("Extra"),
    CUTTING_CHART("Cutting Chart"),
    LENGTH_CHART("Length Chart"),
    GLASS_CHART("Glass Chart"),
    BILLING("Quotation"),
    HISTORY("History"),
    RATES_AND_SETTINGS("Account & License")
}

data class BossAppState(
    val activeTab: ClientTab = ClientTab.WINDOWS,
    val windows: List<WindowItem> = listOf(
        WindowItem(id = UUID.randomUUID().toString(), width = 48.0, height = 48.0, qty = 1)
    ),
    val doors: List<DoorItem> = emptyList(),
    val fixframes: List<FixFrameItem> = emptyList(),
    val openableWindows: List<OpenableWindowItem> = emptyList(),
    val slidingDoors: List<SlidingDoorItem> = emptyList(),
    val projectExtras: List<ExtraPiece> = emptyList(),
    val customCutPieces: List<CutPiece> = emptyList(),
    val ctxGauge: String = "1.6",
    val ctxGaugeMode: String = "ON",
    val ctxColor: String = "SAHARA / BROWN",
    val ctxFrameType: String = "C",
    val ctxGlassGauge: String = "5",
    val ctxGlassColor: String = "CLEAR",
    val ctxGlassMode: String = "ON",
    val glassFittingType: String = "FIX_RUBBER",
    val measurementUnit: String = "INCH_SUTAR",
    val settingsSubTab: String = "ACCOUNT",
    val letterheadMode: Boolean = true,
    val quotationBgOpacity: Float = 0.92f,
    val quotationLogoWatermarkOpacity: Float = 0.28f,
    val availableGauges: List<String> = listOf("1.2", "1.4", "1.6", "2.0"),
    val availableColors: List<String> = BossDefaultRates.DEFAULT_COLORS,
    val availableGlassGauges: List<String> = listOf("5", "8"),
    val availableGlassColors5mm: List<String> = BossDefaultRates.DEFAULT_GLASS_COLORS_5MM,
    val availableGlassColors8mm: List<String> = BossDefaultRates.DEFAULT_GLASS_COLORS_8MM,
    val availableGlassColorsByGauge: Map<String, List<String>> = mapOf(
        "5" to BossDefaultRates.DEFAULT_GLASS_COLORS_5MM,
        "8" to BossDefaultRates.DEFAULT_GLASS_COLORS_8MM
    ),
    val customSections: List<String> = emptyList(),
    val deletedSections: Set<String> = emptySet(),
    val hiddenSections: Set<String> = emptySet(),
    val rates: Map<String, Map<String, Map<String, Double>>> = BossDefaultRates.getInitialRates(),
    val glassRates: Map<String, Map<String, Double>> = BossDefaultRates.getInitialGlassRates(),
    val winFormulas: WindowFormulaSettings = WindowFormulaSettings(),
    val slidingDoorFormulas: SlidingDoorFormulaSettings = SlidingDoorFormulaSettings(),
    val doorFormulas: FormulaCategorySettings = FormulaCategorySettings(),
    val fixFormulas: FormulaCategorySettings = FormulaCategorySettings(),
    val opwinFormulas: FormulaCategorySettings = FormulaCategorySettings.defaultOpenableWindowSettings(),
    val glassFormulas: Map<String, GlassFormula> = mapOf(
        "win" to GlassFormula(panels = 2),
        "door" to GlassFormula(panels = 1),
        "fix" to GlassFormula(panels = 1),
        "opwin" to GlassFormula(panels = 1),
        "slidingdoor" to GlassFormula(panels = 2)
    ),
    val fSectionFormula: FSectionFormula = FSectionFormula(),
    val customerName: String = "",
    val customerPhone: String = "",
    val alumDiscountPercent: Double = 0.0,
    val customBillItems: List<CustomBillItem> = emptyList(),
    val autoIncludeHardware: Boolean = true,
    val windowHardwareRate: Double = 1500.0,
    val doorHardwareRate: Double = 2500.0,
    val fixHardwareRate: Double = 800.0,
    val slidingDoorHardwareRate: Double = 3000.0,
    val wastagePercent: Double = 0.0,
    val profitPercent: Double = 0.0,
    val taxPercent: Double = 0.0,
    val quotationTerms: String = "1. 50% Advance payment required to start work.\n2. Balance 50% upon material delivery.\n3. Quotation valid for 7 days only.",
    val markupPercent: Double = 0.0,
    val hardwareRatePerUnit: Double = 450.0,
    val transportRent: Double = 0.0,
    val labourCost: Double = 0.0,
    val companyName: String = "YOUR SHOP / BUSINESS NAME",
    val companyTagline: String = "Aluminium & Glass Fabrication Works",
    val companyPhone: String = "",
    val companyAddress: String = "",
    val companyNtn: String = "",
    val customLogoPath: String? = null,
    val customBackgroundPath: String? = null,
    val dragDropBlueprints: List<DragDropBlueprint> = emptyList(),
    val userProfile: UserProfile = UserProfile(),
    val subscriptionState: SubscriptionState = SubscriptionState(),
    val savedQuotations: List<SavedQuotation> = emptyList(),
    val activeOfferBanner: AppOfferBanner? = null,
    val themeConfig: ThemeConfig = ThemeConfigEngine.getPresetConfig(ThemePreset.BOSS_GOLD)
) {
    fun getGlassColorsForGauge(gauge: String): List<String> {
        val clean = gauge.trim().removeSuffix("mm").removeSuffix("MM").trim()
        return availableGlassColorsByGauge[clean]
            ?: glassRates[clean]?.keys?.toList()?.takeIf { it.isNotEmpty() }
            ?: if (clean == "8") availableGlassColors8mm else availableGlassColors5mm
    }
}

class BossAppViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val prefs = context.getSharedPreferences("boss_client_app_prefs", Context.MODE_PRIVATE)
    val securityManager = UserSecurityManager(context)
    private val masterAdminManager = MasterAdminManager.getInstance(context)
    private val masterEngineManager = BossMasterEngineManager.getInstance(context)

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<BossAppState> = _state.asStateFlow()

    init {
        FirebaseCloudManager.initContext(context)
        startMasterLiveSyncListeners()
    }

    private fun loadInitialState(): BossAppState {
        val profile = securityManager.loadUserProfile()
        val sub = securityManager.loadSubscriptionState()
        val offer = masterAdminManager.getActiveOffer()
        val savedDeletedSecs: Set<String> = prefs.getStringSet("deleted_sections_set", emptySet<String>())
            ?.filterNotNull()
            ?.toSet()
            ?: emptySet<String>()
        val globalSections = masterAdminManager.getGlobalCustomSections()
        val dynamicSections = masterEngineManager.getDynamicSections().filter { it.isActive }.map { it.code }
        val mergedCustomSections = (globalSections + dynamicSections).distinct().filterNot { savedDeletedSecs.contains(it) }

        val baseRates = BossDefaultRates.getInitialRates()

        // Apply Master rate overrides
        val rateOverrides = masterAdminManager.getGlobalRateOverrides()
        applyRateOverridesToMap(baseRates, rateOverrides)

        // Apply dynamic section default rates
        masterEngineManager.getDynamicSections().filter { it.isActive }.forEach { dyn ->
            if (dyn.defaultRate > 0 && !savedDeletedSecs.contains(dyn.code)) {
                baseRates.forEach { (g, colorMap) ->
                    val ratio = (g.toDoubleOrNull() ?: 1.2) / (dyn.defaultGauge.toDoubleOrNull() ?: 1.6).coerceAtLeast(1.0)
                    colorMap.forEach { (_, secMap) ->
                        secMap[dyn.code] = (dyn.defaultRate * ratio).roundToLong().toDouble()
                    }
                }
            }
        }

        // Remove any user-deleted sections from baseRates
        if (savedDeletedSecs.isNotEmpty()) {
            val upperDeleted = savedDeletedSecs.map { it.trim().uppercase() }.toSet()
            baseRates.forEach { (_, colorMap) ->
                colorMap.forEach { (_, secMap) ->
                    secMap.keys.removeAll { k -> savedDeletedSecs.contains(k) || upperDeleted.contains(k.trim().uppercase()) }
                }
            }
        }

        val savedGaugesStr = prefs.getString("saved_available_gauges", null)
        val initialGauges = if (!savedGaugesStr.isNullOrBlank()) {
            savedGaugesStr.split("|").map { it.trim().removeSuffix("mm").removeSuffix("MM").trim() }.filter { it.isNotEmpty() }.distinct()
        } else {
            listOf("1.2", "1.4", "1.6", "2.0")
        }

        val savedColorsStr = prefs.getString("saved_available_colors", null)
        val initialColors = if (!savedColorsStr.isNullOrBlank()) {
            savedColorsStr.split("|").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()
        } else {
            BossDefaultRates.DEFAULT_COLORS
        }

        // Ensure all saved gauges and colors exist in baseRates and apply user-saved section rates
        initialGauges.forEach { g ->
            val gMap = baseRates.getOrPut(g) { mutableMapOf() }
            val mult = (g.toDoubleOrNull() ?: 1.2) / 1.2
            val templateGaugeMap = baseRates["1.2"] ?: baseRates.values.firstOrNull() ?: emptyMap()
            initialColors.forEach { col ->
                val cMap = gMap.getOrPut(col) {
                    val templateSecMap = templateGaugeMap[col] ?: templateGaugeMap["DULL"] ?: templateGaugeMap.values.firstOrNull() ?: emptyMap()
                    templateSecMap.mapValues { (_, r) -> (r * mult).roundToLong().toDouble() }.toMutableMap()
                }
                cMap.keys.toList().forEach { sec ->
                    val userSavedRate = prefs.getFloat("user_sec_rate_${g}_${col}_$sec", -1f)
                    if (userSavedRate >= 0f) {
                        cMap[sec] = userSavedRate.toDouble()
                    }
                }
            }
        }
        // Also apply any custom saved section rate keys from prefs
        val customRateKeys = prefs.getStringSet("user_custom_rate_keys", emptySet()) ?: emptySet()
        customRateKeys.forEach { key ->
            val parts = key.split("|")
            if (parts.size == 3) {
                val g = parts[0]
                val col = parts[1]
                val sec = parts[2]
                val r = prefs.getFloat("user_sec_rate_${g}_${col}_$sec", -1f)
                if (r >= 0f && !savedDeletedSecs.contains(sec)) {
                    baseRates.getOrPut(g) { mutableMapOf() }
                        .getOrPut(col) { mutableMapOf() }[sec] = r.toDouble()
                }
            }
        }

        val savedGlassGaugesStr = prefs.getString("saved_available_glass_gauges", null)
        val initialGlassGauges = if (!savedGlassGaugesStr.isNullOrBlank()) {
            savedGlassGaugesStr.split("|")
                .map { it.trim().removeSuffix("mm").removeSuffix("MM").trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        } else {
            listOf("5", "8")
        }

        val savedGlass5Str = prefs.getString("saved_glass_colors_5mm", null)
        val initialGlass5 = if (savedGlass5Str != null) {
            savedGlass5Str.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            BossDefaultRates.DEFAULT_GLASS_COLORS_5MM
        }

        val savedGlass8Str = prefs.getString("saved_glass_colors_8mm", null)
        val initialGlass8 = if (savedGlass8Str != null) {
            savedGlass8Str.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            BossDefaultRates.DEFAULT_GLASS_COLORS_8MM
        }

        val defaultColorsByGauge = mutableMapOf(
            "5" to initialGlass5,
            "8" to initialGlass8
        )
        initialGlassGauges.forEach { g ->
            if (!defaultColorsByGauge.containsKey(g)) {
                defaultColorsByGauge[g] = initialGlass5
            }
        }
        val initialGlassColorsByGauge = BossSerializationHelper.deserializeGlassColorsByGauge(
            prefs.getString("saved_glass_colors_by_gauge", null),
            defaultColorsByGauge
        ).toMutableMap()
        initialGlassColorsByGauge["5"] = initialGlassColorsByGauge["5"] ?: initialGlass5
        initialGlassColorsByGauge["8"] = initialGlassColorsByGauge["8"] ?: initialGlass8
        initialGlassGauges.forEach { g ->
            if (!initialGlassColorsByGauge.containsKey(g)) {
                initialGlassColorsByGauge[g] = initialGlass5
            }
        }

        val baseDefaultGlassRates = BossDefaultRates.getInitialGlassRates()
        val initialGlassRates = mutableMapOf<String, Map<String, Double>>()
        initialGlassGauges.forEach { g ->
            val allowed = (initialGlassColorsByGauge[g] ?: if (g == "8") initialGlass8 else initialGlass5).toSet()
            val templateMap = baseDefaultGlassRates[g] ?: baseDefaultGlassRates["5"] ?: emptyMap()
            val mult = (g.toDoubleOrNull() ?: 5.0) / 5.0
            val filtered = templateMap.filterKeys { allowed.contains(it) }.mapValues { (_, r) ->
                if (baseDefaultGlassRates.containsKey(g)) r else (r * mult).roundToLong().toDouble()
            }.toMutableMap()
            allowed.forEach { col ->
                val savedRate = prefs.getFloat("glass_rate_${g}_$col", -1f)
                if (savedRate >= 0f) {
                    filtered[col] = savedRate.toDouble()
                } else if (!filtered.containsKey(col)) {
                    filtered[col] = if (g == "8") 390.0 else (230.0 * mult).roundToLong().toDouble()
                }
            }
            initialGlassRates[g] = filtered.toMap()
        }

        val defaultGlassFormulas = mapOf(
            "win" to GlassFormula(panels = 2),
            "door" to GlassFormula(panels = 1),
            "fix" to GlassFormula(panels = 1),
            "opwin" to GlassFormula(panels = 1),
            "slidingdoor" to GlassFormula(panels = 2)
        )
        val isLocked = sub.isAppLocked(profile)
        if (isLocked) {
            prefs.edit()
                .remove("win_formulas_json")
                .remove("sliding_door_formulas_json")
                .remove("door_formulas_json")
                .remove("fix_formulas_json")
                .remove("opwin_formulas_json")
                .remove("f_section_formula_json")
                .remove("glass_formulas_json")
                .remove("win_formulas_b64")
                .remove("sliding_door_formulas_b64")
                .remove("door_formulas_b64")
                .remove("fix_formulas_b64")
                .remove("opwin_formulas_b64")
                .remove("f_section_formula_b64")
                .remove("glass_formulas_b64")
                .apply()
            masterEngineManager.clearRuntimeFormulas()
        }
        val canUseCachedFormulas = profile.isRegistered && !isLocked
        val cachedCloud = if (canUseCachedFormulas) masterEngineManager.cloudConfig.value else null
        val restoredWinFormulas = if (canUseCachedFormulas) {
            BossSerializationHelper.deserializeWindowFormulas(
                decodeBase64Pref("win_formulas_b64"),
                cachedCloud?.winFormulas ?: WindowFormulaSettings()
            )
        } else WindowFormulaSettings()
        val restoredSdFormulas = if (canUseCachedFormulas) {
            BossSerializationHelper.deserializeSlidingDoorFormulas(
                decodeBase64Pref("sliding_door_formulas_b64"),
                cachedCloud?.slidingDoorFormulas ?: SlidingDoorFormulaSettings()
            )
        } else SlidingDoorFormulaSettings()
        val restoredDoorFormulas = if (canUseCachedFormulas) {
            BossSerializationHelper.deserializeCategoryFormulas(
                decodeBase64Pref("door_formulas_b64"),
                cachedCloud?.doorFormulas ?: FormulaCategorySettings()
            )
        } else FormulaCategorySettings()
        val restoredFixFormulas = if (canUseCachedFormulas) {
            BossSerializationHelper.deserializeCategoryFormulas(
                decodeBase64Pref("fix_formulas_b64"),
                cachedCloud?.fixFormulas ?: FormulaCategorySettings()
            )
        } else FormulaCategorySettings()
        val restoredOpwinFormulas = if (canUseCachedFormulas) {
            BossSerializationHelper.deserializeCategoryFormulas(
                decodeBase64Pref("opwin_formulas_b64"),
                cachedCloud?.openableWinFormulas ?: FormulaCategorySettings.defaultOpenableWindowSettings()
            )
        } else FormulaCategorySettings.defaultOpenableWindowSettings()
        val restoredFSectionFormula = if (canUseCachedFormulas) {
            BossSerializationHelper.deserializeFSectionFormula(
                decodeBase64Pref("f_section_formula_b64"),
                cachedCloud?.fSectionFormula ?: FSectionFormula()
            )
        } else FSectionFormula()
        val restoredGlassFormulas = if (canUseCachedFormulas) {
            BossSerializationHelper.deserializeGlassFormulas(
                decodeBase64Pref("glass_formulas_b64"),
                cachedCloud?.glassFormulas ?: defaultGlassFormulas
            )
        } else defaultGlassFormulas

        val savedQuotesJson = prefs.getString("saved_quotations_json", null)
        val savedQuotes = BossSerializationHelper.deserializeSavedQuotations(savedQuotesJson)

        val savedPresetName = prefs.getString("theme_preset", ThemePreset.BOSS_GOLD.name) ?: ThemePreset.BOSS_GOLD.name
        val preset = try { ThemePreset.valueOf(savedPresetName) } catch (_: Exception) { ThemePreset.BOSS_GOLD }
        val baseThemeConfig = ThemeConfigEngine.getPresetConfig(preset)
        val savedTabPosName = prefs.getString("theme_tab_pos", baseThemeConfig.tabPosition.name) ?: baseThemeConfig.tabPosition.name
        val savedBtnStyleName = prefs.getString("theme_btn_style", baseThemeConfig.buttonStyle.name) ?: baseThemeConfig.buttonStyle.name
        val savedFontWeightName = prefs.getString("theme_font_weight", baseThemeConfig.fontWeightMode.name) ?: baseThemeConfig.fontWeightMode.name
        val savedFontScale = prefs.getFloat("theme_font_scale", -1f)
        val restoredThemeConfig = baseThemeConfig.copy(
            customFontScale = if (savedFontScale > 0f) savedFontScale else baseThemeConfig.customFontScale,
            isHighContrastText = prefs.getBoolean("theme_high_contrast", baseThemeConfig.isHighContrastText),
            tabPosition = try { TabBarPosition.valueOf(savedTabPosName) } catch (_: Exception) { baseThemeConfig.tabPosition },
            buttonStyle = try { ButtonStyleType.valueOf(savedBtnStyleName) } catch (_: Exception) { baseThemeConfig.buttonStyle },
            fontWeightMode = try { FontWeightMode.valueOf(savedFontWeightName) } catch (_: Exception) { baseThemeConfig.fontWeightMode },
            customBackgroundPath = prefs.getString("theme_custom_bg_path", baseThemeConfig.customBackgroundPath),
            customBackgroundDim = prefs.getFloat("theme_custom_bg_dim", baseThemeConfig.customBackgroundDim)
        )

        val defaultCompanyName = prefs.getString("company_name", null)
            ?: if (profile.isVipOwner) "BOSS ALUMINIUM & GLASS" else profile.shopAddress.ifBlank { "YOUR SHOP / BUSINESS NAME" }
        val defaultTagline = prefs.getString("company_tagline", null)
            ?: if (profile.isVipOwner) "STRONGER | SMARTER | BETTER" else "Aluminium & Glass Fabrication Works"

        return BossAppState(
            ctxGauge = prefs.getString("ctx_gauge", "1.6") ?: "1.6",
            ctxGaugeMode = prefs.getString("ctx_gauge_mode", "ON") ?: "ON",
            ctxColor = prefs.getString("ctx_color", "SAHARA / BROWN") ?: "SAHARA / BROWN",
            ctxFrameType = prefs.getString("ctx_frame_type", "C") ?: "C",
            ctxGlassGauge = prefs.getString("ctx_glass_gauge", "5") ?: "5",
            ctxGlassColor = prefs.getString("ctx_glass_color", "CLEAR") ?: "CLEAR",
            ctxGlassMode = prefs.getString("ctx_glass_mode", "ON") ?: "ON",
            glassFittingType = prefs.getString("glass_fitting_type", "FIX_RUBBER") ?: "FIX_RUBBER",
            measurementUnit = prefs.getString("measurement_unit", "INCH_SUTAR") ?: "INCH_SUTAR",
            letterheadMode = prefs.getBoolean("letterhead_mode", true),
            quotationBgOpacity = prefs.getFloat("quotation_bg_opacity", 0.92f),
            quotationLogoWatermarkOpacity = prefs.getFloat("quotation_watermark_opacity", 0.28f),
            availableGauges = initialGauges,
            availableColors = initialColors,
            availableGlassGauges = initialGlassGauges,
            availableGlassColors5mm = initialGlassColorsByGauge["5"] ?: initialGlass5,
            availableGlassColors8mm = initialGlassColorsByGauge["8"] ?: initialGlass8,
            availableGlassColorsByGauge = initialGlassColorsByGauge.toMap(),
            customSections = mergedCustomSections,
            deletedSections = savedDeletedSecs,
            rates = baseRates,
            glassRates = initialGlassRates,
            winFormulas = restoredWinFormulas,
            slidingDoorFormulas = restoredSdFormulas,
            doorFormulas = restoredDoorFormulas,
            fixFormulas = restoredFixFormulas,
            opwinFormulas = restoredOpwinFormulas,
            fSectionFormula = restoredFSectionFormula,
            glassFormulas = restoredGlassFormulas,
            alumDiscountPercent = prefs.getFloat("alum_discount_pct", 0f).toDouble(),
            autoIncludeHardware = prefs.getBoolean("auto_include_hw", true),
            windowHardwareRate = prefs.getFloat("win_hw_rate", 1500f).toDouble(),
            doorHardwareRate = prefs.getFloat("door_hw_rate", 2500f).toDouble(),
            fixHardwareRate = prefs.getFloat("fix_hw_rate", 800f).toDouble(),
            slidingDoorHardwareRate = prefs.getFloat("sd_hw_rate", 3000f).toDouble(),
            wastagePercent = 0.0,
            profitPercent = prefs.getFloat("profit_pct", 0f).toDouble(),
            taxPercent = 0.0,
            transportRent = prefs.getFloat("transport_rent", 0f).toDouble(),
            labourCost = prefs.getFloat("labour_cost", 0f).toDouble(),
            quotationTerms = prefs.getString(
                "quotation_terms",
                "1. 50% Advance payment required to start work.\n2. Balance 50% upon material delivery.\n3. Quotation valid for 7 days only."
            ) ?: "1. 50% Advance payment required to start work.\n2. Balance 50% upon material delivery.\n3. Quotation valid for 7 days only.",
            companyName = defaultCompanyName,
            companyTagline = defaultTagline,
            companyPhone = prefs.getString("company_phone", profile.phone) ?: profile.phone,
            companyAddress = prefs.getString("company_address", profile.shopAddress) ?: profile.shopAddress,
            companyNtn = prefs.getString("company_ntn", "") ?: "",
            customLogoPath = prefs.getString("custom_logo_path", null),
            customBackgroundPath = prefs.getString("custom_background_path", null),
            userProfile = profile,
            subscriptionState = sub,
            savedQuotations = savedQuotes,
            activeOfferBanner = offer,
            themeConfig = restoredThemeConfig
        )
    }

    private fun encodeBase64Pref(json: String): String =
        android.util.Base64.encodeToString(json.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)

    private fun decodeBase64Pref(key: String): String? {
        val b64 = prefs.getString(key, null) ?: return null
        return try {
            String(android.util.Base64.decode(b64, android.util.Base64.NO_WRAP), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveCachedFormulasToPrefs(
        win: WindowFormulaSettings,
        sd: SlidingDoorFormulaSettings,
        door: FormulaCategorySettings,
        fix: FormulaCategorySettings,
        opwin: FormulaCategorySettings,
        fSec: FSectionFormula,
        glass: Map<String, GlassFormula>
    ) {
        prefs.edit()
            .putString("win_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeWindowFormulas(win)))
            .putString("sliding_door_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeSlidingDoorFormulas(sd)))
            .putString("door_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeCategoryFormulas(door)))
            .putString("fix_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeCategoryFormulas(fix)))
            .putString("opwin_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeCategoryFormulas(opwin)))
            .putString("f_section_formula_b64", encodeBase64Pref(BossSerializationHelper.serializeFSectionFormula(fSec)))
            .putString("glass_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeGlassFormulas(glass)))
            .apply()
    }

    private fun applyRateOverridesToMap(
        baseRates: MutableMap<String, MutableMap<String, MutableMap<String, Double>>>,
        overrides: Map<String, Double>
    ) {
        if (overrides.isEmpty()) return
        overrides.forEach { (key, rateVal) ->
            val parts = key.split("|")
            if (parts.size == 3) {
                val g = parts[0]
                val col = parts[1]
                val sec = parts[2]
                baseRates.getOrPut(g) { mutableMapOf() }
                    .getOrPut(col) { mutableMapOf() }[sec] = rateVal
            } else {
                val sec = key.trim()
                baseRates.forEach { (g, colorMap) ->
                    val mult = (g.toDoubleOrNull() ?: 1.2) / 1.2
                    colorMap.forEach { (_, secMap) ->
                        secMap[sec] = (rateVal * mult).roundToLong().toDouble()
                    }
                }
            }
        }
    }

    private var lastServerVerifyEpochMs: Long = 0L

    fun isUserVerifiedAndSubscribed(): Boolean {
        val s = _state.value
        val prof = s.userProfile
        val sub = s.subscriptionState
        return prof.isRegistered && !sub.isAppLocked(prof)
    }

    fun verifyUserSecurityWithServerNow(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastServerVerifyEpochMs) < 15_000L) return
        lastServerVerifyEpochMs = now
        val prof = _state.value.userProfile
        if (!prof.isRegistered) return

        FirebaseCloudManager.verifyUserSecurityOnServer(
            context = getApplication(),
            profile = prof
        ) { serverTimeMs, isDeviceMismatch, isBlocked, blockReason, isActivated, isLifetime, expiryMs ->
            val localNow = System.currentTimeMillis()
            val clockTampered = (localNow < serverTimeMs - 10L * 60L * 1000L) && !prof.isVipOwner
            val currentSub = _state.value.subscriptionState
            val effectiveActivated = isActivated ?: currentSub.isActivated
            val effectiveExpiry = expiryMs ?: currentSub.activatedUntil
            val expiredOnServer = !isLifetime && !prof.isVipOwner && effectiveActivated && effectiveExpiry > 0L && effectiveExpiry <= serverTimeMs

            securityManager.applyRemoteSubscriptionUpdate(
                isActivated = effectiveActivated && !expiredOnServer && !isBlocked && !isDeviceMismatch,
                isLifetimeFree = isLifetime,
                isBlocked = isBlocked || isDeviceMismatch,
                blockReason = blockReason,
                trialStartTime = currentSub.trialStartTime,
                activatedUntil = if (isLifetime) 0L else effectiveExpiry
            )

            val refreshedSub = securityManager.loadSubscriptionState().copy(
                isDeviceMismatch = isDeviceMismatch,
                isClockTampered = clockTampered || currentSub.isClockTampered,
                lockReason = when {
                    isDeviceMismatch -> blockReason ?: "Single-Device Hardware Lock: Account bound to another device."
                    isBlocked -> blockReason ?: "Account blocked by Master Admin."
                    else -> null
                }
            )
            _state.update { it.copy(subscriptionState = refreshedSub) }

            if (refreshedSub.isAppLocked(prof)) {
                masterEngineManager.clearRuntimeFormulas()
                resetAllFormulasToDefault()
            } else if (_state.value.winFormulas.m23HeightDeduction.totalInches == 0.0) {
                syncMasterCloudConfigNow()
            }
        }
    }

    private fun startMasterLiveSyncListeners() {
        // 1. Verify User Security & Hardware Lock against `users` table with Server Timestamp
        verifyUserSecurityWithServerNow(force = true)

        // 2. Real-time Master Engine Config listener (Formulas loaded ONLY when verified & active)
        masterEngineManager.startRealtimeMasterSync(
            isVerifiedAndSubscribedProvider = { isUserVerifiedAndSubscribed() }
        ) { cloudConfig ->
            applyMasterCloudConfig(cloudConfig)
        }

        // 3. Real-time Broadcast Offer Banner listener
        masterAdminManager.startRealtimeOfferListener { offer ->
            _state.update { it.copy(activeOfferBanner = offer) }
        }

        // 4. Initial fetch from Supabase `app_configs` (key: `master_engine_config`)
        syncMasterCloudConfigNow()

        // 5. Real-time Account / Subscription / Lock status listener
        restartAccountWatcher()
    }

    private fun restartAccountWatcher() {
        val currentProfile = _state.value.userProfile
        if (currentProfile.isRegistered) {
            FirebaseCloudManager.startRealtimeAccountWatcher(
                deviceId = securityManager.getDeviceId(),
                phone = currentProfile.phone,
                cnic = currentProfile.cnic,
                email = currentProfile.email,
                onAccountStateChanged = { isActivated, isLifetimeFree, isBlocked, blockReason, trialStart, activatedUntil ->
                    securityManager.applyRemoteSubscriptionUpdate(
                        isActivated = isActivated,
                        isLifetimeFree = isLifetimeFree,
                        isBlocked = isBlocked,
                        blockReason = blockReason,
                        trialStartTime = trialStart,
                        activatedUntil = activatedUntil
                    )
                    _state.update {
                        it.copy(
                            userProfile = securityManager.loadUserProfile(),
                            subscriptionState = securityManager.loadSubscriptionState()
                        )
                    }
                }
            )
        }
    }

    fun syncMasterCloudConfigNow() {
        viewModelScope.launch {
            verifyUserSecurityWithServerNow(force = true)
            val cloudConfig = masterEngineManager.fetchLatestFromCloud(isVerifiedAndSubscribed = isUserVerifiedAndSubscribed())
            if (cloudConfig != null) {
                applyMasterCloudConfig(cloudConfig)
            }
        }
    }

    private fun decodeBase64ToInternalFile(base64OrDataUri: String, fileName: String): String? {
        if (base64OrDataUri.isBlank()) return null
        return try {
            val cleanBase64 = base64OrDataUri.substringAfter("base64,", base64OrDataUri).trim()
            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            if (bytes.isEmpty()) return null
            val outFile = java.io.File(getApplication<Application>().filesDir, fileName)
            outFile.writeBytes(bytes)
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun clearAllRateListData() {
        val editor = prefs.edit()
        val existingKeys = prefs.getStringSet("user_custom_rate_keys", emptySet()) ?: emptySet()
        existingKeys.forEach { k ->
            val parts = k.split("|")
            if (parts.size == 3) {
                editor.remove("user_sec_rate_${parts[0]}_${parts[1]}_${parts[2]}")
            }
        }
        editor.remove("user_custom_rate_keys").apply()

        _state.update { cur ->
            val zeroedRates = cur.rates.mapValues { (_, colMap) ->
                colMap.mapValues { (_, secMap) ->
                    secMap.mapValues { 0.0 }
                }
            }
            val zeroedGlassRates = cur.glassRates.mapValues { (_, colMap) ->
                colMap.mapValues { 0.0 }
            }
            cur.copy(
                rates = zeroedRates,
                glassRates = zeroedGlassRates
            )
        }
    }

    private fun applyMasterCloudConfig(cloudConfig: MasterEngineCloudConfig) {
        val currentProf = _state.value.userProfile
        val currentSub = _state.value.subscriptionState
        if (currentSub.isAppLocked(currentProf)) {
            masterEngineManager.clearRuntimeFormulas()
            resetAllFormulasToDefault()
            return
        }
        if (!currentProf.isRegistered) {
            return
        }

        val lastClearTs = prefs.getLong("last_admin_clear_rate_ts", 0L)
        if (cloudConfig.clearRateListCommand || (cloudConfig.clearRateListTimestamp > 0L && cloudConfig.clearRateListTimestamp > lastClearTs)) {
            if (cloudConfig.clearRateListTimestamp > 0L) {
                prefs.edit().putLong("last_admin_clear_rate_ts", cloudConfig.clearRateListTimestamp).apply()
            }
            clearAllRateListData()
        }

        val adminLogoPath = if (cloudConfig.mainLogoBase64.isNotBlank()) {
            decodeBase64ToInternalFile(cloudConfig.mainLogoBase64, "admin_golden_lion_logo.png")
        } else null
        val adminLetterheadPath = if (cloudConfig.quotationLetterheadBase64.isNotBlank()) {
            decodeBase64ToInternalFile(cloudConfig.quotationLetterheadBase64, "admin_quotation_letterhead_bg.png")
        } else null

        _state.update { cur ->
            val updatedRates = cur.rates.mapValues { (_, colorMap) ->
                colorMap.mapValues { (_, secMap) -> secMap.toMutableMap() }.toMutableMap()
            }.toMutableMap()

            cloudConfig.rates?.forEach { (g, colMap) ->
                val targetG = updatedRates.getOrPut(g) { mutableMapOf() }
                colMap.forEach { (col, secMap) ->
                    val targetCol = targetG.getOrPut(col) { mutableMapOf() }
                    secMap.forEach { (sec, r) ->
                        if (r >= 0.0) targetCol[sec] = r
                    }
                }
            }

            applyRateOverridesToMap(updatedRates, cloudConfig.rateOverrides)

            val updatedGlassRates = cur.glassRates.mapValues { (_, colMap) -> colMap.toMutableMap() }.toMutableMap()
            cloudConfig.glassRates?.forEach { (g, colMap) ->
                val targetG = updatedGlassRates.getOrPut(g) { mutableMapOf() }
                colMap.forEach { (col, r) ->
                    if (r >= 0.0) targetG[col] = r
                }
            }

            cloudConfig.customSections.filter { it.isActive }.forEach { dyn ->
                if (dyn.defaultRate > 0) {
                    updatedRates.forEach { (g, colorMap) ->
                        val ratio = (g.toDoubleOrNull() ?: 1.2) / (dyn.defaultGauge.toDoubleOrNull() ?: 1.6).coerceAtLeast(1.0)
                        colorMap.forEach { (_, secMap) ->
                            secMap[dyn.code] = (dyn.defaultRate * ratio).roundToLong().toDouble()
                        }
                    }
                }
            }

            val dynCodes = cloudConfig.customSections.filter { it.isActive }.map { it.code }
            val upperDeleted = cur.deletedSections.map { it.trim().uppercase() }.toSet()
            val mergedCustomSecs = (cur.customSections + dynCodes).distinct().filterNot {
                cur.deletedSections.contains(it) || upperDeleted.contains(it.trim().uppercase())
            }
            if (cur.deletedSections.isNotEmpty()) {
                updatedRates.forEach { (_, colorMap) ->
                    colorMap.forEach { (_, secMap) ->
                        secMap.keys.removeAll { k -> cur.deletedSections.contains(k) || upperDeleted.contains(k.trim().uppercase()) }
                    }
                }
            }

            // Re-apply user-saved local rate overrides so user defaults are never lost
            val customRateKeys = prefs.getStringSet("user_custom_rate_keys", emptySet()) ?: emptySet()
            customRateKeys.forEach { key ->
                val parts = key.split("|")
                if (parts.size == 3) {
                    val g = parts[0]
                    val col = parts[1]
                    val sec = parts[2]
                    val r = prefs.getFloat("user_sec_rate_${g}_${col}_$sec", -1f)
                    if (r >= 0f && !cur.deletedSections.contains(sec)) {
                        updatedRates.getOrPut(g) { mutableMapOf() }
                            .getOrPut(col) { mutableMapOf() }[sec] = r.toDouble()
                    }
                }
            }

            var updatedWinFormulas = cloudConfig.winFormulas ?: cur.winFormulas
            var updatedSdFormulas = cloudConfig.slidingDoorFormulas ?: cur.slidingDoorFormulas
            val updatedDoorFormulas = cloudConfig.doorFormulas ?: cur.doorFormulas
            val updatedFixFormulas = cloudConfig.fixFormulas ?: cur.fixFormulas
            val updatedOpwinFormulas = cloudConfig.openableWinFormulas ?: cur.opwinFormulas
            var updatedFSectionFormula = cloudConfig.fSectionFormula ?: cur.fSectionFormula
            val updatedGlassFormulas = cloudConfig.glassFormulas ?: cur.glassFormulas

            val fa = cloudConfig.formulaAdjustments
            if (fa.isNotEmpty()) {
                val m23Inch = fa["win_m23_h_inch"] ?: updatedWinFormulas.m23HeightDeduction.inch
                val m23Sutar = fa["win_m23_h_sutar"] ?: updatedWinFormulas.m23HeightDeduction.sutar
                val m24Inch = fa["win_m24_w_inch"] ?: updatedWinFormulas.m24WidthDeduction.inch
                val m24Sutar = fa["win_m24_w_sutar"] ?: updatedWinFormulas.m24WidthDeduction.sutar
                val d29HInch = fa["win_d29_h_inch"] ?: updatedWinFormulas.d29HeightDeduction.inch
                val d29HSutar = fa["win_d29_h_sutar"] ?: updatedWinFormulas.d29HeightDeduction.sutar
                val d29WInch = fa["win_d29_w_inch"] ?: updatedWinFormulas.d29WidthDeduction.inch
                val d29WSutar = fa["win_d29_w_sutar"] ?: updatedWinFormulas.d29WidthDeduction.sutar
                val pushSutar = fa["push_handle_d29_sutar"] ?: updatedWinFormulas.d29PushHandleDeduction.sutar

                updatedWinFormulas = updatedWinFormulas.copy(
                    m23HeightDeduction = DeductionDim(m23Inch, m23Sutar),
                    m24WidthDeduction = DeductionDim(m24Inch, m24Sutar),
                    d29HeightDeduction = DeductionDim(d29HInch, d29HSutar),
                    d29WidthDeduction = DeductionDim(d29WInch, d29WSutar),
                    d29PushHandleDeduction = DeductionDim(0.0, pushSutar)
                )

                val sdM23Inch = fa["sd_m23_h_inch"] ?: updatedSdFormulas.m23HeightDeduction.inch
                val sdM23Sutar = fa["sd_m23_h_sutar"] ?: updatedSdFormulas.m23HeightDeduction.sutar
                val sdM24Inch = fa["sd_m24_w_inch"] ?: updatedSdFormulas.m24WidthDeduction.inch
                val sdM24Sutar = fa["sd_m24_w_sutar"] ?: updatedSdFormulas.m24WidthDeduction.sutar
                updatedSdFormulas = updatedSdFormulas.copy(
                    m23HeightDeduction = DeductionDim(sdM23Inch, sdM23Sutar),
                    m24WidthDeduction = DeductionDim(sdM24Inch, sdM24Sutar),
                    d29PushHandleDeduction = DeductionDim(0.0, pushSutar)
                )

                val fInch = fa["f_sec_h_inch"] ?: updatedFSectionFormula.heightDim.inch
                val fSutar = fa["f_sec_h_sutar"] ?: updatedFSectionFormula.heightDim.sutar
                updatedFSectionFormula = updatedFSectionFormula.copy(
                    heightDim = DeductionDim(fInch, fSutar),
                    widthDim = DeductionDim(fInch, fSutar)
                )
            }

            saveCachedFormulasToPrefs(
                win = updatedWinFormulas,
                sd = updatedSdFormulas,
                door = updatedDoorFormulas,
                fix = updatedFixFormulas,
                opwin = updatedOpwinFormulas,
                fSec = updatedFSectionFormula,
                glass = updatedGlassFormulas
            )

            cur.copy(
                rates = updatedRates,
                glassRates = updatedGlassRates,
                customSections = mergedCustomSecs,
                winFormulas = updatedWinFormulas,
                slidingDoorFormulas = updatedSdFormulas,
                doorFormulas = updatedDoorFormulas,
                fixFormulas = updatedFixFormulas,
                opwinFormulas = updatedOpwinFormulas,
                fSectionFormula = updatedFSectionFormula,
                glassFormulas = updatedGlassFormulas,
                dragDropBlueprints = if (cloudConfig.dragDropBlueprints.isNotEmpty()) cloudConfig.dragDropBlueprints else cur.dragDropBlueprints,
                customLogoPath = adminLogoPath ?: cur.customLogoPath,
                customBackgroundPath = adminLetterheadPath ?: cur.customBackgroundPath,
                activeOfferBanner = cloudConfig.activeBanner ?: cur.activeOfferBanner
            )
        }
    }

    fun selectTab(tab: ClientTab) {
        _state.update { it.copy(activeTab = tab) }
    }

    fun setSettingsSubTab(subTab: String) {
        _state.update { it.copy(settingsSubTab = subTab) }
    }

    fun setGlassFittingType(type: String) {
        prefs.edit().putString("glass_fitting_type", type).apply()
        _state.update { it.copy(glassFittingType = type) }
    }

    fun setLetterheadMode(enabled: Boolean) {
        prefs.edit().putBoolean("letterhead_mode", enabled).apply()
        _state.update { it.copy(letterheadMode = enabled) }
    }

    fun setQuotationBgSettings(bgOpacity: Float, watermarkOpacity: Float) {
        val cleanBg = bgOpacity.coerceIn(0.2f, 1.0f)
        val cleanWm = watermarkOpacity.coerceIn(0.0f, 0.8f)
        prefs.edit()
            .putFloat("quotation_bg_opacity", cleanBg)
            .putFloat("quotation_watermark_opacity", cleanWm)
            .apply()
        _state.update {
            it.copy(
                quotationBgOpacity = cleanBg,
                quotationLogoWatermarkOpacity = cleanWm
            )
        }
    }

    fun updateThemeConfig(newTheme: ThemeConfig) {
        prefs.edit()
            .putString("theme_preset", newTheme.preset.name)
            .putFloat("theme_font_scale", newTheme.customFontScale ?: -1f)
            .putBoolean("theme_high_contrast", newTheme.isHighContrastText)
            .putString("theme_tab_pos", newTheme.tabPosition.name)
            .putString("theme_btn_style", newTheme.buttonStyle.name)
            .putString("theme_font_weight", newTheme.fontWeightMode.name)
            .putString("theme_custom_bg_path", newTheme.customBackgroundPath)
            .putFloat("theme_custom_bg_dim", newTheme.customBackgroundDim)
            .apply()
        _state.update { it.copy(themeConfig = newTheme) }
    }

    fun setMasterGauge(gaugeOrOff: String) {
        if (gaugeOrOff == "OFF") {
            prefs.edit().putString("ctx_gauge_mode", "OFF").apply()
            _state.update { it.copy(ctxGaugeMode = "OFF") }
        } else {
            prefs.edit()
                .putString("ctx_gauge", gaugeOrOff)
                .putString("ctx_gauge_mode", "ON")
                .apply()
            _state.update { cur ->
                cur.copy(
                    ctxGauge = gaugeOrOff,
                    ctxGaugeMode = "ON",
                    windows = cur.windows.map { it.copy(sectionGauge = gaugeOrOff) },
                    doors = cur.doors.map { it.copy(sectionGauge = gaugeOrOff) },
                    fixframes = cur.fixframes.map { it.copy(sectionGauge = gaugeOrOff) },
                    openableWindows = cur.openableWindows.map { it.copy(sectionGauge = gaugeOrOff) },
                    slidingDoors = cur.slidingDoors.map { it.copy(sectionGauge = gaugeOrOff) }
                )
            }
        }
    }

    fun setMasterColor(color: String) {
        prefs.edit().putString("ctx_color", color).apply()
        _state.update { it.copy(ctxColor = color) }
    }

    fun setMasterFrameType(frameType: String) {
        prefs.edit().putString("ctx_frame_type", frameType).apply()
        _state.update { cur ->
            if (frameType == "OFF") {
                cur.copy(ctxFrameType = "OFF")
            } else {
                cur.copy(
                    ctxFrameType = frameType,
                    windows = cur.windows.map {
                        it.copy(
                            frameType = frameType,
                            outerSection = if (frameType == "F") "DC30F" else if (frameType == "C") "DC30C" else frameType
                        )
                    },
                    slidingDoors = cur.slidingDoors.map {
                        it.copy(
                            outer = when (frameType) {
                                "F" -> "F"
                                "C" -> "NORMAL"
                                else -> frameType
                            }
                        )
                    }
                )
            }
        }
    }

    fun setMasterGlass(gauge: String, color: String, mode: String) {
        if (mode == "OFF") {
            prefs.edit().putString("ctx_glass_mode", "OFF").apply()
            _state.update { it.copy(ctxGlassMode = "OFF") }
        } else {
            prefs.edit()
                .putString("ctx_glass_gauge", gauge)
                .putString("ctx_glass_color", color)
                .putString("ctx_glass_mode", "ON")
                .apply()
            _state.update { cur ->
                cur.copy(
                    ctxGlassGauge = gauge,
                    ctxGlassColor = color,
                    ctxGlassMode = "ON",
                    windows = cur.windows.map { it.copy(glassGauge = gauge, glassColor = color) },
                    doors = cur.doors.map { it.copy(glassGauge = gauge, glassColor = color) },
                    fixframes = cur.fixframes.map { it.copy(glassGauge = gauge, glassColor = color) },
                    openableWindows = cur.openableWindows.map { it.copy(glassGauge = gauge, glassColor = color) },
                    slidingDoors = cur.slidingDoors.map { it.copy(glassGauge = gauge, glassColor = color) }
                )
            }
        }
    }

    fun setMeasurementUnit(unit: String) {
        prefs.edit().putString("measurement_unit", unit).apply()
        _state.update { it.copy(measurementUnit = unit) }
    }

    fun resetCurrentJob() {
        _state.update { cur ->
            cur.copy(
                windows = listOf(
                    WindowItem(
                        id = UUID.randomUUID().toString(),
                        width = 48.0,
                        height = 48.0,
                        qty = 1,
                        frameType = if (cur.ctxFrameType == "OFF") "C" else cur.ctxFrameType,
                        sectionGauge = cur.ctxGauge,
                        glassGauge = cur.ctxGlassGauge,
                        glassColor = cur.ctxGlassColor
                    )
                ),
                doors = emptyList(),
                fixframes = emptyList(),
                openableWindows = emptyList(),
                slidingDoors = emptyList(),
                projectExtras = emptyList(),
                customerName = "",
                customerPhone = ""
            )
        }
    }

    // Windows CRUD
    fun addWindow() {
        _state.update { cur ->
            val newWin = WindowItem(
                id = UUID.randomUUID().toString(),
                width = 48.0,
                height = 48.0,
                qty = 1,
                frameType = if (cur.ctxFrameType == "OFF") "C" else cur.ctxFrameType,
                sectionGauge = cur.ctxGauge,
                glassGauge = cur.ctxGlassGauge,
                glassColor = cur.ctxGlassColor
            )
            cur.copy(windows = cur.windows + newWin)
        }
    }

    fun updateWindow(updated: WindowItem) {
        _state.update { cur ->
            cur.copy(windows = cur.windows.map { if (it.id == updated.id) updated else it })
        }
    }

    fun removeWindow(id: String) {
        _state.update { cur ->
            cur.copy(windows = cur.windows.filterNot { it.id == id })
        }
    }

    // Doors CRUD
    fun addDoor() {
        _state.update { cur ->
            val newDoor = DoorItem(
                id = UUID.randomUUID().toString(),
                width = 36.0,
                height = 84.0,
                qty = 1,
                outer = "D54A",
                sectionGauge = cur.ctxGauge,
                glassGauge = cur.ctxGlassGauge,
                glassColor = cur.ctxGlassColor
            )
            cur.copy(doors = cur.doors + newDoor)
        }
    }

    fun updateDoor(updated: DoorItem) {
        _state.update { cur ->
            cur.copy(doors = cur.doors.map { if (it.id == updated.id) updated else it })
        }
    }

    fun removeDoor(id: String) {
        _state.update { cur ->
            cur.copy(doors = cur.doors.filterNot { it.id == id })
        }
    }

    // Fix Frames CRUD
    fun addFixFrame() {
        _state.update { cur ->
            val item = FixFrameItem(
                id = UUID.randomUUID().toString(),
                width = 48.0,
                height = 48.0,
                qty = 1,
                outer = "D54A",
                sectionGauge = cur.ctxGauge,
                glassGauge = cur.ctxGlassGauge,
                glassColor = cur.ctxGlassColor
            )
            cur.copy(fixframes = cur.fixframes + item)
        }
    }

    fun updateFixFrame(updated: FixFrameItem) {
        _state.update { cur ->
            cur.copy(fixframes = cur.fixframes.map { if (it.id == updated.id) updated else it })
        }
    }

    fun removeFixFrame(id: String) {
        _state.update { cur ->
            cur.copy(fixframes = cur.fixframes.filterNot { it.id == id })
        }
    }

    // Openable Windows CRUD
    fun addOpenableWindow() {
        _state.update { cur ->
            val item = OpenableWindowItem(
                id = UUID.randomUUID().toString(),
                width = 36.0,
                height = 48.0,
                qty = 1,
                outer = "D54A",
                sectionGauge = cur.ctxGauge,
                glassGauge = cur.ctxGlassGauge,
                glassColor = cur.ctxGlassColor
            )
            cur.copy(openableWindows = cur.openableWindows + item)
        }
    }

    fun updateOpenableWindow(updated: OpenableWindowItem) {
        _state.update { cur ->
            cur.copy(openableWindows = cur.openableWindows.map { if (it.id == updated.id) updated else it })
        }
    }

    fun removeOpenableWindow(id: String) {
        _state.update { cur ->
            cur.copy(openableWindows = cur.openableWindows.filterNot { it.id == id })
        }
    }

    // Sliding Doors CRUD
    fun addSlidingDoor() {
        _state.update { cur ->
            val item = SlidingDoorItem(
                id = UUID.randomUUID().toString(),
                width = 72.0,
                height = 84.0,
                qty = 1,
                outer = "NORMAL",
                sectionGauge = cur.ctxGauge,
                glassGauge = cur.ctxGlassGauge,
                glassColor = cur.ctxGlassColor
            )
            cur.copy(slidingDoors = cur.slidingDoors + item)
        }
    }

    fun updateSlidingDoor(updated: SlidingDoorItem) {
        _state.update { cur ->
            cur.copy(slidingDoors = cur.slidingDoors.map { if (it.id == updated.id) updated else it })
        }
    }

    fun removeSlidingDoor(id: String) {
        _state.update { cur ->
            cur.copy(slidingDoors = cur.slidingDoors.filterNot { it.id == id })
        }
    }

    // Project Extra Pieces CRUD
    fun addProjectExtra(
        section: String,
        lengthFt: Double,
        qty: Int,
        gauge: String = _state.value.ctxGauge
    ) {
        _state.update { cur ->
            val extra = ExtraPiece(
                section = section.trim().uppercase(),
                lengthFt = lengthFt,
                qty = qty,
                gauge = gauge
            )
            cur.copy(projectExtras = cur.projectExtras + extra)
        }
    }

    fun removeProjectExtra(index: Int) {
        _state.update { cur ->
            if (index in cur.projectExtras.indices) {
                val mutable = cur.projectExtras.toMutableList()
                mutable.removeAt(index)
                cur.copy(projectExtras = mutable)
            } else cur
        }
    }

    fun clearProjectExtras() {
        _state.update { it.copy(projectExtras = emptyList()) }
    }

    // Custom Sections & Section Deletion
    fun addCustomSection(sectionName: String, rate12mm: Double) {
        val clean = sectionName.trim().uppercase()
        if (clean.isBlank()) return
        val effectiveRate = if (rate12mm > 0) rate12mm else 250.0
        masterAdminManager.addGlobalCustomSection(clean)
        if (rate12mm > 0) {
            masterAdminManager.saveGlobalRateOverride(clean, rate12mm)
        }
        _state.update { cur ->
            val updatedDeleted = cur.deletedSections - clean
            prefs.edit().putStringSet("deleted_sections_set", updatedDeleted).apply()
            val updatedCustom = (cur.customSections + clean).distinct()
            val updatedRates = cur.rates.mapValues { (g, colorMap) ->
                val mult = (g.toDoubleOrNull() ?: 1.2) / 1.2
                colorMap.mapValues { (_, secMap) ->
                    val m = secMap.toMutableMap()
                    m[clean] = (effectiveRate * mult).roundToLong().toDouble()
                    m
                }
            }
            cur.copy(
                customSections = updatedCustom,
                deletedSections = updatedDeleted,
                hiddenSections = cur.hiddenSections - clean,
                rates = updatedRates
            )
        }
    }

    fun addCustomSectionPair(sec1: String, rate1: Double, sec2: String, rate2: Double): String {
        val s1 = sec1.trim().uppercase()
        val s2 = sec2.trim().uppercase()
        addCustomSection(s1, rate1)
        if (s2.isNotBlank() && s2 != s1) {
            addCustomSection(s2, rate2)
        }
        val pairLabel = if (s2.isNotBlank() && s2 != s1) "$s1 / $s2" else s1
        _state.update { cur ->
            val updatedDeleted = cur.deletedSections - pairLabel
            prefs.edit().putStringSet("deleted_sections_set", updatedDeleted).apply()
            cur.copy(
                customSections = (cur.customSections + pairLabel).distinct(),
                deletedSections = updatedDeleted
            )
        }
        return pairLabel
    }

    fun removeSection(sectionName: String) {
        removeMultipleSections(setOf(sectionName))
    }

    fun removeMultipleSections(sectionsToRemove: Set<String>) {
        if (sectionsToRemove.isEmpty()) return
        val cleanedSet = mutableSetOf<String>()
        sectionsToRemove.forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isNotEmpty()) {
                cleanedSet.add(trimmed)
                cleanedSet.add(trimmed.uppercase())
                if (trimmed.contains("/")) {
                    trimmed.split("/").map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
                        cleanedSet.add(part)
                        cleanedSet.add(part.uppercase())
                    }
                }
            }
        }
        cleanedSet.forEach { sec ->
            masterAdminManager.removeGlobalCustomSection(sec)
        }
        _state.update { cur ->
            val updatedDeleted = cur.deletedSections + cleanedSet
            prefs.edit().putStringSet("deleted_sections_set", updatedDeleted).apply()
            val updatedCustom = cur.customSections.filterNot { cleanedSet.contains(it) || cleanedSet.contains(it.uppercase()) }
            val updatedRates = cur.rates.mapValues { (_, colorMap) ->
                colorMap.mapValues { (_, secMap) ->
                    secMap.filterKeys { k -> !cleanedSet.contains(k) && !cleanedSet.contains(k.uppercase()) }
                }
            }
            val nextFrameType = if (cleanedSet.contains(cur.ctxFrameType)) "C" else cur.ctxFrameType
            cur.copy(
                customSections = updatedCustom,
                deletedSections = updatedDeleted,
                hiddenSections = cur.hiddenSections - cleanedSet,
                rates = updatedRates,
                ctxFrameType = nextFrameType
            )
        }
    }

    fun removeCustomSection(sectionName: String) {
        removeSection(sectionName)
    }

    fun restoreDeletedSection(sectionName: String) {
        val clean = sectionName.trim().uppercase()
        if (clean.isBlank()) return
        val defaultInitialRates = BossDefaultRates.getInitialRates()
        _state.update { cur ->
            val updatedDeleted = cur.deletedSections - sectionName - clean
            prefs.edit().putStringSet("deleted_sections_set", updatedDeleted).apply()
            val updatedRates = cur.rates.mapValues { (g, colorMap) ->
                colorMap.mapValues { (c, secMap) ->
                    val m = secMap.toMutableMap()
                    if (!m.containsKey(clean)) {
                        val defRate = defaultInitialRates[g]?.get(c)?.get(clean)
                            ?: defaultInitialRates["1.2"]?.get("SAHARA / BROWN")?.get(clean)
                            ?: 250.0
                        m[clean] = defRate
                    }
                    m.toMap()
                }
            }
            cur.copy(
                deletedSections = updatedDeleted,
                rates = updatedRates
            )
        }
    }

    fun restoreAllDeletedSections() {
        val defaultInitialRates = BossDefaultRates.getInitialRates()
        prefs.edit().remove("deleted_sections_set").apply()
        _state.update { cur ->
            val toRestore = cur.deletedSections
            val updatedRates = cur.rates.mapValues { (g, colorMap) ->
                colorMap.mapValues { (c, secMap) ->
                    val m = secMap.toMutableMap()
                    toRestore.forEach { sec ->
                        val clean = sec.trim().uppercase()
                        if (!m.containsKey(clean)) {
                            val defRate = defaultInitialRates[g]?.get(c)?.get(clean)
                                ?: defaultInitialRates["1.2"]?.get("SAHARA / BROWN")?.get(clean)
                                ?: 250.0
                            m[clean] = defRate
                        }
                    }
                    m.toMap()
                }
            }
            cur.copy(
                deletedSections = emptySet(),
                rates = updatedRates
            )
        }
    }

    fun unhideAllSections() {
        _state.update { it.copy(hiddenSections = emptySet()) }
    }

    fun unhideSection(section: String) {
        _state.update { it.copy(hiddenSections = it.hiddenSections - section) }
    }

    fun hideSection(section: String) {
        _state.update { it.copy(hiddenSections = it.hiddenSections + section) }
    }

    fun resetRatesToDefault() {
        val customRateKeys = prefs.getStringSet("user_custom_rate_keys", emptySet()) ?: emptySet()
        val editor = prefs.edit()
            .remove("deleted_sections_set")
            .remove("saved_available_gauges")
            .remove("saved_available_colors")
            .remove("saved_available_glass_gauges")
            .remove("saved_glass_colors_5mm")
            .remove("saved_glass_colors_8mm")
            .remove("saved_glass_colors_by_gauge")
            .remove("user_custom_rate_keys")
        customRateKeys.forEach { key ->
            val parts = key.split("|")
            if (parts.size == 3) {
                editor.remove("user_sec_rate_${parts[0]}_${parts[1]}_${parts[2]}")
            }
        }
        editor.apply()
        _state.update {
            it.copy(
                deletedSections = emptySet(),
                hiddenSections = emptySet(),
                availableGauges = listOf("1.2", "1.4", "1.6", "2.0"),
                availableColors = BossDefaultRates.DEFAULT_COLORS,
                availableGlassGauges = listOf("5", "8"),
                availableGlassColors5mm = BossDefaultRates.DEFAULT_GLASS_COLORS_5MM,
                availableGlassColors8mm = BossDefaultRates.DEFAULT_GLASS_COLORS_8MM,
                availableGlassColorsByGauge = mapOf(
                    "5" to BossDefaultRates.DEFAULT_GLASS_COLORS_5MM,
                    "8" to BossDefaultRates.DEFAULT_GLASS_COLORS_8MM
                ),
                rates = BossDefaultRates.getInitialRates(),
                glassRates = BossDefaultRates.getInitialGlassRates()
            )
        }
    }

    fun restoreDefaultGaugesAndColors() {
        prefs.edit()
            .remove("saved_available_gauges")
            .remove("saved_available_colors")
            .apply()
        _state.update { cur ->
            val defaultGauges = listOf("1.2", "1.4", "1.6", "2.0")
            val defaultColors = BossDefaultRates.DEFAULT_COLORS
            val baseRates = BossDefaultRates.getInitialRates()
            cur.copy(
                availableGauges = defaultGauges,
                availableColors = defaultColors,
                rates = baseRates,
                ctxGauge = if (defaultGauges.contains(cur.ctxGauge)) cur.ctxGauge else "1.6",
                ctxColor = if (defaultColors.contains(cur.ctxColor)) cur.ctxColor else "SAHARA / BROWN"
            )
        }
    }

    fun addCustomGlassGauge(gaugeName: String) {
        val clean = gaugeName.trim().removeSuffix("mm").removeSuffix("MM").trim()
        if (clean.isBlank()) return
        _state.update { cur ->
            val updatedGlassGauges = (cur.availableGlassGauges + clean).distinct()
            val updatedByGauge = cur.availableGlassColorsByGauge.toMutableMap()
            val defaultColorsForGauge = updatedByGauge[clean]
                ?: cur.availableGlassColorsByGauge["5"]
                ?: BossDefaultRates.DEFAULT_GLASS_COLORS_5MM
            updatedByGauge[clean] = defaultColorsForGauge

            val updatedGlassRates = cur.glassRates.toMutableMap()
            if (!updatedGlassRates.containsKey(clean)) {
                val mult = (clean.toDoubleOrNull() ?: 5.0) / 5.0
                val templateMap = cur.glassRates["5"] ?: BossDefaultRates.getInitialGlassRates()["5"] ?: emptyMap()
                val newMap = defaultColorsForGauge.associateWith { col ->
                    val baseR = templateMap[col] ?: 230.0
                    (baseR * mult).roundToLong().toDouble()
                }
                updatedGlassRates[clean] = newMap
            }

            prefs.edit()
                .putString("saved_available_glass_gauges", updatedGlassGauges.joinToString("|"))
                .putString("saved_glass_colors_by_gauge", BossSerializationHelper.serializeGlassColorsByGauge(updatedByGauge))
                .putString("ctx_glass_gauge", clean)
                .apply()

            cur.copy(
                availableGlassGauges = updatedGlassGauges,
                availableGlassColorsByGauge = updatedByGauge,
                glassRates = updatedGlassRates,
                ctxGlassGauge = clean
            )
        }
    }

    fun removeGlassGauge(gaugeName: String) {
        val clean = gaugeName.trim().removeSuffix("mm").removeSuffix("MM").trim()
        _state.update { cur ->
            if (cur.availableGlassGauges.size <= 1) return@update cur
            val updatedGlassGauges = cur.availableGlassGauges.filterNot { it.equals(clean, ignoreCase = true) }
            val updatedByGauge = cur.availableGlassColorsByGauge.filterKeys { !it.equals(clean, ignoreCase = true) }
            val updatedGlassRates = cur.glassRates.filterKeys { !it.equals(clean, ignoreCase = true) }
            val nextActive = if (cur.ctxGlassGauge.equals(clean, ignoreCase = true)) {
                updatedGlassGauges.firstOrNull() ?: "5"
            } else cur.ctxGlassGauge

            prefs.edit()
                .putString("saved_available_glass_gauges", updatedGlassGauges.joinToString("|"))
                .putString("saved_glass_colors_by_gauge", BossSerializationHelper.serializeGlassColorsByGauge(updatedByGauge))
                .putString("ctx_glass_gauge", nextActive)
                .apply()

            cur.copy(
                availableGlassGauges = updatedGlassGauges,
                availableGlassColorsByGauge = updatedByGauge,
                glassRates = updatedGlassRates,
                ctxGlassGauge = nextActive
            )
        }
    }

    fun addCustomGauge(gaugeName: String) {
        val clean = gaugeName.trim().removeSuffix("mm").removeSuffix("MM").trim()
        if (clean.isBlank()) return
        _state.update { cur ->
            val updatedGauges = (cur.availableGauges + clean).distinct()
            val mult = (clean.toDoubleOrNull() ?: 1.2) / 1.2
            val updatedRates = cur.rates.toMutableMap()
            if (!updatedRates.containsKey(clean)) {
                val base12 = cur.rates["1.2"] ?: cur.rates.values.firstOrNull() ?: emptyMap()
                val newGaugeColorMap = base12.mapValues { (_, secMap) ->
                    secMap.mapValues { (_, r) -> (r * mult).roundToLong().toDouble() }
                }
                updatedRates[clean] = newGaugeColorMap
            }
            prefs.edit()
                .putString("saved_available_gauges", updatedGauges.joinToString("|"))
                .putString("ctx_gauge", clean)
                .apply()
            cur.copy(
                availableGauges = updatedGauges,
                rates = updatedRates,
                ctxGauge = clean
            )
        }
    }

    fun removeCustomGauge(gaugeName: String) {
        val clean = gaugeName.trim().removeSuffix("mm").removeSuffix("MM").trim()
        _state.update { cur ->
            if (cur.availableGauges.size <= 1) return@update cur
            val updatedGauges = cur.availableGauges.filterNot { it.equals(clean, ignoreCase = true) }
            val nextActive = if (cur.ctxGauge.equals(clean, ignoreCase = true)) {
                updatedGauges.firstOrNull() ?: "1.6"
            } else cur.ctxGauge
            prefs.edit()
                .putString("saved_available_gauges", updatedGauges.joinToString("|"))
                .putString("ctx_gauge", nextActive)
                .apply()
            cur.copy(
                availableGauges = updatedGauges,
                ctxGauge = nextActive
            )
        }
    }

    fun addCustomColor(colorName: String) {
        val clean = colorName.trim().uppercase()
        if (clean.isBlank()) return
        _state.update { cur ->
            val updatedColors = (cur.availableColors + clean).distinct()
            val updatedRates = cur.rates.mapValues { (_, colorMap) ->
                val mutableColorMap = colorMap.toMutableMap()
                if (!mutableColorMap.containsKey(clean)) {
                    val templateRates = colorMap["DULL"] ?: colorMap.values.firstOrNull() ?: emptyMap()
                    mutableColorMap[clean] = templateRates.toMap()
                }
                mutableColorMap.toMap()
            }
            prefs.edit()
                .putString("saved_available_colors", updatedColors.joinToString("|"))
                .putString("ctx_color", clean)
                .apply()
            cur.copy(availableColors = updatedColors, rates = updatedRates, ctxColor = clean)
        }
    }

    fun addCustomAluminiumColor(colorName: String) = addCustomColor(colorName)

    fun removeCustomColor(colorName: String) {
        _state.update { cur ->
            if (cur.availableColors.size <= 1) return@update cur
            val updated = cur.availableColors.filterNot { it.equals(colorName, ignoreCase = true) }
            val nextActive = if (cur.ctxColor.equals(colorName, ignoreCase = true)) {
                updated.firstOrNull() ?: "SAHARA / BROWN"
            } else cur.ctxColor
            prefs.edit()
                .putString("saved_available_colors", updated.joinToString("|"))
                .putString("ctx_color", nextActive)
                .apply()
            cur.copy(availableColors = updated, ctxColor = nextActive)
        }
    }

    fun addCustomGlassColor(colorName: String, gauge: String, initialRate: Double = 0.0) {
        val clean = colorName.trim().uppercase()
        val cleanGauge = gauge.trim().removeSuffix("mm").removeSuffix("MM").trim()
        if (clean.isBlank() || cleanGauge.isBlank()) return
        _state.update { cur ->
            val updated5 = if (cleanGauge == "5") (cur.availableGlassColors5mm + clean).distinct() else cur.availableGlassColors5mm
            val updated8 = if (cleanGauge == "8") (cur.availableGlassColors8mm + clean).distinct() else cur.availableGlassColors8mm
            val updatedByGauge = cur.availableGlassColorsByGauge.toMutableMap()
            val curListForGauge = updatedByGauge[cleanGauge] ?: cur.getGlassColorsForGauge(cleanGauge)
            updatedByGauge[cleanGauge] = (curListForGauge + clean).distinct()

            val mult = (cleanGauge.toDoubleOrNull() ?: 5.0) / 5.0
            val effectiveRate = if (initialRate > 0) initialRate else (if (cleanGauge == "8") 390.0 else (230.0 * mult).roundToLong().toDouble())
            val updatedGlassRates = cur.glassRates.toMutableMap()
            val gaugeMap = (updatedGlassRates[cleanGauge] ?: emptyMap()).toMutableMap()
            gaugeMap[clean] = effectiveRate
            updatedGlassRates[cleanGauge] = gaugeMap.toMap()

            prefs.edit()
                .putString("saved_glass_colors_5mm", updated5.joinToString("|"))
                .putString("saved_glass_colors_8mm", updated8.joinToString("|"))
                .putString("saved_glass_colors_by_gauge", BossSerializationHelper.serializeGlassColorsByGauge(updatedByGauge))
                .putFloat("glass_rate_${cleanGauge}_$clean", effectiveRate.toFloat())
                .apply()
            cur.copy(
                availableGlassColors5mm = updated5,
                availableGlassColors8mm = updated8,
                availableGlassColorsByGauge = updatedByGauge,
                glassRates = updatedGlassRates
            )
        }
    }

    fun removeGlassColor(colorName: String, gauge: String? = null) {
        val clean = colorName.trim()
        if (clean.isBlank()) return
        if (gauge != null) {
            val cleanG = gauge.trim().removeSuffix("mm").removeSuffix("MM").trim()
            removeMultipleGlassColors(setOf("$cleanG|$clean"))
        } else {
            val allSet = _state.value.availableGlassGauges.map { "$it|$clean" }.toSet()
            removeMultipleGlassColors(allSet)
        }
    }

    fun removeMultipleGlassColors(entriesToRemove: Set<String>) {
        if (entriesToRemove.isEmpty()) return
        _state.update { cur ->
            val removeByGauge = mutableMapOf<String, MutableSet<String>>()
            entriesToRemove.forEach { entry ->
                if (entry.contains("|")) {
                    val parts = entry.split("|", limit = 2)
                    val g = parts[0].trim().removeSuffix("mm").removeSuffix("MM").trim()
                    val c = parts[1].trim().uppercase()
                    removeByGauge.getOrPut(g) { mutableSetOf() }.add(c)
                } else {
                    cur.availableGlassGauges.forEach { g ->
                        removeByGauge.getOrPut(g) { mutableSetOf() }.add(entry.trim().uppercase())
                    }
                }
            }

            val updatedByGauge = cur.availableGlassColorsByGauge.mapValues { (g, list) ->
                val rem = removeByGauge[g] ?: emptySet()
                val filtered = list.filterNot { rem.contains(it.uppercase()) }
                if (filtered.isEmpty()) list.take(1) else filtered
            }.toMutableMap()

            val updated5 = updatedByGauge["5"] ?: cur.availableGlassColors5mm.filterNot { (removeByGauge["5"] ?: emptySet()).contains(it.uppercase()) }
            val updated8 = updatedByGauge["8"] ?: cur.availableGlassColors8mm.filterNot { (removeByGauge["8"] ?: emptySet()).contains(it.uppercase()) }
            updatedByGauge["5"] = updated5
            updatedByGauge["8"] = updated8

            val updatedGlassRates = cur.glassRates.mapValues { (g, colMap) ->
                val remSet = removeByGauge[g] ?: emptySet()
                colMap.filterKeys { k -> !remSet.contains(k.uppercase()) }
            }

            prefs.edit()
                .putString("saved_glass_colors_5mm", updated5.joinToString("|"))
                .putString("saved_glass_colors_8mm", updated8.joinToString("|"))
                .putString("saved_glass_colors_by_gauge", BossSerializationHelper.serializeGlassColorsByGauge(updatedByGauge))
                .apply()

            val activeList = updatedByGauge[cur.ctxGlassGauge] ?: if (cur.ctxGlassGauge == "8") updated8 else updated5
            val nextGlassColor = if (activeList.any { it.equals(cur.ctxGlassColor, ignoreCase = true) }) {
                cur.ctxGlassColor
            } else {
                activeList.firstOrNull() ?: "CLEAR"
            }

            cur.copy(
                availableGlassColors5mm = updated5,
                availableGlassColors8mm = updated8,
                availableGlassColorsByGauge = updatedByGauge,
                glassRates = updatedGlassRates,
                ctxGlassColor = nextGlassColor
            )
        }
    }

    fun restoreDefaultGlassColors() {
        prefs.edit()
            .remove("saved_available_glass_gauges")
            .remove("saved_glass_colors_5mm")
            .remove("saved_glass_colors_8mm")
            .remove("saved_glass_colors_by_gauge")
            .apply()
        _state.update { cur ->
            cur.copy(
                availableGlassGauges = listOf("5", "8"),
                availableGlassColors5mm = BossDefaultRates.DEFAULT_GLASS_COLORS_5MM,
                availableGlassColors8mm = BossDefaultRates.DEFAULT_GLASS_COLORS_8MM,
                availableGlassColorsByGauge = mapOf(
                    "5" to BossDefaultRates.DEFAULT_GLASS_COLORS_5MM,
                    "8" to BossDefaultRates.DEFAULT_GLASS_COLORS_8MM
                ),
                glassRates = BossDefaultRates.getInitialGlassRates()
            )
        }
    }

    // Custom Cutting Pieces CRUD
    fun addCustomCutPiece(piece: CutPiece) {
        _state.update { cur ->
            cur.copy(customCutPieces = cur.customCutPieces + piece)
        }
    }

    fun addCustomCutPiece(
        section: String,
        lengthInches: Double,
        qty: Int,
        gauge: String = _state.value.ctxGauge,
        source: String = "Custom Cut"
    ) {
        _state.update { cur ->
            val piece = CutPiece(
                section = section.trim().uppercase(),
                length = lengthInches,
                qty = qty.coerceAtLeast(1),
                source = source.ifBlank { "Custom Cut" },
                label = "Custom Cut",
                gauge = gauge
            )
            cur.copy(customCutPieces = cur.customCutPieces + piece)
        }
    }

    fun removeCustomCutPiece(index: Int) {
        _state.update { cur ->
            if (index in cur.customCutPieces.indices) {
                val mutable = cur.customCutPieces.toMutableList()
                mutable.removeAt(index)
                cur.copy(customCutPieces = mutable)
            } else cur
        }
    }

    fun clearCustomCutPieces() {
        _state.update { it.copy(customCutPieces = emptyList()) }
    }

    // Formula Settings Updaters (Saved in Base64-obfuscated SharedPreferences for offline access)
    fun updateWindowFormulas(updated: WindowFormulaSettings) {
        prefs.edit().putString("win_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeWindowFormulas(updated))).apply()
        _state.update { it.copy(winFormulas = updated) }
    }

    fun updateSlidingDoorFormulas(updated: SlidingDoorFormulaSettings) {
        prefs.edit().putString("sliding_door_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeSlidingDoorFormulas(updated))).apply()
        _state.update { it.copy(slidingDoorFormulas = updated) }
    }

    fun updateDoorFormulas(updated: FormulaCategorySettings) {
        prefs.edit().putString("door_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeCategoryFormulas(updated))).apply()
        _state.update { it.copy(doorFormulas = updated) }
    }

    fun updateFixFormulas(updated: FormulaCategorySettings) {
        prefs.edit().putString("fix_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeCategoryFormulas(updated))).apply()
        _state.update { it.copy(fixFormulas = updated) }
    }

    fun updateOpenableWindowFormulas(updated: FormulaCategorySettings) {
        prefs.edit().putString("opwin_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeCategoryFormulas(updated))).apply()
        _state.update { it.copy(opwinFormulas = updated) }
    }

    fun updateFSectionFormula(updated: FSectionFormula) {
        prefs.edit().putString("f_section_formula_b64", encodeBase64Pref(BossSerializationHelper.serializeFSectionFormula(updated))).apply()
        _state.update { it.copy(fSectionFormula = updated) }
    }

    fun updateGlassFormula(key: String, updated: GlassFormula) {
        _state.update { cur ->
            val m = cur.glassFormulas.toMutableMap()
            m[key] = updated
            prefs.edit().putString("glass_formulas_b64", encodeBase64Pref(BossSerializationHelper.serializeGlassFormulas(m))).apply()
            cur.copy(glassFormulas = m)
        }
    }

    fun resetAllFormulasToDefault() {
        prefs.edit()
            .remove("win_formulas_json")
            .remove("sliding_door_formulas_json")
            .remove("door_formulas_json")
            .remove("fix_formulas_json")
            .remove("opwin_formulas_json")
            .remove("f_section_formula_json")
            .remove("glass_formulas_json")
            .remove("win_formulas_b64")
            .remove("sliding_door_formulas_b64")
            .remove("door_formulas_b64")
            .remove("fix_formulas_b64")
            .remove("opwin_formulas_b64")
            .remove("f_section_formula_b64")
            .remove("glass_formulas_b64")
            .apply()
        _state.update {
            it.copy(
                winFormulas = WindowFormulaSettings(),
                slidingDoorFormulas = SlidingDoorFormulaSettings(),
                doorFormulas = FormulaCategorySettings(),
                fixFormulas = FormulaCategorySettings(),
                opwinFormulas = FormulaCategorySettings.defaultOpenableWindowSettings(),
                fSectionFormula = FSectionFormula(),
                glassFormulas = mapOf(
                    "win" to GlassFormula(panels = 2),
                    "door" to GlassFormula(panels = 1),
                    "fix" to GlassFormula(panels = 1),
                    "opwin" to GlassFormula(panels = 1),
                    "slidingdoor" to GlassFormula(panels = 2)
                )
            )
        }
    }

    // Rates & Billing
    fun updateSectionRate(gauge: String, color: String, section: String, newRate: Double) {
        val existingKeys = (prefs.getStringSet("user_custom_rate_keys", emptySet()) ?: emptySet()).toMutableSet()
        existingKeys.add("$gauge|$color|$section")
        prefs.edit()
            .putFloat("user_sec_rate_${gauge}_${color}_$section", newRate.toFloat())
            .putStringSet("user_custom_rate_keys", existingKeys)
            .apply()
        _state.update { cur ->
            val updatedRates = cur.rates.toMutableMap().apply {
                val colorMap = (this[gauge] ?: emptyMap()).toMutableMap()
                val secMap = (colorMap[color] ?: emptyMap()).toMutableMap()
                secMap[section] = newRate
                colorMap[color] = secMap
                this[gauge] = colorMap
            }
            cur.copy(rates = updatedRates)
        }
    }

    fun saveAllCurrentRatesAndSettingsAsDefault() {
        val s = _state.value
        val existingKeys = (prefs.getStringSet("user_custom_rate_keys", emptySet()) ?: emptySet()).toMutableSet()
        val editor = prefs.edit()
            .putString("ctx_gauge", s.ctxGauge)
            .putString("ctx_gauge_mode", s.ctxGaugeMode)
            .putString("ctx_color", s.ctxColor)
            .putString("ctx_frame_type", s.ctxFrameType)
            .putString("ctx_glass_gauge", s.ctxGlassGauge)
            .putString("ctx_glass_color", s.ctxGlassColor)
            .putString("ctx_glass_mode", s.ctxGlassMode)
            .putString("glass_fitting_type", s.glassFittingType)
            .putString("measurement_unit", s.measurementUnit)
            .putString("saved_available_gauges", s.availableGauges.joinToString("|"))
            .putString("saved_available_colors", s.availableColors.joinToString("|"))
            .putString("saved_available_glass_gauges", s.availableGlassGauges.joinToString("|"))
            .putString("saved_glass_colors_5mm", s.availableGlassColors5mm.joinToString("|"))
            .putString("saved_glass_colors_8mm", s.availableGlassColors8mm.joinToString("|"))
            .putString("saved_glass_colors_by_gauge", BossSerializationHelper.serializeGlassColorsByGauge(s.availableGlassColorsByGauge))
            .putFloat("alum_discount_pct", s.alumDiscountPercent.toFloat())
            .putBoolean("auto_include_hw", s.autoIncludeHardware)
            .putFloat("win_hw_rate", s.windowHardwareRate.toFloat())
            .putFloat("door_hw_rate", s.doorHardwareRate.toFloat())
            .putFloat("fix_hw_rate", s.fixHardwareRate.toFloat())
            .putFloat("sd_hw_rate", s.slidingDoorHardwareRate.toFloat())
            .putFloat("labour_cost", s.labourCost.toFloat())
            .putFloat("transport_rent", s.transportRent.toFloat())
            .putFloat("profit_pct", s.profitPercent.toFloat())
            .putString("quotation_terms", s.quotationTerms)
            .putString("company_name", s.companyName)
            .putString("company_tagline", s.companyTagline)
            .putString("company_phone", s.companyPhone)
            .putString("company_address", s.companyAddress)
            .putString("company_ntn", s.companyNtn)
        s.rates.forEach { (g, colMap) ->
            colMap.forEach { (col, secMap) ->
                secMap.forEach { (sec, r) ->
                    existingKeys.add("$g|$col|$sec")
                    editor.putFloat("user_sec_rate_${g}_${col}_$sec", r.toFloat())
                }
            }
        }
        s.glassRates.forEach { (g, colMap) ->
            colMap.forEach { (col, r) ->
                editor.putFloat("glass_rate_${g}_$col", r.toFloat())
            }
        }
        editor.putStringSet("user_custom_rate_keys", existingKeys)
        editor.apply()
    }

    fun applyDetectedRateItems(items: List<DetectedRateItem>) {
        val existingKeys = (prefs.getStringSet("user_custom_rate_keys", emptySet()) ?: emptySet()).toMutableSet()
        val editor = prefs.edit()
        _state.update { cur ->
            val updatedRates = cur.rates.mapValues { (_, colorMap) ->
                colorMap.mapValues { (_, secMap) -> secMap.toMutableMap() }.toMutableMap()
            }.toMutableMap()

            val customAdded = mutableListOf<String>()
            items.forEach { item ->
                if (item.isCustomSection) {
                    customAdded.add(item.section)
                }
                val g = item.gauge.ifBlank { cur.ctxGauge }
                val gMap = updatedRates.getOrPut(g) { mutableMapOf() }
                item.rates.forEach { (colKey, rateVal) ->
                    if (rateVal > 0) {
                        val cMap = gMap.getOrPut(colKey) { mutableMapOf() }
                        cMap[item.section] = rateVal
                        existingKeys.add("$g|$colKey|${item.section}")
                        editor.putFloat("user_sec_rate_${g}_${colKey}_${item.section}", rateVal.toFloat())
                    }
                }
            }
            editor.putStringSet("user_custom_rate_keys", existingKeys).apply()

            cur.copy(
                rates = updatedRates,
                customSections = (cur.customSections + customAdded).distinct()
            )
        }
    }

    fun updateGlassRate(gauge: String, color: String, newRate: Double) {
        prefs.edit().putFloat("glass_rate_${gauge}_$color", newRate.toFloat()).apply()
        _state.update { cur ->
            val updatedGlass = cur.glassRates.mapValues { (g, colMap) ->
                if (g == gauge) {
                    val m = colMap.toMutableMap()
                    m[color] = newRate
                    m
                } else colMap
            }
            cur.copy(glassRates = updatedGlass)
        }
    }

    fun updateCustomerDetails(name: String, phone: String) {
        _state.update { it.copy(customerName = name, customerPhone = phone) }
    }

    fun updateAlumDiscount(discountPct: Double) {
        val clean = discountPct.coerceIn(0.0, 100.0)
        prefs.edit().putFloat("alum_discount_pct", clean.toFloat()).apply()
        _state.update { it.copy(alumDiscountPercent = clean) }
    }

    fun updateSingleHardwareAmount(hardwareAmount: Double) {
        val clean = hardwareAmount.coerceAtLeast(0.0)
        prefs.edit()
            .putBoolean("auto_include_hw", true)
            .putFloat("win_hw_rate", clean.toFloat())
            .apply()
        _state.update {
            it.copy(
                autoIncludeHardware = true,
                windowHardwareRate = clean
            )
        }
    }

    fun addCustomBillItem(description: String, amount: Double) {
        val desc = description.trim()
        if (desc.isBlank() || amount <= 0.0) return
        _state.update { cur ->
            cur.copy(customBillItems = cur.customBillItems + CustomBillItem(description = desc, amount = amount))
        }
    }

    fun removeCustomBillItem(id: String) {
        _state.update { cur ->
            cur.copy(customBillItems = cur.customBillItems.filterNot { it.id == id })
        }
    }

    fun saveHardwareAndQuotationDefaults(
        autoInclude: Boolean,
        winHw: Double,
        doorHw: Double,
        fixHw: Double,
        sdHw: Double,
        profitPct: Double,
        labour: Double,
        transport: Double,
        terms: String
    ) {
        prefs.edit()
            .putBoolean("auto_include_hw", autoInclude)
            .putFloat("win_hw_rate", winHw.toFloat())
            .putFloat("door_hw_rate", doorHw.toFloat())
            .putFloat("fix_hw_rate", fixHw.toFloat())
            .putFloat("sd_hw_rate", sdHw.toFloat())
            .putFloat("wastage_pct", 0f)
            .putFloat("profit_pct", profitPct.toFloat())
            .putFloat("tax_pct", 0f)
            .putFloat("labour_cost", labour.toFloat())
            .putFloat("transport_rent", transport.toFloat())
            .putString("quotation_terms", terms)
            .apply()
        _state.update {
            it.copy(
                autoIncludeHardware = autoInclude,
                windowHardwareRate = winHw,
                doorHardwareRate = doorHw,
                fixHardwareRate = fixHw,
                slidingDoorHardwareRate = sdHw,
                wastagePercent = 0.0,
                profitPercent = profitPct,
                taxPercent = 0.0,
                labourCost = labour,
                transportRent = transport,
                quotationTerms = terms,
                markupPercent = profitPct
            )
        }
    }

    fun updateLabourAndTransport(labour: Double, transport: Double) {
        prefs.edit()
            .putFloat("labour_cost", labour.toFloat())
            .putFloat("transport_rent", transport.toFloat())
            .apply()
        _state.update {
            it.copy(
                labourCost = labour.coerceAtLeast(0.0),
                transportRent = transport.coerceAtLeast(0.0)
            )
        }
    }

    fun updateBillingExtras(markup: Double, hwRate: Double, transport: Double, labour: Double) {
        _state.update {
            it.copy(
                markupPercent = markup,
                hardwareRatePerUnit = hwRate,
                transportRent = transport,
                labourCost = labour
            )
        }
    }

    fun updateCompanyBranding(name: String, tagline: String, phone: String, address: String, ntn: String) {
        prefs.edit()
            .putString("company_name", name)
            .putString("company_tagline", tagline)
            .putString("company_phone", phone)
            .putString("company_address", address)
            .putString("company_ntn", ntn)
            .apply()
        _state.update {
            it.copy(
                companyName = name,
                companyTagline = tagline,
                companyPhone = phone,
                companyAddress = address,
                companyNtn = ntn
            )
        }
    }

    fun updateCustomLogoPath(path: String?) {
        prefs.edit().putString("custom_logo_path", path).apply()
        _state.update { it.copy(customLogoPath = path) }
    }

    fun updateCustomBackgroundPath(path: String?) {
        prefs.edit().putString("custom_background_path", path).apply()
        _state.update { it.copy(customBackgroundPath = path) }
    }

    fun onProfileCompleted(profile: UserProfile) {
        val sub = securityManager.loadSubscriptionState()
        _state.update {
            it.copy(
                userProfile = profile,
                subscriptionState = sub,
                companyName = if (it.companyName == "YOUR SHOP / BUSINESS NAME" && profile.shopAddress.isNotBlank()) profile.shopAddress else it.companyName,
                companyPhone = it.companyPhone.ifBlank { profile.phone },
                companyAddress = it.companyAddress.ifBlank { profile.shopAddress }
            )
        }
        restartAccountWatcher()
        syncMasterCloudConfigNow()
    }

    fun refreshSecurityState() {
        _state.update {
            it.copy(
                userProfile = securityManager.loadUserProfile(),
                subscriptionState = securityManager.loadSubscriptionState()
            )
        }
        syncMasterCloudConfigNow()
    }

    fun logoutUser() {
        securityManager.logoutUser()
        masterEngineManager.clearRuntimeFormulas()
        resetAllFormulasToDefault()
        _state.update {
            it.copy(
                userProfile = securityManager.loadUserProfile(),
                subscriptionState = securityManager.loadSubscriptionState()
            )
        }
    }

    // Core Calculations using BossCalculationEngine
    fun getAllCutPieces(): List<CutPiece> {
        verifyUserSecurityWithServerNow(force = false)
        val s = _state.value
        if (!isUserVerifiedAndSubscribed()) return emptyList()
        val pieces = mutableListOf<CutPiece>()
        val overrideFrame = if (s.ctxFrameType == "OFF") null else s.ctxFrameType

        s.windows.forEachIndexed { idx, w ->
            val effectiveWin = if (s.ctxGaugeMode == "ON") w.copy(sectionGauge = s.ctxGauge) else w
            pieces.addAll(
                BossCalculationEngine.computeSlidingWindowPieces(
                    w = effectiveWin,
                    overrideFrameType = overrideFrame,
                    formula = s.winFormulas,
                    fFormula = s.fSectionFormula,
                    itemNumber = idx + 1
                )
            )
        }

        s.doors.forEachIndexed { idx, d ->
            val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else d.sectionGauge
            val effectiveOuter = BossCalculationEngine.resolveDoorFixOuterSection(d.outer, overrideFrame)
            pieces.addAll(
                BossCalculationEngine.computeOpenablePieces(
                    width = d.width,
                    height = d.height,
                    qty = d.qty,
                    outer = effectiveOuter,
                    kind = "door",
                    categorySettings = s.doorFormulas,
                    extras = d.extras,
                    gauge = g,
                    fFormula = s.fSectionFormula,
                    d59Qty = d.d59Qty,
                    d59aQty = d.d59aQty,
                    d52Qty = d.d52Qty,
                    itemNumber = idx + 1
                )
            )
        }

        s.fixframes.forEachIndexed { idx, f ->
            val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else f.sectionGauge
            val effectiveOuter = BossCalculationEngine.resolveDoorFixOuterSection(f.outer, overrideFrame)
            pieces.addAll(
                BossCalculationEngine.computeOpenablePieces(
                    width = f.width,
                    height = f.height,
                    qty = f.qty,
                    outer = effectiveOuter,
                    kind = "fix",
                    categorySettings = s.fixFormulas,
                    extras = f.extras,
                    gauge = g,
                    fFormula = s.fSectionFormula,
                    d59Qty = f.d59Qty,
                    d59aQty = f.d59aQty,
                    d52Qty = f.d52Qty,
                    itemNumber = idx + 1
                )
            )
        }

        s.openableWindows.forEachIndexed { idx, op ->
            val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else op.sectionGauge
            val effectiveOuter = BossCalculationEngine.resolveDoorFixOuterSection(op.outer, overrideFrame)
            pieces.addAll(
                BossCalculationEngine.computeOpenablePieces(
                    width = op.width,
                    height = op.height,
                    qty = op.qty,
                    outer = effectiveOuter,
                    kind = "openwin",
                    categorySettings = s.opwinFormulas,
                    extras = op.extras,
                    gauge = g,
                    fFormula = s.fSectionFormula,
                    d59Qty = op.d59Qty,
                    d59aQty = op.d59aQty,
                    d52Qty = op.d52Qty,
                    itemNumber = idx + 1
                )
            )
        }

        s.slidingDoors.forEachIndexed { idx, sd ->
            val effectiveOuter = when (overrideFrame) {
                "F" -> "F"
                "C" -> if (sd.outer.equals("F", ignoreCase = true)) "NORMAL" else sd.outer
                null -> sd.outer
                else -> overrideFrame
            }
            val effectiveSd = sd.copy(
                outer = effectiveOuter,
                sectionGauge = if (s.ctxGaugeMode == "ON") s.ctxGauge else sd.sectionGauge
            )
            pieces.addAll(
                BossCalculationEngine.computeSlidingDoorPieces(
                    it = effectiveSd,
                    formula = s.slidingDoorFormulas,
                    fFormula = s.fSectionFormula,
                    itemNumber = idx + 1
                )
            )
        }

        s.projectExtras.forEach { ex ->
            if (ex.lengthFt > 0 && ex.qty > 0) {
                val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else ex.gauge
                pieces.add(
                    CutPiece(
                        section = ex.section,
                        length = ex.lengthFt * 12.0,
                        qty = ex.qty,
                        source = "Project Extra",
                        label = "Extra piece",
                        gauge = g
                    )
                )
            }
        }

        s.customCutPieces.forEach { cp ->
            if (cp.length > 0 && cp.qty > 0) {
                val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else cp.gauge
                pieces.add(cp.copy(gauge = g))
            }
        }

        return pieces
    }

    fun getGlassCuts(): List<GlassCutResult> {
        verifyUserSecurityWithServerNow(force = false)
        val s = _state.value
        if (!isUserVerifiedAndSubscribed()) return emptyList()
        val results = mutableListOf<GlassCutResult>()
        val upvcDelta = if (s.glassFittingType == "UPVC") -0.25 else 0.0

        fun addSlidingGlass(
            labelPrefix: String,
            idx: Int,
            width: Double,
            height: Double,
            qty: Int,
            panels: Int,
            itemGlassGauge: String,
            itemGlassColor: String,
            itemGlassEnabled: Boolean,
            gf: GlassFormula,
            m23DedInches: Double,
            m24DedInches: Double
        ) {
            if (!itemGlassEnabled && s.ctxGlassMode != "ON") return
            val gGauge = if (s.ctxGlassMode == "ON") s.ctxGlassGauge else itemGlassGauge
            val gColor = if (s.ctxGlassMode == "ON") s.ctxGlassColor else itemGlassColor
            val pCount = panels.coerceIn(2, 4)

            val sashH = max(0.0, height - m23DedInches)
            val sashW = max(0.0, (width - m24DedInches) / pCount.toDouble())

            val gH = max(0.0, sashH + gf.getEffectiveMarginH(pCount) + upvcDelta)
            val gW = max(0.0, sashW + gf.getEffectiveMarginW(pCount) + upvcDelta)
            val billedH = BossCalculationEngine.billFeet(gH)
            val billedW = BossCalculationEngine.billFeet(gW)
            val totalPieces = pCount * qty.coerceAtLeast(1)
            val sqft = billedH * billedW * totalPieces

            results.add(
                GlassCutResult(
                    frameLabel = "$labelPrefix #${idx + 1}",
                    frameDim = "${BossCalculationEngine.toDotSutar(width)}\" x ${BossCalculationEngine.toDotSutar(height)}\"",
                    glassGauge = gGauge,
                    glassColor = gColor,
                    gH = gH,
                    gWpanel = gW,
                    billedHFt = billedH,
                    billedWFt = billedW,
                    pieces = totalPieces,
                    sqft = sqft
                )
            )
        }

        fun addSingleGlass(
            labelPrefix: String,
            idx: Int,
            width: Double,
            height: Double,
            qty: Int,
            itemGlassGauge: String,
            itemGlassColor: String,
            itemGlassEnabled: Boolean,
            gf: GlassFormula
        ) {
            if (!itemGlassEnabled && s.ctxGlassMode != "ON") return
            val gGauge = if (s.ctxGlassMode == "ON") s.ctxGlassGauge else itemGlassGauge
            val gColor = if (s.ctxGlassMode == "ON") s.ctxGlassColor else itemGlassColor
            val gH = max(0.0, height + gf.getEffectiveMarginH(1) + upvcDelta)
            val gW = max(0.0, width + gf.getEffectiveMarginW(1) + upvcDelta)
            val billedH = BossCalculationEngine.billFeet(gH)
            val billedW = BossCalculationEngine.billFeet(gW)
            val totalPieces = qty.coerceAtLeast(1)
            val sqft = billedH * billedW * totalPieces

            results.add(
                GlassCutResult(
                    frameLabel = "$labelPrefix #${idx + 1}",
                    frameDim = "${BossCalculationEngine.toDotSutar(width)}\" x ${BossCalculationEngine.toDotSutar(height)}\"",
                    glassGauge = gGauge,
                    glassColor = gColor,
                    gH = gH,
                    gWpanel = gW,
                    billedHFt = billedH,
                    billedWFt = billedW,
                    pieces = totalPieces,
                    sqft = sqft
                )
            )
        }

        val winGf = s.glassFormulas["win"] ?: GlassFormula(panels = 2)
        s.windows.forEachIndexed { i, w ->
            val pCount = w.panelCount.coerceIn(2, 4)
            val m24Ded = when (pCount) {
                3 -> s.winFormulas.m24WidthDeduction3.totalInches
                4 -> s.winFormulas.m24WidthDeduction4.totalInches
                else -> s.winFormulas.m24WidthDeduction.totalInches
            }
            addSlidingGlass(
                "Window", i, w.width, w.height, w.qty, w.panelCount,
                w.glassGauge, w.glassColor, w.glassEnabled, winGf,
                s.winFormulas.m23HeightDeduction.totalInches, m24Ded
            )
        }

        val doorGf = s.glassFormulas["door"] ?: GlassFormula(panels = 1)
        s.doors.forEachIndexed { i, d ->
            addSingleGlass("Door", i, d.width, d.height, d.qty, d.glassGauge, d.glassColor, d.glassEnabled, doorGf)
        }

        val fixGf = s.glassFormulas["fix"] ?: GlassFormula(panels = 1)
        s.fixframes.forEachIndexed { i, f ->
            addSingleGlass("Fix Frame", i, f.width, f.height, f.qty, f.glassGauge, f.glassColor, f.glassEnabled, fixGf)
        }

        val opGf = s.glassFormulas["opwin"] ?: GlassFormula(panels = 1)
        s.openableWindows.forEachIndexed { i, op ->
            addSingleGlass("Openable Win", i, op.width, op.height, op.qty, op.glassGauge, op.glassColor, op.glassEnabled, opGf)
        }

        val sdGf = s.glassFormulas["slidingdoor"] ?: GlassFormula(panels = 2)
        s.slidingDoors.forEachIndexed { i, sd ->
            val pCount = sd.panelCount.coerceIn(2, 4)
            val m24Ded = when (pCount) {
                3 -> s.slidingDoorFormulas.m24WidthDeduction3.totalInches
                4 -> s.slidingDoorFormulas.m24WidthDeduction4.totalInches
                else -> s.slidingDoorFormulas.m24WidthDeduction.totalInches
            }
            addSlidingGlass(
                "Sliding Door", i, sd.width, sd.height, sd.qty, sd.panelCount,
                sd.glassGauge, sd.glassColor, sd.glassEnabled, sdGf,
                s.slidingDoorFormulas.m23HeightDeduction.totalInches, m24Ded
            )
        }

        return results
    }

    fun calculateQuotation(frameTypeOverride: String? = null): QuotationResult {
        verifyUserSecurityWithServerNow(force = false)
        val s = _state.value
        if (!isUserVerifiedAndSubscribed()) {
            return QuotationResult(
                aluminiumRows = emptyList(),
                alumTotal = 0.0,
                discountPct = 0.0,
                discountAmt = 0.0,
                netAlumTotal = 0.0,
                alumWithMarkup = 0.0,
                markupPct = 0.0,
                totalUnits = 0,
                hardwareTotal = 0.0,
                glassTotal = 0.0,
                glassLines = emptyList(),
                transportRent = 0.0,
                labourCost = 0.0,
                grandTotal = 0.0
            )
        }
        val activeFrame = frameTypeOverride ?: (if (s.ctxFrameType == "OFF") null else s.ctxFrameType)
        val rawPieces = mutableListOf<CutPiece>()

        s.windows.forEachIndexed { idx, w ->
            val effectiveWin = if (s.ctxGaugeMode == "ON") w.copy(sectionGauge = s.ctxGauge) else w
            rawPieces.addAll(
                BossCalculationEngine.computeSlidingWindowPieces(
                    w = effectiveWin,
                    overrideFrameType = activeFrame,
                    formula = s.winFormulas,
                    fFormula = s.fSectionFormula,
                    itemNumber = idx + 1
                )
            )
        }
        s.doors.forEachIndexed { idx, d ->
            val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else d.sectionGauge
            val effectiveOuter = BossCalculationEngine.resolveDoorFixOuterSection(d.outer, activeFrame)
            rawPieces.addAll(
                BossCalculationEngine.computeOpenablePieces(
                    width = d.width, height = d.height, qty = d.qty, outer = effectiveOuter, kind = "door",
                    categorySettings = s.doorFormulas, extras = d.extras, gauge = g, fFormula = s.fSectionFormula,
                    d59Qty = d.d59Qty, d59aQty = d.d59aQty, d52Qty = d.d52Qty, itemNumber = idx + 1
                )
            )
        }
        s.fixframes.forEachIndexed { idx, f ->
            val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else f.sectionGauge
            val effectiveOuter = BossCalculationEngine.resolveDoorFixOuterSection(f.outer, activeFrame)
            rawPieces.addAll(
                BossCalculationEngine.computeOpenablePieces(
                    width = f.width, height = f.height, qty = f.qty, outer = effectiveOuter, kind = "fix",
                    categorySettings = s.fixFormulas, extras = f.extras, gauge = g, fFormula = s.fSectionFormula,
                    d59Qty = f.d59Qty, d59aQty = f.d59aQty, d52Qty = f.d52Qty, itemNumber = idx + 1
                )
            )
        }
        s.openableWindows.forEachIndexed { idx, op ->
            val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else op.sectionGauge
            val effectiveOuter = BossCalculationEngine.resolveDoorFixOuterSection(op.outer, activeFrame)
            rawPieces.addAll(
                BossCalculationEngine.computeOpenablePieces(
                    width = op.width, height = op.height, qty = op.qty, outer = effectiveOuter, kind = "openwin",
                    categorySettings = s.opwinFormulas, extras = op.extras, gauge = g, fFormula = s.fSectionFormula,
                    d59Qty = op.d59Qty, d59aQty = op.d59aQty, d52Qty = op.d52Qty, itemNumber = idx + 1
                )
            )
        }
        s.slidingDoors.forEachIndexed { idx, sd ->
            val effectiveOuter = when (activeFrame) {
                "F" -> "F"
                "C" -> if (sd.outer.equals("F", ignoreCase = true)) "NORMAL" else sd.outer
                null -> sd.outer
                else -> activeFrame
            }
            val effectiveSd = sd.copy(
                outer = effectiveOuter,
                sectionGauge = if (s.ctxGaugeMode == "ON") s.ctxGauge else sd.sectionGauge
            )
            rawPieces.addAll(
                BossCalculationEngine.computeSlidingDoorPieces(
                    it = effectiveSd, formula = s.slidingDoorFormulas, fFormula = s.fSectionFormula, itemNumber = idx + 1
                )
            )
        }
        s.projectExtras.forEach { ex ->
            if (ex.lengthFt > 0 && ex.qty > 0) {
                val g = if (s.ctxGaugeMode == "ON") s.ctxGauge else ex.gauge
                rawPieces.add(CutPiece(ex.section, ex.lengthFt * 12.0, ex.qty, "Project Extra", "Extra piece", gauge = g))
            }
        }
        val allPieces = rawPieces.filter { !s.hiddenSections.contains(it.section) }
        val grouped = allPieces.groupBy { piece ->
            val effectiveG = if (s.ctxGaugeMode == "ON") {
                BossCalculationEngine.pricingGaugeForSection(piece.section, s.ctxGauge)
            } else {
                BossCalculationEngine.pricingGaugeForSection(piece.section, piece.gauge)
            }
            "${piece.section}|$effectiveG"
        }

        val alumRows = mutableListOf<AluminiumCostRow>()
        grouped.entries.sortedBy { it.key }.forEach { (key, list) ->
            val parts = key.split("|")
            val sec = parts[0]
            val g = parts.getOrElse(1) { s.ctxGauge }
            val packed = BossCalculationEngine.packSection(list, BossCalculationEngine.barLengthsFor(sec))
            var barsCount = 0
            var totalFeet = 0
            val tallyParts = mutableListOf<String>()
            packed.tally.entries.sortedByDescending { it.key }.forEach { (lenFt, info) ->
                barsCount += info.count
                totalFeet += lenFt * info.count
                tallyParts.add("${lenFt}ftx${info.count}${if (info.isCustom) " (offcut)" else ""}")
            }
            val ratePerFt = s.rates[g]?.get(s.ctxColor)?.get(sec)
                ?: s.rates[s.ctxGauge]?.get(s.ctxColor)?.get(sec)
                ?: 0.0
            val cost = totalFeet * ratePerFt
            if (barsCount > 0 || totalFeet > 0) {
                alumRows.add(
                    AluminiumCostRow(
                        section = sec,
                        gauge = g,
                        bars = barsCount,
                        totalFt = totalFeet,
                        ratePerFt = ratePerFt,
                        cost = cost,
                        tallySummary = tallyParts.joinToString(", ")
                    )
                )
            }
        }

        val alumTotal = alumRows.sumOf { it.cost }
        val effectiveDiscountPct = if (s.alumDiscountPercent > 0.0) s.alumDiscountPercent else s.profitPercent.coerceIn(0.0, 100.0)
        val discountAmt = alumTotal * (effectiveDiscountPct / 100.0)
        val netAlum = (alumTotal - discountAmt).coerceAtLeast(0.0)
        val alumWithMarkup = netAlum

        val winCount = s.windows.sumOf { it.qty } + s.openableWindows.sumOf { it.qty }
        val doorCount = s.doors.sumOf { it.qty }
        val fixCount = s.fixframes.sumOf { it.qty }
        val sdCount = s.slidingDoors.sumOf { it.qty }
        val totalUnits = winCount + doorCount + fixCount + sdCount

        val hardwareTotal = if (s.autoIncludeHardware) {
            s.windowHardwareRate.coerceAtLeast(0.0)
        } else 0.0

        val glassCuts = getGlassCuts()
        val glassGrouped = glassCuts.groupBy { "${it.glassGauge}|${it.glassColor}" }
        val glassLines = glassGrouped.map { (k, cuts) ->
            val parts = k.split("|")
            val gg = parts[0]
            val gc = parts.getOrElse(1) { "CLEAR" }
            val sqft = cuts.sumOf { it.sqft }
            val rate = s.glassRates[gg]?.get(gc) ?: 0.0
            GlassCostLine(
                description = "${gg}mm $gc Glass (${cuts.sumOf { it.pieces }} pcs)",
                sqft = sqft,
                rate = rate,
                total = sqft * rate
            )
        }
        val glassTotal = glassLines.sumOf { it.total }
        val customTotal = s.customBillItems.sumOf { it.amount }
        val baseSubTotal = netAlum + glassTotal + hardwareTotal + customTotal
        val grandTotal = baseSubTotal + s.transportRent + s.labourCost

        return QuotationResult(
            aluminiumRows = alumRows,
            alumTotal = alumTotal,
            discountPct = effectiveDiscountPct,
            discountAmt = discountAmt,
            netAlumTotal = netAlum,
            alumWithMarkup = alumWithMarkup,
            markupPct = 0.0,
            totalUnits = totalUnits,
            hardwareTotal = hardwareTotal,
            glassTotal = glassTotal,
            glassLines = glassLines,
            transportRent = s.transportRent,
            labourCost = s.labourCost,
            grandTotal = grandTotal
        )
    }

    fun saveCurrentQuotation() {
        val s = _state.value
        val quote = calculateQuotation()
        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
        val summary = "${s.windows.size} Win, ${s.doors.size} Door, ${s.fixframes.size} Fix"
        val saved = SavedQuotation(
            id = System.currentTimeMillis(),
            timestamp = nowStr,
            label = s.customerName.ifBlank { "Project #${s.savedQuotations.size + 1}" },
            customerName = s.customerName.ifBlank { "Valued Client" },
            customerPhone = s.customerPhone,
            gauge = s.ctxGauge,
            color = s.ctxColor,
            frameType = s.ctxFrameType,
            grandTotal = quote.grandTotal,
            alumTotal = quote.alumWithMarkup,
            hardwareTotal = quote.hardwareTotal,
            glassTotal = quote.glassTotal,
            transportRent = quote.transportRent,
            labourCost = quote.labourCost,
            totalUnits = quote.totalUnits,
            itemsSummary = summary,
            windowsJson = BossSerializationHelper.serializeWindows(s.windows),
            doorsJson = BossSerializationHelper.serializeDoors(s.doors),
            fixframesJson = BossSerializationHelper.serializeFixFrames(s.fixframes),
            openableWindowsJson = BossSerializationHelper.serializeOpenableWindows(s.openableWindows),
            slidingDoorsJson = BossSerializationHelper.serializeSlidingDoors(s.slidingDoors),
            projectExtrasJson = BossSerializationHelper.serializeProjectExtras(s.projectExtras)
        )
        val updatedList = listOf(saved) + s.savedQuotations
        persistSavedQuotations(updatedList)
        _state.update { it.copy(savedQuotations = updatedList) }
    }

    fun loadSavedQuotation(sq: SavedQuotation) {
        _state.update { cur ->
            cur.copy(
                customerName = sq.customerName,
                customerPhone = sq.customerPhone,
                ctxGauge = sq.gauge,
                ctxColor = sq.color,
                ctxFrameType = sq.frameType,
                windows = if (sq.windowsJson.isNotBlank()) BossSerializationHelper.deserializeWindows(sq.windowsJson) else cur.windows,
                doors = if (sq.doorsJson.isNotBlank()) BossSerializationHelper.deserializeDoors(sq.doorsJson) else emptyList(),
                fixframes = if (sq.fixframesJson.isNotBlank()) BossSerializationHelper.deserializeFixFrames(sq.fixframesJson) else emptyList(),
                openableWindows = if (sq.openableWindowsJson.isNotBlank()) BossSerializationHelper.deserializeOpenableWindows(sq.openableWindowsJson) else emptyList(),
                slidingDoors = if (sq.slidingDoorsJson.isNotBlank()) BossSerializationHelper.deserializeSlidingDoors(sq.slidingDoorsJson) else emptyList(),
                projectExtras = if (sq.projectExtrasJson.isNotBlank()) BossSerializationHelper.deserializeProjectExtras(sq.projectExtrasJson) else emptyList()
            )
        }
    }

    fun deleteSavedQuotation(id: Long) {
        val updated = _state.value.savedQuotations.filterNot { it.id == id }
        persistSavedQuotations(updated)
        _state.update { it.copy(savedQuotations = updated) }
    }

    private fun persistSavedQuotations(list: List<SavedQuotation>) {
        val jsonStr = BossSerializationHelper.serializeSavedQuotations(list)
        prefs.edit().putString("saved_quotations_json", jsonStr).apply()
    }

    override fun onCleared() {
        super.onCleared()
        masterEngineManager.stopRealtimeMasterSync()
        masterAdminManager.stopRealtimeOfferListener()
        FirebaseCloudManager.stopRealtimeAccountWatcher()
    }
}

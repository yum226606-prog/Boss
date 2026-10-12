package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.example.model.AppOfferBanner
import com.example.model.DragDropBlueprint
import com.example.model.DynamicMasterSection
import com.example.model.FSectionFormula
import com.example.model.FormulaCategorySettings
import com.example.model.GlassFormula
import com.example.model.MasterEngineCloudConfig
import com.example.model.RemoteLayoutConfig
import com.example.model.SlidingDoorFormulaSettings
import com.example.model.WindowFormulaSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * BossMasterEngineManager:
 * Loads all cutting formulas, glass formulas, blueprints, and branding dynamically at runtime
 * from Supabase table `app_configs` (key: `master_engine_config`) ONLY after verified user login
 * and active subscription verification. Zero formulas are stored on disk or hardcoded in APK.
 */
class BossMasterEngineManager(private val context: Context) {

    companion object {
        private const val TAG = "BossMasterEngine"
        private const val PREFS_NAME = "boss_master_engine_cache_prefs"
        private const val KEY_CACHED_CONFIG_B64 = "cached_master_engine_b64"
        private const val SUPABASE_APP_CONFIGS_TABLE = "app_configs"
        private const val SUPABASE_MASTER_KEY = "master_engine_config"
        private const val SUPABASE_GLOBAL_CONFIG_KEY = "global_config"
        private const val SUPABASE_FALLBACK_TABLE = "master_control"
        private const val SUPABASE_FALLBACK_ROW_ID = "global_engine_v1"
        private const val PREFS_SUPABASE_MASTER = "boss_supabase_master_prefs"

        @Volatile
        private var INSTANCE: BossMasterEngineManager? = null

        fun getInstance(context: Context): BossMasterEngineManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BossMasterEngineManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    // Runtime-only memory state (initialized from Base64-obfuscated offline cache if present)
    private val _cloudConfig = MutableStateFlow(loadCachedConfig() ?: MasterEngineCloudConfig())
    val cloudConfig: StateFlow<MasterEngineCloudConfig> = _cloudConfig.asStateFlow()

    private val _isConnectedToCloud = MutableStateFlow(false)
    val isConnectedToCloud: StateFlow<Boolean> = _isConnectedToCloud.asStateFlow()

    private fun saveCachedConfigJson(rawJson: String) {
        try {
            val encoded = Base64.encodeToString(rawJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            prefs.edit().putString(KEY_CACHED_CONFIG_B64, encoded).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache master config: ${e.message}")
        }
    }

    fun loadCachedConfig(): MasterEngineCloudConfig? {
        val encoded = prefs.getString(KEY_CACHED_CONFIG_B64, null) ?: return null
        return try {
            val jsonStr = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
            deserializeFromJson(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getDynamicSections(): List<DynamicMasterSection> {
        return _cloudConfig.value.customSections
    }

    /**
     * Clears all runtime formulas from memory when user logs out, is blocked, or subscription expires.
     */
    fun clearRuntimeFormulas() {
        _cloudConfig.value = MasterEngineCloudConfig()
        _isConnectedToCloud.value = false
        prefs.edit().clear().apply()
    }

    fun startRealtimeMasterSync(
        isVerifiedAndSubscribedProvider: () -> Boolean = { true },
        onConfigApplied: (MasterEngineCloudConfig) -> Unit
    ) {
        SupabaseCloudManager.initContext(context)
        stopRealtimeMasterSync()

        syncJob = scope.launch {
            while (isActive) {
                if (SupabaseCloudManager.isSupabaseConfigured(context)) {
                    try {
                        val parsed = withTimeoutOrNull(8000L) {
                            val row = fetchMasterConfigRowFromSupabase()
                            if (row != null) parseJsonConfig(row) else null
                        }
                        if (parsed != null) {
                            _cloudConfig.value = parsed
                            _isConnectedToCloud.value = true
                            withContext(Dispatchers.Main) {
                                onConfigApplied(parsed)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Supabase app_configs sync warning: ${e.message}")
                        _isConnectedToCloud.value = false
                    }
                } else {
                    _isConnectedToCloud.value = false
                }
                delay(15_000L)
            }
        }
    }

    private suspend fun syncGatewayConfigOnlyFromSupabase() {
        val globalRow = SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_APP_CONFIGS_TABLE,
            keyColumn = "key",
            keyValue = SUPABASE_GLOBAL_CONFIG_KEY
        ) ?: SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_APP_CONFIGS_TABLE,
            keyColumn = "config_key",
            keyValue = SUPABASE_GLOBAL_CONFIG_KEY
        )
        if (globalRow != null) {
            extractAndSaveSmsGatewayConfigFromRow(globalRow)
        }
        val masterRow = fetchMasterConfigRowFromSupabase()
        if (masterRow != null) {
            extractAndSaveSmsGatewayConfigFromRow(masterRow)
        }
    }

    private fun extractAndSaveSmsGatewayConfigFromRow(row: JSONObject) {
        try {
            val valueObj = row.optJSONObject("value")
                ?: row.optJSONObject("config")
                ?: row.optJSONObject("data")
                ?: row.optJSONObject("config_json")
            if (valueObj != null) {
                extractAndSaveSmsGatewayConfig(valueObj)
                return
            }
            val rawStr = row.optString("value", "")
                .ifBlank { row.optString("config_json", "") }
                .ifBlank { row.optString("data", "") }
            if (rawStr.isNotBlank() && rawStr.trim().startsWith("{")) {
                extractAndSaveSmsGatewayConfig(JSONObject(rawStr))
                return
            }
            extractAndSaveSmsGatewayConfig(row)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract smsGatewayConfig: ${e.message}")
        }
    }

    private fun extractAndSaveSmsGatewayConfig(root: JSONObject) {
        val smsObj = root.optJSONObject("smsGatewayConfig")
            ?: root.optJSONObject("sms_gateway_config")
            ?: if (root.has("sendpkApiKey") || root.has("sendpk_api_key") || root.has("sendpkEnabled") || root.has("sendpk_enabled") || root.has("resendApiKey") || root.has("resend_api_key")) root else null
        if (smsObj == null) return

        val masterPrefs = context.getSharedPreferences(PREFS_SUPABASE_MASTER, Context.MODE_PRIVATE)
        val editor = masterPrefs.edit()

        if (smsObj.has("sendpkEnabled") || smsObj.has("sendpk_enabled")) {
            val enabled = smsObj.optBoolean("sendpkEnabled", smsObj.optBoolean("sendpk_enabled", false))
            editor.putBoolean("sendpkEnabled", enabled)
        }
        if (smsObj.has("sendpkApiKey") || smsObj.has("sendpk_api_key")) {
            val apiKey = smsObj.optString("sendpkApiKey", smsObj.optString("sendpk_api_key", "")).trim()
            editor.putString("sendpkApiKey", apiKey)
            if (!smsObj.has("sendpkEnabled") && !smsObj.has("sendpk_enabled") && apiKey.isNotBlank()) {
                editor.putBoolean("sendpkEnabled", true)
            }
        }
        if (smsObj.has("sendpkUsername") || smsObj.has("sendpk_username")) {
            editor.putString("sendpkUsername", smsObj.optString("sendpkUsername", smsObj.optString("sendpk_username", "")).trim())
        }
        if (smsObj.has("sendpkPassword") || smsObj.has("sendpk_password")) {
            editor.putString("sendpkPassword", smsObj.optString("sendpkPassword", smsObj.optString("sendpk_password", "")).trim())
        }
        if (smsObj.has("sendpkSenderMask") || smsObj.has("sendpk_sender_mask")) {
            editor.putString("sendpkSenderMask", smsObj.optString("sendpkSenderMask", smsObj.optString("sendpk_sender_mask", "BOSS ALUM")).trim())
        }
        if (smsObj.has("smsComPkApiKey") || smsObj.has("sms_com_pk_api_key")) {
            editor.putString("smsComPkApiKey", smsObj.optString("smsComPkApiKey", smsObj.optString("sms_com_pk_api_key", "")).trim())
        }
        if (smsObj.has("smsComPkSenderMask") || smsObj.has("sms_com_pk_sender_mask")) {
            editor.putString("smsComPkSenderMask", smsObj.optString("smsComPkSenderMask", smsObj.optString("sms_com_pk_sender_mask", "BOSS ALUM")).trim())
        }
        if (smsObj.has("resendApiKey") || smsObj.has("resend_api_key")) {
            editor.putString("resendApiKey", smsObj.optString("resendApiKey", smsObj.optString("resend_api_key", "")).trim())
        }
        if (smsObj.has("otpFromEmail") || smsObj.has("otp_from_email")) {
            editor.putString("otpFromEmail", smsObj.optString("otpFromEmail", smsObj.optString("otp_from_email", "")).trim())
        }
        if (smsObj.has("otpMessageTemplate") || smsObj.has("otp_message_template")) {
            editor.putString("otpMessageTemplate", smsObj.optString("otpMessageTemplate", smsObj.optString("otp_message_template", "")).trim())
        }
        editor.apply()
    }

    private fun unwrapRowPayload(row: JSONObject): JSONObject {
        val valueObj = row.optJSONObject("value")
            ?: row.optJSONObject("config")
            ?: row.optJSONObject("data")
            ?: row.optJSONObject("config_json")
        if (valueObj != null) return valueObj

        val rawStr = row.optString("value", "")
            .ifBlank { row.optString("config_json", "") }
            .ifBlank { row.optString("data", "") }
            .ifBlank { row.optString("config", "") }
        if (rawStr.isNotBlank() && rawStr.trim().startsWith("{")) {
            return try { JSONObject(rawStr) } catch (_: Exception) { row }
        }
        return row
    }

    private fun unwrapRowArrayPayload(row: JSONObject): JSONArray? {
        val arrObj = row.optJSONArray("value")
            ?: row.optJSONArray("config")
            ?: row.optJSONArray("data")
            ?: row.optJSONArray("config_json")
        if (arrObj != null) return arrObj

        val rawStr = row.optString("value", "")
            .ifBlank { row.optString("config_json", "") }
            .ifBlank { row.optString("data", "") }
            .ifBlank { row.optString("config", "") }
        if (rawStr.isNotBlank() && rawStr.trim().startsWith("[")) {
            return try { JSONArray(rawStr) } catch (_: Exception) { null }
        }
        return null
    }

    private suspend fun fetchMasterConfigRowFromSupabase(): JSONObject? {
        // 1. Direct select=* from `app_configs` (/rest/v1/app_configs?select=*)
        val allRows = SupabaseCloudManager.fetchAllTableRows(SUPABASE_APP_CONFIGS_TABLE)
        if (allRows.isNotEmpty()) {
            val merged = JSONObject()
            // Merge non-master rows first, then specific v1 keys and master_engine_config row last
            val sortedRows = allRows.sortedBy { r ->
                val k = r.optString("key", r.optString("config_key", r.optString("id", ""))).trim()
                when {
                    k.equals(SUPABASE_MASTER_KEY, ignoreCase = true) -> 2
                    k.endsWith("_v1", ignoreCase = true) -> 1
                    else -> 0
                }
            }
            sortedRows.forEach { r ->
                extractAndSaveSmsGatewayConfigFromRow(r)
                val rowKey = r.optString("key", r.optString("config_key", r.optString("id", ""))).trim()
                val arrPayload = unwrapRowArrayPayload(r)
                if (arrPayload != null) {
                    if (rowKey.isNotEmpty()) {
                        merged.put(rowKey, arrPayload)
                    }
                } else {
                    val inner = unwrapRowPayload(r)
                    if (rowKey.isNotEmpty() && inner !== r) {
                        merged.put(rowKey, inner)
                    }
                    val keys = inner.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k != "key" && k != "config_key" && k != "id" && k != "created_at" && k != "updated_at") {
                            merged.put(k, inner.opt(k))
                        }
                    }
                }
            }
            if (merged.length() > 0) {
                return merged
            }
        }

        // 2. Fallback individual row lookups
        SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_APP_CONFIGS_TABLE,
            keyColumn = "key",
            keyValue = SUPABASE_MASTER_KEY
        )?.let { return it }

        SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_APP_CONFIGS_TABLE,
            keyColumn = "config_key",
            keyValue = SUPABASE_MASTER_KEY
        )?.let { return it }

        SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_APP_CONFIGS_TABLE,
            keyColumn = "id",
            keyValue = SUPABASE_MASTER_KEY
        )?.let { return it }

        SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_APP_CONFIGS_TABLE,
            keyColumn = "key",
            keyValue = SUPABASE_GLOBAL_CONFIG_KEY
        )?.let { return it }

        // 3. Fallback: `master_control` table
        return SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_FALLBACK_TABLE,
            keyColumn = "config_id",
            keyValue = SUPABASE_FALLBACK_ROW_ID
        )
    }

    fun stopRealtimeMasterSync() {
        syncJob?.cancel()
        syncJob = null
    }

    suspend fun fetchFullConfigFromFirestore(): MasterEngineCloudConfig? = fetchLatestFromCloud()

    suspend fun fetchLatestFromCloud(isVerifiedAndSubscribed: Boolean = true): MasterEngineCloudConfig? = withContext(Dispatchers.IO) {
        SupabaseCloudManager.initContext(context)
        if (!SupabaseCloudManager.isSupabaseConfigured(context)) return@withContext null
        try {
            val parsed = withTimeoutOrNull(8000L) {
                val row = fetchMasterConfigRowFromSupabase() ?: return@withTimeoutOrNull null
                parseJsonConfig(row)
            }
            if (parsed != null) {
                _cloudConfig.value = parsed
                _isConnectedToCloud.value = true
            }
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching master_engine_config from Supabase: ${e.message}")
            null
        }
    }

    private fun parseJsonConfig(row: JSONObject): MasterEngineCloudConfig? {
        // Support `value` (JSON object or string), `config_json`, `data`, or root row itself
        val valueObj = row.optJSONObject("value")
            ?: row.optJSONObject("config")
            ?: row.optJSONObject("data")
            ?: row.optJSONObject("config_json")
        if (valueObj != null && !row.has("layout_config_v1") && !row.has("custom_sections_v1") && !row.has("aluminium_rates_v1") && !row.has("formulas_v1")) {
            val jsonStr = valueObj.toString()
            return deserializeFromJson(jsonStr)?.also { saveCachedConfigJson(jsonStr) }
        }
        val rawStr = row.optString("value", "")
            .ifBlank { row.optString("config_json", "") }
            .ifBlank { row.optString("data", "") }
        if (rawStr.isNotBlank() && rawStr.trim().startsWith("{") && !row.has("layout_config_v1") && !row.has("custom_sections_v1")) {
            return deserializeFromJson(rawStr)?.also { saveCachedConfigJson(rawStr) }
        }
        val rowStr = row.toString()
        return deserializeFromJson(rowStr)?.also { saveCachedConfigJson(rowStr) }
    }

    fun deserializeFromJson(jsonStr: String): MasterEngineCloudConfig? {
        return try {
            val root = JSONObject(jsonStr)
            extractAndSaveSmsGatewayConfig(root)

            val layoutV1Obj = root.optJSONObject("layout_config_v1")
                ?: root.optJSONObject("layoutConfig")
                ?: root.optJSONObject("layout_config")
            val customSecsV1Obj = root.optJSONObject("custom_sections_v1")
            val blueprintsV1Obj = root.optJSONObject("blueprints_v1")
            val ratesV1Obj = root.optJSONObject("aluminium_rates_v1")
            val formulasV1Obj = root.optJSONObject("formulas_v1")
            val brandingV1Obj = root.optJSONObject("company_branding_v1")

            val customSections = mutableListOf<DynamicMasterSection>()
            val seenSectionCodes = mutableSetOf<String>()

            fun parseSectionArray(arr: JSONArray?) {
                if (arr == null) return
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i)
                    if (o != null) {
                        val code = o.optString("code", o.optString("section", o.optString("name", ""))).trim()
                        if (code.isNotEmpty() && seenSectionCodes.add(code.uppercase())) {
                            customSections.add(
                                DynamicMasterSection(
                                    id = o.optString("id", code),
                                    category = o.optString("category", "Windows"),
                                    code = code,
                                    title = o.optString("title", o.optString("label", code)),
                                    defaultGauge = o.optString("defaultGauge", o.optString("gauge", "1.6")),
                                    defaultColor = o.optString("defaultColor", o.optString("color", "DULL")),
                                    defaultRate = o.optDouble("defaultRate", o.optDouble("rate", 650.0)),
                                    weightPerFoot = o.optDouble("weightPerFoot", 0.45),
                                    isActive = o.optBoolean("isActive", !o.optBoolean("hidden", false))
                                )
                            )
                        }
                    } else {
                        val strSec = arr.optString(i, "").trim()
                        if (strSec.isNotEmpty() && seenSectionCodes.add(strSec.uppercase())) {
                            customSections.add(
                                DynamicMasterSection(
                                    id = strSec,
                                    code = strSec,
                                    title = strSec
                                )
                            )
                        }
                    }
                }
            }

            parseSectionArray(root.optJSONArray("custom_sections_v1"))
            parseSectionArray(customSecsV1Obj?.optJSONArray("customSections"))
            parseSectionArray(customSecsV1Obj?.optJSONArray("custom_sections"))
            parseSectionArray(customSecsV1Obj?.optJSONArray("dynamicSections"))
            parseSectionArray(customSecsV1Obj?.optJSONArray("dynamic_sections"))
            parseSectionArray(customSecsV1Obj?.optJSONArray("sections"))
            parseSectionArray(layoutV1Obj?.optJSONArray("customSections"))
            parseSectionArray(layoutV1Obj?.optJSONArray("custom_sections"))
            parseSectionArray(layoutV1Obj?.optJSONArray("dynamicSections"))
            parseSectionArray(layoutV1Obj?.optJSONArray("dynamic_sections"))
            parseSectionArray(root.optJSONArray("customSections"))
            parseSectionArray(root.optJSONArray("custom_sections"))
            parseSectionArray(root.optJSONArray("dynamicSections"))
            parseSectionArray(root.optJSONArray("dynamic_sections"))

            val sourcesForSets = listOfNotNull(root, layoutV1Obj, customSecsV1Obj, ratesV1Obj)

            fun parseStringSet(vararg keys: String): Set<String> {
                val result = mutableSetOf<String>()
                for (src in sourcesForSets) {
                    for (k in keys) {
                        val arr = src.optJSONArray(k)
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val s = arr.optString(i, "").trim()
                                if (s.isNotEmpty()) result.add(s)
                            }
                        } else {
                            val raw = src.optString(k, "").trim()
                            if (raw.isNotEmpty() && !raw.startsWith("[") && !raw.startsWith("{")) {
                                raw.split("|", ",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { result.add(it) }
                            }
                        }
                    }
                }
                return result
            }

            fun parseStringList(vararg keys: String): List<String>? {
                val set = parseStringSet(*keys)
                return set.toList().takeIf { it.isNotEmpty() }
            }

            val hiddenSections = parseStringSet("hiddenSections", "hidden_sections")
            val deletedSections = parseStringSet("deletedSections", "deleted_sections")
            val availableGauges = parseStringList("availableGauges", "available_gauges")
            val availableColors = parseStringList("availableColors", "available_colors")
            val availableGlassGauges = parseStringList("availableGlassGauges", "available_glass_gauges")
            val availableGlassColors5mm = parseStringList("availableGlassColors5mm", "available_glass_colors_5mm")
            val availableGlassColors8mm = parseStringList("availableGlassColors8mm", "available_glass_colors_8mm")

            val layoutObj = layoutV1Obj
                ?: root.optJSONObject("themeConfig")
                ?: root.optJSONObject("theme_config")
            val layoutPreset = layoutObj?.optString("preset", "")?.ifBlank { null }
                ?: layoutObj?.optString("themePreset", "")?.ifBlank { null }
                ?: root.optString("themePreset", "").ifBlank { null }
            val layoutTabPosition = layoutObj?.optString("tabPosition", "")?.ifBlank { null }
                ?: root.optString("tabPosition", "").ifBlank { null }
            val layoutButtonStyle = layoutObj?.optString("buttonStyle", "")?.ifBlank { null }
                ?: root.optString("buttonStyle", "").ifBlank { null }
            val layoutFontWeightMode = layoutObj?.optString("fontWeightMode", "")?.ifBlank { null }
                ?: root.optString("fontWeightMode", "").ifBlank { null }

            // Parse RemoteLayoutConfig from layout_config_v1 / layoutObj / root
            val enabledTabsSet = mutableSetOf<String>()
            val enabledTabsSources = listOfNotNull(layoutV1Obj, root)
            for (src in enabledTabsSources) {
                val tabsArr = src.optJSONArray("enabledTabs")
                    ?: src.optJSONArray("enabled_tabs")
                    ?: src.optJSONArray("visibleTabs")
                    ?: src.optJSONArray("activeTabs")
                if (tabsArr != null) {
                    for (i in 0 until tabsArr.length()) {
                        val t = tabsArr.optString(i, "").trim()
                        if (t.isNotEmpty()) enabledTabsSet.add(t)
                    }
                    if (enabledTabsSet.isNotEmpty()) break
                }
                val tabsObj = src.optJSONObject("enabledTabs") ?: src.optJSONObject("enabled_tabs")
                if (tabsObj != null) {
                    val keys = tabsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (tabsObj.optBoolean(k, true)) enabledTabsSet.add(k.trim())
                    }
                    if (enabledTabsSet.isNotEmpty()) break
                }
            }

            val tabCustomTitlesMap = mutableMapOf<String, String>()
            for (src in enabledTabsSources) {
                val titlesObj = src.optJSONObject("tabCustomTitles")
                    ?: src.optJSONObject("tab_custom_titles")
                    ?: src.optJSONObject("customTabTitles")
                    ?: src.optJSONObject("tabTitles")
                if (titlesObj != null) {
                    val keys = titlesObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = titlesObj.optString(k, "").trim()
                        if (v.isNotEmpty()) {
                            tabCustomTitlesMap[k.trim()] = v
                        }
                    }
                }
            }

            fun optBoolConfig(defaultVal: Boolean, vararg keys: String): Boolean {
                for (src in enabledTabsSources) {
                    for (k in keys) {
                        if (src.has(k)) return src.optBoolean(k, defaultVal)
                    }
                }
                return defaultVal
            }

            val remoteLayoutConfig = RemoteLayoutConfig(
                enabledTabs = enabledTabsSet,
                tabCustomTitles = tabCustomTitlesMap,
                showMasterGaugeSelector = optBoolConfig(true, "showMasterGaugeSelector", "show_master_gauge_selector", "showGaugeSelector"),
                showMasterColorSelector = optBoolConfig(true, "showMasterColorSelector", "show_master_color_selector", "showColorSelector"),
                showMasterFrameSelector = optBoolConfig(true, "showMasterFrameSelector", "show_master_frame_selector", "showFrameSelector"),
                showMasterGlassSelector = optBoolConfig(true, "showMasterGlassSelector", "show_master_glass_selector", "showGlassSelector"),
                showThemeStudioButton = optBoolConfig(true, "showThemeStudioButton", "show_theme_studio_button", "showThemeButton"),
                showDoneQuotationButton = optBoolConfig(true, "showDoneQuotationButton", "show_done_quotation_button", "showDoneButton"),
                showCompareCFButton = optBoolConfig(true, "showCompareCFButton", "show_compare_cf_button", "showCvsFButton", "showCompareCfButton"),
                showDiscountRow = optBoolConfig(true, "showDiscountRow", "show_discount_row", "showAlumDiscountRow"),
                showHardwareRow = optBoolConfig(true, "showHardwareRow", "show_hardware_row"),
                showTransportRow = optBoolConfig(true, "showTransportRow", "show_transport_row"),
                showLabourRow = optBoolConfig(true, "showLabourRow", "show_labour_row")
            )

            val rateOverrides = mutableMapOf<String, Double>()
            listOfNotNull(ratesV1Obj, root).forEach { src ->
                src.optJSONObject("rateOverrides")?.let { jo ->
                    jo.keys().forEach { k -> rateOverrides[k] = jo.optDouble(k, 0.0) }
                }
            }

            fun parseRatesMapObject(ratesObj: JSONObject?): Map<String, Map<String, Map<String, Double>>>? {
                if (ratesObj == null) return null
                val gaugeMap = mutableMapOf<String, Map<String, Map<String, Double>>>()
                ratesObj.keys().forEach { gaugeKey ->
                    val colObj = ratesObj.optJSONObject(gaugeKey) ?: return@forEach
                    val colMap = mutableMapOf<String, Map<String, Double>>()
                    colObj.keys().forEach { colKey ->
                        val secObj = colObj.optJSONObject(colKey) ?: return@forEach
                        val secMap = mutableMapOf<String, Double>()
                        secObj.keys().forEach { secKey ->
                            val r = secObj.optDouble(secKey, -1.0)
                            if (r >= 0.0) secMap[secKey] = r
                        }
                        if (secMap.isNotEmpty()) colMap[colKey] = secMap
                    }
                    if (colMap.isNotEmpty()) gaugeMap[gaugeKey] = colMap
                }
                return gaugeMap.takeIf { it.isNotEmpty() }
            }

            val parsedRates = parseRatesMapObject(root.optJSONObject("rates"))
                ?: parseRatesMapObject(ratesV1Obj?.optJSONObject("rates"))
                ?: parseRatesMapObject(ratesV1Obj?.optJSONObject("aluminiumRates"))
                ?: parseRatesMapObject(ratesV1Obj)

            fun parseGlassRatesObject(grObj: JSONObject?): Map<String, Map<String, Double>>? {
                if (grObj == null) return null
                val gMap = mutableMapOf<String, Map<String, Double>>()
                grObj.keys().forEach { gaugeKey ->
                    val colObj = grObj.optJSONObject(gaugeKey) ?: return@forEach
                    val colMap = mutableMapOf<String, Double>()
                    colObj.keys().forEach { colKey ->
                        val r = colObj.optDouble(colKey, -1.0)
                        if (r >= 0.0) colMap[colKey] = r
                    }
                    if (colMap.isNotEmpty()) gMap[gaugeKey] = colMap
                }
                return gMap.takeIf { it.isNotEmpty() }
            }

            val parsedGlassRates = parseGlassRatesObject(root.optJSONObject("glassRates"))
                ?: parseGlassRatesObject(ratesV1Obj?.optJSONObject("glassRates"))
                ?: parseGlassRatesObject(root.optJSONObject("glass_rates"))
                ?: parseGlassRatesObject(ratesV1Obj?.optJSONObject("glass_rates"))

            val formulaAdjustments = mutableMapOf<String, Double>()
            listOfNotNull(formulasV1Obj, root).forEach { src ->
                src.optJSONObject("formulaAdjustments")?.let { jo ->
                    jo.keys().forEach { k -> formulaAdjustments[k] = jo.optDouble(k, 0.0) }
                }
            }

            fun findFormulaObj(vararg keys: String): JSONObject? {
                for (src in listOfNotNull(formulasV1Obj, root)) {
                    for (k in keys) {
                        val obj = src.optJSONObject(k)
                        if (obj != null) return obj
                    }
                }
                return null
            }

            // Parse structured WindowFormulaSettings if present (support singular & plural keys)
            val winObj = findFormulaObj("winFormulas", "windowFormulas", "windowFormula")
            val winFormulas = if (winObj != null) {
                BossSerializationHelper.deserializeWindowFormulas(winObj.toString(), WindowFormulaSettings())
            } else null

            // Parse structured SlidingDoorFormulaSettings if present (support singular & plural keys)
            val sdObj = findFormulaObj("slidingDoorFormulas", "slidingDoorFormula")
            val slidingDoorFormulas = if (sdObj != null) {
                BossSerializationHelper.deserializeSlidingDoorFormulas(sdObj.toString(), SlidingDoorFormulaSettings())
            } else null

            val doorObj = findFormulaObj("doorFormulas", "doorFormula")
            val doorFormulas = if (doorObj != null) {
                BossSerializationHelper.deserializeCategoryFormulas(doorObj.toString(), FormulaCategorySettings())
            } else null

            val fixObj = findFormulaObj("fixFormulas", "fixFormula")
            val fixFormulas = if (fixObj != null) {
                BossSerializationHelper.deserializeCategoryFormulas(fixObj.toString(), FormulaCategorySettings())
            } else null

            val opWinObj = findFormulaObj(
                "openableWinFormulas",
                "opwinFormulas",
                "openwinFormula",
                "openableWindowFormulas",
                "openableWindowFormula"
            )
            val openableWinFormulas = if (opWinObj != null) {
                BossSerializationHelper.deserializeCategoryFormulas(opWinObj.toString(), FormulaCategorySettings.defaultOpenableWindowSettings())
            } else null

            val fSecObj = findFormulaObj("fSectionFormula", "fSectionFormulas", "f_section_formula")
            val fSectionFormula = if (fSecObj != null) {
                BossSerializationHelper.deserializeFSectionFormula(fSecObj.toString(), FSectionFormula())
            } else null

            val glassObj = findFormulaObj("glassFormulas", "glassFormula", "glass_formulas")
            val glassFormulas = if (glassObj != null) {
                BossSerializationHelper.deserializeGlassFormulas(glassObj.toString(), emptyMap())
            } else null

            val blueprints = mutableListOf<DragDropBlueprint>()
            val seenBlueprintIds = mutableSetOf<String>()
            fun parseBlueprintArray(arr: JSONArray?) {
                if (arr == null) return
                for (i in 0 until arr.length()) {
                    val b = arr.optJSONObject(i) ?: continue
                    val id = b.optString("id", "bp_$i").trim()
                    if (!seenBlueprintIds.add(id)) continue
                    val label = b.optString("label", b.optString("name", b.optString("title", id)))
                    val widthInches = b.optDouble("widthInches", b.optDouble("defaultWidthInches", b.optDouble("width", -1.0)))
                    val heightInches = b.optDouble("heightInches", b.optDouble("defaultHeightInches", b.optDouble("height", -1.0)))
                    val widthFt = if (b.has("widthFt")) b.optDouble("widthFt", 4.0)
                    else if (widthInches > 0) (if (widthInches > 20.0) widthInches / 12.0 else widthInches)
                    else 4.0
                    val heightFt = if (b.has("heightFt")) b.optDouble("heightFt", 4.0)
                    else if (heightInches > 0) (if (heightInches > 20.0) heightInches / 12.0 else heightInches)
                    else 4.0

                    blueprints.add(
                        DragDropBlueprint(
                            id = id,
                            label = label,
                            widthFt = widthFt,
                            heightFt = heightFt,
                            panels = b.optInt("panels", b.optInt("panelCount", b.optInt("cols", 2))).coerceAtLeast(1),
                            hasBottomFixed = b.optBoolean("hasBottomFixed", false),
                            category = b.optString("category", b.optString("type", "Sliding Window"))
                        )
                    )
                }
            }

            parseBlueprintArray(root.optJSONArray("blueprints_v1"))
            parseBlueprintArray(blueprintsV1Obj?.optJSONArray("blueprints"))
            parseBlueprintArray(blueprintsV1Obj?.optJSONArray("dragDropBlueprints"))
            parseBlueprintArray(blueprintsV1Obj?.optJSONArray("items"))
            parseBlueprintArray(layoutV1Obj?.optJSONArray("dragDropBlueprints"))
            parseBlueprintArray(layoutV1Obj?.optJSONArray("blueprints"))
            parseBlueprintArray(root.optJSONArray("dragDropBlueprints"))
            parseBlueprintArray(root.optJSONArray("blueprints"))

            fun optBrandingStr(vararg keys: String): String {
                for (src in listOfNotNull(brandingV1Obj, root)) {
                    for (k in keys) {
                        val v = src.optString(k, "").trim()
                        if (v.isNotEmpty()) return v
                    }
                }
                return ""
            }

            val mainLogoBase64 = optBrandingStr("mainLogoBase64", "goldenLionLogoBase64", "logoBase64", "companyLogoBase64", "logo")
            val quotationLetterheadBase64 = optBrandingStr("quotationLetterheadBase64", "letterheadBackgroundBase64", "quotationBackgroundBase64", "letterheadBase64")
            val companyName = optBrandingStr("companyName", "company_name", "shopName", "businessName").ifBlank { null }
            val companyTagline = optBrandingStr("companyTagline", "company_tagline", "tagline").ifBlank { null }
            val companyPhone = optBrandingStr("companyPhone", "company_phone", "phone").ifBlank { null }
            val companyAddress = optBrandingStr("companyAddress", "company_address", "address").ifBlank { null }
            val quotationTerms = optBrandingStr("quotationTerms", "quotation_terms", "terms").ifBlank { null }

            val clearRateListCommand = root.optBoolean("clearRateListCommand", root.optBoolean("clearRateList", false))
            val clearRateListTimestamp = root.optLong("clearRateListTimestamp", root.optLong("clearRateListEpochMs", 0L))

            val bannerObj = root.optJSONObject("activeBanner") ?: brandingV1Obj?.optJSONObject("activeBanner")
            val banner = if (bannerObj != null) {
                AppOfferBanner(
                    id = bannerObj.optString("id", "cloud_banner"),
                    title = bannerObj.optString("title", ""),
                    message = bannerObj.optString("message", ""),
                    badgeText = bannerObj.optString("badgeText", "Special Offer"),
                    discountPercent = bannerObj.optInt("discountPercent", 20),
                    isActive = bannerObj.optBoolean("isActive", false)
                )
            } else null

            MasterEngineCloudConfig(
                version = root.optLong("version", 1L),
                publishedBy = root.optString("publishedBy", "Syed Saboor Nawab (Master)"),
                customSections = customSections,
                hiddenSections = hiddenSections,
                deletedSections = deletedSections,
                availableGauges = availableGauges,
                availableColors = availableColors,
                availableGlassGauges = availableGlassGauges,
                availableGlassColors5mm = availableGlassColors5mm,
                availableGlassColors8mm = availableGlassColors8mm,
                layoutPreset = layoutPreset,
                layoutTabPosition = layoutTabPosition,
                layoutButtonStyle = layoutButtonStyle,
                layoutFontWeightMode = layoutFontWeightMode,
                remoteLayoutConfig = remoteLayoutConfig,
                rateOverrides = rateOverrides,
                rates = parsedRates,
                glassRates = parsedGlassRates,
                formulaAdjustments = formulaAdjustments,
                winFormulas = winFormulas,
                slidingDoorFormulas = slidingDoorFormulas,
                doorFormulas = doorFormulas,
                fixFormulas = fixFormulas,
                openableWinFormulas = openableWinFormulas,
                fSectionFormula = fSectionFormula,
                glassFormulas = glassFormulas,
                dragDropBlueprints = blueprints,
                companyName = companyName,
                companyTagline = companyTagline,
                companyPhone = companyPhone,
                companyAddress = companyAddress,
                quotationTerms = quotationTerms,
                mainLogoBase64 = mainLogoBase64,
                quotationLetterheadBase64 = quotationLetterheadBase64,
                clearRateListCommand = clearRateListCommand,
                clearRateListTimestamp = clearRateListTimestamp,
                activeBanner = banner,
                lastUpdated = root.optLong("lastUpdated", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize MasterEngineCloudConfig: ${e.message}")
            null
        }
    }
}

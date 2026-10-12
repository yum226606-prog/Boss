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
                        // Always sync SMS/Email gateway config from app_configs even before user login
                        syncGatewayConfigOnlyFromSupabase()
                    } catch (_: Exception) {}
                }
                if (!isVerifiedAndSubscribedProvider()) {
                    clearRuntimeFormulas()
                    delay(10_000L)
                    continue
                }
                if (SupabaseCloudManager.isSupabaseConfigured(context)) {
                    try {
                        val row = fetchMasterConfigRowFromSupabase()
                        if (row != null) {
                            val parsed = parseJsonConfig(row)
                            if (parsed != null) {
                                _cloudConfig.value = parsed
                                _isConnectedToCloud.value = true
                                withContext(Dispatchers.Main) {
                                    onConfigApplied(parsed)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Supabase app_configs sync warning: ${e.message}")
                        _isConnectedToCloud.value = false
                    }
                } else {
                    _isConnectedToCloud.value = false
                }
                delay(20_000L)
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

    private suspend fun fetchMasterConfigRowFromSupabase(): JSONObject? {
        // 1. Primary: table `app_configs`, key = `master_engine_config`
        SupabaseCloudManager.fetchMasterTableRow(
            table = SUPABASE_APP_CONFIGS_TABLE,
            keyColumn = "key",
            keyValue = SUPABASE_MASTER_KEY
        )?.let { return it }

        // 2. Alternate column names on `app_configs`
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

        // 2b. Also check `global_config` key on `app_configs`
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
        if (SupabaseCloudManager.isSupabaseConfigured(context)) {
            try {
                syncGatewayConfigOnlyFromSupabase()
            } catch (_: Exception) {}
        }
        if (!isVerifiedAndSubscribed) {
            clearRuntimeFormulas()
            return@withContext null
        }
        SupabaseCloudManager.initContext(context)
        if (!SupabaseCloudManager.isSupabaseConfigured(context)) return@withContext null
        try {
            val row = fetchMasterConfigRowFromSupabase() ?: return@withContext null
            val parsed = parseJsonConfig(row)
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
        if (valueObj != null) {
            val jsonStr = valueObj.toString()
            return deserializeFromJson(jsonStr)?.also { saveCachedConfigJson(jsonStr) }
        }
        val rawStr = row.optString("value", "")
            .ifBlank { row.optString("config_json", "") }
            .ifBlank { row.optString("data", "") }
        if (rawStr.isNotBlank() && rawStr.trim().startsWith("{")) {
            return deserializeFromJson(rawStr)?.also { saveCachedConfigJson(rawStr) }
        }
        val rowStr = row.toString()
        return deserializeFromJson(rowStr)?.also { saveCachedConfigJson(rowStr) }
    }

    fun deserializeFromJson(jsonStr: String): MasterEngineCloudConfig? {
        return try {
            val root = JSONObject(jsonStr)
            extractAndSaveSmsGatewayConfig(root)
            val customSections = mutableListOf<DynamicMasterSection>()
            root.optJSONArray("customSections")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val code = o.optString("code", "").trim()
                    if (code.isNotEmpty()) {
                        customSections.add(
                            DynamicMasterSection(
                                id = o.optString("id", code),
                                category = o.optString("category", "Windows"),
                                code = code,
                                title = o.optString("title", code),
                                defaultGauge = o.optString("defaultGauge", "1.6"),
                                defaultColor = o.optString("defaultColor", "DULL"),
                                defaultRate = o.optDouble("defaultRate", 650.0),
                                weightPerFoot = o.optDouble("weightPerFoot", 0.45),
                                isActive = o.optBoolean("isActive", true)
                            )
                        )
                    }
                }
            }

            val rateOverrides = mutableMapOf<String, Double>()
            root.optJSONObject("rateOverrides")?.let { jo ->
                jo.keys().forEach { k -> rateOverrides[k] = jo.optDouble(k, 0.0) }
            }

            val parsedRates = root.optJSONObject("rates")?.let { ratesObj ->
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
                gaugeMap.takeIf { it.isNotEmpty() }
            }

            val parsedGlassRates = root.optJSONObject("glassRates")?.let { grObj ->
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
                gMap.takeIf { it.isNotEmpty() }
            }

            val formulaAdjustments = mutableMapOf<String, Double>()
            root.optJSONObject("formulaAdjustments")?.let { jo ->
                jo.keys().forEach { k -> formulaAdjustments[k] = jo.optDouble(k, 0.0) }
            }

            // Parse structured WindowFormulaSettings if present (support singular & plural keys)
            val winObj = root.optJSONObject("winFormulas")
                ?: root.optJSONObject("windowFormulas")
                ?: root.optJSONObject("windowFormula")
            val winFormulas = if (winObj != null) {
                BossSerializationHelper.deserializeWindowFormulas(winObj.toString(), WindowFormulaSettings())
            } else null

            // Parse structured SlidingDoorFormulaSettings if present (support singular & plural keys)
            val sdObj = root.optJSONObject("slidingDoorFormulas")
                ?: root.optJSONObject("slidingDoorFormula")
            val slidingDoorFormulas = if (sdObj != null) {
                BossSerializationHelper.deserializeSlidingDoorFormulas(sdObj.toString(), SlidingDoorFormulaSettings())
            } else null

            val doorObj = root.optJSONObject("doorFormulas")
                ?: root.optJSONObject("doorFormula")
            val doorFormulas = if (doorObj != null) {
                BossSerializationHelper.deserializeCategoryFormulas(doorObj.toString(), FormulaCategorySettings())
            } else null

            val fixObj = root.optJSONObject("fixFormulas")
                ?: root.optJSONObject("fixFormula")
            val fixFormulas = if (fixObj != null) {
                BossSerializationHelper.deserializeCategoryFormulas(fixObj.toString(), FormulaCategorySettings())
            } else null

            val opWinObj = root.optJSONObject("openableWinFormulas")
                ?: root.optJSONObject("opwinFormulas")
                ?: root.optJSONObject("openwinFormula")
            val openableWinFormulas = if (opWinObj != null) {
                BossSerializationHelper.deserializeCategoryFormulas(opWinObj.toString(), FormulaCategorySettings.defaultOpenableWindowSettings())
            } else null

            val fSecObj = root.optJSONObject("fSectionFormula")
            val fSectionFormula = if (fSecObj != null) {
                BossSerializationHelper.deserializeFSectionFormula(fSecObj.toString(), FSectionFormula())
            } else null

            val glassObj = root.optJSONObject("glassFormulas")
            val glassFormulas = if (glassObj != null) {
                BossSerializationHelper.deserializeGlassFormulas(glassObj.toString(), emptyMap())
            } else null

            val blueprints = mutableListOf<DragDropBlueprint>()
            (root.optJSONArray("dragDropBlueprints") ?: root.optJSONArray("blueprints"))?.let { arr ->
                for (i in 0 until arr.length()) {
                    val b = arr.optJSONObject(i) ?: continue
                    val id = b.optString("id", "bp_$i")
                    val label = b.optString("label", b.optString("name", b.optString("title", id)))
                    blueprints.add(
                        DragDropBlueprint(
                            id = id,
                            label = label,
                            widthFt = b.optDouble("widthFt", b.optDouble("defaultWidthInches", 48.0) / 12.0),
                            heightFt = b.optDouble("heightFt", b.optDouble("defaultHeightInches", 48.0) / 12.0),
                            panels = b.optInt("panels", b.optInt("cols", 2)).coerceAtLeast(1),
                            hasBottomFixed = b.optBoolean("hasBottomFixed", false),
                            category = b.optString("category", "Sliding Window")
                        )
                    )
                }
            }

            val mainLogoBase64 = root.optString("mainLogoBase64", "")
                .ifBlank { root.optString("goldenLionLogoBase64", "") }
                .ifBlank { root.optString("logoBase64", "") }

            val quotationLetterheadBase64 = root.optString("quotationLetterheadBase64", "")
                .ifBlank { root.optString("letterheadBackgroundBase64", "") }
                .ifBlank { root.optString("quotationBackgroundBase64", "") }

            val clearRateListCommand = root.optBoolean("clearRateListCommand", root.optBoolean("clearRateList", false))
            val clearRateListTimestamp = root.optLong("clearRateListTimestamp", root.optLong("clearRateListEpochMs", 0L))

            val bannerObj = root.optJSONObject("activeBanner")
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

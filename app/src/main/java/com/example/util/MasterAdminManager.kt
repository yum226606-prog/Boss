package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.AdminAuditEntry
import com.example.model.AppOfferBanner
import com.example.model.ClientSupportRequest
import com.example.model.MasterAdminState
import com.example.model.PromotionalOffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class MasterAdminManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var offerWatcherJob: Job? = null

    companion object {
        private const val TAG = "MasterAdminManager"
        private const val PREFS_NAME = "boss_master_admin_prefs"
        private const val KEY_OFFER_ACTIVE = "key_offer_active"
        private const val KEY_OFFER_TITLE = "key_offer_title"
        private const val KEY_OFFER_DESC = "key_offer_desc"
        private const val KEY_OFFER_DISCOUNT = "key_offer_discount"
        private const val KEY_OFFER_CODE = "key_offer_code"
        private const val KEY_OFFER_VALID_UNTIL = "key_offer_valid_until"
        private const val KEY_OFFER_DURATION_MONTHS = "key_offer_duration_months"
        private const val KEY_OFFER_SPECIAL_PRICE = "key_offer_special_price"
        private const val KEY_OFFER_TARGET_TIER = "key_offer_target_tier"
        private const val KEY_GLOBAL_CUSTOM_SECTIONS = "key_global_custom_sections"
        private const val KEY_GLOBAL_RATE_OVERRIDES = "key_global_rate_overrides"
        private const val KEY_AUDIT_LOGS = "key_admin_audit_logs"

        @Volatile
        private var INSTANCE: MasterAdminManager? = null

        fun getInstance(context: Context): MasterAdminManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MasterAdminManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getActiveOffer(): AppOfferBanner? {
        val promo = loadPromotionalOffer()
        return if (promo.isActive && promo.title.isNotBlank()) promo.toOfferBanner() else null
    }

    fun getActiveOfferBanner(): AppOfferBanner? = getActiveOffer()

    fun getGlobalCustomSections(): List<String> = loadGlobalCustomSections()

    fun getGlobalRateOverrides(): Map<String, Double> = loadGlobalRateOverrides()

    fun submitSupportRequest(req: ClientSupportRequest) {
        appendAuditLog("SUPPORT_REQUEST", "${req.requestType}: ${req.message} (${req.userName})")
        FirebaseCloudManager.submitSupportRequestToAdmin(appContext, req)
        scope.launch {
            SupabaseCloudManager.initContext(appContext)
            if (SupabaseCloudManager.isSupabaseConfigured(appContext)) {
                val payload = JSONObject().apply {
                    put("id", req.id)
                    put("type", "CLIENT_SUPPORT_REQUEST")
                    put("title", "${req.requestType} from ${req.userName}")
                    put("message", req.message)
                    put("user_name", req.userName)
                    put("phone", req.userPhone)
                    put("cnic", req.userCnic)
                    put("email", req.userEmail)
                    put("request_type", req.requestType)
                    put("status", "UNREAD")
                    put("created_at", req.createdAt)
                }
                SupabaseCloudManager.upsertMasterTableRow(
                    table = "admin_notifications",
                    conflictColumn = "id",
                    payload = payload
                )
            }
        }
    }

    fun loadMasterAdminState(): MasterAdminState {
        return MasterAdminState(
            promotionalOffer = loadPromotionalOffer(),
            globalCustomSections = loadGlobalCustomSections(),
            globalSectionRates12mm = loadGlobalRateOverrides(),
            auditLogs = loadAuditLogs()
        )
    }

    fun savePromotionalOffer(offer: PromotionalOffer) {
        prefs.edit()
            .putBoolean(KEY_OFFER_ACTIVE, offer.isActive)
            .putString(KEY_OFFER_TITLE, offer.title)
            .putString(KEY_OFFER_DESC, offer.description)
            .putInt(KEY_OFFER_DISCOUNT, offer.discountPercent)
            .putString(KEY_OFFER_CODE, offer.promoCode.uppercase().trim())
            .putString(KEY_OFFER_VALID_UNTIL, offer.validUntilText)
            .putInt(KEY_OFFER_DURATION_MONTHS, offer.offerDurationMonths)
            .putInt(KEY_OFFER_SPECIAL_PRICE, offer.specialPricePkr)
            .putString(KEY_OFFER_TARGET_TIER, offer.targetTierName)
            .apply()
        appendAuditLog(
            "PROMO_OFFER_UPDATE",
            "Offer '${offer.title}' (${offer.offerDurationMonths}M @ Rs.${offer.specialPricePkr}, active=${offer.isActive})"
        )

        // Sync Promotional Offer to Supabase Master Table
        scope.launch {
            SupabaseCloudManager.initContext(appContext)
            if (SupabaseCloudManager.isSupabaseConfigured(appContext)) {
                val payload = JSONObject().apply {
                    put("config_key", "promotional_offer")
                    put("is_active", offer.isActive)
                    put("title", offer.title)
                    put("description", offer.description)
                    put("discount_percent", offer.discountPercent)
                    put("promo_code", offer.promoCode.uppercase().trim())
                    put("valid_until_text", offer.validUntilText)
                    put("offer_duration_months", offer.offerDurationMonths)
                    put("special_price_pkr", offer.specialPricePkr)
                    put("target_tier_name", offer.targetTierName)
                    put("updated_at_epoch_ms", System.currentTimeMillis())
                }
                SupabaseCloudManager.upsertMasterTableRow(
                    table = "master_config",
                    conflictColumn = "config_key",
                    payload = payload
                )
            }
        }
    }

    fun startRealtimeOfferListener(onOfferUpdated: (AppOfferBanner?) -> Unit) {
        SupabaseCloudManager.initContext(appContext)
        stopRealtimeOfferListener()

        offerWatcherJob = scope.launch {
            while (isActive) {
                if (SupabaseCloudManager.isSupabaseConfigured(appContext)) {
                    try {
                        val row = SupabaseCloudManager.fetchMasterTableRow(
                            table = "master_config",
                            keyColumn = "config_key",
                            keyValue = "promotional_offer"
                        )
                        if (row != null) {
                            val cloudOffer = PromotionalOffer(
                                isActive = row.optBoolean("is_active", false),
                                title = row.optString("title", "Special Limited Offer"),
                                description = row.optString("description", ""),
                                discountPercent = row.optInt("discount_percent", 20),
                                promoCode = row.optString("promo_code", "BOSS5M"),
                                validUntilText = row.optString("valid_until_text", "Limited Time Offer"),
                                offerDurationMonths = row.optInt("offer_duration_months", 5),
                                specialPricePkr = row.optInt("special_price_pkr", 4500),
                                targetTierName = row.optString("target_tier_name", "PRO_5_MONTHS")
                            )
                            prefs.edit()
                                .putBoolean(KEY_OFFER_ACTIVE, cloudOffer.isActive)
                                .putString(KEY_OFFER_TITLE, cloudOffer.title)
                                .putString(KEY_OFFER_DESC, cloudOffer.description)
                                .putInt(KEY_OFFER_DISCOUNT, cloudOffer.discountPercent)
                                .putString(KEY_OFFER_CODE, cloudOffer.promoCode)
                                .putString(KEY_OFFER_VALID_UNTIL, cloudOffer.validUntilText)
                                .putInt(KEY_OFFER_DURATION_MONTHS, cloudOffer.offerDurationMonths)
                                .putInt(KEY_OFFER_SPECIAL_PRICE, cloudOffer.specialPricePkr)
                                .putString(KEY_OFFER_TARGET_TIER, cloudOffer.targetTierName)
                                .apply()
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                onOfferUpdated(if (cloudOffer.isActive && cloudOffer.title.isNotBlank()) cloudOffer.toOfferBanner() else null)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Supabase offer listener warning: ${e.message}")
                    }
                }
                delay(25_000L)
            }
        }
    }

    fun stopRealtimeOfferListener() {
        offerWatcherJob?.cancel()
        offerWatcherJob = null
    }

    fun loadPromotionalOffer(): PromotionalOffer {
        return PromotionalOffer(
            isActive = prefs.getBoolean(KEY_OFFER_ACTIVE, false),
            title = prefs.getString(KEY_OFFER_TITLE, "5-Month Special Discount Package") ?: "5-Month Special Discount Package",
            description = prefs.getString(
                KEY_OFFER_DESC,
                "Unlock 5 Months Full Pro Access at a special discounted rate!"
            ) ?: "",
            discountPercent = prefs.getInt(KEY_OFFER_DISCOUNT, 25),
            promoCode = prefs.getString(KEY_OFFER_CODE, "BOSS5M") ?: "BOSS5M",
            validUntilText = prefs.getString(KEY_OFFER_VALID_UNTIL, "Limited Time Offer") ?: "Limited Time Offer",
            offerDurationMonths = prefs.getInt(KEY_OFFER_DURATION_MONTHS, 5),
            specialPricePkr = prefs.getInt(KEY_OFFER_SPECIAL_PRICE, 4500),
            targetTierName = prefs.getString(KEY_OFFER_TARGET_TIER, "PRO_5_MONTHS") ?: "PRO_5_MONTHS"
        )
    }

    fun addGlobalCustomSection(sectionName: String) {
        val clean = sectionName.trim().uppercase()
        if (clean.isBlank()) return
        val current = loadGlobalCustomSections().toMutableList()
        if (!current.contains(clean)) {
            current.add(clean)
            prefs.edit().putString(KEY_GLOBAL_CUSTOM_SECTIONS, current.joinToString("|")).apply()
            appendAuditLog("SECTION_ADDED", "Added custom section '$clean'")
        }
    }

    fun removeGlobalCustomSection(sectionName: String) {
        val clean = sectionName.trim().uppercase()
        val current = loadGlobalCustomSections().toMutableList()
        if (current.remove(clean)) {
            prefs.edit().putString(KEY_GLOBAL_CUSTOM_SECTIONS, current.joinToString("|")).apply()
            appendAuditLog("SECTION_REMOVED", "Removed custom section '$clean'")
        }
    }

    fun loadGlobalCustomSections(): List<String> {
        val raw = prefs.getString(KEY_GLOBAL_CUSTOM_SECTIONS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun saveGlobalRateOverride(section: String, rate12mm: Double) {
        val current = loadGlobalRateOverrides().toMutableMap()
        current[section.uppercase().trim()] = rate12mm
        val encoded = current.entries.joinToString("|") { "${it.key}:${it.value}" }
        prefs.edit().putString(KEY_GLOBAL_RATE_OVERRIDES, encoded).apply()
    }

    fun loadGlobalRateOverrides(): Map<String, Double> {
        val raw = prefs.getString(KEY_GLOBAL_RATE_OVERRIDES, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Double>()
        raw.split("|").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val r = parts[1].toDoubleOrNull()
                if (r != null) map[parts[0]] = r
            }
        }
        return map
    }

    fun appendAuditLog(action: String, details: String) {
        val current = loadAuditLogs().toMutableList()
        current.add(0, AdminAuditEntry(System.currentTimeMillis(), action, details))
        val trimmed = current.take(40)
        val encoded = trimmed.joinToString("\n") { "${it.timestampMs}:::${it.action}:::${it.details}" }
        prefs.edit().putString(KEY_AUDIT_LOGS, encoded).apply()
    }

    fun loadAuditLogs(): List<AdminAuditEntry> {
        val raw = prefs.getString(KEY_AUDIT_LOGS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split(":::")
            if (parts.size == 3) {
                AdminAuditEntry(
                    timestampMs = parts[0].toLongOrNull() ?: 0L,
                    action = parts[1],
                    details = parts[2]
                )
            } else null
        }
    }
}

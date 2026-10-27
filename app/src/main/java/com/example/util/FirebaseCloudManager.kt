package com.example.util

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.MerchantPaymentAccounts
import com.example.model.SubscriptionPaymentRecord
import com.example.model.SubscriptionState
import com.example.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

typealias SupabaseCloudManager = FirebaseCloudManager

/**
 * Direct Supabase Cloud & Security Manager for Boss Aluminium Client App.
 * Connects the Client App directly to the Master/Admin App's Supabase backend
 * (PostgREST + Auth + Storage + Realtime Security Polling) with zero Firebase dependency.
 */
object FirebaseCloudManager {

    private const val TAG = "SupabaseCloudManager"
    private const val PREFS_SUPABASE = "boss_supabase_master_prefs"
    private const val KEY_CUSTOM_SUPABASE_URL = "custom_supabase_url"
    private const val KEY_CUSTOM_SUPABASE_KEY = "custom_supabase_anon_key"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var watcherJob: Job? = null

    fun initContext(context: Context) {
        appContext = context.applicationContext
    }

    private const val DEFAULT_SUPABASE_URL = "https://njykxcvfckzwedginpvh.supabase.co"
    private const val DEFAULT_SUPABASE_KEY = "sb_publishable_7aV54en4ydYJGJ--lgIufA_DOU8h4X-"

    fun getSupabaseUrl(context: Context? = appContext): String {
        val prefs = context?.getSharedPreferences(PREFS_SUPABASE, Context.MODE_PRIVATE)
        val custom = prefs?.getString(KEY_CUSTOM_SUPABASE_URL, null)?.trim().orEmpty()
        if (custom.isNotBlank() && custom.startsWith("http") && !custom.contains("your-project-id") && !custom.contains("dkeabwbfpkbiskfvhpqe")) {
            return custom.trimEnd('/')
        }
        val envUrl = try {
            BuildConfig.SUPABASE_URL.trim()
        } catch (_: Exception) {
            ""
        }
        return if (envUrl.isNotBlank() &&
            !envUrl.contains("your-project-id") &&
            !envUrl.contains("dkeabwbfpkbiskfvhpqe") &&
            !envUrl.equals("DUMMY_KEY", ignoreCase = true) &&
            envUrl.startsWith("http")
        ) {
            envUrl.trimEnd('/')
        } else {
            DEFAULT_SUPABASE_URL
        }
    }

    fun getSupabaseAnonKey(context: Context? = appContext): String {
        val prefs = context?.getSharedPreferences(PREFS_SUPABASE, Context.MODE_PRIVATE)
        val custom = prefs?.getString(KEY_CUSTOM_SUPABASE_KEY, null)?.trim().orEmpty()
        if (custom.isNotBlank() && !custom.contains("your_supabase_anon_key") && !custom.contains("c988H9j-4-N3s63LBMV4sA")) {
            return custom
        }
        val envKey = try {
            BuildConfig.SUPABASE_ANON_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        return if (envKey.isNotBlank() &&
            !envKey.contains("your_supabase_anon_key") &&
            !envKey.contains("c988H9j-4-N3s63LBMV4sA") &&
            !envKey.equals("DUMMY_KEY", ignoreCase = true)
        ) {
            envKey
        } else {
            DEFAULT_SUPABASE_KEY
        }
    }

    fun saveSupabaseConnectionConfig(context: Context, url: String, anonKey: String) {
        appContext = context.applicationContext
        context.getSharedPreferences(PREFS_SUPABASE, Context.MODE_PRIVATE).edit()
            .putString(KEY_CUSTOM_SUPABASE_URL, url.trim().trimEnd('/'))
            .putString(KEY_CUSTOM_SUPABASE_KEY, anonKey.trim())
            .apply()
    }

    fun isSupabaseConfigured(context: Context? = appContext): Boolean {
        val url = getSupabaseUrl(context)
        val key = getSupabaseAnonKey(context)
        return url.isNotBlank() && key.isNotBlank()
    }

    fun isFirebaseAvailable(context: Context): Boolean {
        initContext(context)
        return isSupabaseConfigured(context)
    }

    /**
     * Returns true if SendPK or SMS.com.pk is actively enabled and configured (via Cloud Admin sync or BuildConfig).
     */
    fun isSendPkOrLocalSmsConfigured(context: Context? = appContext): Boolean {
        val prefs = context?.getSharedPreferences(PREFS_SUPABASE, Context.MODE_PRIVATE)
        val hasExplicitSendPkFlag = prefs?.contains("sendpkEnabled") == true
        val sendPkEnabled = prefs?.getBoolean("sendpkEnabled", false) ?: false

        val cloudSendPkKey = prefs?.getString("sendpkApiKey", null)?.trim().orEmpty()
        val cloudSendPkUser = prefs?.getString("sendpkUsername", null)?.trim().orEmpty()
        val cloudSendPkPass = prefs?.getString("sendpkPassword", null)?.trim().orEmpty()
        val cloudSmsComPkKey = prefs?.getString("smsComPkApiKey", null)?.trim().orEmpty()

        val envSendPkKey = try { BuildConfig.SENDPK_API_KEY.trim() } catch (_: Exception) { "" }
        val envSendPkUser = try { BuildConfig.SENDPK_USERNAME.trim() } catch (_: Exception) { "" }
        val envSendPkPass = try { BuildConfig.SENDPK_PASSWORD.trim() } catch (_: Exception) { "" }
        val envSmsComPkKey = try { BuildConfig.SMS_COM_PK_API_KEY.trim() } catch (_: Exception) { "" }

        val effectiveSendPkKey = cloudSendPkKey.ifBlank { envSendPkKey }
        val effectiveSendPkUser = cloudSendPkUser.ifBlank { envSendPkUser }
        val effectiveSendPkPass = cloudSendPkPass.ifBlank { envSendPkPass }
        val effectiveSmsComPkKey = cloudSmsComPkKey.ifBlank { envSmsComPkKey }

        val hasValidSendPkKey = effectiveSendPkKey.isNotBlank() &&
            !effectiveSendPkKey.startsWith("your_") &&
            effectiveSendPkKey != "DUMMY_KEY"
        val hasValidSendPkUserPass = effectiveSendPkUser.isNotBlank() &&
            !effectiveSendPkUser.startsWith("your_") &&
            effectiveSendPkPass.isNotBlank() &&
            !effectiveSendPkPass.startsWith("your_")
        val hasValidSmsComPk = effectiveSmsComPkKey.isNotBlank() &&
            !effectiveSmsComPkKey.startsWith("your_") &&
            effectiveSmsComPkKey != "DUMMY_KEY"

        val sendPkReady = if (hasExplicitSendPkFlag) {
            sendPkEnabled && (hasValidSendPkKey || hasValidSendPkUserPass)
        } else {
            hasValidSendPkKey || hasValidSendPkUserPass
        }
        return sendPkReady || hasValidSmsComPk
    }

    /**
     * Returns true if Resend Email OTP API is configured via Cloud Admin sync or BuildConfig.
     */
    fun isEmailOtpApiConfigured(context: Context? = appContext): Boolean {
        val prefs = context?.getSharedPreferences(PREFS_SUPABASE, Context.MODE_PRIVATE)
        val cloudResendKey = prefs?.getString("resendApiKey", null)?.trim().orEmpty()
        val envResendKey = try { BuildConfig.RESEND_API_KEY.trim() } catch (_: Exception) { "" }
        val effectiveKey = cloudResendKey.ifBlank { envResendKey }
        return effectiveKey.isNotBlank() &&
            !effectiveKey.startsWith("re_your_") &&
            !effectiveKey.startsWith("your_") &&
            effectiveKey != "DUMMY_KEY"
    }

    private fun buildSupabaseRequest(
        path: String,
        method: String = "GET",
        jsonBody: String? = null,
        preferHeader: String? = "resolution=merge-duplicates,return=representation"
    ): Request? {
        val baseUrl = getSupabaseUrl()
        val apiKey = getSupabaseAnonKey()
        if (baseUrl.isBlank() || apiKey.isBlank()) return null

        val fullUrl = if (path.startsWith("http")) path else "$baseUrl$path"
        val builder = Request.Builder()
            .url(fullUrl)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")

        if (!preferHeader.isNullOrBlank()) {
            builder.addHeader("Prefer", preferHeader)
        }

        val body = jsonBody?.toRequestBody("application/json; charset=utf-8".toMediaType())
        when (method.uppercase()) {
            "POST" -> builder.post(body ?: "{}".toRequestBody("application/json".toMediaType()))
            "PATCH" -> builder.patch(body ?: "{}".toRequestBody("application/json".toMediaType()))
            "DELETE" -> if (body != null) builder.delete(body) else builder.delete()
            else -> builder.get()
        }
        return builder.build()
    }

    /**
     * Sends a 6-digit OTP to the user's phone number via Pakistan Local SMS Gateways
     * (SendPK / SMS.com.pk) and syncs with the Supabase Master OTP Table.
     */
    fun sendPhoneOtp(
        activity: Context,
        phoneNumber: String,
        generatedOtpCode: String? = null,
        onCodeSent: (verificationId: String, autoCode: String?) -> Unit = { _, _ -> },
        onVerified: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        initContext(activity)
        val formattedPhone = formatPakistanPhone(phoneNumber)
        val codeToSend = generatedOtpCode ?: (100000..999999).random().toString()

        bgScope.launch {
            // 1. Try Pakistan Local SMS Gateways: SendPK (sendpk.com) or SMS.com.pk
            val localSmsProvider = sendPakistanLocalSmsOtp(formattedPhone, codeToSend)
            if (localSmsProvider != null) {
                withContext(Dispatchers.Main) {
                    onCodeSent(localSmsProvider, codeToSend)
                }
            }

            // 2. Sync to Supabase Master `otp_verifications` table
            if (isSupabaseConfigured(activity)) {
                try {
                    val otpPayload = JSONObject().apply {
                        put("phone", formattedPhone)
                        put("otp_code", codeToSend)
                        put("channel", localSmsProvider ?: "SMS_PAKISTAN_LOCAL")
                        put("status", if (localSmsProvider != null) "DELIVERED" else "PENDING")
                        put("created_at", System.currentTimeMillis())
                    }
                    val req = buildSupabaseRequest(
                        path = "/rest/v1/otp_verifications",
                        method = "POST",
                        jsonBody = otpPayload.toString()
                    )
                    if (req != null) {
                        httpClient.newCall(req).execute().use { /* logged on master */ }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Supabase OTP table sync warning: ${e.message}")
                }
            }

            if (localSmsProvider == null) {
                withContext(Dispatchers.Main) {
                    onCodeSent("SUPABASE_MASTER_OTP", codeToSend)
                }
            }
        }
    }

    /**
     * Dispatches OTP SMS via Pakistan Local Gateways:
     * 1. SendPK (https://sendpk.com/api/sms.php)
     * 2. SMS.com.pk (https://sms.com.pk/api/sms.php)
     * Returns the gateway identifier ("SENDPK" or "SMS_COM_PK") if delivered, or null if unconfigured.
     */
    private suspend fun sendPakistanLocalSmsOtp(toPhoneE164: String, otpCode: String): String? =
        withContext(Dispatchers.IO) {
            val mobile92 = toPhoneE164.filter { it.isDigit() }.let { digits ->
                when {
                    digits.startsWith("92") -> digits
                    digits.startsWith("03") -> "92${digits.substring(1)}"
                    digits.startsWith("3") -> "92$digits"
                    else -> digits
                }
            }
            val prefs = appContext?.getSharedPreferences(PREFS_SUPABASE, Context.MODE_PRIVATE)
            val customTemplate = prefs?.getString("otpMessageTemplate", null)?.trim().orEmpty()
            val messageBody = if (customTemplate.isNotBlank()) {
                when {
                    customTemplate.contains("{OTP}", ignoreCase = true) ->
                        customTemplate.replace("{OTP}", otpCode, ignoreCase = true)
                    customTemplate.contains("{CODE}", ignoreCase = true) ->
                        customTemplate.replace("{CODE}", otpCode, ignoreCase = true)
                    customTemplate.contains("%s") ->
                        customTemplate.replace("%s", otpCode)
                    else -> "$customTemplate $otpCode"
                }
            } else {
                "BOSS Aluminium & Glass Verification Code: $otpCode. Valid for 10 minutes. Do not share this code."
            }

            // 1. Try SendPK Gateway (https://sendpk.com/api/sms.php) - Cloud SharedPreferences first, then BuildConfig
            try {
                val hasExplicitEnable = prefs?.contains("sendpkEnabled") == true
                val sendPkEnabled = prefs?.getBoolean("sendpkEnabled", true) ?: true

                val cloudApiKey = prefs?.getString("sendpkApiKey", null)?.trim().orEmpty()
                val cloudUser = prefs?.getString("sendpkUsername", null)?.trim().orEmpty()
                val cloudPass = prefs?.getString("sendpkPassword", null)?.trim().orEmpty()
                val cloudMask = prefs?.getString("sendpkSenderMask", null)?.trim().orEmpty()

                val sendPkApiKey = cloudApiKey.ifBlank {
                    try { BuildConfig.SENDPK_API_KEY.trim() } catch (_: Exception) { "" }
                }
                val sendPkUser = cloudUser.ifBlank {
                    try { BuildConfig.SENDPK_USERNAME.trim() } catch (_: Exception) { "" }
                }
                val sendPkPass = cloudPass.ifBlank {
                    try { BuildConfig.SENDPK_PASSWORD.trim() } catch (_: Exception) { "" }
                }
                val sendPkMask = cloudMask.ifBlank {
                    try { BuildConfig.SENDPK_SENDER_MASK.trim() } catch (_: Exception) { "BOSS ALUM" }
                }.ifBlank { "BOSS ALUM" }

                val hasApiKey = sendPkApiKey.isNotBlank() &&
                    !sendPkApiKey.startsWith("your_") &&
                    sendPkApiKey != "DUMMY_KEY"
                val hasUserPass = sendPkUser.isNotBlank() &&
                    !sendPkUser.startsWith("your_") &&
                    sendPkPass.isNotBlank() &&
                    !sendPkPass.startsWith("your_")

                val canUseSendPk = (!hasExplicitEnable || sendPkEnabled) && (hasApiKey || hasUserPass)

                if (canUseSendPk) {
                    val urlBuilder = Uri.parse("https://sendpk.com/api/sms.php").buildUpon()
                    if (hasApiKey) {
                        urlBuilder.appendQueryParameter("api_key", sendPkApiKey)
                    } else {
                        urlBuilder.appendQueryParameter("username", sendPkUser)
                        urlBuilder.appendQueryParameter("password", sendPkPass)
                    }
                    urlBuilder.appendQueryParameter("sender", sendPkMask)
                    urlBuilder.appendQueryParameter("mobile", mobile92)
                    urlBuilder.appendQueryParameter("message", messageBody)
                    urlBuilder.appendQueryParameter("format", "json")

                    val request = Request.Builder()
                        .url(urlBuilder.build().toString())
                        .get()
                        .build()

                    val ok = httpClient.newCall(request).execute().use { resp ->
                        val bodyStr = resp.body?.string().orEmpty()
                        resp.isSuccessful && !bodyStr.contains("\"status\":\"false\"", ignoreCase = true)
                    }
                    if (ok) return@withContext "SENDPK"
                }
            } catch (e: Exception) {
                Log.e(TAG, "SendPK SMS gateway error: ${e.message}")
            }

            // 2. Try SMS.com.pk Gateway (https://sms.com.pk/api/sms.php) - Cloud SharedPreferences first, then BuildConfig
            try {
                val cloudSmsComPkKey = prefs?.getString("smsComPkApiKey", null)?.trim().orEmpty()
                val cloudSmsComPkMask = prefs?.getString("smsComPkSenderMask", null)?.trim().orEmpty()

                val smsComPkKey = cloudSmsComPkKey.ifBlank {
                    try { BuildConfig.SMS_COM_PK_API_KEY.trim() } catch (_: Exception) { "" }
                }
                val smsComPkMask = cloudSmsComPkMask.ifBlank {
                    try { BuildConfig.SMS_COM_PK_SENDER_MASK.trim() } catch (_: Exception) { "BOSS ALUM" }
                }.ifBlank { "BOSS ALUM" }

                if (smsComPkKey.isNotBlank() &&
                    !smsComPkKey.startsWith("your_") &&
                    smsComPkKey != "DUMMY_KEY"
                ) {
                    val formBody = okhttp3.FormBody.Builder()
                        .add("api_key", smsComPkKey)
                        .add("sender", smsComPkMask)
                        .add("mobile", mobile92)
                        .add("message", messageBody)
                        .build()

                    val request = Request.Builder()
                        .url("https://sms.com.pk/api/sms.php")
                        .post(formBody)
                        .build()

                    val ok = httpClient.newCall(request).execute().use { resp ->
                        resp.isSuccessful
                    }
                    if (ok) return@withContext "SMS_COM_PK"
                }
            } catch (e: Exception) {
                Log.e(TAG, "SMS.com.pk gateway error: ${e.message}")
            }

            null
        }

    fun verifyPhoneOtp(
        verificationId: String,
        otpCode: String = "",
        code: String = otpCode,
        onSuccess: () -> Unit = {},
        onVerified: () -> Unit = onSuccess,
        onError: (String) -> Unit = {}
    ) {
        val effectiveCode = code.ifBlank { otpCode }.trim()
        if (effectiveCode.length == 6) {
            onVerified()
        } else {
            onError("Invalid 6-digit OTP code.")
        }
    }

    fun sendEmailOtp(
        email: String,
        code: String,
        onCodeDispatched: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        sendEmailOtp(
            email = email,
            otpCode = code,
            userName = "Valued Partner",
            onSuccess = { onCodeDispatched(code) },
            onError = onError
        )
    }

    /**
     * Sends Email OTP via Resend API and logs verification in Supabase Master Backend.
     */
    fun sendEmailOtp(
        email: String,
        otpCode: String,
        userName: String,
        onSuccess: (deliveredRemotely: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            onError("Invalid email address")
            return
        }

        bgScope.launch {
            var deliveredViaEmailApi = false
            try {
                val prefs = appContext?.getSharedPreferences(PREFS_SUPABASE, Context.MODE_PRIVATE)
                val cloudResendKey = prefs?.getString("resendApiKey", null)?.trim().orEmpty()
                val cloudFromEmail = prefs?.getString("otpFromEmail", null)?.trim().orEmpty()

                val resendApiKey = cloudResendKey.ifBlank {
                    try { BuildConfig.RESEND_API_KEY.trim() } catch (_: Exception) { "" }
                }
                val fromAddress = cloudFromEmail.ifBlank {
                    try { BuildConfig.OTP_FROM_EMAIL.trim() } catch (_: Exception) { "" }
                }.ifBlank { "onboarding@resend.dev" }

                if (resendApiKey.isNotBlank() &&
                    !resendApiKey.startsWith("re_your_") &&
                    !resendApiKey.startsWith("your_") &&
                    resendApiKey != "DUMMY_KEY"
                ) {
                    val htmlBody = """
                        <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; background: #0D131D; color: #FFFFFF; border-radius: 12px; border: 1px solid #D9A441;">
                            <h2 style="color: #D9A441; margin-top: 0;">BOSS Aluminium &amp; Glass Calculator</h2>
                            <p style="color: #E2E8F0; font-size: 14px;">Assalam-o-Alaikum <strong>${userName.ifBlank { "Valued Partner" }}</strong>,</p>
                            <p style="color: #94A3B8; font-size: 13px;">Your 6-digit security verification OTP code is:</p>
                            <div style="background: #172030; border: 2px solid #D9A441; border-radius: 10px; padding: 16px; text-align: center; margin: 20px 0;">
                                <span style="font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #FDE047;">$otpCode</span>
                            </div>
                        </div>
                    """.trimIndent()

                    val payload = JSONObject().apply {
                        put("from", "BOSS Aluminium Security <$fromAddress>")
                        put("to", JSONArray().put(cleanEmail))
                        put("subject", "Your Verification Code: $otpCode - BOSS Aluminium App")
                        put("html", htmlBody)
                    }

                    val request = Request.Builder()
                        .url("https://api.resend.com/emails")
                        .addHeader("Authorization", "Bearer $resendApiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            deliveredViaEmailApi = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Resend API error: ${e.message}")
            }

            if (isSupabaseConfigured()) {
                try {
                    val otpDoc = JSONObject().apply {
                        put("email", cleanEmail)
                        put("user_name", userName)
                        put("otp_code", otpCode)
                        put("channel", "EMAIL")
                        put("delivered_via_api", deliveredViaEmailApi)
                        put("created_at", System.currentTimeMillis())
                    }
                    val req = buildSupabaseRequest(
                        path = "/rest/v1/otp_verifications",
                        method = "POST",
                        jsonBody = otpDoc.toString()
                    )
                    if (req != null) {
                        httpClient.newCall(req).execute().use { }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Supabase email OTP log error: ${e.message}")
                }
            }

            withContext(Dispatchers.Main) {
                onSuccess(deliveredViaEmailApi)
            }
        }
    }

    /**
     * Uploads user verification documents (Selfie, CNIC Front/Back) to Supabase Storage bucket `user_documents`.
     */
    suspend fun uploadUserDocument(
        context: Context,
        userId: String,
        docType: String,
        localPath: String
    ): String? = withContext(Dispatchers.IO) {
        if (!isSupabaseConfigured(context) || localPath.isBlank()) return@withContext null
        try {
            val file = File(localPath)
            if (!file.exists()) return@withContext null

            val baseUrl = getSupabaseUrl(context)
            val apiKey = getSupabaseAnonKey(context)
            val objectPath = "$userId/${docType}_${System.currentTimeMillis()}.jpg"
            val uploadUrl = "$baseUrl/storage/v1/object/user_documents/$objectPath"

            val requestBody = file.readBytes().toRequestBody("image/jpeg".toMediaType())
            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("x-upsert", "true")
                .post(requestBody)
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    return@withContext "$baseUrl/storage/v1/object/public/user_documents/$objectPath"
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Supabase Storage upload failed ($docType): ${e.message}")
            null
        }
    }

    /**
     * Syncs UserProfile to the Master Admin App's Supabase `users` and `client_users` tables,
     * and syncs any attached photos to `uploaded_photos`.
     */
    fun syncUserProfileToFirestore(
        profile: UserProfile,
        selfieUrl: String? = null,
        cnicFrontUrl: String? = null,
        cnicBackUrl: String? = null
    ) {
        bgScope.launch {
            if (!isSupabaseConfigured()) return@launch
            try {
                val devId = profile.deviceId.ifBlank { profile.userId }
                val androidId = try {
                    android.provider.Settings.Secure.getString(appContext?.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""
                } catch (_: Exception) { "" }
                val hardwareFingerprint = "${android.os.Build.FINGERPRINT}|${android.os.Build.BOARD}|${android.os.Build.HARDWARE}|$androidId"

                val payload = JSONObject().apply {
                    put("device_id", devId)
                    put("user_id", profile.userId)
                    put("hardware_fingerprint", hardwareFingerprint)
                    put("android_id", androidId)
                    put("build_fingerprint", android.os.Build.FINGERPRINT)
                    put("device_model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    put("name", profile.name)
                    put("father_name", profile.fatherName)
                    put("phone", profile.phone)
                    put("email", profile.email)
                    put("whatsapp", profile.whatsapp)
                    put("cnic", profile.cnic)
                    put("shop_address", profile.shopAddress)
                    put("city", profile.city)
                    put("is_phone_verified", profile.isPhoneVerified)
                    put("is_email_verified", profile.isEmailVerified)
                    put("is_profile_completed", profile.isProfileCompleted)
                    put("selfie_url", selfieUrl ?: profile.selfiePhotoUri)
                    put("cnic_front_url", cnicFrontUrl ?: profile.cnicFrontPhotoUri)
                    put("cnic_back_url", cnicBackUrl ?: profile.cnicBackPhotoUri)
                    put("shop_photo_url", profile.shopPhotoPath ?: "")
                    put("account_status", profile.accountStatus)
                    put("isBlocked", profile.accountStatus.equals("BLOCKED", ignoreCase = true))
                    put("is_blocked", profile.accountStatus.equals("BLOCKED", ignoreCase = true))
                    put("app_pin", profile.appPin)
                    put("biometric_enabled", profile.biometricEnabled)
                    put("registered_at_epoch_ms", profile.registeredAtEpochMs)
                    put("updated_at_epoch_ms", System.currentTimeMillis())
                }
                buildSupabaseRequest(
                    path = "/rest/v1/users?on_conflict=device_id",
                    method = "POST",
                    jsonBody = payload.toString()
                )?.let { req ->
                    httpClient.newCall(req).execute().use { resp ->
                        Log.d(TAG, "Supabase users sync HTTP ${resp.code}")
                    }
                }
                val req = buildSupabaseRequest(
                    path = "/rest/v1/client_users?on_conflict=device_id",
                    method = "POST",
                    jsonBody = payload.toString()
                )
                if (req != null) {
                    httpClient.newCall(req).execute().use { resp ->
                        Log.d(TAG, "Supabase client_users sync HTTP ${resp.code}")
                    }
                }

                // Also sync CNIC Front, CNIC Back, and Shop Photo to `uploaded_photos` table
                if (!profile.cnicPhotoPath.isNullOrBlank()) {
                    syncPhotoToUploadedPhotos(profile, "CNIC_FRONT", profile.cnicPhotoPath)
                }
                if (!profile.cnicBackPhotoPath.isNullOrBlank()) {
                    syncPhotoToUploadedPhotos(profile, "CNIC_BACK", profile.cnicBackPhotoPath)
                }
                if (!profile.shopPhotoPath.isNullOrBlank()) {
                    syncPhotoToUploadedPhotos(profile, "SHOP_PHOTO", profile.shopPhotoPath)
                }
                if (!profile.profilePhotoPath.isNullOrBlank()) {
                    syncPhotoToUploadedPhotos(profile, "SHOP_SELFIE", profile.profilePhotoPath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Supabase users/client_users sync error: ${e.message}")
            }
        }
    }

    /**
     * Syncs a user photo (CNIC_FRONT, CNIC_BACK, SHOP_PHOTO, PAYMENT_RECEIPT) to Supabase `uploaded_photos`
     * so the Master Admin Panel can view and download it directly.
     */
    fun syncPhotoToUploadedPhotos(
        profile: UserProfile,
        photoType: String,
        localFilePath: String?,
        extraNote: String = ""
    ) {
        if (localFilePath.isNullOrBlank()) return
        bgScope.launch {
            if (!isSupabaseConfigured()) return@launch
            try {
                val file = File(localFilePath)
                if (!file.exists()) return@launch
                val bytes = file.readBytes()
                val base64DataUri = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val ctx = appContext
                val storageUrl = if (ctx != null) {
                    uploadUserDocument(ctx, profile.deviceId.ifBlank { profile.phone }, photoType, localFilePath)
                } else null

                val devId = profile.deviceId.ifBlank { profile.userId }
                val photoId = "${devId}_${photoType}"
                val payload = JSONObject().apply {
                    put("id", photoId)
                    put("photo_id", photoId)
                    put("device_id", devId)
                    put("user_id", profile.userId)
                    put("user_name", profile.name)
                    put("user_phone", profile.phone)
                    put("user_email", profile.email)
                    put("cnic", profile.cnic)
                    put("shop_address", profile.shopAddress)
                    put("photo_type", photoType)
                    put("type", photoType)
                    put("photo_url", storageUrl ?: base64DataUri)
                    put("image_data", base64DataUri)
                    put("base64_data", base64DataUri)
                    put("note", extraNote)
                    put("created_at", System.currentTimeMillis())
                    put("updated_at", System.currentTimeMillis())
                }
                val req = buildSupabaseRequest(
                    path = "/rest/v1/uploaded_photos?on_conflict=id",
                    method = "POST",
                    jsonBody = payload.toString()
                )
                if (req != null) {
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            // Fallback plain insert if id conflict column not indexed
                            buildSupabaseRequest(
                                path = "/rest/v1/uploaded_photos",
                                method = "POST",
                                jsonBody = payload.toString()
                            )?.let { fallbackReq ->
                                httpClient.newCall(fallbackReq).execute().use { }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "uploaded_photos sync warning ($photoType): ${e.message}")
            }
        }
    }

    /**
     * Syncs SubscriptionState to the Master Admin App's Supabase `subscriptions` table.
     */
    fun syncSubscriptionToFirestore(
        deviceId: String,
        subState: SubscriptionState
    ) {
        bgScope.launch {
            if (!isSupabaseConfigured() || deviceId.isBlank()) return@launch
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("tier", subState.tier.name)
                    put("tier_label", subState.tier.label)
                    put("status", subState.status.name)
                    put("start_date_epoch_ms", subState.startDateEpochMs)
                    put("expiry_date_epoch_ms", subState.expiryDateEpochMs)
                    put("activation_code", subState.activationCode)
                    put("last_verified_epoch_ms", System.currentTimeMillis())
                }
                val req = buildSupabaseRequest(
                    path = "/rest/v1/subscriptions?on_conflict=device_id",
                    method = "POST",
                    jsonBody = payload.toString()
                )
                if (req != null) {
                    httpClient.newCall(req).execute().use { resp ->
                        Log.d(TAG, "Supabase subscriptions sync HTTP ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Supabase subscription sync error: ${e.message}")
            }
        }
    }

    /**
     * Pushes a direct Bank Account / Easypaisa subscription payment record to Supabase
     * (`subscription_payments` & `admin_notifications`) so the Master App tracks it immediately.
     */
    fun syncDirectPaymentToSupabase(
        record: SubscriptionPaymentRecord,
        updatedSubState: SubscriptionState
    ) {
        bgScope.launch {
            if (!isSupabaseConfigured()) return@launch
            try {
                val paymentJson = JSONObject().apply {
                    put("payment_id", record.paymentId)
                    put("user_id", record.userId)
                    put("device_id", record.deviceId)
                    put("user_name", record.userName)
                    put("user_phone", record.userPhone)
                    put("tier_name", record.tierName)
                    put("plan_label", record.planLabel)
                    put("amount_pkr", record.amountPkr)
                    put("payment_method", record.paymentMethod)
                    put("sender_account_number", record.senderAccountNumber)
                    put("sender_account_name", record.senderAccountName)
                    put("transaction_id", record.transactionId)
                    put("receiver_account_summary", record.receiverAccountSummary)
                    put("status", record.status)
                    put("created_at_epoch_ms", record.timestampMs)
                }
                buildSupabaseRequest(
                    path = "/rest/v1/subscription_payments",
                    method = "POST",
                    jsonBody = paymentJson.toString()
                )?.let { req ->
                    httpClient.newCall(req).execute().use { }
                }

                // Also update active subscription in Supabase
                syncSubscriptionToFirestore(record.deviceId, updatedSubState)

                // Also create an instant Master Admin notification
                val notifJson = JSONObject().apply {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("type", "SUBSCRIPTION_AUTO_ACTIVATED")
                    put("title", "Auto-Activated: ${record.planLabel} (Rs. ${record.amountPkr})")
                    put(
                        "message",
                        "${record.userName} (${record.userPhone}) purchased ${record.planLabel} via ${record.paymentMethod}. TID: ${record.transactionId}"
                    )
                    put("user_id", record.userId)
                    put("device_id", record.deviceId)
                    put("amount_pkr", record.amountPkr)
                    put("payment_method", record.paymentMethod)
                    put("transaction_id", record.transactionId)
                    put("status", "AUTO_ACTIVATED")
                    put("created_at", record.timestampMs)
                }
                buildSupabaseRequest(
                    path = "/rest/v1/admin_notifications",
                    method = "POST",
                    jsonBody = notifJson.toString()
                )?.let { req ->
                    httpClient.newCall(req).execute().use { }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync direct payment to Supabase: ${e.message}")
            }
        }
    }

    /**
     * Pushes a real-time notification document to Supabase `admin_notifications` for the Master Admin App.
     */
    fun notifyAdminNewRegistration(
        profile: UserProfile,
        subState: SubscriptionState,
        selfieUrl: String?,
        cnicFrontUrl: String?,
        cnicBackUrl: String?
    ) {
        bgScope.launch {
            if (!isSupabaseConfigured()) return@launch
            try {
                val payload = JSONObject().apply {
                    put("id", java.util.UUID.randomUUID().toString())
                    put("type", "NEW_USER_REGISTRATION")
                    put("title", "New Client Registered: ${profile.name}")
                    put(
                        "message",
                        "${profile.name} S/O ${profile.fatherName} (${profile.city}) registered from ${profile.shopAddress}. Phone: ${profile.phone}"
                    )
                    put("user_id", profile.userId)
                    put("device_id", profile.deviceId)
                    put("user_name", profile.name)
                    put("father_name", profile.fatherName)
                    put("phone", profile.phone)
                    put("email", profile.email)
                    put("cnic", profile.cnic)
                    put("shop_address", profile.shopAddress)
                    put("city", profile.city)
                    put("selfie_url", selfieUrl ?: "")
                    put("cnic_front_url", cnicFrontUrl ?: "")
                    put("cnic_back_url", cnicBackUrl ?: "")
                    put("subscription_tier", subState.tier.name)
                    put("subscription_expiry_ms", subState.expiryDateEpochMs)
                    put("status", "UNREAD")
                    put("account_action_status", "APPROVED")
                    put("created_at", System.currentTimeMillis())
                }
                buildSupabaseRequest(
                    path = "/rest/v1/admin_notifications",
                    method = "POST",
                    jsonBody = payload.toString()
                )?.let { req ->
                    httpClient.newCall(req).execute().use { }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Admin registration notification error: ${e.message}")
            }
        }
    }

    /**
     * Checks Supabase `client_users` table to see if this CNIC, Phone, or Email is already registered
     * or blocked by Master Admin.
     */
    suspend fun checkExistingUserInCloud(
        context: Context,
        cnic: String,
        phone: String,
        email: String
    ): CloudAccountCheckResult = withContext(Dispatchers.IO) {
        initContext(context)
        if (!isSupabaseConfigured(context)) {
            return@withContext CloudAccountCheckResult(exists = false)
        }
        try {
            val cleanCnic = cnic.replace("-", "").trim()
            val cleanPhone = phone.trim()
            val cleanEmail = email.trim().lowercase()

            val queryFilter = buildList {
                if (cleanCnic.length >= 13) add("cnic.eq.$cleanCnic")
                if (cleanPhone.length >= 10) add("phone.eq.$cleanPhone")
                if (cleanEmail.contains("@")) add("email.eq.$cleanEmail")
            }.joinToString(",")

            if (queryFilter.isBlank()) return@withContext CloudAccountCheckResult(exists = false)

            fun queryTable(table: String): JSONObject? {
                val req = buildSupabaseRequest(
                    path = "/rest/v1/$table?or=($queryFilter)&limit=1",
                    method = "GET",
                    preferHeader = null
                ) ?: return null
                return httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    if (arr.length() > 0) arr.getJSONObject(0) else null
                }
            }

            val obj = queryTable("users") ?: queryTable("client_users")
            if (obj != null) {
                val status = obj.optString("account_status", "APPROVED")
                val name = obj.optString("name", "User")
                val matchedBy = when {
                    obj.optString("cnic") == cleanCnic -> "CNIC"
                    obj.optString("phone") == cleanPhone -> "Phone Number"
                    else -> "Email"
                }
                return@withContext CloudAccountCheckResult(
                    exists = true,
                    matchedField = matchedBy,
                    accountStatus = status,
                    existingName = name,
                    rawData = jsonObjectToMap(obj)
                )
            }
            CloudAccountCheckResult(exists = false)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase user lookup failed: ${e.message}")
            CloudAccountCheckResult(exists = false)
        }
    }

    /**
     * Fetches Merchant Bank Account & Easypaisa details from the Master Admin App's Supabase `master_control` table.
     */
    suspend fun fetchMasterPaymentAccounts(context: Context): MerchantPaymentAccounts? =
        withContext(Dispatchers.IO) {
            initContext(context)
            if (!isSupabaseConfigured(context)) return@withContext null
            try {
                val req = buildSupabaseRequest(
                    path = "/rest/v1/master_control?select=*&limit=1",
                    method = "GET",
                    preferHeader = null
                ) ?: return@withContext null

                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    if (arr.length() == 0) return@withContext null
                    val obj = arr.getJSONObject(0)
                    val epTitle = obj.optString("easypaisa_title", "Syed Saboor Nawab (Boss Aluminium)")
                    val epNum = obj.optString("easypaisa_number", "0310-5524225")
                    val autoAct = obj.optBoolean("auto_activate_enabled", true)
                    MerchantPaymentAccounts(
                        easypaisaAccountTitle = epTitle,
                        easypaisaAccountNumber = epNum,
                        easypaisaTitle = epTitle,
                        easypaisaNumber = epNum,
                        bankName = obj.optString("bank_name", "BankIslami / Meezan Bank"),
                        bankAccountTitle = obj.optString("bank_account_title", "BOSS ALUMINIUM & GLASS"),
                        bankAccountNumber = obj.optString("bank_account_number", "300800600380016"),
                        bankIban = obj.optString("bank_iban", "PK36MEZN0001010105678910"),
                        autoActivateOnPayment = autoAct,
                        autoActivateEnabled = autoAct
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch master payment accounts: ${e.message}")
                null
            }
        }

    fun fetchMerchantAccounts(onResult: (MerchantPaymentAccounts?) -> Unit) {
        bgScope.launch {
            val ctx = appContext
            val accounts = if (ctx != null) fetchMasterPaymentAccounts(ctx) else null
            withContext(Dispatchers.Main) {
                onResult(accounts)
            }
        }
    }

    fun syncMerchantPaymentAccountsToCloud(context: Context, accounts: MerchantPaymentAccounts) {
        initContext(context)
        bgScope.launch {
            if (!isSupabaseConfigured(context)) return@launch
            try {
                val payload = JSONObject().apply {
                    put("config_id", "merchant_payment_accounts")
                    put("easypaisa_title", accounts.easypaisaAccountTitle)
                    put("easypaisa_number", accounts.easypaisaAccountNumber)
                    put("bank_name", accounts.bankName)
                    put("bank_account_title", accounts.bankAccountTitle)
                    put("bank_account_number", accounts.bankAccountNumber)
                    put("bank_iban", accounts.bankIban)
                    put("auto_activate_enabled", accounts.autoActivateOnPayment)
                    put("updated_at_epoch_ms", System.currentTimeMillis())
                }
                upsertMasterTableRow("master_control", "config_id", payload)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync merchant payment accounts to Supabase: ${e.message}")
            }
        }
    }

    fun syncDirectSubscriptionPaymentToCloud(
        context: Context,
        profile: UserProfile,
        updatedState: SubscriptionState,
        paymentRecord: SubscriptionPaymentRecord
    ) {
        initContext(context)
        syncDirectPaymentToSupabase(paymentRecord, updatedState)
    }

    fun syncSubscriptionToCloud(
        context: Context,
        profile: UserProfile,
        updatedState: SubscriptionState
    ) {
        initContext(context)
        val devId = profile.deviceId.ifBlank { paymentDeviceIdFallback(profile) }
        syncSubscriptionToFirestore(devId, updatedState)
    }

    private fun paymentDeviceIdFallback(profile: UserProfile): String {
        return profile.phone.ifBlank { profile.email.ifBlank { "default_device" } }
    }

    fun startRealtimeAccountWatcher(
        deviceId: String,
        phone: String = "",
        cnic: String = "",
        email: String = "",
        onAccountStateChanged: (
            isActivated: Boolean,
            isLifetimeFree: Boolean,
            isBlocked: Boolean,
            blockReason: String?,
            trialStart: Long,
            activatedUntil: Long,
            tierName: String?
        ) -> Unit
    ) {
        val ctx = appContext ?: return
        startRealtimeAccountWatcherInternal(
            context = ctx,
            deviceId = deviceId,
            phone = phone,
            cnic = cnic,
            email = email,
            onAccountStateChanged = onAccountStateChanged
        )
    }

    private fun normalizeEpochMs(rawVal: Long): Long {
        if (rawVal <= 0L) return rawVal
        // Convert 10-digit epoch seconds to milliseconds if needed
        return if (rawVal in 1_000_000_000L..9_999_999_999L) rawVal * 1000L else rawVal
    }

    private fun parseExpiryFromObj(obj: JSONObject): Long? {
        val keys = listOf("subscription_expiry", "activated_until", "expiry_date_epoch_ms", "subscriptionExpiry", "activatedUntil")
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val longVal = obj.optLong(k, -1L)
                if (longVal >= 0L) return normalizeEpochMs(longVal)
                val strVal = obj.optString(k, "").trim()
                if (strVal.isNotEmpty()) {
                    strVal.toLongOrNull()?.let { return normalizeEpochMs(it) }
                }
            }
        }
        return null
    }

    /**
     * Verifies Single-Device Hardware Lock (device_id), Account Block Status (isBlocked),
     * and Subscription Expiry against the Supabase `users` (and `client_users` / `subscriptions`)
     * tables using the authoritative server HTTP Date timestamp to prevent clock rollback theft.
     */
    fun verifyUserSecurityOnServer(
        context: Context,
        profile: UserProfile,
        onVerified: (
            serverTimeMs: Long,
            isDeviceMismatch: Boolean,
            isBlocked: Boolean,
            blockReason: String?,
            isActivated: Boolean?,
            isLifetime: Boolean,
            expiryEpochMs: Long?,
            tierName: String?
        ) -> Unit
    ) {
        initContext(context)
        val devId = profile.deviceId.ifBlank { paymentDeviceIdFallback(profile) }
        if ((devId.isBlank() && profile.phone.isBlank() && profile.cnic.isBlank()) || !isSupabaseConfigured(context)) return

        bgScope.launch {
            try {
                var serverTimeMs = System.currentTimeMillis()
                var isBlocked = false
                var isDeviceMismatch = false
                var blockReason: String? = null
                var isActivated: Boolean? = null
                var isLifetime = false
                var expiryMs: Long? = null
                var tierName: String? = null
                var matchedDeviceId = devId

                fun parseServerDateHeader(resp: okhttp3.Response) {
                    val dateStr = resp.header("Date") ?: resp.header("date")
                    if (!dateStr.isNullOrBlank()) {
                        try {
                            val parsed = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US).parse(dateStr)
                            if (parsed != null && parsed.time > 1700000000000L) {
                                serverTimeMs = parsed.time
                            }
                        } catch (_: Exception) {}
                    }
                }

                val cleanPhone = profile.phone.trim()
                val formattedPhone = if (cleanPhone.isNotBlank()) formatPakistanPhone(cleanPhone) else ""
                val localPhone = if (formattedPhone.startsWith("+92")) "0" + formattedPhone.removePrefix("+92") else cleanPhone
                val cleanCnic = profile.cnic.replace("-", "").trim()
                val dashedCnic = profile.cnic.trim()

                val filterParts = mutableListOf<String>()
                if (devId.isNotBlank()) filterParts.add("device_id.eq.$devId")
                if (cleanPhone.length >= 10) filterParts.add("phone.eq.$cleanPhone")
                if (localPhone.length >= 10 && localPhone != cleanPhone) filterParts.add("phone.eq.$localPhone")
                if (formattedPhone.length >= 10 && formattedPhone != cleanPhone) filterParts.add("phone.eq.$formattedPhone")
                if (cleanCnic.length >= 13) filterParts.add("cnic.eq.$cleanCnic")
                if (dashedCnic.length >= 13 && dashedCnic != cleanCnic) filterParts.add("cnic.eq.$dashedCnic")
                val usersFilter = filterParts.distinct().joinToString(",")

                fun applyUserRow(uObj: JSONObject, checkDeviceLock: Boolean) {
                    val remoteDevId = uObj.optString("device_id", "").trim()
                    if (remoteDevId.isNotBlank()) {
                        matchedDeviceId = remoteDevId
                        if (checkDeviceLock && devId.isNotBlank() && !remoteDevId.equals(devId, ignoreCase = true) && !profile.isVipOwner) {
                            isDeviceMismatch = true
                            blockReason = "Single-Device Hardware Lock: Account bound to another device ($remoteDevId)."
                        }
                    }
                    val accStatus = uObj.optString("account_status", "APPROVED")
                    val blockedFlag = uObj.optBoolean("isBlocked", false) ||
                        uObj.optBoolean("is_blocked", false) ||
                        accStatus.equals("BLOCKED", ignoreCase = true) ||
                        accStatus.equals("SUSPENDED", ignoreCase = true)
                    if (blockedFlag) {
                        isBlocked = true
                        blockReason = uObj.optString("lock_reason", "")
                            .ifBlank { uObj.optString("block_reason", "") }
                            .ifBlank { uObj.optString("lockReason", "") }
                            .ifBlank { "Account blocked by Master Admin." }
                    }
                    parseExpiryFromObj(uObj)?.let { exp ->
                        expiryMs = exp
                    }
                    val tStr = uObj.optString("subscription_tier", uObj.optString("tier", "")).trim()
                    if (tStr.isNotEmpty()) {
                        tierName = tStr
                    }
                    val lifetimeFlag = tStr.contains("LIFETIME", ignoreCase = true) ||
                        uObj.optBoolean("is_lifetime", false) ||
                        uObj.optBoolean("isLifetime", false) ||
                        uObj.optBoolean("is_lifetime_free", false)
                    val subStatusStr = uObj.optString("subscription_status", uObj.optString("status", ""))
                    if (lifetimeFlag) {
                        isLifetime = true
                        isActivated = true
                    } else if (uObj.has("is_activated") || uObj.has("isActivated")) {
                        val act = uObj.optBoolean("is_activated", uObj.optBoolean("isActivated", false))
                        isActivated = act && (expiryMs == null || expiryMs == 0L || expiryMs!! > serverTimeMs)
                    } else if (subStatusStr.equals("ACTIVE", ignoreCase = true) || subStatusStr.equals("AUTO_ACTIVATED", ignoreCase = true)) {
                        isActivated = expiryMs == null || expiryMs == 0L || expiryMs!! > serverTimeMs
                    } else if (expiryMs != null && expiryMs!! > serverTimeMs) {
                        isActivated = true
                    } else if (expiryMs != null && expiryMs!! in 1L..serverTimeMs) {
                        isActivated = false
                    }
                }

                // 1. Check `users` table
                if (usersFilter.isNotBlank()) {
                    val usersReq = buildSupabaseRequest(
                        path = "/rest/v1/users?or=($usersFilter)&limit=1",
                        method = "GET",
                        preferHeader = null
                    )
                    if (usersReq != null) {
                        httpClient.newCall(usersReq).execute().use { resp ->
                            parseServerDateHeader(resp)
                            if (resp.isSuccessful) {
                                val arr = JSONArray(resp.body?.string().orEmpty())
                                if (arr.length() > 0) {
                                    applyUserRow(arr.getJSONObject(0), checkDeviceLock = true)
                                }
                            }
                        }
                    }
                }

                // 2. Check `client_users` table with same filter
                if (usersFilter.isNotBlank()) {
                    val clientUsersReq = buildSupabaseRequest(
                        path = "/rest/v1/client_users?or=($usersFilter)&limit=1",
                        method = "GET",
                        preferHeader = null
                    )
                    if (clientUsersReq != null) {
                        httpClient.newCall(clientUsersReq).execute().use { resp ->
                            parseServerDateHeader(resp)
                            if (resp.isSuccessful) {
                                val arr = JSONArray(resp.body?.string().orEmpty())
                                if (arr.length() > 0) {
                                    applyUserRow(arr.getJSONObject(0), checkDeviceLock = true)
                                }
                            }
                        }
                    }
                }

                // 3. Check `subscriptions` table
                val subFilterParts = mutableListOf<String>()
                if (matchedDeviceId.isNotBlank()) subFilterParts.add("device_id.eq.$matchedDeviceId")
                if (devId.isNotBlank() && devId != matchedDeviceId) subFilterParts.add("device_id.eq.$devId")
                val subFilter = subFilterParts.joinToString(",")
                if (subFilter.isNotBlank()) {
                    val subReq = buildSupabaseRequest(
                        path = "/rest/v1/subscriptions?or=($subFilter)&limit=1",
                        method = "GET",
                        preferHeader = null
                    )
                    if (subReq != null) {
                        httpClient.newCall(subReq).execute().use { resp ->
                            parseServerDateHeader(resp)
                            if (resp.isSuccessful) {
                                val arr = JSONArray(resp.body?.string().orEmpty())
                                if (arr.length() > 0) {
                                    val sObj = arr.getJSONObject(0)
                                    val subStatus = sObj.optString("status", sObj.optString("subscription_status", ""))
                                    val sTier = sObj.optString("subscription_tier", sObj.optString("tier", "")).trim()
                                    if (sTier.isNotEmpty()) tierName = sTier
                                    parseExpiryFromObj(sObj)?.let { expiryMs = it }
                                    if (sTier.contains("LIFETIME", ignoreCase = true) ||
                                        sObj.optBoolean("is_lifetime", false) ||
                                        sObj.optBoolean("isLifetime", false)
                                    ) {
                                        isLifetime = true
                                        isActivated = true
                                    } else if (sObj.has("is_activated") || sObj.has("isActivated")) {
                                        val act = sObj.optBoolean("is_activated", sObj.optBoolean("isActivated", false))
                                        isActivated = act && (expiryMs == null || expiryMs == 0L || expiryMs!! > serverTimeMs)
                                    } else if (subStatus.equals("ACTIVE", ignoreCase = true) || subStatus.equals("AUTO_ACTIVATED", ignoreCase = true)) {
                                        isActivated = expiryMs == null || expiryMs == 0L || expiryMs!! > serverTimeMs
                                    } else if (subStatus.equals("EXPIRED", ignoreCase = true) || subStatus.equals("LOCKED", ignoreCase = true)) {
                                        isActivated = false
                                    }
                                    if (sObj.optBoolean("isBlocked", false) || sObj.optBoolean("is_blocked", false)) {
                                        isBlocked = true
                                        blockReason = sObj.optString("lock_reason", sObj.optString("block_reason", blockReason ?: "Account blocked by Master Admin."))
                                    }
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onVerified(serverTimeMs, isDeviceMismatch, isBlocked, blockReason, isActivated, isLifetime, expiryMs, tierName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "verifyUserSecurityOnServer warning: ${e.message}")
            }
        }
    }

    /**
     * Starts real-time security & subscription watcher against the Master Admin App's Supabase tables
     * (`users`, `client_users`, and `subscriptions`) matching `device_id`, `phone`, or `cnic`
     * so any block, unblock, or subscription update from the Admin App immediately applies.
     */
    private fun startRealtimeAccountWatcherInternal(
        context: Context,
        deviceId: String,
        phone: String,
        cnic: String,
        email: String,
        onAccountStateChanged: (
            isActivated: Boolean,
            isLifetimeFree: Boolean,
            isBlocked: Boolean,
            blockReason: String?,
            trialStart: Long,
            activatedUntil: Long,
            tierName: String?
        ) -> Unit
    ) {
        initContext(context)
        stopRealtimeAccountWatcher()
        if (deviceId.isBlank() && phone.isBlank() && cnic.isBlank()) return

        watcherJob = bgScope.launch {
            while (isActive) {
                if (isSupabaseConfigured(context)) {
                    try {
                        var isBlocked = false
                        var lockReason: String? = null
                        var isActivated = false
                        var isLifetime = false
                        var tierName: String? = null
                        var expiryMs: Long? = null
                        var trialStartMs = 0L
                        var serverNow = System.currentTimeMillis()
                        var matchedDeviceId = deviceId

                        val cleanPhone = phone.trim()
                        val formattedPhone = if (cleanPhone.isNotBlank()) formatPakistanPhone(cleanPhone) else ""
                        val localPhone = if (formattedPhone.startsWith("+92")) "0" + formattedPhone.removePrefix("+92") else cleanPhone
                        val cleanCnic = cnic.replace("-", "").trim()
                        val dashedCnic = cnic.trim()

                        val filterParts = mutableListOf<String>()
                        if (deviceId.isNotBlank()) filterParts.add("device_id.eq.$deviceId")
                        if (cleanPhone.length >= 10) filterParts.add("phone.eq.$cleanPhone")
                        if (localPhone.length >= 10 && localPhone != cleanPhone) filterParts.add("phone.eq.$localPhone")
                        if (formattedPhone.length >= 10 && formattedPhone != cleanPhone) filterParts.add("phone.eq.$formattedPhone")
                        if (cleanCnic.length >= 13) filterParts.add("cnic.eq.$cleanCnic")
                        if (dashedCnic.length >= 13 && dashedCnic != cleanCnic) filterParts.add("cnic.eq.$dashedCnic")
                        val usersFilter = filterParts.distinct().joinToString(",")

                        fun parseRow(uObj: JSONObject) {
                            val remoteDev = uObj.optString("device_id", "").trim()
                            if (remoteDev.isNotBlank()) matchedDeviceId = remoteDev
                            val accStatus = uObj.optString("account_status", "APPROVED")
                            val blk = uObj.optBoolean("isBlocked", false) ||
                                uObj.optBoolean("is_blocked", false) ||
                                accStatus.equals("BLOCKED", ignoreCase = true) ||
                                accStatus.equals("SUSPENDED", ignoreCase = true)
                            if (blk) {
                                isBlocked = true
                                lockReason = uObj.optString("lock_reason", "")
                                    .ifBlank { uObj.optString("block_reason", "") }
                                    .ifBlank { uObj.optString("lockReason", "") }
                                    .ifBlank { "Account blocked by Master Admin ($accStatus)" }
                            }
                            val tStart = uObj.optLong("trial_start_time", uObj.optLong("start_date_epoch_ms", 0L))
                            if (tStart > 0L) trialStartMs = normalizeEpochMs(tStart)

                            parseExpiryFromObj(uObj)?.let { exp ->
                                expiryMs = exp
                            }
                            val tStr = uObj.optString("subscription_tier", uObj.optString("tier", "")).trim()
                            if (tStr.isNotEmpty()) tierName = tStr

                            val lifeFlag = tStr.contains("LIFETIME", ignoreCase = true) ||
                                uObj.optBoolean("is_lifetime", false) ||
                                uObj.optBoolean("isLifetime", false) ||
                                uObj.optBoolean("is_lifetime_free", false)
                            val subSt = uObj.optString("subscription_status", uObj.optString("status", ""))

                            if (lifeFlag) {
                                isLifetime = true
                                isActivated = true
                            } else if (uObj.has("is_activated") || uObj.has("isActivated")) {
                                val act = uObj.optBoolean("is_activated", uObj.optBoolean("isActivated", false))
                                isActivated = act && (expiryMs == null || expiryMs == 0L || expiryMs!! > serverNow)
                            } else if (subSt.equals("ACTIVE", ignoreCase = true) || subSt.equals("AUTO_ACTIVATED", ignoreCase = true)) {
                                isActivated = expiryMs == null || expiryMs == 0L || expiryMs!! > serverNow
                            } else if (subSt.equals("EXPIRED", ignoreCase = true) || subSt.equals("LOCKED", ignoreCase = true)) {
                                isActivated = false
                            } else if (expiryMs != null && expiryMs!! > serverNow) {
                                isActivated = true
                            }
                        }

                        // 1. Check `users` table (device_id, phone, or cnic)
                        if (usersFilter.isNotBlank()) {
                            val usersReq = buildSupabaseRequest(
                                path = "/rest/v1/users?or=($usersFilter)&limit=1",
                                method = "GET",
                                preferHeader = null
                            )
                            if (usersReq != null) {
                                httpClient.newCall(usersReq).execute().use { resp ->
                                    val dateStr = resp.header("Date") ?: resp.header("date")
                                    if (!dateStr.isNullOrBlank()) {
                                        try {
                                            val parsed = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US).parse(dateStr)
                                            if (parsed != null && parsed.time > 1700000000000L) serverNow = parsed.time
                                        } catch (_: Exception) {}
                                    }
                                    if (resp.isSuccessful) {
                                        val arr = JSONArray(resp.body?.string().orEmpty())
                                        if (arr.length() > 0) {
                                            parseRow(arr.getJSONObject(0))
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Check `client_users` table (device_id, phone, or cnic)
                        if (usersFilter.isNotBlank()) {
                            val userReq = buildSupabaseRequest(
                                path = "/rest/v1/client_users?or=($usersFilter)&limit=1",
                                method = "GET",
                                preferHeader = null
                            )
                            if (userReq != null) {
                                httpClient.newCall(userReq).execute().use { resp ->
                                    if (resp.isSuccessful) {
                                        val arr = JSONArray(resp.body?.string().orEmpty())
                                        if (arr.length() > 0) {
                                            parseRow(arr.getJSONObject(0))
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Check `subscriptions` table
                        val subFilterParts = mutableListOf<String>()
                        if (matchedDeviceId.isNotBlank()) subFilterParts.add("device_id.eq.$matchedDeviceId")
                        if (deviceId.isNotBlank() && deviceId != matchedDeviceId) subFilterParts.add("device_id.eq.$deviceId")
                        val subFilter = subFilterParts.joinToString(",")
                        if (subFilter.isNotBlank()) {
                            val subReq = buildSupabaseRequest(
                                path = "/rest/v1/subscriptions?or=($subFilter)&limit=1",
                                method = "GET",
                                preferHeader = null
                            )
                            if (subReq != null) {
                                httpClient.newCall(subReq).execute().use { resp ->
                                    if (resp.isSuccessful) {
                                        val arr = JSONArray(resp.body?.string().orEmpty())
                                        if (arr.length() > 0) {
                                            parseRow(arr.getJSONObject(0))
                                        }
                                    }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            onAccountStateChanged(
                                isActivated && !isBlocked,
                                isLifetime,
                                isBlocked,
                                lockReason,
                                trialStartMs,
                                expiryMs ?: 0L,
                                tierName
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Supabase security poll warning: ${e.message}")
                    }
                }
                delay(15_000L)
            }
        }
    }

    fun stopRealtimeAccountWatcher() {
        watcherJob?.cancel()
        watcherJob = null
    }

    fun submitSupportRequestToAdmin(context: Context, req: com.example.model.ClientSupportRequest) {
        initContext(context)
        bgScope.launch {
            if (isSupabaseConfigured(context)) {
                try {
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
                    upsertMasterTableRow(
                        table = "admin_notifications",
                        conflictColumn = "id",
                        payload = payload
                    )
                    upsertMasterTableRow(
                        table = "client_support_requests",
                        conflictColumn = "id",
                        payload = payload
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Support request cloud dispatch warning: ${e.message}")
                }
            }
        }
    }

    /**
     * Fetches all rows from a Supabase table (`/rest/v1/$table?select=*`) so the Client App
     * can load all Admin Panel configuration keys directly on startup without a registration gate.
     */
    suspend fun fetchAllTableRows(table: String): List<JSONObject> =
        withContext(Dispatchers.IO) {
            if (!isSupabaseConfigured()) return@withContext emptyList()
            try {
                val req = buildSupabaseRequest(
                    path = "/rest/v1/$table?select=*",
                    method = "GET",
                    preferHeader = null
                ) ?: return@withContext emptyList()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { list.add(it) }
                    }
                    list
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * Generic helper for Master Engine & Master Admin managers to query or upsert Supabase JSON documents.
     */
    suspend fun fetchMasterTableRow(table: String, keyColumn: String, keyValue: String): JSONObject? =
        withContext(Dispatchers.IO) {
            if (!isSupabaseConfigured()) return@withContext null
            try {
                val req = buildSupabaseRequest(
                    path = "/rest/v1/$table?$keyColumn=eq.$keyValue&limit=1",
                    method = "GET",
                    preferHeader = null
                ) ?: return@withContext null
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    if (arr.length() > 0) arr.getJSONObject(0) else null
                }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun upsertMasterTableRow(table: String, conflictColumn: String, payload: JSONObject): Boolean =
        withContext(Dispatchers.IO) {
            if (!isSupabaseConfigured()) return@withContext false
            try {
                val req = buildSupabaseRequest(
                    path = "/rest/v1/$table?on_conflict=$conflictColumn",
                    method = "POST",
                    jsonBody = payload.toString()
                ) ?: return@withContext false
                httpClient.newCall(req).execute().use { resp ->
                    resp.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = obj.opt(k)
        }
        return map
    }

    fun formatPakistanPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("92") && digits.length >= 12 -> "+$digits"
            digits.startsWith("03") && digits.length == 11 -> "+92${digits.substring(1)}"
            digits.startsWith("3") && digits.length == 10 -> "+92$digits"
            raw.startsWith("+") -> raw
            else -> "+92$digits"
        }
    }
}

data class CloudAccountCheckResult(
    val exists: Boolean,
    val matchedField: String = "",
    val accountStatus: String = "APPROVED",
    val existingName: String = "",
    val rawData: Map<String, Any?> = emptyMap()
)

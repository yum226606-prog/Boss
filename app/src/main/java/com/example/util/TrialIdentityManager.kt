package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.model.SubscriptionState
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IdentityRecord(
    val deviceId: String,
    val cnic: String,
    val phone: String,
    val email: String,
    val name: String = "",
    val shopAddress: String = "",
    val ipAddress: String = "",
    val cnicPhotoPath: String? = null,
    val cnicBackPhotoPath: String? = null,
    val trialStartTime: Long = 0L,
    val firstRegisteredAt: Long = 0L,
    val isActivated: Boolean = false,
    val isBlocked: Boolean = false,
    val blockReason: String = "",
    val isLifetimeFree: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

sealed class IdentityTrialResult {
    data class Eligible(val trialStartTime: Long) : IdentityTrialResult()
    data class InheritedActive(val trialStartTime: Long, val reason: String) : IdentityTrialResult()
    data class BlockedExpired(val reason: String, val originalStartTime: Long) : IdentityTrialResult()
}

class TrialIdentityManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("boss_identity_vault", Context.MODE_PRIVATE)
    private val vaultFile = File(context.filesDir, "identity_trial_vault.json")
    private val deviceKeyFile = File(context.filesDir, "device_security_fingerprint.dat")
    private val cnicOverridesFile = File(context.filesDir, "cnic_owner_overrides.txt")

    companion object {
        private const val KEY_RECORDS_JSON = "key_identity_records_json"
        private const val KEY_CNIC_OVERRIDES = "key_cnic_overrides"
    }

    @SuppressLint("HardwareIds")
    fun getPersistentDeviceId(): String {
        try {
            if (deviceKeyFile.exists() && deviceKeyFile.length() > 0) {
                val savedId = deviceKeyFile.readText().trim()
                if (savedId.isNotBlank()) return savedId
            }
        } catch (_: Exception) {}

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Exception) {
            ""
        }

        val rawFingerprint = buildString {
            append(androidId)
            append("|")
            append(Build.BOARD)
            append("|")
            append(Build.BRAND)
            append("|")
            append(Build.DEVICE)
            append("|")
            append(Build.HARDWARE)
            append("|")
            append(Build.MANUFACTURER)
            append("|")
            append(Build.MODEL)
            append("|")
            append(Build.PRODUCT)
        }

        val hash = try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawFingerprint.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }.take(16).uppercase(Locale.ROOT)
        } catch (_: Exception) {
            "DEV-${androidId.takeLast(8).uppercase(Locale.ROOT)}"
        }

        val finalId = "DEV-$hash"
        try {
            deviceKeyFile.writeText(finalId)
        } catch (_: Exception) {}
        return finalId
    }

    @Synchronized
    fun loadIdentityRecords(): List<IdentityRecord> {
        val list = mutableListOf<IdentityRecord>()
        var jsonStr: String? = null

        try {
            if (vaultFile.exists() && vaultFile.length() > 0) {
                jsonStr = vaultFile.readText()
            }
        } catch (_: Exception) {}

        if (jsonStr.isNullOrBlank()) {
            jsonStr = prefs.getString(KEY_RECORDS_JSON, null)
        }

        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        IdentityRecord(
                            deviceId = obj.optString("deviceId", ""),
                            cnic = obj.optString("cnic", ""),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            name = obj.optString("name", ""),
                            shopAddress = obj.optString("shopAddress", ""),
                            ipAddress = obj.optString("ipAddress", ""),
                            cnicPhotoPath = obj.optString("cnicPhotoPath", null),
                            cnicBackPhotoPath = obj.optString("cnicBackPhotoPath", null),
                            trialStartTime = obj.optLong("trialStartTime", 0L),
                            firstRegisteredAt = obj.optLong("firstRegisteredAt", 0L),
                            isActivated = obj.optBoolean("isActivated", false),
                            isBlocked = obj.optBoolean("isBlocked", false),
                            blockReason = obj.optString("blockReason", ""),
                            isLifetimeFree = obj.optBoolean("isLifetimeFree", false),
                            latitude = if (obj.has("latitude")) obj.optDouble("latitude") else null,
                            longitude = if (obj.has("longitude")) obj.optDouble("longitude") else null
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        return list
    }

    fun getDeviceIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val ip = addr.hostAddress
                        if (!ip.isNullOrBlank()) return ip
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    @Synchronized
    fun recordIdentity(
        deviceId: String,
        cnic: String,
        phone: String,
        email: String,
        trialStartTime: Long,
        name: String = "",
        shopAddress: String = "",
        ipAddress: String = "",
        cnicPhotoPath: String? = null,
        cnicBackPhotoPath: String? = null,
        isActivated: Boolean = false,
        isBlocked: Boolean = false,
        blockReason: String = ""
    ) {
        val existing = loadIdentityRecords().toMutableList()
        val cleanCnic = RegistrationValidator.cleanDigits(cnic)
        val cleanPhone = RegistrationValidator.cleanDigits(phone)
        val cleanEmail = email.trim().lowercase(Locale.ROOT)

        val matchedIdx = existing.indexOfFirst {
            it.deviceId == deviceId ||
                    (cleanCnic.isNotBlank() && RegistrationValidator.cleanDigits(it.cnic) == cleanCnic) ||
                    (cleanPhone.isNotBlank() && RegistrationValidator.cleanDigits(it.phone) == cleanPhone) ||
                    (cleanEmail.isNotBlank() && it.email.trim().lowercase(Locale.ROOT) == cleanEmail)
        }

        val now = System.currentTimeMillis()
        val effectiveTrialStart = if (matchedIdx >= 0 && existing[matchedIdx].trialStartTime > 0) existing[matchedIdx].trialStartTime else trialStartTime
        val prevRecord = if (matchedIdx >= 0) existing[matchedIdx] else null

        val newRecord = IdentityRecord(
            deviceId = deviceId,
            cnic = cnic.trim(),
            phone = phone.trim(),
            email = email.trim(),
            name = name.ifBlank { prevRecord?.name ?: "" },
            shopAddress = shopAddress.ifBlank { prevRecord?.shopAddress ?: "" },
            ipAddress = ipAddress.ifBlank { prevRecord?.ipAddress ?: getDeviceIpAddress() },
            cnicPhotoPath = cnicPhotoPath ?: prevRecord?.cnicPhotoPath,
            cnicBackPhotoPath = cnicBackPhotoPath ?: prevRecord?.cnicBackPhotoPath,
            trialStartTime = effectiveTrialStart,
            firstRegisteredAt = if (matchedIdx >= 0 && existing[matchedIdx].firstRegisteredAt > 0) existing[matchedIdx].firstRegisteredAt else now,
            isActivated = isActivated || (prevRecord?.isActivated ?: false),
            isBlocked = isBlocked || (prevRecord?.isBlocked ?: false),
            blockReason = blockReason.ifBlank { prevRecord?.blockReason ?: "" }
        )

        if (matchedIdx >= 0) {
            existing[matchedIdx] = newRecord
        } else {
            existing.add(newRecord)
        }

        saveIdentityRecords(existing)
    }

    private fun saveIdentityRecords(list: List<IdentityRecord>) {
        val array = JSONArray()
        list.forEach { r ->
            val obj = JSONObject().apply {
                put("deviceId", r.deviceId)
                put("cnic", r.cnic)
                put("phone", r.phone)
                put("email", r.email)
                put("name", r.name)
                put("shopAddress", r.shopAddress)
                if (r.ipAddress.isNotBlank()) put("ipAddress", r.ipAddress)
                if (r.cnicPhotoPath != null) put("cnicPhotoPath", r.cnicPhotoPath)
                if (r.cnicBackPhotoPath != null) put("cnicBackPhotoPath", r.cnicBackPhotoPath)
                put("trialStartTime", r.trialStartTime)
                put("firstRegisteredAt", r.firstRegisteredAt)
                put("isActivated", r.isActivated)
                put("isBlocked", r.isBlocked)
                put("blockReason", r.blockReason)
                put("isLifetimeFree", r.isLifetimeFree)
                if (r.latitude != null) put("latitude", r.latitude)
                if (r.longitude != null) put("longitude", r.longitude)
            }
            array.put(obj)
        }
        val jsonString = array.toString()
        prefs.edit().putString(KEY_RECORDS_JSON, jsonString).apply()
        try {
            vaultFile.writeText(jsonString)
        } catch (_: Exception) {}
    }

    fun checkStrictUniqueness(
        name: String,
        cnic: String,
        phone: String,
        email: String,
        ipAddress: String,
        deviceId: String
    ): String? {
        if (UserProfile.OWNER_VIP_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }) {
            return null
        }

        val records = loadIdentityRecords()
        val cleanName = name.trim().lowercase(Locale.ROOT)
        val cleanCnic = RegistrationValidator.cleanDigits(cnic)
        val cleanPhone = RegistrationValidator.cleanDigits(phone)
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanIp = ipAddress.trim()

        if (cleanIp.isNotBlank() && cleanIp != "127.0.0.1") {
            val ipMatch = records.firstOrNull {
                it.ipAddress.isNotBlank() && it.ipAddress == cleanIp && it.deviceId != deviceId
            }
            if (ipMatch != null) {
                return "IP Restriction: Is IP Address ($cleanIp) par pehle se ek account (${ipMatch.name.ifBlank { "User" }}) registered hai."
            }
        }

        if (cleanName.isNotBlank()) {
            val nameMatch = records.firstOrNull {
                it.name.trim().lowercase(Locale.ROOT) == cleanName && it.deviceId != deviceId
            }
            if (nameMatch != null) {
                return "Name Restriction: Yeh Naam ('$name') pehle se kisi account par registered hai."
            }
        }

        if (cleanCnic.isNotBlank() && !isCnicOverrideAllowed(cleanCnic)) {
            val cnicMatch = records.firstOrNull {
                RegistrationValidator.cleanDigits(it.cnic) == cleanCnic && it.deviceId != deviceId
            }
            if (cnicMatch != null) {
                return "CNIC Restriction: Yeh CNIC ($cnic) pehle se registered hai."
            }
        }

        if (cleanPhone.isNotBlank()) {
            val phoneMatch = records.firstOrNull {
                RegistrationValidator.cleanDigits(it.phone) == cleanPhone && it.deviceId != deviceId
            }
            if (phoneMatch != null) {
                return "Mobile Restriction: Yeh Mobile Number ($phone) pehle se registered hai."
            }
        }

        if (cleanEmail.isNotBlank()) {
            val emailMatch = records.firstOrNull {
                it.email.trim().lowercase(Locale.ROOT) == cleanEmail && it.deviceId != deviceId
            }
            if (emailMatch != null) {
                return "Email Restriction: Yeh Email ($email) pehle se registered hai."
            }
        }

        return null
    }

    fun checkIdentityTrialStatus(
        deviceId: String,
        cnic: String,
        phone: String,
        email: String
    ): IdentityTrialResult {
        if (UserProfile.OWNER_VIP_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }) {
            return IdentityTrialResult.Eligible(System.currentTimeMillis())
        }

        val records = loadIdentityRecords()
        val cleanCnic = RegistrationValidator.cleanDigits(cnic)
        val cleanPhone = RegistrationValidator.cleanDigits(phone)
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val blockedEntry = records.firstOrNull {
            (it.deviceId.isNotBlank() && it.deviceId == deviceId) ||
                    (cleanCnic.isNotBlank() && RegistrationValidator.cleanDigits(it.cnic) == cleanCnic) ||
                    (cleanPhone.isNotBlank() && RegistrationValidator.cleanDigits(it.phone) == cleanPhone) ||
                    (cleanEmail.isNotBlank() && it.email.trim().lowercase(Locale.ROOT) == cleanEmail)
        }
        if (blockedEntry != null && blockedEntry.isBlocked) {
            return IdentityTrialResult.BlockedExpired(
                reason = "Account Blocked by Administrator: ${blockedEntry.blockReason.ifBlank { "Aapka account owner ki taraf say block kar diya gaya hai. Syed Saboor Nawab (0310-5524225) say rabta karein." }}",
                originalStartTime = blockedEntry.trialStartTime
            )
        }

        if (cleanCnic.isNotBlank() && !isCnicOverrideAllowed(cleanCnic)) {
            val existingCnicRecord = records.firstOrNull {
                RegistrationValidator.cleanDigits(it.cnic) == cleanCnic &&
                        (it.deviceId.isNotBlank() && it.deviceId != deviceId)
            }
            if (existingCnicRecord != null) {
                return IdentityTrialResult.BlockedExpired(
                    reason = "1-Account Per CNIC Restriction: Is CNIC ($cnic) par pehle say ek account registered hai.",
                    originalStartTime = existingCnicRecord.trialStartTime
                )
            }
        }

        if (cleanCnic.isNotBlank()) {
            val cnicConflict = records.firstOrNull {
                RegistrationValidator.cleanDigits(it.cnic) == cleanCnic &&
                        it.deviceId.isNotBlank() && it.deviceId != deviceId
            }
            if (cnicConflict != null) {
                return IdentityTrialResult.BlockedExpired(
                    reason = "Single Device Restriction: Yeh CNIC ($cnic) pehle se doosri device par active hai (${cnicConflict.deviceId.take(8)}...).",
                    originalStartTime = cnicConflict.trialStartTime
                )
            }
        }
        if (cleanPhone.isNotBlank()) {
            val phoneConflict = records.firstOrNull {
                RegistrationValidator.cleanDigits(it.phone) == cleanPhone &&
                        it.deviceId.isNotBlank() && it.deviceId != deviceId
            }
            if (phoneConflict != null) {
                return IdentityTrialResult.BlockedExpired(
                    reason = "Single Device Restriction: Yeh Mobile No ($phone) pehle se doosri device par active hai (${phoneConflict.deviceId.take(8)}...).",
                    originalStartTime = phoneConflict.trialStartTime
                )
            }
        }
        if (cleanEmail.isNotBlank()) {
            val emailConflict = records.firstOrNull {
                it.email.trim().lowercase(Locale.ROOT) == cleanEmail &&
                        it.deviceId.isNotBlank() && it.deviceId != deviceId
            }
            if (emailConflict != null) {
                return IdentityTrialResult.BlockedExpired(
                    reason = "Single Device Restriction: Yeh Email ($email) pehle se doosri device par active hai (${emailConflict.deviceId.take(8)}...).",
                    originalStartTime = emailConflict.trialStartTime
                )
            }
        }

        val deviceRecord = records.firstOrNull { it.deviceId.isNotBlank() && it.deviceId == deviceId }
        if (deviceRecord != null && deviceRecord.trialStartTime > 0) {
            val elapsed = now - deviceRecord.trialStartTime
            if (elapsed > SubscriptionState.TRIAL_DURATION_MS) {
                val startStr = sdf.format(Date(deviceRecord.trialStartTime))
                return IdentityTrialResult.BlockedExpired(
                    reason = "This device ($deviceId) already used the 2-Day Free Trial on $startStr.",
                    originalStartTime = deviceRecord.trialStartTime
                )
            } else {
                return IdentityTrialResult.InheritedActive(
                    trialStartTime = deviceRecord.trialStartTime,
                    reason = "Active trial inherited for this device."
                )
            }
        }

        if (cleanCnic.isNotBlank()) {
            val cnicRecord = records.firstOrNull {
                RegistrationValidator.cleanDigits(it.cnic) == cleanCnic
            }
            if (cnicRecord != null && cnicRecord.trialStartTime > 0) {
                val elapsed = now - cnicRecord.trialStartTime
                if (elapsed > SubscriptionState.TRIAL_DURATION_MS) {
                    val startStr = sdf.format(Date(cnicRecord.trialStartTime))
                    return IdentityTrialResult.BlockedExpired(
                        reason = "CNIC $cnic has already utilized the 2-Day Free Trial on $startStr.",
                        originalStartTime = cnicRecord.trialStartTime
                    )
                } else {
                    return IdentityTrialResult.InheritedActive(
                        trialStartTime = cnicRecord.trialStartTime,
                        reason = "Active trial inherited for this CNIC."
                    )
                }
            }
        }

        if (cleanPhone.isNotBlank()) {
            val phoneRecord = records.firstOrNull {
                RegistrationValidator.cleanDigits(it.phone) == cleanPhone
            }
            if (phoneRecord != null && phoneRecord.trialStartTime > 0) {
                val elapsed = now - phoneRecord.trialStartTime
                if (elapsed > SubscriptionState.TRIAL_DURATION_MS) {
                    val startStr = sdf.format(Date(phoneRecord.trialStartTime))
                    return IdentityTrialResult.BlockedExpired(
                        reason = "Mobile number $phone has already utilized the 2-Day Free Trial on $startStr.",
                        originalStartTime = phoneRecord.trialStartTime
                    )
                } else {
                    return IdentityTrialResult.InheritedActive(
                        trialStartTime = phoneRecord.trialStartTime,
                        reason = "Active trial inherited for this Mobile Number."
                    )
                }
            }
        }

        if (cleanEmail.isNotBlank()) {
            val emailRecord = records.firstOrNull {
                it.email.trim().lowercase(Locale.ROOT) == cleanEmail
            }
            if (emailRecord != null && emailRecord.trialStartTime > 0) {
                val elapsed = now - emailRecord.trialStartTime
                if (elapsed > SubscriptionState.TRIAL_DURATION_MS) {
                    val startStr = sdf.format(Date(emailRecord.trialStartTime))
                    return IdentityTrialResult.BlockedExpired(
                        reason = "Email $email has already utilized the 2-Day Free Trial on $startStr.",
                        originalStartTime = emailRecord.trialStartTime
                    )
                } else {
                    return IdentityTrialResult.InheritedActive(
                        trialStartTime = emailRecord.trialStartTime,
                        reason = "Active trial inherited for this Email."
                    )
                }
            }
        }

        return IdentityTrialResult.Eligible(trialStartTime = now)
    }

    fun isCnicOverrideAllowed(cnic: String): Boolean {
        val clean = RegistrationValidator.cleanDigits(cnic)
        if (clean.isBlank()) return false
        val set = prefs.getStringSet(KEY_CNIC_OVERRIDES, emptySet()) ?: emptySet()
        return set.contains(clean)
    }

    fun allowCnicRegistration(cnic: String) {
        val clean = RegistrationValidator.cleanDigits(cnic)
        if (clean.isBlank()) return
        val set = (prefs.getStringSet(KEY_CNIC_OVERRIDES, emptySet()) ?: emptySet()).toMutableSet()
        set.add(clean)
        prefs.edit().putStringSet(KEY_CNIC_OVERRIDES, set).apply()
        try {
            cnicOverridesFile.writeText(set.joinToString("\n"))
        } catch (_: Exception) {}
    }

    fun setUserBlocked(identifier: String, isBlocked: Boolean, reason: String = "") {
        val clean = RegistrationValidator.cleanDigits(identifier)
        val records = loadIdentityRecords().toMutableList()
        val updated = records.map { r ->
            val match = r.deviceId.equals(identifier.trim(), ignoreCase = true) ||
                    (clean.isNotBlank() && (RegistrationValidator.cleanDigits(r.cnic) == clean || RegistrationValidator.cleanDigits(r.phone) == clean)) ||
                    r.email.trim().equals(identifier.trim(), ignoreCase = true)
            if (match) {
                r.copy(isBlocked = isBlocked, blockReason = if (isBlocked) reason else "")
            } else {
                r
            }
        }
        saveIdentityRecords(updated)
    }

    fun setUserActivation(identifier: String, isActivated: Boolean) {
        val clean = RegistrationValidator.cleanDigits(identifier)
        val records = loadIdentityRecords().toMutableList()
        val updated = records.map { r ->
            val match = r.deviceId.equals(identifier.trim(), ignoreCase = true) ||
                    (clean.isNotBlank() && (RegistrationValidator.cleanDigits(r.cnic) == clean || RegistrationValidator.cleanDigits(r.phone) == clean)) ||
                    r.email.trim().equals(identifier.trim(), ignoreCase = true)
            if (match) {
                r.copy(isActivated = isActivated)
            } else {
                r
            }
        }
        saveIdentityRecords(updated)
    }

    fun setUserLifetimeFree(identifier: String, isFree: Boolean) {
        val clean = RegistrationValidator.cleanDigits(identifier)
        val records = loadIdentityRecords().toMutableList()
        val updated = records.map { r ->
            val match = r.deviceId.equals(identifier.trim(), ignoreCase = true) ||
                    (clean.isNotBlank() && (RegistrationValidator.cleanDigits(r.cnic) == clean || RegistrationValidator.cleanDigits(r.phone) == clean)) ||
                    r.email.trim().equals(identifier.trim(), ignoreCase = true)
            if (match) {
                r.copy(isLifetimeFree = isFree, isActivated = if (isFree) true else r.isActivated)
            } else {
                r
            }
        }
        saveIdentityRecords(updated)
    }

    fun extendUserTrialDays(identifier: String, extraDays: Int) {
        val clean = RegistrationValidator.cleanDigits(identifier)
        val extraMs = extraDays.toLong() * 24L * 60L * 60L * 1000L
        val records = loadIdentityRecords().toMutableList()
        val updated = records.map { r ->
            val match = r.deviceId.equals(identifier.trim(), ignoreCase = true) ||
                    (clean.isNotBlank() && (RegistrationValidator.cleanDigits(r.cnic) == clean || RegistrationValidator.cleanDigits(r.phone) == clean)) ||
                    r.email.trim().equals(identifier.trim(), ignoreCase = true)
            if (match) {
                val newStart = if (r.trialStartTime > 0) r.trialStartTime + extraMs else System.currentTimeMillis()
                r.copy(trialStartTime = newStart, isBlocked = false, blockReason = "")
            } else {
                r
            }
        }
        saveIdentityRecords(updated)
    }

    fun getAllUserRegistrations(): List<IdentityRecord> = loadIdentityRecords()

    fun removeIdentityRecord(identifier: String) {
        val clean = RegistrationValidator.cleanDigits(identifier)
        val lower = identifier.trim().lowercase(Locale.ROOT)
        val records = loadIdentityRecords().filterNot { r ->
            (clean.isNotBlank() && (RegistrationValidator.cleanDigits(r.phone) == clean || RegistrationValidator.cleanDigits(r.cnic) == clean)) ||
            (lower.isNotBlank() && r.email.trim().lowercase(Locale.ROOT) == lower) ||
            (identifier.trim().isNotBlank() && r.deviceId.equals(identifier.trim(), ignoreCase = true))
        }
        saveIdentityRecords(records)
    }

    fun findRegisteredIdentity(identifier: String): IdentityRecord? {
        val clean = RegistrationValidator.cleanDigits(identifier)
        val lower = identifier.trim().lowercase(Locale.ROOT)
        val records = loadIdentityRecords()
        return records.firstOrNull { r ->
            (clean.isNotBlank() && (RegistrationValidator.cleanDigits(r.phone) == clean || RegistrationValidator.cleanDigits(r.cnic) == clean)) ||
            (lower.isNotBlank() && r.email.trim().lowercase(Locale.ROOT) == lower) ||
            (identifier.trim().isNotBlank() && r.deviceId.equals(identifier.trim(), ignoreCase = true))
        }
    }

    fun unbindDeviceForIdentity(cnic: String, phone: String, email: String) {
        val records = loadIdentityRecords().toMutableList()
        val cleanCnic = RegistrationValidator.cleanDigits(cnic)
        val cleanPhone = RegistrationValidator.cleanDigits(phone)
        val cleanEmail = email.trim().lowercase(Locale.ROOT)

        val updated = records.map { r ->
            val matches = (cleanCnic.isNotBlank() && RegistrationValidator.cleanDigits(r.cnic) == cleanCnic) ||
                    (cleanPhone.isNotBlank() && RegistrationValidator.cleanDigits(r.phone) == cleanPhone) ||
                    (cleanEmail.isNotBlank() && r.email.trim().lowercase(Locale.ROOT) == cleanEmail)
            if (matches) {
                r.copy(deviceId = "")
            } else {
                r
            }
        }
        saveIdentityRecords(updated)
    }

    fun unbindAllDevicesForOwnerReset() {
        val records = loadIdentityRecords().map { it.copy(deviceId = "") }
        saveIdentityRecords(records)
    }

    fun generateOtpCode(): String {
        return (100000..999999).random().toString()
    }
}

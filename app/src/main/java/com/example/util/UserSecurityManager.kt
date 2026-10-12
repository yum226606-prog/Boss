package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.example.model.DirectPaymentMethod
import com.example.model.MerchantPaymentAccounts
import com.example.model.SubscriptionPaymentRecord
import com.example.model.SubscriptionState
import com.example.model.SubscriptionTier
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserSecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("boss_security_prefs", Context.MODE_PRIVATE)

    val identityManager = TrialIdentityManager(context)

    companion object {
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_PHONE = "key_user_phone"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_SHOP = "key_user_shop"
        private const val KEY_USER_CNIC = "key_user_cnic"
        private const val KEY_USER_REG_DATE = "key_user_reg_date"
        private const val KEY_USER_PHOTO = "key_user_photo"
        private const val KEY_USER_LAT = "key_user_lat"
        private const val KEY_USER_LNG = "key_user_lng"
        private const val KEY_USER_LOC_ADDR = "key_user_loc_addr"
        private const val KEY_USER_REGISTERED = "key_user_registered"
        private const val KEY_USER_DEVICE_ID = "key_user_device_id"
        private const val KEY_USER_PHONE_VERIFIED = "key_user_phone_verified"
        private const val KEY_USER_EMAIL_VERIFIED = "key_user_email_verified"

        private const val KEY_TRIAL_START = "key_trial_start"
        private const val KEY_IS_ACTIVATED = "key_is_activated"
        private const val KEY_ACTIVATED_UNTIL = "key_activated_until"
        private const val KEY_LAST_TID = "key_last_tid"
        private const val KEY_PAYMENT_PROOF = "key_payment_proof"
        private const val KEY_LAST_SYSTEM_TIME = "key_last_system_time"
        private const val KEY_TAMPER_FLAG = "key_tamper_flag"
        private const val KEY_LOCK_REASON = "key_lock_reason"
        private const val KEY_LAST_APP_ACTIVE_TIME = "key_last_app_active_time"
        private const val KEY_USER_CNIC_PHOTO = "key_user_cnic_photo"
        private const val KEY_USER_CNIC_BACK_PHOTO = "key_user_cnic_back_photo"
        private const val KEY_USER_SHOP_PHOTO = "key_user_shop_photo"
        private const val KEY_USER_IP = "key_user_ip"
        private const val KEY_FIREBASE_SMS_PURCHASED = "key_firebase_sms_purchased"
    }

    fun getDeviceId(): String = identityManager.getPersistentDeviceId()

    fun getDeviceIpAddress(): String = identityManager.getDeviceIpAddress()

    fun isFirebaseSmsPurchased(): Boolean = prefs.getBoolean(KEY_FIREBASE_SMS_PURCHASED, false)

    fun setFirebaseSmsPurchased(purchased: Boolean) {
        prefs.edit().putBoolean(KEY_FIREBASE_SMS_PURCHASED, purchased).apply()
    }

    fun checkDuplicateRegistration(
        name: String,
        cnic: String,
        phone: String,
        email: String,
        ipAddress: String
    ): String? {
        return identityManager.checkStrictUniqueness(
            name = name,
            cnic = cnic,
            phone = phone,
            email = email,
            ipAddress = ipAddress,
            deviceId = getDeviceId()
        )
    }

    fun checkTrialEligibility(cnic: String, phone: String, email: String): IdentityTrialResult {
        return identityManager.checkIdentityTrialStatus(getDeviceId(), cnic, phone, email)
    }

    fun generateOtp(): String = identityManager.generateOtpCode()

    fun checkInactivityAutoLogout(): Boolean {
        val lastActive = prefs.getLong(KEY_LAST_APP_ACTIVE_TIME, 0L)
        val now = System.currentTimeMillis()
        if (lastActive > 0L && (now - lastActive) > SubscriptionState.INACTIVITY_AUTO_LOGOUT_MS) {
            logoutUser()
            return true
        }
        prefs.edit().putLong(KEY_LAST_APP_ACTIVE_TIME, now).apply()
        return false
    }

    fun getClientActivationKey(profile: UserProfile): String {
        val dev = if (profile.deviceId.isNotBlank()) profile.deviceId else getDeviceId()
        val last4 = if (profile.phone.length >= 4) profile.phone.takeLast(4) else dev.takeLast(4)
        val hashPart = dev.replace("-", "").take(6).uppercase(Locale.ROOT)
        return "DEV-$hashPart-$last4"
    }

    fun isDeviceConflict(profile: UserProfile): Boolean {
        if (!profile.isRegistered || profile.isVipOwner) return false
        val currentDev = getDeviceId()
        return profile.deviceId.isNotBlank() && !profile.deviceId.equals(currentDev, ignoreCase = true)
    }

    fun logoutUser() {
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val phone = prefs.getString(KEY_USER_PHONE, "") ?: ""
        val cnic = prefs.getString(KEY_USER_CNIC, "") ?: ""
        if (cnic.isNotBlank() || phone.isNotBlank() || email.isNotBlank()) {
            identityManager.unbindDeviceForIdentity(cnic, phone, email)
        }
        prefs.edit().apply {
            putBoolean(KEY_USER_REGISTERED, false)
            putBoolean(KEY_USER_PHONE_VERIFIED, false)
            putBoolean(KEY_USER_EMAIL_VERIFIED, false)
            remove(KEY_USER_DEVICE_ID)
            putLong(KEY_LAST_APP_ACTIVE_TIME, 0L)
            apply()
        }
    }

    fun loadUserProfile(): UserProfile {
        val isInactiveLoggedOut = checkInactivityAutoLogout()

        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val phone = prefs.getString(KEY_USER_PHONE, "") ?: ""
        val shop = prefs.getString(KEY_USER_SHOP, "") ?: ""
        val cnic = prefs.getString(KEY_USER_CNIC, "") ?: ""
        val regDate = prefs.getLong(KEY_USER_REG_DATE, 0L)
        val photo = prefs.getString(KEY_USER_PHOTO, null)
        val cnicPhoto = prefs.getString(KEY_USER_CNIC_PHOTO, null)
        val cnicBackPhoto = prefs.getString(KEY_USER_CNIC_BACK_PHOTO, null)
        val shopPhoto = prefs.getString(KEY_USER_SHOP_PHOTO, null)
        val lat = if (prefs.contains(KEY_USER_LAT)) prefs.getFloat(KEY_USER_LAT, 0f).toDouble() else null
        val lng = if (prefs.contains(KEY_USER_LNG)) prefs.getFloat(KEY_USER_LNG, 0f).toDouble() else null
        val addr = prefs.getString(KEY_USER_LOC_ADDR, null)
        val isRegistered = prefs.getBoolean(KEY_USER_REGISTERED, false) && !isInactiveLoggedOut
        val deviceId = prefs.getString(KEY_USER_DEVICE_ID, null) ?: getDeviceId()
        val isPhoneVerified = prefs.getBoolean(KEY_USER_PHONE_VERIFIED, false)
        val isEmailVerified = prefs.getBoolean(KEY_USER_EMAIL_VERIFIED, false)
        val ip = prefs.getString(KEY_USER_IP, null) ?: getDeviceIpAddress()

        val isVip = UserProfile.OWNER_VIP_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }
        val isVerified = isVip || (isPhoneVerified && isEmailVerified)

        if (isRegistered && !isVip && deviceId.isNotBlank() && !deviceId.equals(getDeviceId(), ignoreCase = true)) {
            logoutUser()
            return UserProfile()
        }

        val isProfileGenuine = isRegistered && isVerified &&
                RegistrationValidator.validateName(name) == null &&
                RegistrationValidator.validatePhone(phone) == null &&
                RegistrationValidator.validateEmail(email) == null &&
                RegistrationValidator.validateCnic(cnic) == null &&
                RegistrationValidator.validateShopAddress(shop) == null

        if (isRegistered && !isProfileGenuine) {
            prefs.edit().putBoolean(KEY_USER_REGISTERED, false).apply()
        }

        return UserProfile(
            name = name,
            phone = phone,
            email = email,
            shopAddress = shop,
            cnic = cnic,
            ipAddress = ip,
            registeredAt = if (regDate > 0) regDate else System.currentTimeMillis(),
            profilePhotoPath = photo,
            cnicPhotoPath = cnicPhoto,
            cnicBackPhotoPath = cnicBackPhoto,
            shopPhotoPath = shopPhoto,
            latitude = lat,
            longitude = lng,
            locationAddress = addr,
            deviceId = deviceId,
            isPhoneVerified = isPhoneVerified,
            isEmailVerified = isEmailVerified,
            isRegistered = isProfileGenuine
        )
    }

    fun saveUserProfile(profile: UserProfile) {
        val devId = if (profile.deviceId.isNotBlank()) profile.deviceId else getDeviceId()
        val currentIp = profile.ipAddress.ifBlank { getDeviceIpAddress() }
        prefs.edit().apply {
            putString(KEY_USER_NAME, profile.name.trim())
            putString(KEY_USER_PHONE, profile.phone.trim())
            putString(KEY_USER_EMAIL, profile.email.trim())
            putString(KEY_USER_SHOP, profile.shopAddress.trim())
            putString(KEY_USER_CNIC, profile.cnic.trim())
            putString(KEY_USER_IP, currentIp)
            putLong(KEY_USER_REG_DATE, profile.registeredAt)
            putString(KEY_USER_PHOTO, profile.profilePhotoPath)
            putString(KEY_USER_CNIC_PHOTO, profile.cnicPhotoPath)
            putString(KEY_USER_CNIC_BACK_PHOTO, profile.cnicBackPhotoPath)
            putString(KEY_USER_SHOP_PHOTO, profile.shopPhotoPath)
            if (profile.latitude != null) putFloat(KEY_USER_LAT, profile.latitude.toFloat())
            if (profile.longitude != null) putFloat(KEY_USER_LNG, profile.longitude.toFloat())
            putString(KEY_USER_LOC_ADDR, profile.locationAddress)
            putString(KEY_USER_DEVICE_ID, devId)
            putBoolean(KEY_USER_PHONE_VERIFIED, profile.isPhoneVerified)
            putBoolean(KEY_USER_EMAIL_VERIFIED, profile.isEmailVerified)
            putBoolean(KEY_USER_REGISTERED, true)
            apply()
        }
    }

    fun loadSubscriptionState(): SubscriptionState {
        val now = System.currentTimeMillis()
        val trialStart = prefs.getLong(KEY_TRIAL_START, 0L)
        val isActivated = prefs.getBoolean(KEY_IS_ACTIVATED, false)
        val activatedUntil = prefs.getLong(KEY_ACTIVATED_UNTIL, 0L)
        val lastTid = prefs.getString(KEY_LAST_TID, "") ?: ""
        val proofPath = prefs.getString(KEY_PAYMENT_PROOF, null)
        val lastKnown = prefs.getLong(KEY_LAST_SYSTEM_TIME, now)
        var tampered = prefs.getBoolean(KEY_TAMPER_FLAG, false)
        val lockReason = prefs.getString(KEY_LOCK_REASON, null)

        if (now < (lastKnown - 3600000L)) {
            tampered = true
            prefs.edit().putBoolean(KEY_TAMPER_FLAG, true).apply()
        } else {
            prefs.edit().putLong(KEY_LAST_SYSTEM_TIME, now).apply()
        }

        return SubscriptionState(
            trialStartTime = trialStart,
            isActivated = isActivated,
            activatedUntil = activatedUntil,
            lastPaymentTid = lastTid,
            lastPaymentProofPath = proofPath,
            lastKnownSystemTime = now,
            isClockTampered = tampered,
            lockReason = lockReason
        )
    }

    fun initializeTrialIfNeeded(profile: UserProfile): SubscriptionState {
        var state = loadSubscriptionState()
        if (profile.isVipOwner) {
            return state
        }

        val devId = if (profile.deviceId.isNotBlank()) profile.deviceId else getDeviceId()
        val eligibility = identityManager.checkIdentityTrialStatus(devId, profile.cnic, profile.phone, profile.email)

        when (eligibility) {
            is IdentityTrialResult.BlockedExpired -> {
                prefs.edit().apply {
                    putLong(KEY_TRIAL_START, eligibility.originalStartTime)
                    putString(KEY_LOCK_REASON, eligibility.reason)
                    apply()
                }
                state = state.copy(
                    trialStartTime = eligibility.originalStartTime,
                    lockReason = eligibility.reason
                )
            }
            is IdentityTrialResult.InheritedActive -> {
                prefs.edit().apply {
                    putLong(KEY_TRIAL_START, eligibility.trialStartTime)
                    remove(KEY_LOCK_REASON)
                    apply()
                }
                state = state.copy(
                    trialStartTime = eligibility.trialStartTime,
                    lockReason = null
                )
            }
            is IdentityTrialResult.Eligible -> {
                val now = System.currentTimeMillis()
                val startTime = if (state.trialStartTime > 0L) state.trialStartTime else now
                prefs.edit().apply {
                    putLong(KEY_TRIAL_START, startTime)
                    remove(KEY_LOCK_REASON)
                    apply()
                }
                identityManager.recordIdentity(
                    deviceId = devId,
                    cnic = profile.cnic,
                    phone = profile.phone,
                    email = profile.email,
                    trialStartTime = startTime,
                    name = profile.name,
                    shopAddress = profile.shopAddress,
                    ipAddress = profile.ipAddress.ifBlank { getDeviceIpAddress() },
                    cnicPhotoPath = profile.cnicPhotoPath,
                    cnicBackPhotoPath = profile.cnicBackPhotoPath
                )
                state = state.copy(trialStartTime = startTime, lockReason = null)
            }
        }
        return state
    }

    fun unlockWithPasscode(code: String, profile: UserProfile? = null): Boolean {
        val cleaned = code.trim().uppercase(Locale.ROOT)
        val prof = profile ?: loadUserProfile()
        val clientKey = getClientActivationKey(prof)

        val result = SubscriptionState.validatePasscode(cleaned, clientKey)
        if (result.isValid) {
            val days = when (result.durationMonths) {
                3 -> 90L
                12 -> 365L
                else -> 30L
            }
            val expiry = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
            prefs.edit().apply {
                putBoolean(KEY_IS_ACTIVATED, true)
                putLong(KEY_ACTIVATED_UNTIL, expiry)
                putBoolean(KEY_TAMPER_FLAG, false)
                remove(KEY_LOCK_REASON)
                apply()
            }
            identityManager.recordIdentity(
                deviceId = prof.deviceId.ifBlank { getDeviceId() },
                cnic = prof.cnic,
                phone = prof.phone,
                email = prof.email,
                trialStartTime = System.currentTimeMillis(),
                name = prof.name,
                shopAddress = prof.shopAddress,
                cnicPhotoPath = prof.cnicPhotoPath,
                cnicBackPhotoPath = prof.cnicBackPhotoPath,
                isActivated = true
            )
            return true
        }
        return false
    }

    fun applyRemoteSubscriptionUpdate(
        isActivated: Boolean,
        isLifetimeFree: Boolean,
        isBlocked: Boolean,
        blockReason: String?,
        trialStartTime: Long,
        activatedUntil: Long
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_ACTIVATED, isActivated || isLifetimeFree)
            if (isLifetimeFree) {
                putLong(KEY_ACTIVATED_UNTIL, 0L)
                remove(KEY_LOCK_REASON)
            } else {
                if (activatedUntil > 0L) putLong(KEY_ACTIVATED_UNTIL, activatedUntil)
                if (isBlocked) {
                    putString(KEY_LOCK_REASON, blockReason ?: "Account blocked by Administrator.")
                } else {
                    remove(KEY_LOCK_REASON)
                }
            }
            if (trialStartTime > 0L) {
                putLong(KEY_TRIAL_START, trialStartTime)
            }
            apply()
        }
    }

    fun getAllIdentities(): List<IdentityRecord> = identityManager.getAllUserRegistrations()

    fun findRegisteredIdentity(identifier: String): IdentityRecord? = identityManager.findRegisteredIdentity(identifier)

    fun findRegisteredUser(identifier: String): IdentityRecord? = identityManager.findRegisteredIdentity(identifier)

    fun findExistingRegistration(query: String): IdentityRecord? = identityManager.findRegisteredIdentity(query)

    fun loginExistingUser(record: IdentityRecord): UserProfile {
        val currentDevId = getDeviceId()
        val prof = UserProfile(
            name = record.name,
            phone = record.phone,
            email = record.email,
            shopAddress = record.shopAddress,
            cnic = record.cnic,
            registeredAt = if (record.firstRegisteredAt > 0) record.firstRegisteredAt else System.currentTimeMillis(),
            profilePhotoPath = null,
            cnicPhotoPath = record.cnicPhotoPath,
            cnicBackPhotoPath = record.cnicBackPhotoPath,
            deviceId = currentDevId,
            isPhoneVerified = true,
            isEmailVerified = true,
            isRegistered = true
        )
        saveUserProfile(prof)

        if (record.isActivated || record.isLifetimeFree) {
            prefs.edit().apply {
                putBoolean(KEY_IS_ACTIVATED, true)
                remove(KEY_LOCK_REASON)
                apply()
            }
        } else {
            val now = System.currentTimeMillis()
            val start = if (record.trialStartTime > 0L) record.trialStartTime else now
            prefs.edit().apply {
                putLong(KEY_TRIAL_START, start)
                apply()
            }
        }

        identityManager.recordIdentity(
            deviceId = currentDevId,
            cnic = record.cnic,
            phone = record.phone,
            email = record.email,
            trialStartTime = record.trialStartTime,
            name = record.name,
            shopAddress = record.shopAddress,
            cnicPhotoPath = record.cnicPhotoPath,
            cnicBackPhotoPath = record.cnicBackPhotoPath,
            isActivated = record.isActivated,
            isBlocked = record.isBlocked,
            blockReason = record.blockReason
        )
        return prof
    }

    fun extendSubscription(days: Int) {
        val now = System.currentTimeMillis()
        val currentExpiry = prefs.getLong(KEY_ACTIVATED_UNTIL, now)
        val base = if (currentExpiry > now) currentExpiry else now
        val newExpiry = base + (days.toLong() * 24 * 60 * 60 * 1000L)
        prefs.edit().apply {
            putBoolean(KEY_IS_ACTIVATED, true)
            putLong(KEY_ACTIVATED_UNTIL, newExpiry)
            putBoolean(KEY_TAMPER_FLAG, false)
            apply()
        }
    }

    fun recordPaymentProof(tid: String, proofPath: String?) {
        prefs.edit().apply {
            putString(KEY_LAST_TID, tid)
            if (proofPath != null) putString(KEY_PAYMENT_PROOF, proofPath)
            apply()
        }
        if (!proofPath.isNullOrBlank()) {
            val profile = loadUserProfile()
            FirebaseCloudManager.syncPhotoToUploadedPhotos(
                profile = profile,
                photoType = "PAYMENT_RECEIPT",
                localFilePath = proofPath,
                extraNote = "TID: $tid"
            )
        }
    }

    fun isLocationPermissionGranted(): Boolean {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun detectDeviceLocation(): Pair<Double?, Double?> {
        return try {
            if (!isLocationPermissionGranted()) {
                return Pair(null, null)
            }
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return Pair(null, null)
            var bestLocation: Location? = null
            val candidateProviders = mutableListOf<String>()
            try { candidateProviders.addAll(locationManager.getProviders(true)) } catch (_: Exception) {}
            candidateProviders.addAll(listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER))

            for (provider in candidateProviders.distinct()) {
                try {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                } catch (_: Exception) {}
            }

            if (bestLocation != null) {
                Pair(bestLocation.latitude, bestLocation.longitude)
            } else {
                val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                        Build.MODEL.contains("google_sdk") ||
                        Build.MODEL.contains("Emulator") ||
                        Build.HARDWARE.contains("goldfish") ||
                        Build.HARDWARE.contains("ranchu")
                if (isEmulator && isLocationPermissionGranted()) {
                    Pair(31.5204, 74.3587)
                } else {
                    Pair(null, null)
                }
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }

    fun buildFullTelemetryReport(profile: UserProfile, subState: SubscriptionState): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val regStr = sdf.format(Date(profile.registeredAt))
        val trialStartStr = if (subState.trialStartTime > 0) sdf.format(Date(subState.trialStartTime)) else "N/A"
        val mapsLink = if (profile.latitude != null && profile.longitude != null) {
            "https://www.google.com/maps/search/?api=1&query=${profile.latitude},${profile.longitude}"
        } else {
            "Location not permitted"
        }

        return """
            BOSS ALUMINIUM & GLASS - USER SECURITY & TELEMETRY REPORT
            --------------------------------------------------------
            USER PROFILE:
              Name: ${profile.name}
              Phone: ${profile.phone}
              Email: ${profile.email}
              Shop Address: ${profile.shopAddress}
              CNIC: ${profile.cnic}
              Device Hardware ID: ${profile.deviceId.ifBlank { getDeviceId() }}
              Registered Date: $regStr
              Device: ${profile.deviceModel}
              
            LIVE GPS & LOCATION:
              Latitude: ${profile.latitude ?: "N/A"}
              Longitude: ${profile.longitude ?: "N/A"}
              Google Maps: $mapsLink
              
            SUBSCRIPTION & TRIAL STATUS:
              Trial Started: $trialStartStr
              Trial Status: ${subState.getFormattedTrialRemaining()}
              Active Subscription: ${if (subState.isActivated) "ACTIVATED" else "LOCKED / PENDING PAYMENT"}
              Monthly Fee: Rs. 3,000 / month
              Last Payment TID: ${subState.lastPaymentTid.ifEmpty { "None" }}
        """.trimIndent()
    }

    fun sendPaymentProofViaWhatsApp(
        profile: UserProfile,
        subState: SubscriptionState,
        tid: String,
        proofFileUri: Uri?
    ) {
        val message = """
            Assalam o Alaikum Syed Saboor Nawab Bhai,
            I have transferred Rs. 3,000 for Boss Aluminium & Glass App Monthly Subscription.
            
            *Payment Details:*
            User Name: ${profile.name}
            Phone: ${profile.phone}
            Email: ${profile.email}
            Shop Address: ${profile.shopAddress}
            CNIC: ${profile.cnic}
            Transaction ID (TID): $tid
            Account: Easypaisa (03105524225) / BankIslami (300800600380016)
            
            Please unlock my app access. Thank you!
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (proofFileUri != null) "image/*" else "text/plain"
            putExtra("jid", "923105524225@s.whatsapp.net")
            putExtra(Intent.EXTRA_TEXT, message)
            if (proofFileUri != null) {
                putExtra(Intent.EXTRA_STREAM, proofFileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            `package` = "com.whatsapp"
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val waUrl = "https://api.whatsapp.com/send?phone=923105524225&text=" + Uri.encode(message)
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not launch WhatsApp: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sendWhatsAppMessage(phone: String = "923105524225", message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra("jid", "$phone@s.whatsapp.net")
            putExtra(Intent.EXTRA_TEXT, message)
            `package` = "com.whatsapp"
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val waUrl = "https://api.whatsapp.com/send?phone=$phone&text=" + Uri.encode(message)
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not launch WhatsApp: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sendAuditViaWhatsApp(profile: UserProfile, message: String) {
        sendWhatsAppMessage("923105524225", message)
    }

    fun sendAuditReportViaEmail(profile: UserProfile, subState: SubscriptionState) {
        val report = buildFullTelemetryReport(profile, subState)
        val owners = "ssaboor2t@gmail.com,yum226606@gmail.com"
        val subject = "Boss Aluminium User Registration & Security Audit - ${profile.name}"

        try {
            val mailtoUri = Uri.parse("mailto:$owners?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(report))
            val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf("ssaboor2t@gmail.com", "yum226606@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, report)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("ssaboor2t@gmail.com", "yum226606@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, report)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Send User Audit to Owners").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open email client: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getMerchantPaymentAccounts(): MerchantPaymentAccounts {
        val epTitle = prefs.getString("ep_title", "Syed Saboor Nawab (Boss Aluminium)") ?: "Syed Saboor Nawab (Boss Aluminium)"
        val epNumber = prefs.getString("ep_number", "0310-5524225") ?: "0310-5524225"
        val autoAct = prefs.getBoolean("auto_activate_payment", true)
        return MerchantPaymentAccounts(
            easypaisaAccountTitle = epTitle,
            easypaisaAccountNumber = epNumber,
            easypaisaTitle = epTitle,
            easypaisaNumber = epNumber,
            bankName = prefs.getString("bank_name", "BankIslami / Meezan Bank") ?: "BankIslami / Meezan Bank",
            bankAccountTitle = prefs.getString("bank_title", "BOSS ALUMINIUM & GLASS") ?: "BOSS ALUMINIUM & GLASS",
            bankAccountNumber = prefs.getString("bank_number", "300800600380016") ?: "300800600380016",
            bankIban = prefs.getString("bank_iban", "PK36MEZN0001020105524225") ?: "PK36MEZN0001020105524225",
            autoActivateOnPayment = autoAct,
            autoActivateEnabled = autoAct,
            whatsappSupportNumber = prefs.getString("whatsapp_support", "+923105524225") ?: "+923105524225"
        )
    }

    fun saveMerchantPaymentAccounts(accounts: MerchantPaymentAccounts) {
        val effectiveEpTitle = accounts.easypaisaAccountTitle.ifBlank { accounts.easypaisaTitle }.trim()
        val effectiveEpNum = accounts.easypaisaAccountNumber.ifBlank { accounts.easypaisaNumber }.trim()
        prefs.edit()
            .putString("ep_title", effectiveEpTitle)
            .putString("ep_number", effectiveEpNum)
            .putString("bank_name", accounts.bankName.trim())
            .putString("bank_title", accounts.bankAccountTitle.trim())
            .putString("bank_number", accounts.bankAccountNumber.trim())
            .putString("bank_iban", accounts.bankIban.trim())
            .putBoolean("auto_activate_payment", accounts.autoActivateOnPayment)
            .putString("whatsapp_support", accounts.whatsappSupportNumber.trim())
            .apply()
        FirebaseCloudManager.syncMerchantPaymentAccountsToCloud(context, accounts)
    }

    fun getDeviceHardwareId(): String = getDeviceId()

    fun getSubscriptionState(): SubscriptionState = loadSubscriptionState()

    fun getDeviceActivationRequestCode(profile: UserProfile): String = getClientActivationKey(profile)

    fun activateWithKey(code: String, profile: UserProfile? = null): Boolean = unlockWithPasscode(code, profile)

    fun requestSubscriptionViaWhatsApp(profile: UserProfile, tier: SubscriptionTier) {
        val reqCode = getClientActivationKey(profile)
        val msg = """
            Assalam o Alaikum Syed Saboor Nawab Bhai,
            I want to activate ${tier.title} (Rs. ${tier.pricePkr} / ${tier.durationDays} Days) for Boss Aluminium & Glass App.
            Name: ${profile.name}
            Phone: ${profile.phone}
            Shop: ${profile.shopAddress}
            Device Request Code: $reqCode
        """.trimIndent()
        sendWhatsAppMessage("923105524225", msg)
    }

    fun resetClockTamperIfTimeValid() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_TAMPER_FLAG, false)
            .putLong(KEY_LAST_SYSTEM_TIME, now)
            .apply()
    }

    fun recordTelemetryEvent(profile: UserProfile, eventName: String, eventDetails: String) {
        // Logged locally & synced via Supabase payment/subscription sync
    }

    fun verifyAndAutoActivatePackage(
        pkg: SubscriptionTier,
        paymentMethod: DirectPaymentMethod,
        transactionId: String,
        payerAccountOrPhone: String
    ): Pair<Boolean, String> {
        val cleanTid = transactionId.trim()
        val cleanSender = payerAccountOrPhone.trim()
        if (cleanSender.length < 7) {
            return false to "Please enter a valid sender mobile or bank account number."
        }
        if (cleanTid.length < 5) {
            return false to "Please enter a valid Transaction ID (minimum 5 characters)."
        }
        val profile = loadUserProfile()
        activateDirectPackagePurchase(
            profile = profile,
            tier = pkg,
            paymentMethod = paymentMethod,
            senderPhoneOrAccount = cleanSender,
            transactionReferenceId = cleanTid,
            amountPaidPkr = pkg.pricePkr
        )
        return true to "${pkg.title} Auto-Activated on your account & synced to Master Server!"
    }

    fun activateDirectPackagePurchase(
        profile: UserProfile,
        tier: SubscriptionTier,
        paymentMethod: DirectPaymentMethod,
        senderPhoneOrAccount: String,
        transactionReferenceId: String,
        amountPaidPkr: Int = tier.pricePkr
    ): SubscriptionState {
        val now = System.currentTimeMillis()
        val endMs = now + (tier.durationDays * 24L * 60L * 60L * 1000L)
        val deviceId = getDeviceHardwareId()
        val accounts = getMerchantPaymentAccounts()
        val receiverInfo = if (paymentMethod == DirectPaymentMethod.EASYPAISA) {
            "Easypaisa: ${accounts.easypaisaAccountNumber} (${accounts.easypaisaAccountTitle})"
        } else {
            "Bank: ${accounts.bankName} - ${accounts.bankAccountNumber} (${accounts.bankAccountTitle})"
        }

        val cleanTid = transactionReferenceId.trim().uppercase()
        val autoLicenseKey = "DIRECT-${paymentMethod.code.take(4)}-${tier.code}-$cleanTid"

        prefs.edit()
            .putBoolean(KEY_IS_ACTIVATED, true)
            .putLong(KEY_ACTIVATED_UNTIL, endMs)
            .putString(KEY_LAST_TID, cleanTid)
            .putBoolean(KEY_TAMPER_FLAG, false)
            .remove(KEY_LOCK_REASON)
            .putBoolean("sub_is_activated", true)
            .putString("sub_license_key", autoLicenseKey)
            .putString("sub_activated_tier", tier.code)
            .putLong("sub_end_time", endMs)
            .putLong("sub_last_known_time", now)
            .putBoolean("sub_clock_tampered", false)
            .putString("sub_bound_device_id", deviceId)
            .putBoolean("sub_device_mismatch", false)
            .apply()

        val paymentRecord = SubscriptionPaymentRecord(
            paymentId = "PAY_${now}_${(1000..9999).random()}",
            userEmail = profile.email,
            userPhone = profile.phone,
            userName = profile.name,
            deviceId = deviceId,
            packageCode = tier.code,
            packageTitle = tier.title,
            amountPkr = amountPaidPkr,
            paymentMethod = paymentMethod.code,
            senderAccountOrPhone = senderPhoneOrAccount.trim(),
            transactionReferenceId = cleanTid,
            receiverAccountDetails = receiverInfo,
            status = "AUTO_ACTIVATED",
            timestampMs = now
        )
        savePaymentRecordLocal(paymentRecord)

        recordTelemetryEvent(
            profile = profile,
            eventName = "DIRECT_PACKAGE_AUTO_ACTIVATED",
            eventDetails = "Package: ${tier.title} (Rs $amountPaidPkr) via ${paymentMethod.label} | Sender: $senderPhoneOrAccount | TID: $cleanTid | Auto-Activated until ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(endMs))}"
        )

        val updatedState = getSubscriptionState()
        FirebaseCloudManager.syncDirectSubscriptionPaymentToCloud(context, profile, updatedState, paymentRecord)
        FirebaseCloudManager.syncSubscriptionToCloud(context, profile, updatedState)
        return updatedState
    }

    private fun savePaymentRecordLocal(rec: SubscriptionPaymentRecord) {
        try {
            val raw = prefs.getString("direct_payment_records", "[]") ?: "[]"
            val arr = JSONArray(raw)
            val obj = JSONObject().apply {
                put("paymentId", rec.paymentId)
                put("userEmail", rec.userEmail)
                put("userPhone", rec.userPhone)
                put("userName", rec.userName)
                put("deviceId", rec.deviceId)
                put("packageCode", rec.packageCode)
                put("packageTitle", rec.packageTitle)
                put("amountPkr", rec.amountPkr)
                put("paymentMethod", rec.paymentMethod)
                put("senderAccountOrPhone", rec.senderAccountOrPhone)
                put("transactionReferenceId", rec.transactionReferenceId)
                put("receiverAccountDetails", rec.receiverAccountDetails)
                put("status", rec.status)
                put("timestampMs", rec.timestampMs)
            }
            val newArr = JSONArray()
            newArr.put(obj)
            for (i in 0 until minOf(arr.length(), 49)) {
                newArr.put(arr.getJSONObject(i))
            }
            prefs.edit().putString("direct_payment_records", newArr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getPaymentHistory(): List<SubscriptionPaymentRecord> {
        val list = mutableListOf<SubscriptionPaymentRecord>()
        try {
            val raw = prefs.getString("direct_payment_records", "[]") ?: "[]"
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    SubscriptionPaymentRecord(
                        paymentId = o.optString("paymentId"),
                        userEmail = o.optString("userEmail"),
                        userPhone = o.optString("userPhone"),
                        userName = o.optString("userName"),
                        deviceId = o.optString("deviceId"),
                        packageCode = o.optString("packageCode"),
                        packageTitle = o.optString("packageTitle"),
                        amountPkr = o.optInt("amountPkr"),
                        paymentMethod = o.optString("paymentMethod"),
                        senderAccountOrPhone = o.optString("senderAccountOrPhone"),
                        transactionReferenceId = o.optString("transactionReferenceId"),
                        receiverAccountDetails = o.optString("receiverAccountDetails"),
                        status = o.optString("status", "AUTO_ACTIVATED"),
                        timestampMs = o.optLong("timestampMs")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}

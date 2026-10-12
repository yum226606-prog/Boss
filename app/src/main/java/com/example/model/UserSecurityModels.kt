package com.example.model

import android.os.Build

enum class SubscriptionStatus {
    TRIAL,
    ACTIVE,
    EXPIRED,
    LOCKED
}

enum class SubscriptionTier(
    val code: String,
    val title: String,
    val pricePkr: Int,
    val durationDays: Int,
    val badge: String = ""
) {
    ONE_MONTH("1M", "1 Month", 3000, 30, "POPULAR"),
    THREE_MONTHS("3M", "3 Months", 8000, 90, "SAVE Rs. 1,000"),
    SIX_MONTHS("6M", "6 Months", 16000, 180, "SAVE Rs. 2,000"),
    ONE_YEAR("1Y", "1 Year", 31000, 365, "BEST VALUE"),
    LIFETIME_VIP("LIFETIME", "Lifetime Free VIP", 60000, 36500, "LIFETIME VIP");

    val label: String get() = title
    val days: Int get() = durationDays
    val isLifetime: Boolean get() = code == "LIFETIME"
}

typealias SubscriptionPackage = SubscriptionTier

data class UserProfile(
    val name: String = "",
    val fatherName: String = "",
    val phone: String = "",
    val email: String = "",
    val whatsapp: String = phone,
    val shopAddress: String = "",
    val city: String = "",
    val cnic: String = "",
    val ipAddress: String = "",
    val registeredAt: Long = System.currentTimeMillis(),
    val profilePhotoPath: String? = null,
    val cnicPhotoPath: String? = null,
    val cnicBackPhotoPath: String? = null,
    val shopPhotoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String? = null,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
    val deviceId: String = "",
    val isPhoneVerified: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isRegistered: Boolean = false,
    val accountStatus: String = "APPROVED",
    val appPin: String = "",
    val biometricEnabled: Boolean = false
) {
    val userId: String get() = deviceId.ifBlank { phone.ifBlank { email } }
    val isProfileCompleted: Boolean get() = isRegistered
    val selfiePhotoUri: String get() = profilePhotoPath ?: ""
    val cnicFrontPhotoUri: String get() = cnicPhotoPath ?: ""
    val cnicBackPhotoUri: String get() = cnicBackPhotoPath ?: ""
    val registeredAtEpochMs: Long get() = registeredAt

    val isVipOwner: Boolean
        get() = OWNER_VIP_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }

    companion object {
        const val OWNER_VIP_EMAIL = "ssaboor2t@gmail.com"
        const val OWNER_VIP_EMAIL_2 = "yum226606@gmail.com"
        val OWNER_VIP_EMAILS = setOf(OWNER_VIP_EMAIL, OWNER_VIP_EMAIL_2)
        const val OWNER_NAME = "Syed Saboor Nawab"
        const val OWNER_PHONE = "03105524225"
        const val EASYPAISA_ACCOUNT = "03105524225"
        const val BANKISLAMI_ACCOUNT = "300800600380016"
        const val MONTHLY_FEE = 3000
        const val THREE_MONTH_FEE = 8000
    }
}

data class PasscodeValidationResult(
    val isValid: Boolean,
    val durationMonths: Int = 1
)

data class SubscriptionState(
    val trialStartTime: Long = 0L,
    val isActivated: Boolean = false,
    val activatedUntil: Long = 0L,
    val lastPaymentTid: String = "",
    val lastPaymentProofPath: String? = null,
    val lastKnownSystemTime: Long = 0L,
    val isClockTampered: Boolean = false,
    val isDeviceMismatch: Boolean = false,
    val lockReason: String? = null,
    val tier: SubscriptionTier = SubscriptionTier.ONE_MONTH
) {
    val status: SubscriptionStatus
        get() = when {
            isActivated -> SubscriptionStatus.ACTIVE
            lockReason != null -> SubscriptionStatus.LOCKED
            getTrialRemainingMs() <= 0L -> SubscriptionStatus.EXPIRED
            else -> SubscriptionStatus.TRIAL
        }

    val startDateEpochMs: Long get() = trialStartTime
    val expiryDateEpochMs: Long get() = activatedUntil
    val activationCode: String get() = lastPaymentTid

    companion object {
        const val TRIAL_DURATION_MS = 2L * 24 * 60 * 60 * 1000L // 2 days in milliseconds (48 hours)
        const val INACTIVITY_AUTO_LOGOUT_MS = 3L * 24 * 60 * 60 * 1000L // 3 days in milliseconds (72 hours)
        val MASTER_PASSCODES = setOf("BOSS2796", "SABOOR2796")

        fun generateDeviceActivationCode(clientKey: String, durationMonths: Int = 1): String {
            val cleanKey = clientKey.trim().uppercase().replace("-", "")
            if (cleanKey.isBlank()) return "BOSS2796"
            var hash = 1125899906842597L
            val salted = "${cleanKey}_BOSS_SECURE_SALT_${if (durationMonths == 3) "Q90" else "M30"}_2026"
            for (c in salted) {
                hash = 31L * hash + c.code
            }
            val positiveHash = kotlin.math.abs(hash)
            val numPart = (positiveHash % 900000L) + 100000L
            val prefix = if (durationMonths == 3) "BOSS-Q" else "BOSS-M"
            return "$prefix-$numPart"
        }

        fun validatePasscode(inputPasscode: String, clientKey: String): PasscodeValidationResult {
            val cleanInput = inputPasscode.trim().uppercase()

            if (MASTER_PASSCODES.contains(cleanInput)) {
                return PasscodeValidationResult(isValid = true, durationMonths = 12)
            }

            val code1Month = generateDeviceActivationCode(clientKey, 1).uppercase()
            if (cleanInput == code1Month || cleanInput == code1Month.replace("-", "")) {
                return PasscodeValidationResult(isValid = true, durationMonths = 1)
            }

            val code3Month = generateDeviceActivationCode(clientKey, 3).uppercase()
            if (cleanInput == code3Month || cleanInput == code3Month.replace("-", "")) {
                return PasscodeValidationResult(isValid = true, durationMonths = 3)
            }

            var legacyHash = 1125899906842597L
            val legacySalted = "${clientKey.trim().uppercase().replace("-", "")}_BOSS_SECURE_SALT_2026"
            for (c in legacySalted) legacyHash = 31L * legacyHash + c.code
            val legacyPos = kotlin.math.abs(legacyHash)
            val legacyNum = (legacyPos % 900000L) + 100000L
            val legacyPrefix = ('A' + ((legacyPos / 1000000L) % 26).toInt())
            val legacyCode = "BOSS-$legacyPrefix$legacyNum"
            if (cleanInput == legacyCode || cleanInput == legacyCode.replace("-", "")) {
                return PasscodeValidationResult(isValid = true, durationMonths = 1)
            }

            return PasscodeValidationResult(isValid = false, durationMonths = 0)
        }

        fun isPasscodeValidForDevice(inputPasscode: String, clientKey: String): Boolean {
            return validatePasscode(inputPasscode, clientKey).isValid
        }
    }

    fun isAppLocked(userProfile: UserProfile): Boolean {
        if (userProfile.isVipOwner) return false

        val now = System.currentTimeMillis()
        if (isActivated && (activatedUntil == 0L || now <= activatedUntil)) {
            return false
        }

        if (lockReason != null) {
            return true
        }

        if (!userProfile.isRegistered || trialStartTime == 0L) {
            return false
        }

        val elapsed = now - trialStartTime
        return elapsed > TRIAL_DURATION_MS || isClockTampered
    }

    fun getTrialRemainingMs(): Long {
        val now = System.currentTimeMillis()
        if (trialStartTime == 0L) return TRIAL_DURATION_MS
        val elapsed = now - trialStartTime
        val remaining = TRIAL_DURATION_MS - elapsed
        return if (remaining > 0) remaining else 0L
    }

    fun getFormattedTrialRemaining(): String {
        val remMs = getTrialRemainingMs()
        if (remMs <= 0) return "Trial Expired (0h 0m)"
        val hours = remMs / (1000 * 60 * 60)
        val minutes = (remMs % (1000 * 60 * 60)) / (1000 * 60)
        val days = hours / 24
        val remHours = hours % 24
        return if (days > 0) {
            "${days}d ${remHours}h ${minutes}m left"
        } else {
            "${remHours}h ${minutes}m left"
        }
    }

    fun getFormattedSubscriptionRemaining(): String {
        if (!isActivated) return getFormattedTrialRemaining()
        if (activatedUntil == 0L) return "Lifetime Active"
        val remMs = (activatedUntil - System.currentTimeMillis()).coerceAtLeast(0L)
        val days = remMs / (1000L * 60L * 60L * 24L)
        val hours = (remMs % (1000L * 60L * 60L * 24L)) / (1000L * 60L * 60L)
        return if (days > 0) "${days}d ${hours}h remaining" else "${hours}h remaining"
    }
}

data class TelemetryAuditRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val userProfile: UserProfile,
    val eventName: String,
    val eventDetails: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class DirectPaymentMethod(val label: String, val code: String) {
    EASYPAISA("Easypaisa Direct", "EASYPAISA"),
    BANK_ACCOUNT("Bank Account Direct", "BANK_ACCOUNT")
}

data class MerchantPaymentAccounts(
    val easypaisaAccountTitle: String = "Syed Saboor Nawab (Boss Aluminium)",
    val easypaisaAccountNumber: String = "0310-5524225",
    val easypaisaTitle: String = easypaisaAccountTitle,
    val easypaisaNumber: String = easypaisaAccountNumber,
    val bankName: String = "BankIslami / Meezan Bank",
    val bankAccountTitle: String = "BOSS ALUMINIUM & GLASS",
    val bankAccountNumber: String = "300800600380016",
    val bankIban: String = "PK36MEZN0001020105524225",
    val autoActivateOnPayment: Boolean = true,
    val autoActivateEnabled: Boolean = autoActivateOnPayment,
    val whatsappSupportNumber: String = "+923105524225"
)

data class SubscriptionPaymentRecord(
    val paymentId: String,
    val userEmail: String,
    val userPhone: String,
    val userName: String,
    val deviceId: String,
    val packageCode: String,
    val packageTitle: String,
    val amountPkr: Int,
    val paymentMethod: String,
    val senderAccountOrPhone: String,
    val transactionReferenceId: String,
    val receiverAccountDetails: String,
    val status: String = "AUTO_ACTIVATED",
    val timestampMs: Long = System.currentTimeMillis()
) {
    val userId: String get() = userEmail.ifBlank { userPhone.ifBlank { deviceId } }
    val tierName: String get() = packageCode
    val planLabel: String get() = packageTitle
    val senderAccountNumber: String get() = senderAccountOrPhone
    val senderAccountName: String get() = userName
    val transactionId: String get() = transactionReferenceId
    val receiverAccountSummary: String get() = receiverAccountDetails
}

package com.example.util

import android.graphics.Bitmap
import com.example.model.SubscriptionState
import com.example.model.UserProfile

object RegistrationValidator {
    private val FAKE_NAMES = setOf(
        "test", "testing", "fake", "dummy", "asdf", "qwer", "admin", "administrator",
        "guest", "demo", "user", "username", "null", "none", "xyz", "abc", "boss",
        "anonymous", "temp", "qwerty", "na", "n/a", "unknown", "developer"
    )

    private val FAKE_ADDRESSES = setOf(
        "test", "testing", "fake", "shop", "my shop", "factory", "asdf", "none",
        "xyz", "null", "address", "karachi", "lahore", "pakistan", "temp", "qwerty",
        "na", "n/a", "no address", "abc", "shop address", "my address"
    )

    fun cleanDigits(input: String): String = input.filter { it.isDigit() }

    fun formatCnic(input: String): String {
        val digits = cleanDigits(input).take(13)
        return when {
            digits.length > 12 -> "${digits.substring(0, 5)}-${digits.substring(5, 12)}-${digits.substring(12)}"
            digits.length > 5 -> "${digits.substring(0, 5)}-${digits.substring(5)}"
            else -> digits
        }
    }

    fun formatPakMobile(input: String): String {
        val digits = cleanDigits(input).take(11)
        return when {
            digits.length > 4 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
            else -> digits
        }
    }

    fun validateName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return "Full Name is required (*)"
        if (trimmed.length < 3) return "Name must be at least 3 letters"
        if (trimmed.length > 50) return "Name cannot exceed 50 characters"
        if (!trimmed.matches(Regex("^[a-zA-Z\\s.']+$"))) {
            return "Name must contain only alphabetic letters and spaces"
        }
        val lower = trimmed.lowercase()
        if (FAKE_NAMES.contains(lower)) {
            return "Fake/test name is not allowed. Enter genuine full name."
        }
        val distinctChars = trimmed.filter { it.isLetter() }.map { it.lowercaseChar() }.distinct().count()
        if (distinctChars < 3) {
            return "Please enter a valid, non-repetitive full name"
        }
        return null
    }

    fun validatePhone(phone: String): String? {
        var clean = cleanDigits(phone)
        if (phone.trim().startsWith("+92") || phone.trim().startsWith("0092") || (clean.startsWith("92") && clean.length == 12)) {
            clean = "0" + clean.removePrefix("92").removePrefix("0092")
        }
        if (clean.isBlank()) return "Pakistani mobile number is required (*)"
        if (clean.length != 11) return "Pakistani mobile number must be exactly 11 digits (03XXXXXXXXX)"
        if (!clean.startsWith("03")) return "Mobile number must start with 03 (e.g. 03105524225)"

        val thirdDigit = clean[2]
        if (thirdDigit !in '0'..'5') {
            return "Invalid telecom code. Valid Pakistani codes: 030X to 035X"
        }

        val distinctDigits = clean.toSet().size
        if (distinctDigits < 4) {
            return "Fake or repetitive phone number is not allowed"
        }
        val dummySequences = listOf(
            "03123456789", "03012345678", "03987654321", "03000000000",
            "03111111111", "03222222222", "03333333333", "03444444444", "03555555555"
        )
        if (dummySequences.contains(clean)) {
            return "Please enter your real active mobile number"
        }
        return null
    }

    fun validateCnic(cnic: String): String? {
        val clean = cleanDigits(cnic)
        if (clean.isBlank()) return "Pakistani CNIC number is required (*)"
        if (clean.length != 13) return "CNIC must be exactly 13 digits (XXXXX-XXXXXXX-X)"

        val firstDigit = clean[0]
        if (firstDigit !in '1'..'7') {
            return "Invalid CNIC. NADRA province code must start with 1 to 7"
        }

        val distinctDigits = clean.toSet().size
        if (distinctDigits < 5) {
            return "Fake or repetitive CNIC number is not allowed"
        }
        val dummySequences = listOf(
            "1234567890123", "1234512345671", "7654321098765", "1111111111111",
            "2222222222222", "3333333333333", "4444444444444", "5555555555555",
            "3520112345671", "4210112345671"
        )
        if (dummySequences.contains(clean)) {
            return "Please enter a genuine 13-digit Pakistani CNIC"
        }
        return null
    }

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return "Email address is required (*)"
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")
        if (!trimmed.matches(emailRegex)) {
            return "Please enter a valid email address (e.g. user@gmail.com)"
        }
        val prefix = trimmed.substringBefore("@")
        if (prefix.length < 3) {
            return "Email username must be at least 3 characters"
        }
        val dummyEmails = setOf(
            "test@test.com", "admin@admin.com", "fake@fake.com", "abc@abc.com",
            "a@b.com", "test@gmail.com", "asdf@gmail.com", "xyz@gmail.com", "demo@demo.com",
            "null@gmail.com", "guest@gmail.com", "a@a.com"
        )
        if (dummyEmails.contains(trimmed.lowercase())) {
            return "Fake/temporary email addresses are not permitted"
        }
        return null
    }

    fun validateOwnerPasscode(email: String, passcode: String): String? {
        val isOwnerVip = UserProfile.OWNER_VIP_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }
        if (!isOwnerVip) return null
        val cleanCode = passcode.trim().uppercase()
        if (cleanCode.isBlank()) {
            return "Owner Master PIN is required to verify Owner VIP email ($email)"
        }
        val validCodes = SubscriptionState.MASTER_PASSCODES
        if (!validCodes.contains(cleanCode)) {
            return "Unauthorized: Incorrect Owner Master PIN."
        }
        return null
    }

    fun validateShopAddress(address: String): String? {
        val trimmed = address.trim()
        if (trimmed.isBlank()) return "Shop / Factory address is required (*)"
        if (trimmed.length < 8) return "Shop address must be at least 8 characters long"
        val lower = trimmed.lowercase()
        if (FAKE_ADDRESSES.contains(lower)) {
            return "Please enter a complete, detailed shop/market address"
        }
        val distinctChars = trimmed.filter { it.isLetter() }.map { it.lowercaseChar() }.distinct().count()
        if (distinctChars < 4) {
            return "Please enter a valid shop address with market or street name"
        }
        return null
    }

    fun validateLivePhoto(photo: Bitmap?): String? = null

    fun validateLiveLocation(lat: Double?, lng: Double?): String? {
        if (lat == null || lng == null) {
            return "Live GPS Location Required: Please allow Location Permission and turn on GPS."
        }
        if (lat == 0.0 && lng == 0.0) {
            return "GPS coordinates not detected yet. Tap Refresh GPS."
        }
        return null
    }
}

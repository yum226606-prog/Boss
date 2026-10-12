package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

data class BiometricUserInfo(
    val phone: String,
    val email: String,
    val name: String
)

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    UNKNOWN
}

object BiometricAuthManager {
    private const val TAG = "BiometricAuthManager"
    private const val PREFS_NAME = "boss_biometric_vault"
    private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    private const val KEY_BIO_USER_PHONE = "key_bio_user_phone"
    private const val KEY_BIO_USER_EMAIL = "key_bio_user_email"
    private const val KEY_BIO_USER_NAME = "key_bio_user_name"
    private const val KEY_BIO_HAS_ASKED = "key_bio_has_asked_permission"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
                else -> BiometricAvailability.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking biometric availability: ${e.message}")
            BiometricAvailability.UNKNOWN
        }
    }

    fun isBiometricSupported(context: Context): Boolean {
        val avail = checkBiometricAvailability(context)
        return avail == BiometricAvailability.AVAILABLE || avail == BiometricAvailability.NONE_ENROLLED
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun hasAskedPermission(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIO_HAS_ASKED, false)
    }

    fun setHasAskedPermission(context: Context, asked: Boolean = true) {
        getPrefs(context).edit().putBoolean(KEY_BIO_HAS_ASKED, asked).apply()
    }

    fun saveBiometricUser(context: Context, phone: String, email: String, name: String) {
        getPrefs(context).edit().apply {
            putString(KEY_BIO_USER_PHONE, phone.trim())
            putString(KEY_BIO_USER_EMAIL, email.trim())
            putString(KEY_BIO_USER_NAME, name.trim())
            putBoolean(KEY_BIOMETRIC_ENABLED, true)
            putBoolean(KEY_BIO_HAS_ASKED, true)
            apply()
        }
    }

    fun getBiometricUser(context: Context): BiometricUserInfo? {
        val prefs = getPrefs(context)
        val phone = prefs.getString(KEY_BIO_USER_PHONE, null)
        val email = prefs.getString(KEY_BIO_USER_EMAIL, null)
        val name = prefs.getString(KEY_BIO_USER_NAME, null)
        return if (!phone.isNullOrBlank() || !email.isNullOrBlank()) {
            BiometricUserInfo(phone = phone ?: "", email = email ?: "", name = name ?: "")
        } else {
            null
        }
    }

    fun clearBiometricCredentials(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Boss Aluminium Login",
        subtitle: String = "Fingerprint / Face scan karein taake calculator khul sakay",
        onAuthenticated: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Use SMS / Password")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Biometric prompt error")
        }
    }
}

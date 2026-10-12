package com.example.ui.components

import android.accounts.AccountManager
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.model.SubscriptionState
import com.example.model.UserProfile
import com.example.ui.theme.currentBossColors
import com.example.util.AiImageValidator
import com.example.util.BiometricAuthManager
import com.example.util.FirebaseCloudManager
import com.example.util.IdentityRecord
import com.example.util.IdentityTrialResult
import com.example.util.RateListDetector
import com.example.util.RegistrationValidator
import com.example.util.UserSecurityManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileOnboardingDialog(
    initialProfile: UserProfile,
    securityManager: UserSecurityManager,
    onProfileCompleted: (UserProfile) -> Unit,
    onLoginExistingUser: (IdentityRecord) -> Unit = {}
) {
    val context = LocalContext.current
    val appColors = currentBossColors()
    val scrollState = rememberScrollState()
    val loginScrollState = rememberScrollState()
    val deviceId = remember { securityManager.getDeviceId() }
    val coroutineScope = rememberCoroutineScope()

    var activeTabMode by remember { mutableStateOf("REGISTER") }

    var isValidatingCnic by remember { mutableStateOf(false) }
    var firebaseVerificationId by remember { mutableStateOf<String?>(null) }
    var isPhoneSmsGatewayActive by remember { mutableStateOf(FirebaseCloudManager.isSendPkOrLocalSmsConfigured(context)) }
    var isEmailGatewayActive by remember { mutableStateOf(FirebaseCloudManager.isEmailOtpApiConfigured(context)) }
    var isLoginSmsGatewayActive by remember { mutableStateOf(FirebaseCloudManager.isSendPkOrLocalSmsConfigured(context)) }
    var isSubmittingRegistration by remember { mutableStateOf(false) }

    var loginIdentifier by remember { mutableStateOf("") }
    var loginIdentifierError by remember { mutableStateOf<String?>(null) }
    var loginFoundRecord by remember { mutableStateOf<IdentityRecord?>(null) }
    var sentLoginOtp by remember { mutableStateOf<String?>(null) }
    var inputLoginOtp by remember { mutableStateOf("") }
    var loginOwnerPasscode by remember { mutableStateOf("") }
    var loginOwnerPasscodeError by remember { mutableStateOf<String?>(null) }
    var loginErrorBanner by remember { mutableStateOf<String?>(null) }
    var loginSuccessBanner by remember { mutableStateOf<String?>(null) }
    var loginFirebaseVerificationId by remember { mutableStateOf<String?>(null) }
    var isSendingLoginOtp by remember { mutableStateOf(false) }
    var isGoogleLoginFlow by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(initialProfile.name) }
    var phone by remember { mutableStateOf(initialProfile.phone) }
    var email by remember { mutableStateOf(initialProfile.email) }
    var shopAddress by remember { mutableStateOf(initialProfile.shopAddress) }
    var cnic by remember { mutableStateOf(initialProfile.cnic) }
    var ownerPasscode by remember { mutableStateOf("") }

    var isPhoneVerified by remember { mutableStateOf(initialProfile.isPhoneVerified) }
    var isEmailVerified by remember { mutableStateOf(initialProfile.isEmailVerified) }
    var sentPhoneOtp by remember { mutableStateOf<String?>(null) }
    var inputPhoneOtp by remember { mutableStateOf("") }
    var sentEmailOtp by remember { mutableStateOf<String?>(null) }
    var inputEmailOtp by remember { mutableStateOf("") }
    var activeOtpDialogType by remember { mutableStateOf<String?>(null) }
    var otpResendCooldown by remember { mutableStateOf(0) }
    var isResendingOtp by remember { mutableStateOf(false) }

    LaunchedEffect(otpResendCooldown) {
        if (otpResendCooldown > 0) {
            kotlinx.coroutines.delay(1000L)
            otpResendCooldown -= 1
        }
    }

    var capturedCnicBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedCnicBackBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedShopBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedLat by remember { mutableStateOf(initialProfile.latitude) }
    var detectedLng by remember { mutableStateOf(initialProfile.longitude) }
    var locationStatus by remember { mutableStateOf("Pending GPS permission...") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var shopError by remember { mutableStateOf<String?>(null) }
    var cnicError by remember { mutableStateOf<String?>(null) }
    var cnicPhotoError by remember { mutableStateOf<String?>(null) }
    var cnicBackPhotoError by remember { mutableStateOf<String?>(null) }
    var isValidatingCnicBack by remember { mutableStateOf(false) }
    var ownerPasscodeError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var generalErrorBanner by remember { mutableStateOf<String?>(null) }
    var fraudLockAlert by remember { mutableStateOf<String?>(null) }
    var registrationProgressText by remember { mutableStateOf("") }
    var showBiometricEnrollDialog by remember { mutableStateOf(false) }
    var pendingRegisteredProfile by remember { mutableStateOf<UserProfile?>(null) }

    val isVipEmail = UserProfile.OWNER_VIP_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }

    val cnicCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                isValidatingCnic = true
                cnicPhotoError = null
                val (isValid, err) = AiImageValidator.validateIdDocument(context, bitmap)
                isValidatingCnic = false
                if (isValid) {
                    capturedCnicBitmap = bitmap
                    cnicPhotoError = null
                    generalErrorBanner = null
                    Toast.makeText(context, "ID document verified by AI", Toast.LENGTH_SHORT).show()
                } else {
                    capturedCnicBitmap = null
                    cnicPhotoError = err ?: "Invalid Document. Please upload a clear image of your ID."
                    Toast.makeText(context, cnicPhotoError, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val cnicBothCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                capturedCnicBitmap = bitmap
                capturedCnicBackBitmap = bitmap
                cnicPhotoError = null
                cnicBackPhotoError = null
                generalErrorBanner = null
                Toast.makeText(context, "Combined CNIC photo captured for both Front & Back", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cnicGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bmp = RateListDetector.loadBitmapFromUri(context, uri, 1200)
                    if (bmp != null) {
                        isValidatingCnic = true
                        cnicPhotoError = null
                        val (isValid, err) = AiImageValidator.validateIdDocument(context, bmp)
                        isValidatingCnic = false
                        if (isValid) {
                            capturedCnicBitmap = bmp
                            cnicPhotoError = null
                            generalErrorBanner = null
                            Toast.makeText(context, "ID document verified by AI", Toast.LENGTH_SHORT).show()
                        } else {
                            capturedCnicBitmap = null
                            cnicPhotoError = err ?: "Invalid Document. Please upload a clear image of your ID."
                            Toast.makeText(context, cnicPhotoError, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    cnicPhotoError = "Error loading document: ${e.message}"
                }
            }
        }
    }

    val cnicBackCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                isValidatingCnicBack = true
                cnicBackPhotoError = null
                val (isValid, err) = AiImageValidator.validateIdDocument(context, bitmap)
                isValidatingCnicBack = false
                if (isValid) {
                    capturedCnicBackBitmap = bitmap
                    cnicBackPhotoError = null
                    generalErrorBanner = null
                    Toast.makeText(context, "CNIC Back verified by AI", Toast.LENGTH_SHORT).show()
                } else {
                    capturedCnicBackBitmap = null
                    cnicBackPhotoError = err ?: "Invalid Document. Please upload clear back side of CNIC."
                    Toast.makeText(context, cnicBackPhotoError, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val cnicBackGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bmp = RateListDetector.loadBitmapFromUri(context, uri, 1200)
                    if (bmp != null) {
                        isValidatingCnicBack = true
                        cnicBackPhotoError = null
                        val (isValid, err) = AiImageValidator.validateIdDocument(context, bmp)
                        isValidatingCnicBack = false
                        if (isValid) {
                            capturedCnicBackBitmap = bmp
                            cnicBackPhotoError = null
                            generalErrorBanner = null
                            Toast.makeText(context, "CNIC Back verified by AI", Toast.LENGTH_SHORT).show()
                        } else {
                            capturedCnicBackBitmap = null
                            cnicBackPhotoError = err ?: "Invalid Document. Please upload clear back side of CNIC."
                            Toast.makeText(context, cnicBackPhotoError, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    cnicBackPhotoError = "Error loading back side: ${e.message}"
                }
            }
        }
    }

    val googleAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            val cleanEmail = accountName.trim()
            email = cleanEmail
            isEmailVerified = true
            emailError = null

            val existing = securityManager.findExistingRegistration(cleanEmail)
            if (existing != null && (isGoogleLoginFlow || activeTabMode == "LOGIN")) {
                val restored = securityManager.loginExistingUser(existing)
                onLoginExistingUser(existing)
                onProfileCompleted(restored)
                Toast.makeText(context, "Google Sign-In Successful! Welcome, ${existing.name}", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            if (name.isBlank()) {
                val autoName = cleanEmail.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                name = autoName
                nameError = null
            }
            Toast.makeText(context, "Connected Google: $cleanEmail (Email Verified)", Toast.LENGTH_SHORT).show()
        }
    }

    fun startGoogleSignIn(isLogin: Boolean = false) {
        isGoogleLoginFlow = isLogin
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null, null, arrayOf("com.google"), null, null, null, null
            )
            googleAccountLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Please enter your Gmail address below", Toast.LENGTH_SHORT).show()
        }
    }

    val cnicBothGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 2)
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val firstBmp = RateListDetector.loadBitmapFromUri(context, uris[0], 1200)
                    if (firstBmp != null) {
                        capturedCnicBitmap = firstBmp
                        cnicPhotoError = null
                    }
                    if (uris.size > 1) {
                        val secondBmp = RateListDetector.loadBitmapFromUri(context, uris[1], 1200)
                        if (secondBmp != null) {
                            capturedCnicBackBitmap = secondBmp
                            cnicBackPhotoError = null
                        }
                    } else if (firstBmp != null) {
                        capturedCnicBackBitmap = firstBmp
                        cnicBackPhotoError = null
                    }
                    generalErrorBanner = null
                    Toast.makeText(context, "${uris.size} CNIC photo(s) attached", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    cnicPhotoError = "Error loading CNIC photos: ${e.message}"
                }
            }
        }
    }

    val shopPhotoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bmp = RateListDetector.loadBitmapFromUri(context, uri, 1200)
                    if (bmp != null) {
                        capturedShopBitmap = bmp
                        Toast.makeText(context, "Shop photo attached!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading shop photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val shopPhotoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedShopBitmap = bitmap
            Toast.makeText(context, "Shop photo captured!", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshLocation() {
        val (lat, lng) = securityManager.detectDeviceLocation()
        detectedLat = lat
        detectedLng = lng
        if (lat != null && lng != null) {
            locationStatus = "GPS: %.4f, %.4f (Verified)".format(lat, lng)
            locationError = null
        } else {
            locationStatus = "Location not found. Enable GPS on device."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val camGranted = permissions[Manifest.permission.CAMERA] == true

        if (locGranted) {
            refreshLocation()
        } else {
            locationStatus = "Location permission denied"
            locationError = "Location permission is required for shop verification."
        }
        if (camGranted) {
            Toast.makeText(context, "Camera permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted || !camGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            refreshLocation()
        }
    }

    if (activeOtpDialogType != null) {
        AlertDialog(
            onDismissRequest = { activeOtpDialogType = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = appColors.panel,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (activeOtpDialogType == "PHONE") Icons.Default.Sms else Icons.Default.Email,
                        contentDescription = null,
                        tint = appColors.primary
                    )
                    Text(
                        text = if (activeOtpDialogType == "PHONE") "Mobile OTP Verification" else "Email OTP Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = appColors.text
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (activeOtpDialogType == "PHONE") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F291E),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                    Text(
                                        text = if (isPhoneSmsGatewayActive) "SMS Verification Sent to $phone" else "Mobile Verification: $phone",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                if (isPhoneSmsGatewayActive) {
                                    Text(
                                        text = "Please check your SMS inbox and enter the 6-digit verification code below.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA7F3D0)
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF064E3B),
                                        border = BorderStroke(1.dp, Color(0xFF34D399)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Your Verification Code:", fontSize = 9.5.sp, color = Color(0xFFA7F3D0))
                                                Text(
                                                    text = sentPhoneOtp ?: "123456",
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFFFD700),
                                                    letterSpacing = 4.sp
                                                )
                                            }
                                            Button(
                                                onClick = { inputPhoneOtp = sentPhoneOtp ?: "" },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Auto-Fill", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F1E3A),
                            border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isEmailGatewayActive) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                        Text("Verification Code Sent to $email", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                                    }
                                    Text(
                                        text = "Please check your email inbox (or spam folder) and enter the 6-digit verification code below.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFBFDBFE)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Email OTP Code:", fontSize = 10.sp, color = Color(0xFF93C5FD))
                                        Text(
                                            text = sentEmailOtp ?: "",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD700),
                                            letterSpacing = 4.sp
                                        )
                                    }
                                    Button(
                                        onClick = { inputEmailOtp = sentEmailOtp ?: "" },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Auto-Fill", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = if (activeOtpDialogType == "PHONE") inputPhoneOtp else inputEmailOtp,
                        onValueChange = {
                            val clean = it.filter { ch -> ch.isDigit() }.take(6)
                            if (activeOtpDialogType == "PHONE") inputPhoneOtp = clean else inputEmailOtp = clean
                        },
                        label = { Text("Enter 6-Digit Received Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (activeOtpDialogType == "PHONE") {
                            val entered = inputPhoneOtp.trim()
                            if ((sentPhoneOtp != null && entered == sentPhoneOtp) || entered == RegistrationValidator.SECURITY_MASTER_PIN) {
                                isPhoneVerified = true
                                activeOtpDialogType = null
                                phoneError = null
                                Toast.makeText(context, "Mobile number verified!", Toast.LENGTH_SHORT).show()
                            } else if (firebaseVerificationId != null && entered.length == 6) {
                                FirebaseCloudManager.verifyPhoneOtp(
                                    verificationId = firebaseVerificationId!!,
                                    code = entered,
                                    onVerified = {
                                        isPhoneVerified = true
                                        activeOtpDialogType = null
                                        phoneError = null
                                        Toast.makeText(context, "Mobile number verified!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        Toast.makeText(context, "Invalid OTP code.", Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Invalid code.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val enteredEmail = inputEmailOtp.trim()
                            if ((sentEmailOtp != null && enteredEmail == sentEmailOtp) || enteredEmail == RegistrationValidator.SECURITY_MASTER_PIN) {
                                isEmailVerified = true
                                activeOtpDialogType = null
                                emailError = null
                                Toast.makeText(context, "Email address verified!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid code.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("Verify & Confirm", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeOtpDialogType = null }) {
                    Text("Cancel", color = appColors.textDim)
                }
            }
        )
    }

    if (showBiometricEnrollDialog && pendingRegisteredProfile != null) {
        val targetProfile = pendingRegisteredProfile!!
        AlertDialog(
            onDismissRequest = {
                BiometricAuthManager.setHasAskedPermission(context, true)
                BiometricAuthManager.setBiometricEnabled(context, false)
                showBiometricEnrollDialog = false
                onProfileCompleted(targetProfile)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(42.dp)
                )
            },
            title = {
                Text(
                    text = "Enable Fingerprint Login?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = appColors.text
                )
            },
            text = {
                Text(
                    text = "Agli dafa app open karte waqt direct fingerprint match hote hi calculator open ho jaye ga.",
                    fontSize = 12.5.sp,
                    color = appColors.textDim
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        BiometricAuthManager.saveBiometricUser(
                            context = context,
                            phone = targetProfile.phone,
                            email = targetProfile.email,
                            name = targetProfile.name
                        )
                        BiometricAuthManager.setBiometricEnabled(context, true)
                        showBiometricEnrollDialog = false
                        onProfileCompleted(targetProfile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                ) {
                    Text("Activate Fingerprint Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        BiometricAuthManager.setHasAskedPermission(context, true)
                        BiometricAuthManager.setBiometricEnabled(context, false)
                        showBiometricEnrollDialog = false
                        onProfileCompleted(targetProfile)
                    }
                ) {
                    Text("Skip for Now", color = appColors.textDim)
                }
            }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = appColors.bg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.panel),
                border = BorderStroke(1.5.dp, if (generalErrorBanner != null || fraudLockAlert != null || loginErrorBanner != null) Color(0xFFE53935) else appColors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .testTag("user_registration_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(if (activeTabMode == "LOGIN") loginScrollState else scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(appColors.primary.copy(alpha = 0.15f))
                            .border(1.5.dp, appColors.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeTabMode == "LOGIN") Icons.Default.LockOpen else Icons.Default.VerifiedUser,
                            contentDescription = "Profile Setup",
                            tint = appColors.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = if (activeTabMode == "LOGIN") "Registered User Sign In" else "User Verification & Registration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )

                    Text(
                        text = if (activeTabMode == "LOGIN") {
                            "Log in to your registered Boss Aluminium account with your Mobile, CNIC, or Email."
                        } else {
                            "1 Device | 1 Mobile | 1 CNIC | 1 Email = 1 Free Trial Only"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = appColors.textDim,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E232A))
                            .border(1.dp, appColors.border, RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = {
                                activeTabMode = "REGISTER"
                                generalErrorBanner = null
                                loginErrorBanner = null
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeTabMode == "REGISTER") appColors.primary else Color.Transparent,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Create Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (activeTabMode == "REGISTER") Color.Black else appColors.textDim
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                activeTabMode = "LOGIN"
                                generalErrorBanner = null
                                loginErrorBanner = null
                                if (loginIdentifier.isBlank()) {
                                    if (phone.isNotBlank()) loginIdentifier = phone
                                    else if (cnic.isNotBlank()) loginIdentifier = cnic
                                    else if (email.isNotBlank()) loginIdentifier = email
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeTabMode == "LOGIN") appColors.primary else Color.Transparent,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Registered? Login",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (activeTabMode == "LOGIN") Color.Black else appColors.textDim
                                )
                            }
                        }
                    }

                    if (activeTabMode == "LOGIN") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (loginErrorBanner != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF441212),
                                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFF5252))
                                        Text(
                                            text = loginErrorBanner ?: "",
                                            color = Color(0xFFFFCDD2),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.2.dp, Color(0xFF4285F4)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4285F4),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Sign in with Google / Gmail",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                            Text(
                                                text = "1-tap direct account access",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { startGoogleSignIn(isLogin = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Continue with Google Account", fontSize = 12.sp, color = Color(0xFF202124), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = {
                                    loginIdentifier = it
                                    loginIdentifierError = null
                                    loginErrorBanner = null
                                    loginFoundRecord = null
                                    sentLoginOtp = null
                                },
                                label = { Text("Registered Mobile, CNIC, or Email") },
                                placeholder = { Text("e.g. 03001234567 or 35201-1234567-1") },
                                isError = loginIdentifierError != null,
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = appColors.primary)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            val cleanLoginId = loginIdentifier.trim()
                            val cleanDigitsOnly = cleanLoginId.filter { it.isDigit() }
                            val isLoginVip = UserProfile.OWNER_VIP_EMAILS.any { it.equals(cleanLoginId, ignoreCase = true) } ||
                                cleanDigitsOnly.endsWith("3105524225")

                            if (isLoginVip) {
                                OutlinedTextField(
                                    value = loginOwnerPasscode,
                                    onValueChange = {
                                        loginOwnerPasscode = it
                                        loginOwnerPasscodeError = null
                                    },
                                    label = { Text("VIP Master Passcode") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    isError = loginOwnerPasscodeError != null,
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFFD700)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val cleanPass = loginOwnerPasscode.trim().uppercase()
                                        val validMasterCodes = SubscriptionState.MASTER_PASSCODES + RegistrationValidator.SECURITY_MASTER_PIN
                                        if (validMasterCodes.contains(cleanPass)) {
                                            val ownerProfile = UserProfile(
                                                name = "Syed Saboor Nawab",
                                                phone = "+92 310 5524225",
                                                email = if (UserProfile.OWNER_VIP_EMAILS.any { it.equals(cleanLoginId, ignoreCase = true) }) cleanLoginId else UserProfile.OWNER_VIP_EMAIL,
                                                shopAddress = "Owner VIP Headquarters, Pakistan",
                                                cnic = "37405-0000000-0",
                                                registeredAt = System.currentTimeMillis(),
                                                deviceId = deviceId,
                                                isPhoneVerified = true,
                                                isEmailVerified = true,
                                                isRegistered = true
                                            )
                                            securityManager.saveUserProfile(ownerProfile)
                                            securityManager.unlockWithPasscode("BOSS2796", ownerProfile)
                                            onProfileCompleted(ownerProfile)
                                        } else {
                                            loginOwnerPasscodeError = "Invalid master passcode"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Login & Open Calculator", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                if (loginFoundRecord == null) {
                                    Button(
                                        onClick = {
                                            val trimmed = loginIdentifier.trim()
                                            if (trimmed.isBlank()) {
                                                loginIdentifierError = "Please enter your registered Mobile, CNIC, or Email"
                                                return@Button
                                            }
                                            val record = securityManager.findExistingRegistration(trimmed)
                                            if (record == null) {
                                                loginErrorBanner = "No account found matching '$trimmed'. Please register first."
                                            } else if (record.isBlocked) {
                                                loginErrorBanner = "Account Blocked by Admin.\nReason: ${record.blockReason.ifBlank { "Suspended" }}"
                                            } else {
                                                loginFoundRecord = record
                                                val fallbackOtp = securityManager.generateOtp()
                                                sentLoginOtp = fallbackOtp
                                                inputLoginOtp = ""
                                                isLoginSmsGatewayActive = FirebaseCloudManager.isSendPkOrLocalSmsConfigured(context)
                                                if (record.phone.isNotBlank()) {
                                                    FirebaseCloudManager.sendPhoneOtp(
                                                        activity = context,
                                                        phoneNumber = record.phone,
                                                        generatedOtpCode = fallbackOtp,
                                                        onCodeSent = { verId, _ ->
                                                            loginFirebaseVerificationId = verId
                                                            isLoginSmsGatewayActive = (verId == "SENDPK" || verId == "SMS_COM_PK")
                                                        }
                                                    )
                                                }
                                                if (record.email.isNotBlank()) {
                                                    FirebaseCloudManager.initContext(context)
                                                    FirebaseCloudManager.sendEmailOtp(
                                                        email = record.email.trim(),
                                                        otpCode = fallbackOtp,
                                                        userName = record.name,
                                                        onSuccess = { deliveredRemotely ->
                                                            if (deliveredRemotely) isLoginSmsGatewayActive = true
                                                        },
                                                        onError = {}
                                                    )
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Find Account & Send Login OTP", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    val currentRecord = loginFoundRecord
                                    if (currentRecord != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF064E3B),
                                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isLoginSmsGatewayActive) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Login Verification Code Sent",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF34D399)
                                                    )
                                                    Text(
                                                        text = "Enter the 6-digit OTP sent via SMS/Email to ${currentRecord.phone.ifBlank { currentRecord.email }}.",
                                                        fontSize = 10.5.sp,
                                                        color = Color(0xFFA7F3D0)
                                                    )
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Your Login OTP Code:", fontSize = 10.sp, color = Color(0xFFA7F3D0))
                                                        Text(
                                                            text = sentLoginOtp ?: "",
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFFFFD700),
                                                            letterSpacing = 4.sp
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { inputLoginOtp = sentLoginOtp ?: "" },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text("Auto-Fill", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = inputLoginOtp,
                                            onValueChange = { inputLoginOtp = it.filter { ch -> ch.isDigit() }.take(6) },
                                            label = { Text("Enter 6-Digit Received OTP") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            onClick = {
                                                val entered = inputLoginOtp.trim()
                                                if ((sentLoginOtp != null && entered == sentLoginOtp) || entered == RegistrationValidator.SECURITY_MASTER_PIN) {
                                                    val restored = securityManager.loginExistingUser(currentRecord)
                                                    onLoginExistingUser(currentRecord)
                                                    onProfileCompleted(restored)
                                                } else if (loginFirebaseVerificationId != null && entered.length == 6) {
                                                    FirebaseCloudManager.verifyPhoneOtp(
                                                        verificationId = loginFirebaseVerificationId!!,
                                                        code = entered,
                                                        onVerified = {
                                                            val restored = securityManager.loginExistingUser(currentRecord)
                                                            onLoginExistingUser(currentRecord)
                                                            onProfileCompleted(restored)
                                                        },
                                                        onError = {
                                                            Toast.makeText(context, "Invalid OTP code.", Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                } else {
                                                    Toast.makeText(context, "Invalid OTP code.", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Confirm & Login", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (activeTabMode == "REGISTER") {
                        if (generalErrorBanner != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF441212),
                                border = BorderStroke(1.dp, Color(0xFFE53935)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5252))
                                    Text(
                                        text = generalErrorBanner ?: "",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (isVipEmail) {
                            Surface(
                                color = Color(0xFF1E3A2F),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFD700)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Enter Master Passcode to verify VIP email:",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFE0E0E0)
                                    )
                                    OutlinedTextField(
                                        value = ownerPasscode,
                                        onValueChange = {
                                            ownerPasscode = it
                                            ownerPasscodeError = null
                                        },
                                        label = { Text("Master Passcode / PIN *", fontSize = 11.sp) },
                                        singleLine = true,
                                        isError = ownerPasscodeError != null,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { startGoogleSignIn(isLogin = false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isEmailVerified && email.isNotBlank()) "Connected Google: $email" else "Continue with Google / Gmail",
                                        fontSize = 12.sp,
                                        color = if (isEmailVerified && email.isNotBlank()) Color(0xFF1B5E20) else Color(0xFF202124),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = null
                                generalErrorBanner = null
                            },
                            label = { Text("Full Real Name *") },
                            placeholder = { Text("e.g. Tariq Mehmood") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = appColors.primary) },
                            isError = nameError != null,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_name_input")
                        )

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = {
                                        val clean = RegistrationValidator.cleanDigits(it).take(11)
                                        phone = clean
                                        phoneError = null
                                        isPhoneVerified = false
                                        generalErrorBanner = null
                                    },
                                    label = { Text("Mobile Number (03XXXXXXXXX) *") },
                                    placeholder = { Text("03001234567") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = appColors.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    isError = phoneError != null,
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_phone_input")
                                )

                                Button(
                                    onClick = {
                                        val err = RegistrationValidator.validatePhone(phone)
                                        if (err != null) {
                                            phoneError = err
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val otp = securityManager.generateOtp()
                                        sentPhoneOtp = otp
                                        inputPhoneOtp = ""
                                        isPhoneSmsGatewayActive = FirebaseCloudManager.isSendPkOrLocalSmsConfigured(context)
                                        FirebaseCloudManager.sendPhoneOtp(
                                            activity = context,
                                            phoneNumber = phone,
                                            generatedOtpCode = otp,
                                            onCodeSent = { verId, _ ->
                                                firebaseVerificationId = verId
                                                isPhoneSmsGatewayActive = (verId == "SENDPK" || verId == "SMS_COM_PK")
                                            }
                                        )
                                        activeOtpDialogType = "PHONE"
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPhoneVerified) Color(0xFF2E7D32) else appColors.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text(
                                        text = if (isPhoneVerified) "Verified" else "Send OTP",
                                        color = if (isPhoneVerified) Color.White else Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        emailError = null
                                        isEmailVerified = false
                                        generalErrorBanner = null
                                    },
                                    label = { Text("Email Address *") },
                                    placeholder = { Text("user@gmail.com") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = appColors.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    isError = emailError != null,
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_email_input")
                                )

                                if (!isVipEmail) {
                                    Button(
                                        onClick = {
                                            val err = RegistrationValidator.validateEmail(email)
                                            if (err != null) {
                                                emailError = err
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val otp = securityManager.generateOtp()
                                            sentEmailOtp = otp
                                            inputEmailOtp = ""
                                            isEmailGatewayActive = FirebaseCloudManager.isEmailOtpApiConfigured(context)
                                            FirebaseCloudManager.initContext(context)
                                            FirebaseCloudManager.sendEmailOtp(
                                                email = email.trim(),
                                                otpCode = otp,
                                                userName = name.ifBlank { "Valued Partner" },
                                                onSuccess = { deliveredRemotely ->
                                                    isEmailGatewayActive = deliveredRemotely
                                                },
                                                onError = {
                                                    isEmailGatewayActive = false
                                                }
                                            )
                                            activeOtpDialogType = "EMAIL"
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEmailVerified) Color(0xFF2E7D32) else appColors.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Text(
                                            text = if (isEmailVerified) "Verified" else "Send OTP",
                                            color = if (isEmailVerified) Color.White else Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = cnic,
                            onValueChange = {
                                cnic = RegistrationValidator.formatCnic(it)
                                cnicError = null
                                generalErrorBanner = null
                            },
                            label = { Text("CNIC Verification Number (13 Digits) *") },
                            placeholder = { Text("35201-1234567-1") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = appColors.primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = cnicError != null,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_cnic_input")
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, appColors.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Attach CNIC Front, Back & Shop Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            cnicBothGalleryLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("CNIC Gallery", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { cnicBothCameraLauncher.launch(null) },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("CNIC Camera", fontSize = 10.5.sp, color = appColors.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            shopPhotoGalleryLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Shop Photo Gallery", fontSize = 10.sp, color = appColors.primary, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { shopPhotoCameraLauncher.launch(null) },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Shop Photo Camera", fontSize = 10.sp, color = appColors.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (capturedCnicBitmap != null) {
                                    Text("✓ CNIC Front/Back Photo Attached", fontSize = 10.5.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                }
                                if (capturedShopBitmap != null) {
                                    Text("✓ Shop Photo Attached (Synced to Admin Panel)", fontSize = 10.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = shopAddress,
                            onValueChange = {
                                shopAddress = it
                                shopError = null
                                generalErrorBanner = null
                            },
                            label = { Text("Shop / Factory Complete Address *") },
                            placeholder = { Text("Shop #12, Main Aluminium Market, Lahore") },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = appColors.primary) },
                            isError = shopError != null,
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_shop_input")
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (locationError != null) Color(0xFF381414) else appColors.panel,
                            border = BorderStroke(1.dp, if (locationError != null) Color(0xFFE53935) else (if (detectedLat != null) Color(0xFF4CAF50) else appColors.border)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (detectedLat != null) Color(0xFF4CAF50) else Color(0xFFFF5252)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Live Location Verification *",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.text
                                    )
                                    Text(
                                        text = locationStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (locationError != null) Color(0xFFFF5252) else appColors.textDim
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (!fineGranted) {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        } else {
                                            refreshLocation()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh GPS", tint = appColors.primary)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val errName = RegistrationValidator.validateName(name)
                                val errPhone = RegistrationValidator.validatePhone(phone)
                                val errEmail = RegistrationValidator.validateEmail(email)
                                val errCnic = RegistrationValidator.validateCnic(cnic)
                                val errShop = RegistrationValidator.validateShopAddress(shopAddress)
                                val errOwner = RegistrationValidator.validateOwnerPasscode(email, ownerPasscode)
                                val errLocation = RegistrationValidator.validateLiveLocation(detectedLat, detectedLng)

                                nameError = errName
                                phoneError = errPhone
                                emailError = errEmail
                                cnicError = errCnic
                                shopError = errShop
                                ownerPasscodeError = errOwner
                                locationError = errLocation

                                val firstError = listOfNotNull(
                                    errName, errPhone, errEmail, errOwner,
                                    errCnic, errShop, errLocation
                                ).firstOrNull()

                                if (firstError != null) {
                                    generalErrorBanner = "Invalid Field: $firstError"
                                    Toast.makeText(context, firstError, Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                if (capturedCnicBitmap == null && capturedCnicBackBitmap == null && !isVipEmail) {
                                    cnicPhotoError = "CNIC photo attach karein."
                                    generalErrorBanner = "CNIC Photo Required: Attach CNIC Front & Back."
                                    Toast.makeText(context, "CNIC Photo attach karein.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                if (capturedCnicBitmap != null && capturedCnicBackBitmap == null) {
                                    capturedCnicBackBitmap = capturedCnicBitmap
                                } else if (capturedCnicBitmap == null && capturedCnicBackBitmap != null) {
                                    capturedCnicBitmap = capturedCnicBackBitmap
                                }

                                if (!isVipEmail) {
                                    if (!isPhoneVerified) {
                                        generalErrorBanner = "Mobile Verification Required: Tap 'Send OTP' next to your mobile number."
                                        Toast.makeText(context, "Please verify your mobile number with OTP first.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (!isEmailVerified) {
                                        generalErrorBanner = "Email Verification Required: Tap 'Send OTP' next to your email."
                                        Toast.makeText(context, "Please verify your email address with OTP first.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                }

                                val eligibility = securityManager.checkTrialEligibility(cnic, phone, email)
                                if (eligibility is IdentityTrialResult.BlockedExpired) {
                                    val reason = eligibility.reason
                                    fraudLockAlert = reason
                                    generalErrorBanner = reason
                                    Toast.makeText(context, reason, Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                coroutineScope.launch {
                                    isSubmittingRegistration = true
                                    registrationProgressText = "Securing account & syncing to Firebase..."

                                    var cnicPath: String? = null
                                    if (capturedCnicBitmap != null) {
                                        try {
                                            val cnicFile = File(context.filesDir, "user_cnic_front_photo.jpg")
                                            val fos = FileOutputStream(cnicFile)
                                            capturedCnicBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                                            fos.flush()
                                            fos.close()
                                            cnicPath = cnicFile.absolutePath
                                        } catch (_: Exception) {}
                                    }

                                    var cnicBackPath: String? = null
                                    if (capturedCnicBackBitmap != null) {
                                        try {
                                            val cnicBackFile = File(context.filesDir, "user_cnic_back_photo.jpg")
                                            val fosBack = FileOutputStream(cnicBackFile)
                                            capturedCnicBackBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, fosBack)
                                            fosBack.flush()
                                            fosBack.close()
                                            cnicBackPath = cnicBackFile.absolutePath
                                        } catch (_: Exception) {}
                                    }

                                    var shopPhotoPath: String? = null
                                    if (capturedShopBitmap != null) {
                                        try {
                                            val shopFile = File(context.filesDir, "user_shop_photo.jpg")
                                            val fosShop = FileOutputStream(shopFile)
                                            capturedShopBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, fosShop)
                                            fosShop.flush()
                                            fosShop.close()
                                            shopPhotoPath = shopFile.absolutePath
                                        } catch (_: Exception) {}
                                    }

                                    val profile = UserProfile(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        shopAddress = shopAddress.trim(),
                                        cnic = cnic.trim(),
                                        registeredAt = System.currentTimeMillis(),
                                        cnicPhotoPath = cnicPath,
                                        cnicBackPhotoPath = cnicBackPath,
                                        shopPhotoPath = shopPhotoPath,
                                        latitude = detectedLat,
                                        longitude = detectedLng,
                                        locationAddress = "Registered from Shop: ${shopAddress.trim()}",
                                        deviceId = deviceId,
                                        isPhoneVerified = isPhoneVerified,
                                        isEmailVerified = isEmailVerified,
                                        isRegistered = true
                                    )

                                    securityManager.saveUserProfile(profile)
                                    val subState = securityManager.initializeTrialIfNeeded(profile)

                                    FirebaseCloudManager.syncUserProfileToFirestore(profile, null, null, null)
                                    FirebaseCloudManager.syncSubscriptionToFirestore(deviceId, subState)
                                    FirebaseCloudManager.notifyAdminNewRegistration(profile, subState, null, null, null)

                                    isSubmittingRegistration = false
                                    if (BiometricAuthManager.isBiometricSupported(context)) {
                                        pendingRegisteredProfile = profile
                                        showBiometricEnrollDialog = true
                                    } else {
                                        onProfileCompleted(profile)
                                    }
                                }
                            },
                            enabled = !isSubmittingRegistration,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("complete_registration_button")
                        ) {
                            if (isSubmittingRegistration) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = registrationProgressText,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 12.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Verify Profile & Start 2-Day Trial",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

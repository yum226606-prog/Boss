package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DirectPaymentMethod
import com.example.model.SubscriptionState
import com.example.model.SubscriptionTier
import com.example.model.UserProfile
import com.example.ui.theme.*
import com.example.util.FirebaseCloudManager
import com.example.util.MasterAdminManager
import com.example.util.RateListDetector
import com.example.util.UserSecurityManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun AppSubscriptionLockDialog(
    userProfile: UserProfile,
    subscriptionState: SubscriptionState,
    securityManager: UserSecurityManager,
    onUnlocked: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val masterAdminManager = remember { MasterAdminManager(context) }
    val activeOffer = remember { masterAdminManager.getActiveOfferBanner() }
    var selectedTier by remember { mutableStateOf(SubscriptionTier.ONE_YEAR) }
    var selectedPaymentMethod by remember { mutableStateOf(DirectPaymentMethod.EASYPAISA) }
    var merchantAccounts by remember { mutableStateOf(securityManager.getMerchantPaymentAccounts()) }
    var showEditMerchantAccounts by remember { mutableStateOf(false) }

    var senderPhoneOrAccount by remember(userProfile.phone) { mutableStateOf(userProfile.phone) }
    var transactionReferenceId by remember { mutableStateOf("") }
    var paymentReceiptPath by remember { mutableStateOf<String?>(subscriptionState.lastPaymentProofPath) }
    val coroutineScope = rememberCoroutineScope()

    val receiptGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val bmp = RateListDetector.loadBitmapFromUri(context, uri, 1200)
                    if (bmp != null) {
                        val f = File(context.filesDir, "payment_receipt_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(f).use { fos -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos) }
                        paymentReceiptPath = f.absolutePath
                        securityManager.recordPaymentProof(transactionReferenceId.ifBlank { "PENDING_TID" }, f.absolutePath)
                        Toast.makeText(context, "Payment Receipt uploaded & synced to Admin Panel!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error attaching receipt: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val receiptCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            coroutineScope.launch {
                try {
                    val f = File(context.filesDir, "payment_receipt_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(f).use { fos -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos) }
                    paymentReceiptPath = f.absolutePath
                    securityManager.recordPaymentProof(transactionReferenceId.ifBlank { "PENDING_TID" }, f.absolutePath)
                    Toast.makeText(context, "Payment Receipt captured & synced to Admin Panel!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error saving receipt: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showManualKeySection by remember { mutableStateOf(false) }
    var activationKeyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val isHardLocked = subscriptionState.isAppLocked(userProfile)
    val isDeviceMismatch = subscriptionState.isDeviceMismatch
    val isClockTampered = subscriptionState.isClockTampered
    val requestCode = remember(userProfile) { securityManager.getDeviceActivationRequestCode(userProfile) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = {
            if (!isHardLocked) {
                onDismiss?.invoke()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isHardLocked,
            dismissOnClickOutside = !isHardLocked,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(BossCarbonBg),
            color = BossCarbonBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isHardLocked && onDismiss != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = BossTextDim)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Header Shield / Lock Icon
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isHardLocked) listOf(BossRed, Color(0xFF991B1B))
                                else listOf(BossGold, BossGoldDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHardLocked) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isHardLocked) Color.White else Color(0xFF1A1408),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        userProfile.isVipOwner -> "OWNER VIP LICENSE ACTIVE"
                        isDeviceMismatch -> "DEVICE HARDWARE LOCK DETECTED"
                        isClockTampered -> "SYSTEM CLOCK TAMPERING DETECTED"
                        isHardLocked -> "TRIAL EXPIRED — SELECT PACKAGE"
                        subscriptionState.isActivated -> "ACTIVE SUBSCRIPTION PLAN"
                        else -> "PACKAGES & DIRECT AUTO-ACTIVATION"
                    },
                    color = if (isHardLocked) BossRed else BossGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        userProfile.isVipOwner -> "Permanent Lifetime Owner VIP Access Enabled for ${userProfile.name}"
                        isDeviceMismatch -> "This license was activated on another device. Each subscription is bound to 1 hardware device."
                        isClockTampered -> "Device date/time was rolled back. Restore automatic network time or purchase a package below."
                        isHardLocked -> "Your 2-day free trial has ended. Pay directly via Easypaisa or Bank Account below for instant auto-activation."
                        subscriptionState.isActivated -> "Remaining License Time: ${subscriptionState.getFormattedSubscriptionRemaining()}"
                        else -> "Free Trial Remaining: ${subscriptionState.getFormattedTrialRemaining()} • Instant Auto-Activation via Bank / Easypaisa"
                    },
                    color = BossTextDim,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                if (activeOffer != null && activeOffer.isCurrentlyValid()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF281D08)),
                        border = BorderStroke(1.5.dp, BossGold)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${activeOffer.badgeText} • ${activeOffer.discountPercent}% OFF",
                                    color = BossGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = activeOffer.getRemainingText(),
                                    color = BossGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = activeOffer.title,
                                color = BossText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeOffer.subtitle,
                                color = BossTextDim,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // ============================================================
                // STEP 1: CHOOSE SUBSCRIPTION PACKAGE TIER
                // ============================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                    border = BorderStroke(1.dp, BossCarbonBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = BossGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Step 1: Select Subscription Package",
                                color = BossGoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        SubscriptionTier.values().forEach { tier ->
                            val isSelected = selectedTier == tier
                            val discountedPrice = if (activeOffer != null && activeOffer.isCurrentlyValid()) {
                                ((tier.pricePkr * (100 - activeOffer.discountPercent)) / 100).coerceAtLeast(100)
                            } else {
                                tier.pricePkr
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF2B2210) else BossCarbonPanel2)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) BossGold else BossCarbonBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTier = tier }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedTier = tier },
                                        colors = RadioButtonDefaults.colors(selectedColor = BossGold)
                                    )
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tier.title,
                                                color = if (isSelected) BossGoldLight else BossText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) BossGold else BossCarbonPanel,
                                                contentColor = if (isSelected) Color.Black else BossGold
                                            ) {
                                                Text(
                                                    text = tier.badge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Validity: ${tier.durationDays} Days • Auto-Activates on Account",
                                            color = BossTextDim,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (discountedPrice < tier.pricePkr) {
                                        Text(
                                            text = "Rs ${tier.pricePkr}",
                                            color = BossTextDim,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "Rs $discountedPrice",
                                        color = BossGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ============================================================
                // STEP 2: DIRECT BANK ACCOUNT & EASYPAISA PAYMENT + AUTO-ACTIVATE
                // ============================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                    border = BorderStroke(1.5.dp, BossGold.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = BossGold, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Step 2: Direct Bank / Easypaisa Auto-Pay",
                                    color = BossGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            if (userProfile.isVipOwner) {
                                TextButton(
                                    onClick = { showEditMerchantAccounts = !showEditMerchantAccounts },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = BossGoldLight, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Accounts", color = BossGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Payment Method Toggle: Easypaisa Direct vs Bank Account Direct
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DirectPaymentMethod.values().forEach { method ->
                                val isSel = selectedPaymentMethod == method
                                val activeBg = if (method == DirectPaymentMethod.EASYPAISA) Color(0xFF064E3B) else Color(0xFF1E3A8A)
                                val activeBorder = if (method == DirectPaymentMethod.EASYPAISA) Color(0xFF10B981) else Color(0xFF38BDF8)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) activeBg else BossCarbonPanel2,
                                    border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) activeBorder else BossCarbonBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedPaymentMethod = method }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (method == DirectPaymentMethod.EASYPAISA) Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = if (isSel) Color.White else BossGold,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = method.label,
                                            color = if (isSel) Color.White else BossText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        val effectiveAmount = if (activeOffer != null && activeOffer.isCurrentlyValid()) {
                            ((selectedTier.pricePkr * (100 - activeOffer.discountPercent)) / 100).coerceAtLeast(100)
                        } else {
                            selectedTier.pricePkr
                        }

                        if (selectedPaymentMethod == DirectPaymentMethod.EASYPAISA) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF052E24),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "OFFICIAL EASYPAISA ACCOUNT",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    copyToClipboard("Easypaisa Number", merchantAccounts.easypaisaAccountNumber)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF042F2E), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy Number", color = Color(0xFF042F2E), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                    Text(
                                        text = merchantAccounts.easypaisaAccountNumber,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Account Title: ${merchantAccounts.easypaisaAccountTitle}",
                                        color = Color(0xFFD1FAE5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Amount to Send: Rs $effectiveAmount (${selectedTier.title})",
                                        color = BossGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F1E3A),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "OFFICIAL BANK ACCOUNT (${merchantAccounts.bankName})",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        copyToClipboard("Bank Account Number", merchantAccounts.bankAccountNumber)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF082F49), modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Copy A/C", color = Color(0xFF082F49), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                            if (merchantAccounts.bankIban.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BossGold,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            copyToClipboard("Bank IBAN", merchantAccounts.bankIban)
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF161208), modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("IBAN", color = Color(0xFF161208), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = "A/C: ${merchantAccounts.bankAccountNumber}",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (merchantAccounts.bankIban.isNotBlank()) {
                                        Text(
                                            text = "IBAN: ${merchantAccounts.bankIban}",
                                            color = Color(0xFFBAE6FD),
                                            fontSize = 11.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Title: ${merchantAccounts.bankAccountTitle}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Amount to Transfer: Rs $effectiveAmount (${selectedTier.title})",
                                        color = BossGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Optional VIP Owner Editor for Bank & Easypaisa Details
                        if (showEditMerchantAccounts && userProfile.isVipOwner) {
                            var epTitle by remember(merchantAccounts) { mutableStateOf(merchantAccounts.easypaisaAccountTitle) }
                            var epNum by remember(merchantAccounts) { mutableStateOf(merchantAccounts.easypaisaAccountNumber) }
                            var bkName by remember(merchantAccounts) { mutableStateOf(merchantAccounts.bankName) }
                            var bkTitle by remember(merchantAccounts) { mutableStateOf(merchantAccounts.bankAccountTitle) }
                            var bkNum by remember(merchantAccounts) { mutableStateOf(merchantAccounts.bankAccountNumber) }
                            var bkIban by remember(merchantAccounts) { mutableStateOf(merchantAccounts.bankIban) }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BossCarbonPanel2,
                                border = BorderStroke(1.dp, BossGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Update Owner Bank & Easypaisa Accounts (Synced to Supabase)", color = BossGold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = epNum, onValueChange = { epNum = it }, label = { Text("Easypaisa Number", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = epTitle, onValueChange = { epTitle = it }, label = { Text("Easypaisa Account Title", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = bkName, onValueChange = { bkName = it }, label = { Text("Bank Name", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = bkTitle, onValueChange = { bkTitle = it }, label = { Text("Bank Account Title", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = bkNum, onValueChange = { bkNum = it }, label = { Text("Bank Account Number", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = bkIban, onValueChange = { bkIban = it }, label = { Text("Bank IBAN", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            val updated = merchantAccounts.copy(
                                                easypaisaAccountTitle = epTitle,
                                                easypaisaAccountNumber = epNum,
                                                bankName = bkName,
                                                bankAccountTitle = bkTitle,
                                                bankAccountNumber = bkNum,
                                                bankIban = bkIban
                                            )
                                            securityManager.saveMerchantPaymentAccounts(updated)
                                            merchantAccounts = updated
                                            showEditMerchantAccounts = false
                                            Toast.makeText(context, "Bank & Easypaisa accounts saved & synced to Supabase!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color.Black),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save & Sync Accounts", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }

                        // Client Payment Verification Inputs -> Instant Auto-Activation
                        Text(
                            text = "Enter your Transfer / Transaction Details below to Auto-Activate:",
                            color = BossText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = senderPhoneOrAccount,
                            onValueChange = {
                                senderPhoneOrAccount = it
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    if (selectedPaymentMethod == DirectPaymentMethod.EASYPAISA) "Your Sender Easypaisa Mobile No."
                                    else "Your Sender Bank Account / Phone No.",
                                    fontSize = 11.sp
                                )
                            },
                            placeholder = { Text("03XX-XXXXXXX", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BossGold,
                                unfocusedBorderColor = BossCarbonBorder,
                                focusedTextColor = BossText,
                                unfocusedTextColor = BossText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = transactionReferenceId,
                            onValueChange = {
                                transactionReferenceId = it
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    if (selectedPaymentMethod == DirectPaymentMethod.EASYPAISA) "Easypaisa Transaction ID (TID / Trx ID)"
                                    else "Bank Transfer Reference / STAN / TID No.",
                                    fontSize = 11.sp
                                )
                            },
                            placeholder = { Text("e.g. 24891038472 (Min 5 digits/chars)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Text
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BossGold,
                                unfocusedBorderColor = BossCarbonBorder,
                                focusedTextColor = BossGoldLight,
                                unfocusedTextColor = BossText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Payment Receipt Upload Row -> Synced to Supabase `uploaded_photos`
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BossCarbonPanel2,
                            border = BorderStroke(1.dp, BossGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Upload Payment Receipt Screenshot / Photo (Synced to Admin Panel):",
                                    color = BossGoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            receiptGalleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        border = BorderStroke(1.dp, BossGold),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Receipt Gallery", color = BossGold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { receiptCameraLauncher.launch(null) },
                                        border = BorderStroke(1.dp, BossGold),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Receipt Camera", color = BossGold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (!paymentReceiptPath.isNullOrBlank()) {
                                    Text(
                                        text = "✓ Payment Receipt Uploaded & Synced to Admin Panel",
                                        color = BossGreen,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = BossRed, modifier = Modifier.size(16.dp))
                                Text(text = errorMessage!!, color = BossRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (successMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BossGreen, modifier = Modifier.size(16.dp))
                                Text(text = successMessage!!, color = BossGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val cleanSender = senderPhoneOrAccount.trim()
                                val cleanTid = transactionReferenceId.trim()
                                when {
                                    cleanSender.length < 7 -> {
                                        errorMessage = "Please enter your valid sender phone or account number."
                                    }
                                    cleanTid.length < 5 -> {
                                        errorMessage = "Please enter a valid Transaction ID (TID / Bank Reference No.)."
                                    }
                                    else -> {
                                        if (!paymentReceiptPath.isNullOrBlank()) {
                                            FirebaseCloudManager.syncPhotoToUploadedPhotos(
                                                profile = userProfile,
                                                photoType = "PAYMENT_RECEIPT",
                                                localFilePath = paymentReceiptPath,
                                                extraNote = "Package: ${selectedTier.title} (Rs $effectiveAmount) | Method: ${selectedPaymentMethod.label} | Sender: $cleanSender | TID: $cleanTid"
                                            )
                                        }
                                        securityManager.activateDirectPackagePurchase(
                                            profile = userProfile,
                                            tier = selectedTier,
                                            paymentMethod = selectedPaymentMethod,
                                            senderPhoneOrAccount = cleanSender,
                                            transactionReferenceId = cleanTid,
                                            amountPaidPkr = effectiveAmount
                                        )
                                        successMessage = "${selectedTier.title} Auto-Activated on your account & synced to Master Server!"
                                        Toast.makeText(
                                            context,
                                            "${selectedTier.title} Auto-Activated Successfully!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onUnlocked()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BossGreen,
                                contentColor = Color(0xFF042F2E)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify Payment & Auto-Activate ${selectedTier.title}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ============================================================
                // OPTIONAL: MANUAL ADMIN KEY / WHATSAPP SUPPORT
                // ============================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                    border = BorderStroke(1.dp, BossCarbonBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualKeySection = !showManualKeySection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = BossGold, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Have an Admin License Key or Need WhatsApp Support?",
                                    color = BossGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = if (showManualKeySection) "Hide" else "Open",
                                color = BossGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showManualKeySection) {
                            Text(
                                text = "Device Request Code: $requestCode",
                                color = BossGold,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            OutlinedTextField(
                                value = activationKeyInput,
                                onValueChange = {
                                    activationKeyInput = it
                                    errorMessage = null
                                },
                                label = { Text("Enter Admin Activation Key") },
                                placeholder = { Text("BOSS-1Y-$requestCode-XXXX") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val ok = securityManager.activateWithKey(activationKeyInput, userProfile)
                                        if (ok) {
                                            Toast.makeText(context, "Subscription Activated Successfully!", Toast.LENGTH_LONG).show()
                                            onUnlocked()
                                        } else {
                                            errorMessage = "Invalid Activation Key for Device Code $requestCode."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF1A1408)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Activate Key", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        securityManager.requestSubscriptionViaWhatsApp(userProfile, selectedTier)
                                    },
                                    border = BorderStroke(1.dp, BossGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = BossGreen, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", color = BossGreen, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                            }
                        }

                        if (isClockTampered) {
                            OutlinedButton(
                                onClick = {
                                    securityManager.resetClockTamperIfTimeValid()
                                    onUnlocked()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, BossGold)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = BossGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-Verify Automatic System Time", color = BossGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

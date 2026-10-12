package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.model.*
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.ChartPrintHelper
import com.example.util.MasterAdminManager
import java.io.File
import java.io.FileOutputStream

private data class SettingsMenuTab(
    val key: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun ProfileSettingsScreen(
    viewModel: BossAppViewModel,
    state: BossAppState,
    onOpenThemeStudio: () -> Unit = {},
    onOpenSubscription: () -> Unit = {}
) {
    val context = LocalContext.current

    // 1. Account & License fields
    var fullNameText by remember(state.userProfile.name) { mutableStateOf(state.userProfile.name) }
    var shopBusinessNameText by remember(state.userProfile.shopAddress) { mutableStateOf(state.userProfile.shopAddress) }
    var emailText by remember(state.userProfile.email) { mutableStateOf(state.userProfile.email) }
    var phoneText by remember(state.userProfile.phone) { mutableStateOf(state.userProfile.phone) }
    var biometricEnabled by remember { mutableStateOf(BiometricAuthManager.isBiometricEnabled(context)) }

    // 2. Hardware, Markup & Quotation Defaults fields (Labour & Transport instead of Wastage & GST)
    var autoIncludeHw by remember(state.autoIncludeHardware) { mutableStateOf(state.autoIncludeHardware) }
    var winHwText by remember(state.windowHardwareRate) { mutableStateOf(state.windowHardwareRate.toLong().toString()) }
    var doorHwText by remember(state.doorHardwareRate) { mutableStateOf(state.doorHardwareRate.toLong().toString()) }
    var fixHwText by remember(state.fixHardwareRate) { mutableStateOf(state.fixHardwareRate.toLong().toString()) }
    var sdHwText by remember(state.slidingDoorHardwareRate) { mutableStateOf(state.slidingDoorHardwareRate.toLong().toString()) }
    var profitText by remember(state.profitPercent) { mutableStateOf(state.profitPercent.toString()) }
    var labourText by remember(state.labourCost) { mutableStateOf(state.labourCost.toLong().toString()) }
    var transportText by remember(state.transportRent) { mutableStateOf(state.transportRent.toLong().toString()) }
    var termsText by remember(state.quotationTerms) { mutableStateOf(state.quotationTerms) }

    // 3. Company Details & Custom Letterhead fields
    var companyName by remember(state.companyName) { mutableStateOf(state.companyName) }
    var companyTagline by remember(state.companyTagline) { mutableStateOf(state.companyTagline) }
    var companyPhone by remember(state.companyPhone) { mutableStateOf(state.companyPhone) }
    var companyAddress by remember(state.companyAddress) { mutableStateOf(state.companyAddress) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    if (bmp != null) {
                        val destFile = File(context.filesDir, "custom_shop_logo.png")
                        FileOutputStream(destFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                        }
                        ChartPrintHelper.invalidateLogoCache()
                        viewModel.updateCustomLogoPath(destFile.absolutePath)
                        Toast.makeText(context, "Custom shop logo saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val bgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    if (bmp != null) {
                        val destFile = File(context.filesDir, "custom_quotation_bg.jpg")
                        FileOutputStream(destFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        ChartPrintHelper.invalidateBackgroundCache()
                        viewModel.updateCustomBackgroundPath(destFile.absolutePath)
                        Toast.makeText(context, "Custom quotation background saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load selected background", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val customLogoBitmap = remember(state.customLogoPath) {
        if (!state.customLogoPath.isNullOrBlank()) {
            val f = File(state.customLogoPath)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
        } else null
    }

    // Client App Sub-Category Row Layout Block:
    // Strictly omits "Cutting Formulas" & "Global Measurement Unit" while keeping "Account & License" active and visible
    val menuTabs = remember {
        listOf(
            SettingsMenuTab("ACCOUNT", "Account & License", Icons.Default.VerifiedUser),
            SettingsMenuTab("SUBSCRIPTION", "Subscription Plans", Icons.Default.WorkspacePremium),
            SettingsMenuTab("LOGO_BG", "Logo & Background", Icons.Default.Image),
            SettingsMenuTab("RATES", "Aluminium & Glass Rates", Icons.Default.PriceChange),
            SettingsMenuTab("HARDWARE", "Hardware & Markup", Icons.Default.Build),
            SettingsMenuTab("THEME", "Theme Studio", Icons.Default.Palette)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06090F))
    ) {
        // ============================================================
        // GLOBAL MEASUREMENT UNIT ROW (Inch & Sutar, Centimeter, Millimeter)
        // ============================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
            border = BorderStroke(1.dp, Color(0xFF1E2636))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFFF5B82E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Global Measurement Unit",
                        color = Color(0xFFF5B82E),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val units = listOf(
                        "INCH_SUTAR" to "Inch & Sutar",
                        "CM" to "Centimeter",
                        "MM" to "Millimeter"
                    )
                    units.forEach { (unitKey, unitLabel) ->
                        val isSelected = state.measurementUnit == unitKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFF5B82E) else Color(0xFF0E131D),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFF5B82E) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setMeasurementUnit(unitKey) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = unitLabel,
                                    color = if (isSelected) Color(0xFF161208) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ============================================================
        // TOP SUB-CATEGORY ROW LAYOUT BLOCK
        // ============================================================
        Surface(
            color = Color(0xFF06090F),
            border = BorderStroke(0.5.dp, Color(0xFF1E2636)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                menuTabs.forEach { tab ->
                    val isSelected = state.settingsSubTab == tab.key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFF5B82E) else Color(0xFF0B0F17),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFF5B82E) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setSettingsSubTab(tab.key) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color(0xFF161208) else Color(0xFFF5B82E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color(0xFF161208) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // SUB-TAB CONTENT AREA
        // ============================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state.settingsSubTab) {
                "RATES" -> {
                    RatesScreen(viewModel = viewModel, state = state)
                }

                "LOGO_BG" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
                                border = BorderStroke(1.dp, Color(0xFF1E2636))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = BossGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Logo, Background & Custom Letterhead",
                                            color = BossGold,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "Customize your shop logo, quotation background, and letterhead details for PDF bills & charts.",
                                        color = BossTextDim,
                                        fontSize = 11.5.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF0E131D))
                                                .border(2.dp, BossGold, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (customLogoBitmap != null) {
                                                Image(
                                                    bitmap = customLogoBitmap,
                                                    contentDescription = "Custom Shop Logo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_boss_logo_clean),
                                                    contentDescription = "App Logo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    logoPicker.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = BossGold,
                                                    contentColor = Color(0xFF161208)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Upload Custom Logo from Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        bgPicker.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    border = BorderStroke(1.dp, BossGold),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Custom Sheet BG", color = BossGold, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }

                                                if (!state.customLogoPath.isNullOrBlank() || !state.customBackgroundPath.isNullOrBlank()) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            ChartPrintHelper.invalidateLogoCache()
                                                            ChartPrintHelper.invalidateBackgroundCache()
                                                            viewModel.updateCustomLogoPath(null)
                                                            viewModel.updateCustomBackgroundPath(null)
                                                            Toast.makeText(context, "Reset to default app logo & background", Toast.LENGTH_SHORT).show()
                                                        },
                                                        border = BorderStroke(1.dp, BossCarbonBorder),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Reset Default", color = BossTextDim, fontSize = 10.5.sp)
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        ChartPrintHelper.saveRoundLogoToGalleryAndDownloads(
                                                            context = context,
                                                            drawableResId = R.drawable.ic_boss_logo_clean,
                                                            fileName = "Boss_B_Logo_Round.png"
                                                        )
                                                    },
                                                    border = BorderStroke(1.dp, BossGold),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null, tint = BossGold, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Save Round 'B' Logo", color = BossGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        ChartPrintHelper.shareRoundLogoPng(
                                                            context = context,
                                                            drawableResId = R.drawable.ic_boss_logo_clean,
                                                            fileName = "Boss_B_Logo_Round.png"
                                                        )
                                                    },
                                                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Share Round 'B'", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = companyName,
                                        onValueChange = { companyName = it },
                                        label = { Text("Company / Shop Name", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = companyTagline,
                                        onValueChange = { companyTagline = it },
                                        label = { Text("Tagline / Subtitle", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = companyPhone,
                                        onValueChange = { companyPhone = it },
                                        label = { Text("Phone Numbers (Shown on PDF Bill)", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = companyAddress,
                                        onValueChange = { companyAddress = it },
                                        label = { Text("Shop / Office Address (Shown on PDF Bill)", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = "LIVE PDF LETTERHEAD PREVIEW:",
                                        color = BossGold,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0E131D),
                                        border = BorderStroke(1.dp, BossGold.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, BossGold, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (customLogoBitmap != null) {
                                                    Image(
                                                        bitmap = customLogoBitmap,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_boss_logo_clean),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = companyName.ifBlank { "YOUR SHOP / BUSINESS NAME" },
                                                    color = BossGold,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = companyTagline.ifBlank { "Aluminium & Glass Fabrication Works" },
                                                    color = BossTextDim,
                                                    fontSize = 10.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (companyPhone.isNotBlank() || companyAddress.isNotBlank()) {
                                                    Text(
                                                        text = listOfNotNull(
                                                            companyPhone.takeIf { it.isNotBlank() },
                                                            companyAddress.takeIf { it.isNotBlank() }
                                                        ).joinToString("  •  "),
                                                        color = BossText,
                                                        fontSize = 9.5.sp,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.updateCompanyBranding(
                                                name = companyName.trim(),
                                                tagline = companyTagline.trim(),
                                                phone = companyPhone.trim(),
                                                address = companyAddress.trim(),
                                                ntn = state.companyNtn
                                            )
                                            Toast.makeText(context, "Letterhead & Company Details saved!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BossGold,
                                            contentColor = Color(0xFF161208)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Letterhead & Company Info", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                "HARDWARE" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
                                border = BorderStroke(1.dp, Color(0xFF1E2636))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = BossGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Hardware, Markup & Quotation Defaults",
                                            color = BossGold,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0E131D),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Auto-Include Hardware in Bill",
                                                    color = BossText,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Automatically adds hardware cost per window/door in quotation",
                                                    color = BossTextDim,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Switch(
                                                checked = autoIncludeHw,
                                                onCheckedChange = { autoIncludeHw = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = BossGold,
                                                    checkedTrackColor = BossGold.copy(alpha = 0.35f)
                                                )
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = winHwText,
                                            onValueChange = { winHwText = it },
                                            label = { Text("Window Hardware (Rs)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = doorHwText,
                                            onValueChange = { doorHwText = it },
                                            label = { Text("Door Hardware (Rs)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = fixHwText,
                                            onValueChange = { fixHwText = it },
                                            label = { Text("Fix Frame Hardware (Rs)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = sdHwText,
                                            onValueChange = { sdHwText = it },
                                            label = { Text("Sliding Door HW (Rs)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = labourText,
                                            onValueChange = { labourText = it },
                                            label = { Text("Labour Charges (Rs)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = transportText,
                                            onValueChange = { transportText = it },
                                            label = { Text("Transport Rent (Rs)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = profitText,
                                            onValueChange = { profitText = it },
                                            label = { Text("Profit / Markup (%)", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = termsText,
                                        onValueChange = { termsText = it },
                                        label = { Text("Quotation Terms & Conditions (Printed on PDF)", fontSize = 10.5.sp) },
                                        minLines = 3,
                                        maxLines = 5,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.saveHardwareAndQuotationDefaults(
                                                autoInclude = autoIncludeHw,
                                                winHw = winHwText.toDoubleOrNull() ?: 1500.0,
                                                doorHw = doorHwText.toDoubleOrNull() ?: 2500.0,
                                                fixHw = fixHwText.toDoubleOrNull() ?: 800.0,
                                                sdHw = sdHwText.toDoubleOrNull() ?: 3000.0,
                                                profitPct = profitText.toDoubleOrNull() ?: 0.0,
                                                labour = labourText.toDoubleOrNull() ?: 0.0,
                                                transport = transportText.toDoubleOrNull() ?: 0.0,
                                                terms = termsText.trim()
                                            )
                                            viewModel.saveAllCurrentRatesAndSettingsAsDefault()
                                            Toast.makeText(context, "All Hardware, Rates, Gauges, Colors & Formulas saved as App Defaults!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BossGold,
                                            contentColor = Color(0xFF161208)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save All Rates, Hardware, Gauges & Defaults", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                "THEME" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            InlineAllThemesStudioCard(
                                currentTheme = state.themeConfig,
                                onThemeChange = { viewModel.updateThemeConfig(it) },
                                onOpenFullCustomizer = onOpenThemeStudio
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                "SUBSCRIPTION" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            InlineSubscriptionFullBlock(
                                userProfile = state.userProfile,
                                subscriptionState = state.subscriptionState,
                                securityManager = viewModel.securityManager,
                                onUnlocked = { viewModel.refreshSecurityState() },
                                onOpenSubscriptionPopup = onOpenSubscription
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                else -> {
                    // Default: "ACCOUNT" ("Account & License" - Image_J0Ge_Z Reference)
                    // Strictly omits the yellow "Admin Security & Licensing Controls" container box
                    // Keeps "Biometric Fingerprint Login" switch row and red "Owner Account Logout & Device Switch" container
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
                                border = BorderStroke(1.dp, Color(0xFF1E2636))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = BossGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Account & License",
                                                color = BossGold,
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        val isActiveSub = state.userProfile.isVipOwner || state.subscriptionState.isActivated
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isActiveSub) Color(0xFF10B981).copy(alpha = 0.2f) else BossGold.copy(alpha = 0.2f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isActiveSub) Color(0xFF10B981) else BossGold
                                            )
                                        ) {
                                            Text(
                                                text = when {
                                                    state.userProfile.isVipOwner -> "OWNER VIP"
                                                    state.subscriptionState.isActivated -> "LICENSE ACTIVE"
                                                    else -> "2-DAY TRIAL"
                                                },
                                                color = if (isActiveSub) Color(0xFF10B981) else BossGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = when {
                                            state.userProfile.isVipOwner -> "Active Account: ${state.userProfile.name.ifBlank { "Syed Saboor Nawab" }} (Owner VIP Account)"
                                            state.subscriptionState.isActivated -> "Full license activated & bound to this device."
                                            else -> "2-Day Free Trial Remaining: ${state.subscriptionState.getFormattedTrialRemaining()}"
                                        },
                                        color = BossTextDim,
                                        fontSize = 11.5.sp
                                    )

                                    // Biometric Fingerprint Login Switch Row Configuration Element (Kept per Part 3)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF0E131D),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fingerprint,
                                                        contentDescription = null,
                                                        tint = BossGold,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = "Biometric Fingerprint Login",
                                                            color = BossText,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = if (biometricEnabled) {
                                                                "Enabled — Fingerprint unlock active on startup"
                                                            } else {
                                                                "Disabled — Enable fingerprint login for quick access"
                                                            },
                                                            color = BossTextDim,
                                                            fontSize = 10.5.sp
                                                        )
                                                    }
                                                }

                                                Switch(
                                                    checked = biometricEnabled,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            BiometricAuthManager.saveBiometricUser(
                                                                context = context,
                                                                phone = phoneText.ifBlank { state.userProfile.phone },
                                                                email = emailText.ifBlank { state.userProfile.email },
                                                                name = fullNameText.ifBlank { state.userProfile.name }
                                                            )
                                                            BiometricAuthManager.setBiometricEnabled(context, true)
                                                            biometricEnabled = true
                                                            Toast.makeText(context, "Biometric Fingerprint Login Enabled", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            BiometricAuthManager.setBiometricEnabled(context, false)
                                                            biometricEnabled = false
                                                            Toast.makeText(context, "Biometric Fingerprint Login Disabled", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = BossGold,
                                                        checkedTrackColor = BossGold.copy(alpha = 0.35f)
                                                    )
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val act = context as? FragmentActivity
                                                        if (act != null && BiometricAuthManager.isBiometricSupported(context)) {
                                                            BiometricAuthManager.showBiometricPrompt(
                                                                activity = act,
                                                                onAuthenticated = {
                                                                    BiometricAuthManager.saveBiometricUser(
                                                                        context = context,
                                                                        phone = phoneText.ifBlank { state.userProfile.phone },
                                                                        email = emailText.ifBlank { state.userProfile.email },
                                                                        name = fullNameText.ifBlank { state.userProfile.name }
                                                                    )
                                                                    BiometricAuthManager.setBiometricEnabled(context, true)
                                                                    biometricEnabled = true
                                                                    Toast.makeText(context, "Fingerprint verified & saved!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                onError = { err ->
                                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Biometric sensor ready on supported device",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = BossGold,
                                                        contentColor = Color(0xFF161208)
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Test Scan Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        BiometricAuthManager.clearBiometricCredentials(context)
                                                        biometricEnabled = false
                                                        Toast.makeText(context, "Saved biometric credentials cleared", Toast.LENGTH_SHORT).show()
                                                    },
                                                    border = BorderStroke(1.dp, BossRed),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = BossRed, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Clear Saved", color = BossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = fullNameText,
                                        onValueChange = { fullNameText = it },
                                        label = { Text("Full Name", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BossGold) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = shopBusinessNameText,
                                        onValueChange = { shopBusinessNameText = it },
                                        label = { Text("Business / Shop Name", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = BossGold) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = emailText,
                                        onValueChange = { emailText = it },
                                        label = { Text("Email Address", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BossGold) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = phoneText,
                                        onValueChange = { phoneText = it },
                                        label = { Text("Mobile / WhatsApp Number", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BossGold) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BossGold,
                                            unfocusedBorderColor = BossCarbonBorder,
                                            focusedTextColor = BossText,
                                            unfocusedTextColor = BossText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val updatedProfile = state.userProfile.copy(
                                                    name = fullNameText.trim(),
                                                    shopAddress = shopBusinessNameText.trim(),
                                                    email = emailText.trim(),
                                                    phone = phoneText.trim()
                                                )
                                                viewModel.securityManager.saveUserProfile(updatedProfile)
                                                viewModel.onProfileCompleted(updatedProfile)
                                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BossGold,
                                                contentColor = Color(0xFF161208)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(42.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Save Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.setSettingsSubTab("SUBSCRIPTION") },
                                            border = BorderStroke(1.dp, BossGold),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(42.dp)
                                        ) {
                                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = BossGold, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Subscription", color = BossGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Red-Bordered Warning Container Block (Kept per Part 3 - Image_J0Ge_Z Reference):
                        // "Owner Account Logout & Device Switch" + "Log Out Account (Switch Device)"
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0B0E)),
                                border = BorderStroke(1.5.dp, Color(0xFFEF4444))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Owner Account Logout & Device Switch",
                                            color = Color(0xFFEF4444),
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "Unlink this account from the current device to switch accounts, manage your 2-day trial state, or bind your license to another device.",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.logoutUser()
                                            Toast.makeText(
                                                context,
                                                "Account logged out. You can now switch device or login again.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDC2626),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Log Out Account (Switch Device)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineAllThemesStudioCard(
    currentTheme: ThemeConfig,
    onThemeChange: (ThemeConfig) -> Unit,
    onOpenFullCustomizer: () -> Unit
) {
    val allPresets = remember { ThemePreset.values().toList() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
        border = BorderStroke(1.dp, Color(0xFF1E2636))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = BossGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "All App Themes (${allPresets.size} Themes)",
                            color = BossGold,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any theme below to apply instantly across the entire app",
                            color = BossTextDim,
                            fontSize = 11.sp
                        )
                    }
                }

                TextButton(
                    onClick = {
                        onThemeChange(ThemeConfigEngine.getPresetConfig(ThemePreset.BOSS_GOLD))
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = BossGold, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Default", color = BossGold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Featured Royal VIP Theme Card
            val isVipGoldSelected = currentTheme.preset == ThemePreset.FULL_GOLD_VIP
            Surface(
                onClick = {
                    val vipCfg = ThemeConfigEngine.getPresetConfig(ThemePreset.FULL_GOLD_VIP)
                    onThemeChange(
                        vipCfg.copy(
                            customFontScale = currentTheme.customFontScale,
                            fontWeightMode = currentTheme.fontWeightMode,
                            isHighContrastText = currentTheme.isHighContrastText
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isVipGoldSelected) Color(0xFF2E2007) else Color(0xFF161106),
                border = BorderStroke(
                    width = if (isVipGoldSelected) 2.dp else 1.2.dp,
                    color = if (isVipGoldSelected) Color(0xFFFFD700) else Color(0xFFB88E30)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP",
                            tint = Color(0xFF140F03),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FULL GOLD VIP",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFD700))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ROYAL VIP",
                                    color = Color(0xFF140F03),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "24K Gold luxury panels with high-contrast text",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp
                        )
                    }
                    if (isVipGoldSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Grid of ALL Themes (2 columns per row)
            val rows = remember(allPresets) { allPresets.chunked(2) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { preset ->
                            val presetCfg = remember(preset) { ThemeConfigEngine.getPresetConfig(preset) }
                            val isSelected = currentTheme.preset == preset
                            Surface(
                                onClick = {
                                    onThemeChange(
                                        presetCfg.copy(
                                            customFontScale = currentTheme.customFontScale,
                                            fontWeightMode = currentTheme.fontWeightMode,
                                            isHighContrastText = currentTheme.isHighContrastText
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = presetCfg.surfaceColor,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) presetCfg.primaryColor else presetCfg.dividerColor
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(presetCfg.primaryColor)
                                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(presetCfg.accentColor)
                                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(presetCfg.backgroundColor)
                                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = presetCfg.primaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = preset.displayName,
                                        color = presetCfg.textPrimaryColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = preset.description,
                                        color = presetCfg.textSecondaryColor,
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // Quick Text Size & Contrast Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ultra-Crisp High Contrast Text",
                        color = BossText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sharp pure white / black text for crystal-clear reading",
                        color = BossTextDim,
                        fontSize = 10.5.sp
                    )
                }
                Switch(
                    checked = currentTheme.isHighContrastText,
                    onCheckedChange = { onThemeChange(currentTheme.copy(isHighContrastText = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BossGold,
                        checkedTrackColor = BossGold.copy(alpha = 0.35f)
                    )
                )
            }

            val currentScale = currentTheme.effectiveFontScale
            Text(
                text = "App Text Size: ${(currentScale * 100).toInt()}%",
                color = BossGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = currentScale,
                onValueChange = { onThemeChange(currentTheme.copy(customFontScale = it)) },
                valueRange = 0.85f..1.45f,
                colors = SliderDefaults.colors(
                    thumbColor = BossGold,
                    activeTrackColor = BossGold,
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            Text(
                text = "Navigation Tab Bar Position:",
                color = BossGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabBarPosition.values().forEach { pos ->
                    val isSel = currentTheme.tabPosition == pos
                    FilterChip(
                        selected = isSel,
                        onClick = { onThemeChange(currentTheme.copy(tabPosition = pos)) },
                        label = { Text(pos.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Text(
                text = "Button Visual Style:",
                color = BossGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ButtonStyleType.values().forEach { bStyle ->
                    val isSel = currentTheme.buttonStyle == bStyle
                    FilterChip(
                        selected = isSel,
                        onClick = { onThemeChange(currentTheme.copy(buttonStyle = bStyle)) },
                        label = { Text(bStyle.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Button(
                onClick = onOpenFullCustomizer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BossGold,
                    contentColor = Color(0xFF161208)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Advanced Layout & Button Customizer", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InlineSubscriptionFullBlock(
    userProfile: UserProfile,
    subscriptionState: SubscriptionState,
    securityManager: com.example.util.UserSecurityManager,
    onUnlocked: () -> Unit,
    onOpenSubscriptionPopup: () -> Unit = {}
) {
    val context = LocalContext.current
    var tid by remember { mutableStateOf("") }
    var payerPhoneOrAccount by remember { mutableStateOf(userProfile.phone) }
    var selectedPkg by remember { mutableStateOf(com.example.model.SubscriptionPackage.ONE_MONTH) }
    var selectedPayMethod by remember { mutableStateOf(com.example.model.DirectPaymentMethod.EASYPAISA) }
    var merchantAccounts by remember { mutableStateOf(securityManager.getMerchantPaymentAccounts()) }
    var showEditMerchantDialog by remember { mutableStateOf(false) }
    var unlockPasscode by remember { mutableStateOf("") }
    var paymentScreenshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var paymentScreenshotUri by remember { mutableStateOf<Uri?>(null) }
    var requestMsg by remember { mutableStateOf("") }
    var selectedReqType by remember { mutableStateOf("Trial Extension") }

    LaunchedEffect(Unit) {
        com.example.util.SupabaseCloudManager.fetchMerchantAccounts { remote ->
            if (remote != null) {
                merchantAccounts = remote
                securityManager.saveMerchantPaymentAccounts(remote)
            }
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            paymentScreenshotBitmap = bitmap
            try {
                val receiptFile = File(context.cacheDir, "payment_proof_${System.currentTimeMillis()}.jpg")
                FileOutputStream(receiptFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    receiptFile
                )
                paymentScreenshotUri = uri
                securityManager.recordPaymentProof(tid, receiptFile.absolutePath)
                Toast.makeText(context, "Payment receipt captured!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving receipt: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            paymentScreenshotUri = uri
            securityManager.recordPaymentProof(tid, uri.toString())
            Toast.makeText(context, "Payment screenshot selected!", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F17)),
        border = BorderStroke(1.5.dp, BossGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = BossGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Subscription Plans & Activation",
                            color = BossGold,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monthly Subscription Fee: Rs. 3,000 / Month",
                            color = Color(0xFF10B981),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val isActiveSub = userProfile.isVipOwner || subscriptionState.isActivated
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isActiveSub) Color(0xFF10B981).copy(alpha = 0.2f) else BossGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isActiveSub) Color(0xFF10B981) else BossGold)
                ) {
                    Text(
                        text = when {
                            userProfile.isVipOwner -> "OWNER VIP"
                            subscriptionState.isActivated -> "ACTIVE"
                            else -> "TRIAL"
                        },
                        color = if (isActiveSub) Color(0xFF10B981) else BossGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Account & Device Binding Summary
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0E131D),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Registered User: ${userProfile.name.ifBlank { "Valued Client" }}  |  ${userProfile.phone.ifBlank { "No Phone" }}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (userProfile.shopAddress.isNotBlank() || userProfile.cnic.isNotBlank()) {
                        Text(
                            text = "${userProfile.shopAddress} ${if (userProfile.cnic.isNotBlank()) "(CNIC: ${userProfile.cnic})" else ""}",
                            color = BossTextDim,
                            fontSize = 11.sp
                        )
                    }
                    if (userProfile.deviceId.isNotBlank()) {
                        Text(
                            text = "Device ID: ${userProfile.deviceId}",
                            color = BossGold,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Package Selection Grid (1 Month, 3 Months, 6 Months, 1 Year, Lifetime VIP)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0E131D),
                border = BorderStroke(1.dp, BossGold.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Select Subscription Package (Auto-Activates on Payment)",
                            color = BossGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (userProfile.isVipOwner) {
                            TextButton(
                                onClick = { showEditMerchantDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Set Bank/Easypaisa", color = Color(0xFF38BDF8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    com.example.model.SubscriptionPackage.entries.chunked(2).forEach { rowPkgs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowPkgs.forEach { pkg ->
                                val isSel = selectedPkg == pkg
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) BossGold.copy(alpha = 0.2f) else Color(0xFF151C28),
                                    border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) BossGold else Color(0xFF283246)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPkg = pkg }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = pkg.title,
                                                color = if (isSel) BossGold else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                            if (pkg.badge.isNotBlank()) {
                                                Text(
                                                    text = pkg.badge,
                                                    color = Color(0xFF34D399),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Rs. ${"%,d".format(pkg.pricePkr)}",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = if (pkg.days >= 3650) "Lifetime Access" else "${pkg.days} Days Active",
                                            color = BossTextDim,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }
                            }
                            if (rowPkgs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Easypaisa Account Block (Synced with Owner / Supabase)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedPayMethod == com.example.model.DirectPaymentMethod.EASYPAISA) Color(0xFF1B382B) else Color(0xFF12241C),
                border = BorderStroke(
                    if (selectedPayMethod == com.example.model.DirectPaymentMethod.EASYPAISA) 2.dp else 1.dp,
                    Color(0xFF00C853)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedPayMethod = com.example.model.DirectPaymentMethod.EASYPAISA }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RadioButton(
                                selected = selectedPayMethod == com.example.model.DirectPaymentMethod.EASYPAISA,
                                onClick = { selectedPayMethod = com.example.model.DirectPaymentMethod.EASYPAISA },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E676)),
                                modifier = Modifier.size(18.dp)
                            )
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF00E676))
                            Text("Easypaisa Direct Auto-Pay", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { copyToClipboard("Easypaisa Account", merchantAccounts.easypaisaNumber) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Account: ${merchantAccounts.easypaisaNumber}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB9F6CA)
                    )
                    Text(
                        text = "Title: ${merchantAccounts.easypaisaAccountTitle}  •  Selected: Rs. ${"%,d".format(selectedPkg.pricePkr)}",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Direct Bank Account Block (Synced with Owner / Supabase)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedPayMethod == com.example.model.DirectPaymentMethod.BANK_ACCOUNT) Color(0xFF263238) else Color(0xFF172026),
                border = BorderStroke(
                    if (selectedPayMethod == com.example.model.DirectPaymentMethod.BANK_ACCOUNT) 2.dp else 1.dp,
                    Color(0xFF4FC3F7)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { selectedPayMethod = com.example.model.DirectPaymentMethod.BANK_ACCOUNT }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RadioButton(
                                selected = selectedPayMethod == com.example.model.DirectPaymentMethod.BANK_ACCOUNT,
                                onClick = { selectedPayMethod = com.example.model.DirectPaymentMethod.BANK_ACCOUNT },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4FC3F7)),
                                modifier = Modifier.size(18.dp)
                            )
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF4FC3F7))
                            Text("${merchantAccounts.bankName} Direct Transfer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { copyToClipboard("${merchantAccounts.bankName} Account", merchantAccounts.bankAccountNumber) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Account: ${merchantAccounts.bankAccountNumber}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE1F5FE)
                    )
                    if (merchantAccounts.bankIban.isNotBlank()) {
                        Text(
                            text = "IBAN: ${merchantAccounts.bankIban}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF81D4FA)
                        )
                    }
                    Text(
                        text = "Title: ${merchantAccounts.bankAccountTitle}  •  Selected: Rs. ${"%,d".format(selectedPkg.pricePkr)}",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Direct Payment Auto-Verification & Activation Block
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0E131D),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. Direct Payment Auto-Activation (${selectedPkg.title} - Rs. ${"%,d".format(selectedPkg.pricePkr)})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color(0xFF34D399)
                    )

                    OutlinedTextField(
                        value = payerPhoneOrAccount,
                        onValueChange = { payerPhoneOrAccount = it },
                        label = { Text("Sender Mobile / Account Number *", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 03001234567") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BossGold,
                            unfocusedBorderColor = BossCarbonBorder,
                            focusedTextColor = BossText,
                            unfocusedTextColor = BossText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tid,
                        onValueChange = {
                            tid = it
                            securityManager.recordPaymentProof(it, null)
                        },
                        label = { Text("Transaction ID (TID / RAAST / Bank Ref) *", fontSize = 11.sp) },
                        placeholder = { Text("Enter 6 to 16 digit Transaction ID") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BossGold,
                            unfocusedBorderColor = BossCarbonBorder,
                            focusedTextColor = BossText,
                            unfocusedTextColor = BossText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val result = securityManager.verifyAndAutoActivatePackage(
                                pkg = selectedPkg,
                                paymentMethod = selectedPayMethod,
                                transactionId = tid,
                                payerAccountOrPhone = payerPhoneOrAccount
                            )
                            Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                            if (result.first) {
                                tid = ""
                                onUnlocked()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF04130D)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Verify & Auto-Activate ${selectedPkg.title} Now",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            border = BorderStroke(1.dp, BossGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = BossGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Live Camera", color = BossGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            border = BorderStroke(1.dp, BossGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BossGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gallery Shot", color = BossGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (paymentScreenshotBitmap != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = paymentScreenshotBitmap!!.asImageBitmap(),
                                contentDescription = "Receipt Preview",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, BossGold, RoundedCornerShape(6.dp))
                            )
                            Text("Payment proof attached", color = Color(0xFF4CAF50), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (paymentScreenshotUri != null) {
                        Text("Screenshot selected from gallery", color = Color(0xFF4CAF50), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (tid.isBlank()) {
                                Toast.makeText(context, "Please enter Transaction ID first", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            securityManager.sendPaymentProofViaWhatsApp(
                                profile = userProfile,
                                subState = subscriptionState,
                                tid = tid.trim(),
                                proofFileUri = paymentScreenshotUri
                            )
                        },
                        border = BorderStroke(1.dp, Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Also Share Receipt on WhatsApp",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            if (showEditMerchantDialog) {
                var epTitle by remember { mutableStateOf(merchantAccounts.easypaisaAccountTitle) }
                var epNum by remember { mutableStateOf(merchantAccounts.easypaisaNumber) }
                var bName by remember { mutableStateOf(merchantAccounts.bankName) }
                var bTitle by remember { mutableStateOf(merchantAccounts.bankAccountTitle) }
                var bNum by remember { mutableStateOf(merchantAccounts.bankAccountNumber) }
                var bIban by remember { mutableStateOf(merchantAccounts.bankIban) }

                AlertDialog(
                    onDismissRequest = { showEditMerchantDialog = false },
                    containerColor = Color(0xFF0E131D),
                    title = {
                        Text("Direct Bank & Easypaisa Setup", color = BossGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = epTitle, onValueChange = { epTitle = it }, label = { Text("Easypaisa Title") }, singleLine = true)
                            OutlinedTextField(value = epNum, onValueChange = { epNum = it }, label = { Text("Easypaisa Mobile Number") }, singleLine = true)
                            OutlinedTextField(value = bName, onValueChange = { bName = it }, label = { Text("Bank Name (e.g. BankIslami)") }, singleLine = true)
                            OutlinedTextField(value = bTitle, onValueChange = { bTitle = it }, label = { Text("Bank Account Title") }, singleLine = true)
                            OutlinedTextField(value = bNum, onValueChange = { bNum = it }, label = { Text("Bank Account Number") }, singleLine = true)
                            OutlinedTextField(value = bIban, onValueChange = { bIban = it }, label = { Text("IBAN (Optional)") }, singleLine = true)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val updated = merchantAccounts.copy(
                                    easypaisaAccountTitle = epTitle.trim().ifBlank { UserProfile.OWNER_NAME },
                                    easypaisaNumber = epNum.trim().ifBlank { UserProfile.EASYPAISA_ACCOUNT },
                                    bankName = bName.trim().ifBlank { "BankIslami" },
                                    bankAccountTitle = bTitle.trim().ifBlank { UserProfile.OWNER_NAME },
                                    bankAccountNumber = bNum.trim().ifBlank { UserProfile.BANKISLAMI_ACCOUNT },
                                    bankIban = bIban.trim()
                                )
                                merchantAccounts = updated
                                securityManager.saveMerchantPaymentAccounts(updated)
                                showEditMerchantDialog = false
                                Toast.makeText(context, "Direct Bank & Easypaisa accounts synced to Supabase!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color.Black)
                        ) {
                            Text("Save & Sync", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditMerchantDialog = false }) {
                            Text("Cancel", color = BossTextDim)
                        }
                    }
                )
            }

            // Passcode Activation Block
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0E131D),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Have an Activation Passcode / Unlock Code?",
                        color = BossGold,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = unlockPasscode,
                            onValueChange = { unlockPasscode = it },
                            label = { Text("Activation Passcode", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BossGold,
                                unfocusedBorderColor = BossCarbonBorder,
                                focusedTextColor = BossText,
                                unfocusedTextColor = BossText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (securityManager.unlockWithPasscode(unlockPasscode)) {
                                    Toast.makeText(context, "App Unlocked Successfully! Subscription active.", Toast.LENGTH_LONG).show()
                                    unlockPasscode = ""
                                    onUnlocked()
                                } else {
                                    Toast.makeText(context, "Invalid Passcode. Contact Syed Saboor Nawab.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BossGold,
                                contentColor = Color(0xFF161208)
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Unlock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Direct Support / Trial Extension Request Block
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Send Direct Request to Master Admin:",
                        fontSize = 12.5.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Trial Extension", "Payment Inquiry", "Section Request").forEach { type ->
                            FilterChip(
                                selected = selectedReqType == type,
                                onClick = { selectedReqType = type },
                                label = { Text(type, fontSize = 10.5.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = requestMsg,
                        onValueChange = { requestMsg = it },
                        label = { Text("Aap ka Message / Wajah", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 2 din ka trial extend kar dein") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BossGold,
                            unfocusedBorderColor = BossCarbonBorder,
                            focusedTextColor = BossText,
                            unfocusedTextColor = BossText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (requestMsg.isNotBlank()) {
                                val req = ClientSupportRequest(
                                    userName = userProfile.name,
                                    userPhone = userProfile.phone,
                                    userCnic = userProfile.cnic,
                                    userEmail = userProfile.email,
                                    requestType = selectedReqType,
                                    message = requestMsg.trim()
                                )
                                MasterAdminManager.getInstance(context).submitSupportRequest(req)
                                Toast.makeText(context, "Request Master Admin ko send ho gayi hai!", Toast.LENGTH_LONG).show()
                                requestMsg = ""
                            } else {
                                Toast.makeText(context, "Please write a message first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Submit Request to Master", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Text(
                text = "Direct Helpline: 0310-5524225 (Syed Saboor Nawab)",
                fontSize = 11.5.sp,
                color = BossGold,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BossDefaultRates
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.ClientTab
import com.example.ui.theme.*
import com.example.util.ChartPrintHelper
import com.example.util.QuotationPdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun BillingScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    val context = LocalContext.current
    val quote = remember(
        state.windows,
        state.doors,
        state.fixframes,
        state.openableWindows,
        state.slidingDoors,
        state.projectExtras,
        state.customCutPieces,
        state.ctxGauge,
        state.ctxGaugeMode,
        state.ctxColor,
        state.ctxFrameType,
        state.ctxGlassGauge,
        state.ctxGlassColor,
        state.ctxGlassMode,
        state.rates,
        state.glassRates,
        state.winFormulas,
        state.slidingDoorFormulas,
        state.doorFormulas,
        state.fixFormulas,
        state.opwinFormulas,
        state.glassFormulas,
        state.fSectionFormula,
        state.alumDiscountPercent,
        state.profitPercent,
        state.autoIncludeHardware,
        state.windowHardwareRate,
        state.doorHardwareRate,
        state.fixHardwareRate,
        state.slidingDoorHardwareRate,
        state.customBillItems,
        state.transportRent,
        state.labourCost,
        state.remoteLayoutConfig
    ) { viewModel.calculateQuotation() }

    var showCvsFDialog by remember { mutableStateOf(false) }
    var showAdjustBgDialog by remember { mutableStateOf(false) }
    var showExtraBillControls by remember { mutableStateOf(false) }

    var discountInput by remember(state.alumDiscountPercent) {
        mutableStateOf(if (state.alumDiscountPercent == 0.0) "0" else state.alumDiscountPercent.toString())
    }
    var customItemDesc by remember { mutableStateOf("") }
    var customItemAmount by remember { mutableStateOf("") }

    val currentDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date()) }
    val quoteRefCode = remember { "#BOSS-${(1000..9999).random()}" }

    val displayShortColor = remember(state.ctxColor) {
        when {
            state.ctxColor.contains("SAHARA", ignoreCase = true) -> "SAHARA"
            state.ctxColor.contains("DULL", ignoreCase = true) -> "DULL"
            state.ctxColor.contains("BLACK", ignoreCase = true) -> "BLACK"
            state.ctxColor.contains("CHAMP", ignoreCase = true) -> "CHAMPAGNE"
            state.ctxColor.contains("WHITE", ignoreCase = true) -> "WHITE"
            state.ctxColor.contains("WOOD", ignoreCase = true) -> "WOOD"
            else -> state.ctxColor
        }
    }
    val frameDisplay = if (state.ctxFrameType == "F") "DC30F/DC26F" else "DC30C/DC26C"

    // Totals calculation
    val alumSubtotal = quote.alumTotal
    val showDiscountRow = state.remoteLayoutConfig.showDiscountRow
    val discountAmt = if (showDiscountRow) alumSubtotal * (state.alumDiscountPercent / 100.0) else 0.0
    val netAlumTotal = (alumSubtotal - discountAmt).coerceAtLeast(0.0)

    val winCount = state.windows.sumOf { it.qty } + state.openableWindows.sumOf { it.qty }
    val doorCount = state.doors.sumOf { it.qty }
    val fixCount = state.fixframes.sumOf { it.qty }
    val sdCount = state.slidingDoors.sumOf { it.qty }
    val totalFrameItems = winCount + doorCount + fixCount + sdCount
    val defaultHwRatePerItem = state.slidingDoorHardwareRate.takeIf { it > 0 } ?: 3000.0

    val autoHardwareCost = if (state.remoteLayoutConfig.showHardwareRow && state.autoIncludeHardware) {
        (winCount * state.windowHardwareRate) +
            (doorCount * state.doorHardwareRate) +
            (fixCount * state.fixHardwareRate) +
            (sdCount * state.slidingDoorHardwareRate)
    } else 0.0

    val effectiveLabourCost = if (state.remoteLayoutConfig.showLabourRow) state.labourCost else 0.0
    val effectiveTransportRent = if (state.remoteLayoutConfig.showTransportRow) state.transportRent else 0.0
    val customItemsTotal = state.customBillItems.sumOf { it.amount }
    val baseSubTotal = netAlumTotal + quote.glassTotal + autoHardwareCost + customItemsTotal + effectiveLabourCost + effectiveTransportRent
    val profitAmt = baseSubTotal * (state.profitPercent / 100.0)
    val grandTotal = baseSubTotal + profitAmt

    val sheetBgBitmap = remember(state.customBackgroundPath) {
        ChartPrintHelper.getQuotationBackgroundBitmap(context, state.customBackgroundPath)?.asImageBitmap()
    }

    val customLogoBitmap = remember(state.customLogoPath) {
        if (!state.customLogoPath.isNullOrBlank()) {
            val f = File(state.customLogoPath)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
        } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06090F))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ============================================================
        // 1. CUSTOMER & JOB DETAILS CARD (Matches Screenshot 4 - 021022.png)
        // ============================================================
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0B0F17),
                border = BorderStroke(1.dp, Color(0xFF1E2636)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFF5B82E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Customer & Job Details",
                                color = Color(0xFFF5B82E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• $currentDate",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF151922),
                            border = BorderStroke(1.dp, Color(0xFFD9A441)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.setSettingsSubTab("LOGO_BG")
                                    viewModel.selectTab(ClientTab.RATES_AND_SETTINGS)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Header / Letterhead",
                                    tint = Color(0xFFF5B82E),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Edit Header / Letterhead",
                                    color = Color(0xFFF5B82E),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Customer Name, Phone Number & Aluminium Discount (%) side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Customer Name",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = state.customerName,
                                onValueChange = { viewModel.updateCustomerDetails(it, state.customerPhone) },
                                placeholder = {
                                    Text(
                                        text = "e.g. Tariq Mehmood",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0E131D),
                                    unfocusedContainerColor = Color(0xFF0E131D),
                                    focusedBorderColor = Color(0xFFF5B82E),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Phone Number",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = state.customerPhone,
                                onValueChange = { viewModel.updateCustomerDetails(state.customerName, it) },
                                placeholder = {
                                    Text(
                                        text = "0300-1234567",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0E131D),
                                    unfocusedContainerColor = Color(0xFF0E131D),
                                    focusedBorderColor = Color(0xFFF5B82E),
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (showDiscountRow) {
                            Column(
                                modifier = Modifier.weight(0.75f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Alum Disc (%)",
                                    color = Color(0xFFF5B82E),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = {
                                        discountInput = it
                                        viewModel.updateAlumDiscount(it.toDoubleOrNull() ?: 0.0)
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0%",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0E131D),
                                        unfocusedContainerColor = Color(0xFF0E131D),
                                        focusedBorderColor = Color(0xFFF5B82E),
                                        unfocusedBorderColor = Color(0xFF1E293B),
                                        focusedTextColor = Color(0xFFF5B82E),
                                        unfocusedTextColor = Color(0xFFF5B82E)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Summary Pill Row: Color • Gauge • Frame
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0E131D),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Color: $displayShortColor",
                                color = Color(0xFFF5B82E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "•", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(
                                text = "Gauge: ${state.ctxGauge}mm",
                                color = Color(0xFFF5B82E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "•", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(
                                text = "Frame: $frameDisplay",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // 2. FIVE ACTION BUTTONS ROW (Print, PDF, Save, C vs F, Share)
        // ============================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Print (Gold)
                Button(
                    onClick = { ChartPrintHelper.printQuotationBill(context, state, quote) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF5B82E),
                        contentColor = Color(0xFF161208)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.05f)
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Print", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 2. PDF (Sky Blue)
                Button(
                    onClick = { QuotationPdfGenerator.saveAndOpenPdf(context, quote, state) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF60A5FA),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.05f)
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Save (Dark with Gold Bookmark icon -> Saves to History)
                OutlinedButton(
                    onClick = {
                        viewModel.saveCurrentQuotation()
                        Toast.makeText(context, "Quotation saved to History!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF0B0F17),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(42.dp)
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = "Save",
                        tint = Color(0xFFF5B82E),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // 4. C vs F (Dark with Sky Blue border & text -> Opens Frame C vs F Comparison)
                if (state.remoteLayoutConfig.showCompareCFButton) {
                    OutlinedButton(
                        onClick = { showCvsFDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF0B0F17),
                            contentColor = Color(0xFF38BDF8)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.05f)
                            .height(42.dp)
                    ) {
                        Icon(
                            Icons.Default.CompareArrows,
                            contentDescription = "C vs F",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("C vs F", color = Color(0xFF38BDF8), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 5. Share (Square Icon Button)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0B0F17),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            QuotationPdfGenerator.sharePdf(context, quote, state)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Quotation",
                            tint = Color(0xFFF5B82E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ============================================================
        // 3. PRINTABLE QUOTATION SHEET HEADER BAR (Matches Screenshot 3 & 4)
        // ============================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PRINTABLE QUOTATION SHEET",
                        color = Color(0xFFD9A441),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (state.letterheadMode) Color(0xFF231B0A) else Color(0xFF0E131D),
                        border = BorderStroke(1.dp, Color(0xFFD9A441)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.setLetterheadMode(!state.letterheadMode) }
                    ) {
                        Text(
                            text = if (state.letterheadMode) "Letterhead Mode" else "Plain Mode",
                            color = Color(0xFFF5B82E),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0B0F17),
                    border = BorderStroke(1.dp, Color(0xFFD9A441)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAdjustBgDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Adjust Background",
                            tint = Color(0xFFF5B82E),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Adjust Background",
                            color = Color(0xFFF8FAFC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ============================================================
        // 4. CLEAN HIGH-CONTRAST PRINTABLE QUOTATION SHEET PREVIEW (With Header Logo & Compact Height)
        // ============================================================
        item {
            val isLightSheet = state.letterheadMode
            val sheetBaseBg = if (isLightSheet) Color(0xFFFDFBF6) else Color(0xFF0B0F17)
            val primaryInk = if (isLightSheet) Color(0xFF0F172A) else Color.White
            val secondaryInk = if (isLightSheet) Color(0xFF334155) else Color(0xFFCBD5E1)
            val accentGoldInk = if (isLightSheet) Color(0xFF92400E) else Color(0xFFF5B82E)
            val accentBlueInk = if (isLightSheet) Color(0xFF0369A1) else Color(0xFF38BDF8)
            val accentGreenInk = if (isLightSheet) Color(0xFF047857) else Color(0xFF34D399)
            val rowDividerColor = if (isLightSheet) Color(0xFFCBD5E1) else Color(0xFF1E293B)
            val shopDisplayName = remember(state.companyName, state.userProfile) {
                if (state.companyName.isNotBlank() && state.companyName != "YOUR SHOP / BUSINESS NAME") {
                    state.companyName
                } else if (state.userProfile.isVipOwner) {
                    "BOSS ALUMINIUM & GLASS"
                } else {
                    state.userProfile.shopAddress.ifBlank { state.userProfile.name.ifBlank { "BOSS ALUMINIUM & GLASS" } }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = sheetBaseBg,
                border = BorderStroke(1.5.dp, Color(0xFFC89D28)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Clean High-Contrast Background Letterhead
                    if (state.letterheadMode) {
                        if (!state.customBackgroundPath.isNullOrBlank() && sheetBgBitmap != null) {
                            Image(
                                bitmap = sheetBgBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(state.quotationBgOpacity)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.bg_boss_quotation_crisp),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(state.quotationBgOpacity)
                            )
                        }
                    }

                    // Subtle Center Watermark Logo (low alpha so text stays 100% readable)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val watermarkAlpha = if (isLightSheet) {
                            (state.quotationLogoWatermarkOpacity * 0.35f).coerceIn(0.05f, 0.16f)
                        } else {
                            state.quotationLogoWatermarkOpacity.coerceIn(0.08f, 0.35f)
                        }
                        Box(modifier = Modifier.alpha(watermarkAlpha)) {
                            if (customLogoBitmap != null) {
                                Image(
                                    bitmap = customLogoBitmap,
                                    contentDescription = "Watermark Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_boss_logo_clean),
                                    contentDescription = "Watermark Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }

                    // Foreground Quotation Content (Compact Height & Zero Wasted Vertical Gaps)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // QUOTATION HEADER WITH COMPANY LOGO + SHOP DETAILS + REF/DATE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Header Company Logo Emblem
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.5.dp, Color(0xFFD4AF37)),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    if (customLogoBitmap != null) {
                                        Image(
                                            bitmap = customLogoBitmap,
                                            contentDescription = "Company Header Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_boss_logo_clean),
                                            contentDescription = "Company Header Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                    Text(
                                        text = shopDisplayName,
                                        color = primaryInk,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 15.sp
                                    )
                                    Text(
                                        text = state.companyTagline.ifBlank { "FABRICATION & GLAZING ESTIMATE" },
                                        color = accentGoldInk,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        lineHeight = 12.sp
                                    )
                                    val contactLine = buildList {
                                        if (state.companyPhone.isNotBlank()) add("Ph: ${state.companyPhone}")
                                        if (state.companyAddress.isNotBlank()) add(state.companyAddress)
                                    }.joinToString(" • ")
                                    if (contactLine.isNotBlank()) {
                                        Text(
                                            text = contactLine,
                                            color = secondaryInk,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFFD4AF37))
                                ) {
                                    Text(
                                        text = "QUOTATION",
                                        color = Color(0xFFF5B82E),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    )
                                }
                                Text(
                                    text = "$currentDate  •  $quoteRefCode",
                                    color = secondaryInk,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFC89D28),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // Compact Single-Strip Customer & Active Spec Banner
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLightSheet) Color(0xFFF1ECE1) else Color(0xFF151C28),
                            border = BorderStroke(0.8.dp, Color(0xFFC89D28).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val custPhoneSuffix = if (state.customerPhone.isNotBlank()) " (${state.customerPhone})" else ""
                                    Text(
                                        text = "Client: ${state.customerName.ifBlank { "Valued Client" }}$custPhoneSuffix",
                                        color = primaryInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$displayShortColor • ${state.ctxGauge}mm • $frameDisplay",
                                    color = accentGoldInk,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Compact Table Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Section / Description",
                                color = Color(0xFFF5B82E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1.9f)
                            )
                            Text(
                                text = "Bars",
                                color = Color(0xFFF5B82E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = "Ft/Sqft",
                                color = Color(0xFFF5B82E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.65f)
                            )
                            Text(
                                text = "Rate",
                                color = Color(0xFFF5B82E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.7f)
                            )
                            Text(
                                text = "Amount",
                                color = Color(0xFFF5B82E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.05f)
                            )
                        }

                        // Compact Table Body (Tight row padding so height stays short)
                        if (quote.aluminiumRows.isEmpty() && quote.glassLines.isEmpty()) {
                            Text(
                                text = "No items added yet. Add windows or doors to generate quotation.",
                                color = secondaryInk,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                            )
                        } else {
                            quote.aluminiumRows.forEachIndexed { idx, row ->
                                val rowBg = if (isLightSheet && idx % 2 == 1) Color(0xFFF5EFE2).copy(alpha = 0.55f) else Color.Transparent
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1.9f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = row.section,
                                            color = primaryInk,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (row.tallySummary.isNotBlank()) {
                                            Text(
                                                text = "(${row.tallySummary})",
                                                color = secondaryInk,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${row.bars}",
                                        color = primaryInk,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.5f)
                                    )
                                    Text(
                                        text = "${row.totalFt}",
                                        color = accentBlueInk,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.65f)
                                    )
                                    Text(
                                        text = "${row.ratePerFt.toInt()}",
                                        color = secondaryInk,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    Text(
                                        text = "Rs ${"%,d".format(row.cost.roundToLong())}",
                                        color = primaryInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.05f)
                                    )
                                }
                                HorizontalDivider(color = rowDividerColor.copy(alpha = 0.55f), thickness = 0.5.dp)
                            }

                            if (quote.aluminiumRows.isNotEmpty()) {
                                if (showDiscountRow && state.alumDiscountPercent > 0.0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Aluminium Gross: Rs ${"%,d".format(alumSubtotal.roundToLong())}  •  Alum Disc (${state.alumDiscountPercent}%)",
                                            color = accentGreenInk,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "- Rs ${"%,d".format(discountAmt.roundToLong())}",
                                            color = accentGreenInk,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isLightSheet) Color(0xFFFEF3C7) else Color(0xFF281E0B).copy(alpha = 0.75f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showDiscountRow) "Net Aluminium Total" else "Total Aluminium Amount",
                                        color = accentGoldInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Rs ${"%,d".format(netAlumTotal.roundToLong())}",
                                        color = accentGoldInk,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            if (quote.glassLines.isNotEmpty()) {
                                quote.glassLines.forEach { gl ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 1.5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = gl.description,
                                            color = accentBlueInk,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1.9f)
                                        )
                                        Text(
                                            text = "-",
                                            color = secondaryInk,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.5f)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", gl.sqft),
                                            color = accentBlueInk,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.65f)
                                        )
                                        Text(
                                            text = "${gl.rate.toInt()}",
                                            color = secondaryInk,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.7f)
                                        )
                                        Text(
                                            text = "Rs ${"%,d".format(gl.total.roundToLong())}",
                                            color = primaryInk,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1.05f)
                                        )
                                    }
                                    HorizontalDivider(color = rowDividerColor.copy(alpha = 0.55f), thickness = 0.5.dp)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isLightSheet) Color(0xFFE0F2FE) else Color(0xFF0C2438).copy(alpha = 0.75f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Glass Amount",
                                        color = accentBlueInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Rs ${"%,d".format(quote.glassTotal.roundToLong())}",
                                        color = accentBlueInk,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        if (state.remoteLayoutConfig.showHardwareRow && autoHardwareCost > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hardware ($totalFrameItems Frames)",
                                    color = secondaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Rs ${"%,d".format(autoHardwareCost.roundToLong())}",
                                    color = primaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (state.remoteLayoutConfig.showLabourRow && effectiveLabourCost > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Labour Charges",
                                    color = secondaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Rs ${"%,d".format(effectiveLabourCost.roundToLong())}",
                                    color = primaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (state.remoteLayoutConfig.showTransportRow && effectiveTransportRent > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transport / Rent",
                                    color = secondaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Rs ${"%,d".format(effectiveTransportRent.roundToLong())}",
                                    color = primaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (profitAmt > 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Service / Markup (${state.profitPercent}%)",
                                    color = secondaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Rs ${"%,d".format(profitAmt.roundToLong())}",
                                    color = primaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        state.customBillItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.description,
                                    color = secondaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Rs ${"%,d".format(item.amount.roundToLong())}",
                                    color = primaryInk,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // NET GRAND TOTAL Banner (High Contrast Navy & Gold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.2.dp, Color(0xFFD4AF37)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "NET GRAND TOTAL",
                                        color = Color(0xFFF5B82E),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Total Payable Estimate",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 9.5.sp
                                    )
                                }

                                Text(
                                    text = "Rs ${"%,d".format(grandTotal.roundToLong())}",
                                    color = Color(0xFFF5B82E),
                                    fontSize = 18.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Compact Terms & Authorized Signature Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(
                                modifier = Modifier.weight(1.35f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = "Terms & Conditions:",
                                    color = accentGoldInk,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = state.quotationTerms.ifBlank { "1. Valid for 7 days. 2. 50% advance upon order confirmation." },
                                    color = secondaryInk,
                                    fontSize = 9.5.sp,
                                    lineHeight = 12.sp
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(start = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(105.dp)
                                        .height(1.dp)
                                        .background(primaryInk.copy(alpha = 0.6f))
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Authorized Signature",
                                    color = accentGoldInk,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // C vs F COMPARISON DIALOG
    // ============================================================
    if (showCvsFDialog) {
        val quoteC = remember(state) { viewModel.calculateQuotation(frameTypeOverride = "C") }
        val quoteF = remember(state) { viewModel.calculateQuotation(frameTypeOverride = "F") }
        val diff = quoteF.grandTotal - quoteC.grandTotal

        AlertDialog(
            onDismissRequest = { showCvsFDialog = false },
            containerColor = Color(0xFF0E131D),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color(0xFF38BDF8))
                    Text(
                        text = "Frame C vs Frame F Comparison",
                        color = Color(0xFFF5B82E),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Compare quotation costs between Standard Collar Frame (DC30C/DC26C) and Full Frame (DC30F/DC26F):",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Frame C Column
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF070A0F),
                            border = BorderStroke(
                                1.dp,
                                if (state.ctxFrameType == "C") Color(0xFFF5B82E) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Frame C (Collar)", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("DC30C / DC26C", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                HorizontalDivider(color = Color(0xFF1E293B))
                                Text("Alum: Rs ${"%,d".format(quoteC.alumTotal.roundToLong())}", color = Color.White, fontSize = 11.sp)
                                Text("Glass: Rs ${"%,d".format(quoteC.glassTotal.roundToLong())}", color = Color.White, fontSize = 11.sp)
                                Text(
                                    text = "Total: Rs ${"%,d".format(quoteC.grandTotal.roundToLong())}",
                                    color = Color(0xFFF5B82E),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        viewModel.setMasterFrameType("C")
                                        showCvsFDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.ctxFrameType == "C") Color(0xFFF5B82E) else Color(0xFF1E293B),
                                        contentColor = if (state.ctxFrameType == "C") Color(0xFF161208) else Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Text(if (state.ctxFrameType == "C") "Active" else "Use Frame C", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Frame F Column
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF070A0F),
                            border = BorderStroke(
                                1.dp,
                                if (state.ctxFrameType == "F") Color(0xFFF5B82E) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Frame F (Full)", color = Color(0xFFF5B82E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("DC30F / DC26F", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                HorizontalDivider(color = Color(0xFF1E293B))
                                Text("Alum: Rs ${"%,d".format(quoteF.alumTotal.roundToLong())}", color = Color.White, fontSize = 11.sp)
                                Text("Glass: Rs ${"%,d".format(quoteF.glassTotal.roundToLong())}", color = Color.White, fontSize = 11.sp)
                                Text(
                                    text = "Total: Rs ${"%,d".format(quoteF.grandTotal.roundToLong())}",
                                    color = Color(0xFFF5B82E),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        viewModel.setMasterFrameType("F")
                                        showCvsFDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (state.ctxFrameType == "F") Color(0xFFF5B82E) else Color(0xFF1E293B),
                                        contentColor = if (state.ctxFrameType == "F") Color(0xFF161208) else Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Text(if (state.ctxFrameType == "F") "Active" else "Use Frame F", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF070A0F),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Difference: Rs ${"%,d".format(abs(diff).roundToLong())} (${if (diff >= 0) "Frame F higher" else "Frame C higher"})",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCvsFDialog = false }) {
                    Text("Close", color = Color(0xFFF5B82E), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ============================================================
    // ADJUST BACKGROUND DIALOG
    // ============================================================
    if (showAdjustBgDialog) {
        var bgOpacity by remember(state.quotationBgOpacity) { mutableStateOf(state.quotationBgOpacity) }
        var wmOpacity by remember(state.quotationLogoWatermarkOpacity) { mutableStateOf(state.quotationLogoWatermarkOpacity) }

        AlertDialog(
            onDismissRequest = { showAdjustBgDialog = false },
            containerColor = Color(0xFF0E131D),
            title = {
                Text(
                    text = "Adjust Quotation Sheet Background",
                    color = Color(0xFFF5B82E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Letterhead Ornate Border", color = Color.White, fontSize = 12.5.sp)
                        Switch(
                            checked = state.letterheadMode,
                            onCheckedChange = { viewModel.setLetterheadMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF5B82E))
                        )
                    }

                    Column {
                        Text(
                            text = "Frame Brightness / Opacity: ${(bgOpacity * 100).toInt()}%",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp
                        )
                        Slider(
                            value = bgOpacity,
                            onValueChange = {
                                bgOpacity = it
                                viewModel.setQuotationBgSettings(bgOpacity, wmOpacity)
                            },
                            valueRange = 0.25f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF5B82E),
                                activeTrackColor = Color(0xFFF5B82E)
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "App Logo Watermark Opacity: ${(wmOpacity * 100).toInt()}%",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp
                        )
                        Slider(
                            value = wmOpacity,
                            onValueChange = {
                                wmOpacity = it
                                viewModel.setQuotationBgSettings(bgOpacity, wmOpacity)
                            },
                            valueRange = 0.05f..0.75f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF5B82E),
                                activeTrackColor = Color(0xFFF5B82E)
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showAdjustBgDialog = false
                            viewModel.setSettingsSubTab("LOGO_BG")
                            viewModel.selectTab(ClientTab.RATES_AND_SETTINGS)
                        },
                        border = BorderStroke(1.dp, Color(0xFFD9A441)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFF5B82E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Logo & Background Settings", color = Color(0xFFF5B82E), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAdjustBgDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF5B82E),
                        contentColor = Color(0xFF161208)
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

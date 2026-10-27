@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BossCalculationEngine
import com.example.model.BossDefaultRates
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.components.MasterAiRateScannerDialog
import com.example.ui.theme.*
import com.example.util.RateListDetector
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@Composable
fun RatesScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeRateTab by remember { mutableStateOf(0) } // 0 = Aluminium Rates, 1 = Glass Rates
    var rateViewMode by remember { mutableStateOf(0) } // 0 = 3-Column Grid Cards, 1 = 3-Column Table (Section | Bar Rate | Ft Rate)
    var searchSection by remember { mutableStateOf("") }
    var isScanningRateList by remember { mutableStateOf(false) }
    var showMasterAiScanner by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showAddGlassColorDialog by remember { mutableStateOf(false) }
    var preselectedGlassGaugeForAdd by remember { mutableStateOf("5") }
    var showAddGlassGaugeDialog by remember { mutableStateOf(false) }
    var showAddGaugeDialog by remember { mutableStateOf(false) }
    var showAddColorDialog by remember { mutableStateOf(false) }
    var isMultiDeleteSectionsMode by remember { mutableStateOf(false) }
    var selectedSectionsForDelete by remember { mutableStateOf(setOf<String>()) }
    var isMultiDeleteGlassMode by remember { mutableStateOf(false) }
    var selectedGlassForDelete by remember { mutableStateOf(setOf<String>()) } // format: "gauge|color"

    val ratePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isScanningRateList = true
                val result = RateListDetector.detectRatesFromFile(context, uri)
                isScanningRateList = false
                if (result.items.isNotEmpty()) {
                    viewModel.applyDetectedRateItems(result.items)
                    Toast.makeText(
                        context,
                        "Applied ${result.items.size} section rates via ${result.engine}!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        result.error ?: "No rates detected from the selected image.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val rateFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isScanningRateList = true
                val result = RateListDetector.detectRatesFromFile(context, uri)
                isScanningRateList = false
                if (result.items.isNotEmpty()) {
                    viewModel.applyDetectedRateItems(result.items)
                    Toast.makeText(
                        context,
                        "Imported ${result.items.size} section rates from file!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        result.error ?: "Could not parse rates from file.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val currentColorRates = remember(state.rates, state.ctxGauge, state.ctxColor) {
        val gVariants = BossDefaultRates.normalizeGaugeVariants(state.ctxGauge)
        val cVariants = BossDefaultRates.normalizeColorVariants(state.ctxColor)
        var found: Map<String, Double>? = null
        for (g in gVariants) {
            val colMap = state.rates[g] ?: continue
            for (c in cVariants) {
                val secMap = colMap[c]
                if (!secMap.isNullOrEmpty()) {
                    found = secMap
                    break
                }
            }
            if (found != null) break
        }
        found ?: state.rates[state.ctxGauge]?.get(state.ctxColor)
            ?: state.rates[state.ctxGauge]?.values?.firstOrNull()
            ?: state.rates.values.firstOrNull()?.values?.firstOrNull()
            ?: emptyMap()
    }

    // Order sections in the natural order defined in BossDefaultRates.DEFAULT_SECTIONS + customSections
    val orderedSectionEntries = remember(currentColorRates, searchSection, state.hiddenSections, state.customSections, state.deletedSections) {
        val upperDeleted = state.deletedSections.map { it.trim().uppercase() }.toSet()
        val preferredOrder = (BossDefaultRates.DEFAULT_SECTIONS + state.customSections).distinct()
        val entries = currentColorRates.entries
            .filter {
                !state.hiddenSections.contains(it.key) &&
                    !state.deletedSections.contains(it.key) &&
                    !upperDeleted.contains(it.key.trim().uppercase())
            }
            .sortedBy { entry ->
                val idx = preferredOrder.indexOf(entry.key)
                if (idx >= 0) idx else 9999
            }
        if (searchSection.isBlank()) entries
        else entries.filter {
            val label = BossDefaultRates.SECTION_LABELS[it.key] ?: ""
            it.key.contains(searchSection.trim(), ignoreCase = true) ||
                label.contains(searchSection.trim(), ignoreCase = true)
        }
    }

    val sectionRowsOfThree = remember(orderedSectionEntries) {
        orderedSectionEntries.chunked(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BossCarbonBg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                border = BorderStroke(1.dp, BossCarbonBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Rates Setup (${state.ctxGauge}mm - ${state.ctxColor})",
                                color = BossGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "3-Column Rate List • Auto-adjusts for gauges & colors",
                                color = BossTextDim,
                                fontSize = 11.sp
                            )
                        }

                        // Layout Mode Switcher: 3-Col Grid vs 3-Col Table
                        Row(
                            modifier = Modifier
                                .background(BossCarbonPanel2, RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (rateViewMode == 0) BossGold else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { rateViewMode = 0 }
                            ) {
                                Text(
                                    text = "3-Col Grid",
                                    color = if (rateViewMode == 0) Color(0xFF161208) else BossTextDim,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (rateViewMode == 1) BossGold else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { rateViewMode = 1 }
                            ) {
                                Text(
                                    text = "3-Col Table",
                                    color = if (rateViewMode == 1) Color(0xFF161208) else BossTextDim,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Master Admin Industrial AI Rate Scanner Button
                    Button(
                        onClick = { showMasterAiScanner = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BossGold,
                            contentColor = Color(0xFF161208)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚡ Industrial AI Rate Scanner (3, 4+ Photos • 5-Col Table)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ratePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isScanningRateList,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = BossCarbonPanel2,
                                contentColor = BossGoldLight
                            ),
                            border = BorderStroke(1.dp, BossGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            if (isScanningRateList) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = BossGold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = BossGoldLight, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Quick 1-Photo", color = BossGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { rateFilePicker.launch("*/*") },
                            enabled = !isScanningRateList,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = BossCarbonPanel2,
                                contentColor = BossGoldLight
                            ),
                            border = BorderStroke(1.dp, BossCarbonBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import File", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.saveAllCurrentRatesAndSettingsAsDefault()
                                Toast.makeText(context, "All Rates, Gauges & Colors saved as Default!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF04130D)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Default", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    TabRow(
                        selectedTabIndex = activeRateTab,
                        containerColor = BossCarbonPanel2,
                        contentColor = BossGold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = activeRateTab == 0,
                            onClick = { activeRateTab = 0 },
                            text = {
                                Text(
                                    text = "Aluminium Rates (3 Cols)",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeRateTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeRateTab == 0) BossGold else BossTextDim
                                )
                            }
                        )
                        Tab(
                            selected = activeRateTab == 1,
                            onClick = { activeRateTab = 1 },
                            text = {
                                Text(
                                    text = "Glass Rates (3 Cols)",
                                    fontSize = 12.sp,
                                    fontWeight = if (activeRateTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeRateTab == 1) BossGold else BossTextDim
                                )
                            }
                        )
                    }
                }
            }
        }

        if (activeRateTab == 0) {
            // Search Section + "+ Section" + "Multi-Del" button row + Gauge & Color Add/Delete
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Gauge & Color Add / Delete Management Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                        border = BorderStroke(1.dp, BossCarbonBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Aluminium Gauges (Select / Add / Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aluminium Gauges (Tap to Select / × to Delete):",
                                    color = BossGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BossGold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showAddGaugeDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Gauge", tint = Color(0xFF161208), modifier = Modifier.size(13.dp))
                                        Text("+ Add Gauge", color = Color(0xFF161208), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                state.availableGauges.forEach { g ->
                                    val isSel = state.ctxGauge == g
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isSel) BossGold else BossCarbonPanel2,
                                        border = BorderStroke(1.dp, if (isSel) BossGold else BossCarbonBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .clickable { viewModel.setMasterGauge(g) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = if (g.endsWith("mm", ignoreCase = true)) g else "${g}mm",
                                                color = if (isSel) Color(0xFF161208) else BossText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (state.availableGauges.size > 1) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Gauge $g",
                                                    tint = if (isSel) Color(0xFF161208) else BossRed,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            viewModel.removeCustomGauge(g)
                                                            Toast.makeText(context, "Gauge ${g}mm removed", Toast.LENGTH_SHORT).show()
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = BossCarbonBorder)

                            // 2. Aluminium Colors (Select / Add / Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aluminium Colors (Tap to Select / × to Delete):",
                                    color = BossGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BossGold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showAddColorDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Color", tint = Color(0xFF161208), modifier = Modifier.size(13.dp))
                                        Text("+ Add Color", color = Color(0xFF161208), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                state.availableColors.forEach { col ->
                                    val isSel = state.ctxColor.equals(col, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isSel) BossGold else BossCarbonPanel2,
                                        border = BorderStroke(1.dp, if (isSel) BossGold else BossCarbonBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .clickable { viewModel.setMasterColor(col) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = col,
                                                color = if (isSel) Color(0xFF161208) else BossText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (state.availableColors.size > 1) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Color $col",
                                                    tint = if (isSel) Color(0xFF161208) else BossRed,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            viewModel.removeCustomColor(col)
                                                            Toast.makeText(context, "Color $col removed", Toast.LENGTH_SHORT).show()
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchSection,
                            onValueChange = { searchSection = it },
                            label = { Text("Search Section (e.g. D54A, M23)", fontSize = 11.sp) },
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
                            onClick = { showAddSectionDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BossGold,
                                contentColor = Color(0xFF161208)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Section", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                isMultiDeleteSectionsMode = !isMultiDeleteSectionsMode
                                if (!isMultiDeleteSectionsMode) selectedSectionsForDelete = emptySet()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isMultiDeleteSectionsMode) BossRed.copy(alpha = 0.2f) else BossCarbonPanel2,
                                contentColor = if (isMultiDeleteSectionsMode) BossRed else BossText
                            ),
                            border = BorderStroke(1.dp, if (isMultiDeleteSectionsMode) BossRed else BossCarbonBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Multi-Delete Sections", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Multi-Del", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isMultiDeleteSectionsMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A1215),
                            border = BorderStroke(1.dp, BossRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedSectionsForDelete.size} selected",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            val allVisible = orderedSectionEntries.map { it.key }.toSet()
                                            selectedSectionsForDelete = if (selectedSectionsForDelete.size == allVisible.size) emptySet() else allVisible
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (selectedSectionsForDelete.size == orderedSectionEntries.size) "Unselect All" else "Select All",
                                            color = BossGoldLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (selectedSectionsForDelete.isNotEmpty()) {
                                                viewModel.removeMultipleSections(selectedSectionsForDelete)
                                                Toast.makeText(
                                                    context,
                                                    "Removed ${selectedSectionsForDelete.size} section(s) from Rate List",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                selectedSectionsForDelete = emptySet()
                                                isMultiDeleteSectionsMode = false
                                            }
                                        },
                                        enabled = selectedSectionsForDelete.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BossRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete (${selectedSectionsForDelete.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Restore Bar for Deleted Default Sections
            if (state.deletedSections.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BossCarbonPanel,
                        border = BorderStroke(1.dp, BossRed.copy(alpha = 0.6f)),
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
                                    text = "Removed Sections (Tap to Restore):",
                                    color = BossGoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Restore All (${state.deletedSections.size})",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { viewModel.restoreAllDeletedSections() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.deletedSections.toList().sorted().forEach { delSec ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BossCarbonPanel2,
                                        border = BorderStroke(1.dp, BossRed.copy(alpha = 0.7f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.restoreDeletedSection(delSec) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = "Restore $delSec",
                                                tint = BossGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = delSec,
                                                color = BossText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // One-by-One Unhide Bar for Hidden Sections
            if (state.hiddenSections.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BossCarbonPanel,
                        border = BorderStroke(1.dp, BossGold.copy(alpha = 0.5f)),
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
                                    text = "Hidden Sections (Tap to Unhide One-by-One):",
                                    color = BossGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Unhide All (${state.hiddenSections.size})",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { viewModel.unhideAllSections() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.hiddenSections.toList().sorted().forEach { hiddenSec ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BossCarbonPanel2,
                                        border = BorderStroke(1.dp, BossGold),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.unhideSection(hiddenSec) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Unhide $hiddenSec",
                                                tint = BossGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = hiddenSec,
                                                color = BossText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (rateViewMode == 0) {
                // ============================================================
                // MODE 0 (DEFAULT): 3-COLUMN GRID OF SECTION RATE CARDS
                // ============================================================
                items(sectionRowsOfThree) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { (secCode, currentRate) ->
                            var rateText by remember(secCode, currentRate, state.ctxGauge, state.ctxColor) {
                                mutableStateOf(
                                    if (currentRate % 1.0 == 0.0) "${currentRate.toInt()}" else currentRate.toString()
                                )
                            }
                            val secTitle = BossDefaultRates.SECTION_LABELS[secCode] ?: secCode
                            val isSelectedForDel = selectedSectionsForDelete.contains(secCode)
                            val maxBarFt = BossCalculationEngine.barLengthsFor(secCode).maxOrNull() ?: 19
                            val liveRateVal = rateText.toDoubleOrNull() ?: currentRate
                            val oneBarPrice = (liveRateVal * maxBarFt).roundToLong()

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isMultiDeleteSectionsMode) {
                                            Modifier.clickable {
                                                selectedSectionsForDelete = if (isSelectedForDel) {
                                                    selectedSectionsForDelete - secCode
                                                } else {
                                                    selectedSectionsForDelete + secCode
                                                }
                                            }
                                        } else Modifier
                                    ),
                                shape = RoundedCornerShape(9.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelectedForDel) Color(0xFF3A161B) else BossCarbonPanel
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelectedForDel) BossRed else BossCarbonBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (isMultiDeleteSectionsMode) {
                                                Checkbox(
                                                    checked = isSelectedForDel,
                                                    onCheckedChange = { checked ->
                                                        selectedSectionsForDelete = if (checked) {
                                                            selectedSectionsForDelete + secCode
                                                        } else {
                                                            selectedSectionsForDelete - secCode
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = BossRed),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = secCode,
                                                color = BossGold,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(5.dp),
                                                color = BossRed.copy(alpha = 0.16f),
                                                border = BorderStroke(0.8.dp, BossRed.copy(alpha = 0.7f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .clickable {
                                                        viewModel.removeSection(secCode)
                                                        Toast.makeText(context, "Removed $secCode from Rate List", Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete $secCode",
                                                    tint = BossRed,
                                                    modifier = Modifier.padding(4.dp).size(14.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(5.dp),
                                                color = BossCarbonPanel2,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .clickable { viewModel.hideSection(secCode) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VisibilityOff,
                                                    contentDescription = "Hide $secCode",
                                                    tint = BossTextDim,
                                                    modifier = Modifier.padding(4.dp).size(13.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = secTitle,
                                        color = BossTextDim,
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "${maxBarFt}ft: Rs ${"%,d".format(oneBarPrice)}",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BossCarbonPanel2,
                                        border = BorderStroke(1.dp, BossGold.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Rs/ft:",
                                                color = BossTextDim,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            BasicTextField(
                                                value = rateText,
                                                onValueChange = {
                                                    rateText = it
                                                    val parsed = it.toDoubleOrNull()
                                                    if (parsed != null && parsed >= 0.0) {
                                                        viewModel.updateSectionRate(state.ctxGauge, state.ctxColor, secCode, parsed)
                                                    }
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                cursorBrush = SolidColor(BossGold),
                                                textStyle = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.End
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Fill remaining columns in the 3-column row if fewer than 3 items
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // ============================================================
                // MODE 1: 3-COLUMN RATE TABLE (Col 1: Section | Col 2: Full Bar Rate | Col 3: Per-Ft Rate)
                // ============================================================
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF281E0B),
                        border = BorderStroke(1.dp, BossGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. SECTION NAME",
                                color = BossGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                text = "2. FULL BAR (RS)",
                                color = BossGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "3. PER FOOT (RS/FT)",
                                color = BossGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                items(orderedSectionEntries, key = { it.key }) { (secCode, currentRate) ->
                    val maxBarFt = BossCalculationEngine.barLengthsFor(secCode).maxOrNull() ?: 19
                    var rateText by remember(secCode, currentRate, state.ctxGauge, state.ctxColor) {
                        mutableStateOf(
                            if (currentRate % 1.0 == 0.0) "${currentRate.toInt()}" else currentRate.toString()
                        )
                    }
                    var barPriceText by remember(secCode, currentRate, state.ctxGauge, state.ctxColor) {
                        mutableStateOf(((currentRate * maxBarFt).roundToLong()).toString())
                    }
                    val secTitle = BossDefaultRates.SECTION_LABELS[secCode] ?: secCode
                    val isSelectedForDel = selectedSectionsForDelete.contains(secCode)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isMultiDeleteSectionsMode) {
                                    Modifier.clickable {
                                        selectedSectionsForDelete = if (isSelectedForDel) {
                                            selectedSectionsForDelete - secCode
                                        } else {
                                            selectedSectionsForDelete + secCode
                                        }
                                    }
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelectedForDel) Color(0xFF3A161B) else BossCarbonPanel
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelectedForDel) BossRed else BossCarbonBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Column 1: Section Code & Title
                            Column(modifier = Modifier.weight(1.3f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isMultiDeleteSectionsMode) {
                                        Checkbox(
                                            checked = isSelectedForDel,
                                            onCheckedChange = { checked ->
                                                selectedSectionsForDelete = if (checked) {
                                                    selectedSectionsForDelete + secCode
                                                } else {
                                                    selectedSectionsForDelete - secCode
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = BossRed),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = secCode,
                                        color = BossGold,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "(${maxBarFt}ft)",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = secTitle,
                                    color = BossTextDim,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Column 2: Full Bar Price (Editable)
                            OutlinedTextField(
                                value = barPriceText,
                                onValueChange = {
                                    barPriceText = it
                                    val barVal = it.toDoubleOrNull()
                                    if (barVal != null && barVal >= 0.0 && maxBarFt > 0) {
                                        val perFt = kotlin.math.round((barVal / maxBarFt) * 100.0) / 100.0
                                        rateText = if (perFt % 1.0 == 0.0) "${perFt.toInt()}" else perFt.toString()
                                        viewModel.updateSectionRate(state.ctxGauge, state.ctxColor, secCode, perFt)
                                    }
                                },
                                label = { Text("${maxBarFt}ft Bar Rs", fontSize = 9.5.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = BossCarbonBorder,
                                    focusedTextColor = Color(0xFF38BDF8),
                                    unfocusedTextColor = Color(0xFF38BDF8)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Column 3: Rate per Foot + Delete & Hide icons
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = rateText,
                                    onValueChange = {
                                        rateText = it
                                        val parsed = it.toDoubleOrNull()
                                        if (parsed != null && parsed >= 0.0) {
                                            barPriceText = ((parsed * maxBarFt).roundToLong()).toString()
                                            viewModel.updateSectionRate(state.ctxGauge, state.ctxColor, secCode, parsed)
                                        }
                                    },
                                    label = { Text("Rs / ft", fontSize = 9.5.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BossGold,
                                        unfocusedBorderColor = BossCarbonBorder,
                                        focusedTextColor = BossText,
                                        unfocusedTextColor = BossText
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        viewModel.removeSection(secCode)
                                        Toast.makeText(context, "Removed $secCode from Rate List", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete $secCode",
                                        tint = BossRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.hideSection(secCode) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = "Hide $secCode",
                                        tint = BossTextDim,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Glass Rates Tab in 3 Columns
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                    border = BorderStroke(1.dp, BossCarbonBorder)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Glass Gauges & Colors (Add / Delete)",
                                    color = BossGold,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Add or delete glass gauges (mm) and glass colors per sq.ft.",
                                    color = BossTextDim,
                                    fontSize = 11.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { showAddGlassGaugeDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BossSteel,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Glass Gauge", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        preselectedGlassGaugeForAdd = state.ctxGlassGauge.ifBlank { "5" }
                                        showAddGlassColorDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BossGold,
                                        contentColor = Color(0xFF161208)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Glass Color", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        isMultiDeleteGlassMode = !isMultiDeleteGlassMode
                                        if (!isMultiDeleteGlassMode) selectedGlassForDelete = emptySet()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isMultiDeleteGlassMode) BossRed.copy(alpha = 0.2f) else BossCarbonPanel2,
                                        contentColor = if (isMultiDeleteGlassMode) BossRed else BossText
                                    ),
                                    border = BorderStroke(1.dp, if (isMultiDeleteGlassMode) BossRed else BossCarbonBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Multi-Delete Glass", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Multi-Del", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Active Glass Gauges Row with Delete (x) Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Glass Gauges:",
                                color = BossTextDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val glassGaugesList = state.availableGlassGauges.ifEmpty { listOf("5", "8") }
                            glassGaugesList.forEach { g ->
                                val isSelected = state.ctxGlassGauge == g
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (isSelected) BossGold.copy(alpha = 0.22f) else BossCarbonPanel2,
                                    border = BorderStroke(1.dp, if (isSelected) BossGold else BossCarbonBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .clickable {
                                            val firstCol = state.getGlassColorsForGauge(g).firstOrNull() ?: "CLEAR"
                                            viewModel.setMasterGlass(g, firstCol, "ON")
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = "${g}mm Glass",
                                            color = if (isSelected) BossGold else BossText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (glassGaugesList.size > 1) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete ${g}mm Glass Gauge",
                                                tint = BossRed,
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clickable {
                                                        viewModel.removeGlassGauge(g)
                                                        Toast.makeText(context, "Removed ${g}mm Glass Gauge", Toast.LENGTH_SHORT).show()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.restoreDefaultGlassColors()
                                    Toast.makeText(context, "Restored default glass gauges & colors", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF0F291E),
                                    contentColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(7.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Defaults", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isMultiDeleteGlassMode) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2A1215),
                                border = BorderStroke(1.dp, BossRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedGlassForDelete.size} glass color(s) selected",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = {
                                            if (selectedGlassForDelete.isNotEmpty()) {
                                                viewModel.removeMultipleGlassColors(selectedGlassForDelete)
                                            }
                                            Toast.makeText(
                                                context,
                                                "Removed ${selectedGlassForDelete.size} glass color(s)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            selectedGlassForDelete = emptySet()
                                            isMultiDeleteGlassMode = false
                                        },
                                        enabled = selectedGlassForDelete.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BossRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete (${selectedGlassForDelete.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val glassGaugesToRender = state.availableGlassGauges.ifEmpty { listOf("5", "8") }
            glassGaugesToRender.forEach { gGauge ->
                item {
                    val colorsForGauge = state.getGlassColorsForGauge(gGauge)
                    val ratesMap = state.glassRates[gGauge] ?: emptyMap()
                    val glassRowsOfThree = colorsForGauge.chunked(3)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                        border = BorderStroke(1.dp, BossCarbonBorder)
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
                                Text(
                                    text = "${gGauge}mm Glass Rates (Rs. / Sq.Ft)",
                                    color = BossGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            preselectedGlassGaugeForAdd = gGauge
                                            showAddGlassColorDialog = true
                                        },
                                        border = BorderStroke(1.dp, BossGold),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = BossGold, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("+ ${gGauge}mm Color", color = BossGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (glassGaugesToRender.size > 1) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.removeGlassGauge(gGauge)
                                                Toast.makeText(context, "Deleted ${gGauge}mm Glass Gauge", Toast.LENGTH_SHORT).show()
                                            },
                                            border = BorderStroke(1.dp, BossRed),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = BossRed, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Del ${gGauge}mm", color = BossRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = BossCarbonBorder)

                            glassRowsOfThree.forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowColors.forEach { gc ->
                                        val curRate = ratesMap[gc] ?: if (gGauge == "8") 390.0 else 230.0
                                        val glassKey = "$gGauge|$gc"
                                        val isGlassSelectedForDel = selectedGlassForDelete.contains(glassKey)
                                        var gRateText by remember(gGauge, gc, curRate) {
                                            mutableStateOf(
                                                if (curRate % 1.0 == 0.0) "${curRate.toInt()}" else curRate.toString()
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isGlassSelectedForDel) Color(0xFF3A161B) else BossCarbonPanel2,
                                            border = BorderStroke(1.dp, if (isGlassSelectedForDel) BossRed else BossCarbonBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .then(
                                                    if (isMultiDeleteGlassMode) {
                                                        Modifier.clickable {
                                                            selectedGlassForDelete = if (isGlassSelectedForDel) {
                                                                selectedGlassForDelete - glassKey
                                                            } else {
                                                                selectedGlassForDelete + glassKey
                                                            }
                                                        }
                                                    } else Modifier
                                                )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        if (isMultiDeleteGlassMode) {
                                                            Checkbox(
                                                                checked = isGlassSelectedForDel,
                                                                onCheckedChange = { checked ->
                                                                    selectedGlassForDelete = if (checked) {
                                                                        selectedGlassForDelete + glassKey
                                                                    } else {
                                                                        selectedGlassForDelete - glassKey
                                                                    }
                                                                },
                                                                colors = CheckboxDefaults.colors(checkedColor = BossRed),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = "${gGauge}mm $gc",
                                                            color = BossGold,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = BossRed.copy(alpha = 0.16f),
                                                        border = BorderStroke(0.8.dp, BossRed.copy(alpha = 0.7f)),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .clickable {
                                                                viewModel.removeGlassColor(gc, gGauge)
                                                                Toast.makeText(context, "Removed ${gGauge}mm $gc from Rate List", Toast.LENGTH_SHORT).show()
                                                            }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete $gc",
                                                            tint = BossRed,
                                                            modifier = Modifier.padding(4.dp).size(14.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Per Sq.Ft",
                                                    color = BossTextDim,
                                                    fontSize = 9.5.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BossCarbonPanel,
                                                    border = BorderStroke(1.dp, BossGold.copy(alpha = 0.6f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("Rs:", color = BossTextDim, fontSize = 9.5.sp)
                                                        BasicTextField(
                                                            value = gRateText,
                                                            onValueChange = {
                                                                gRateText = it
                                                                val parsed = it.toDoubleOrNull()
                                                                if (parsed != null && parsed >= 0.0) {
                                                                    viewModel.updateGlassRate(gGauge, gc, parsed)
                                                                }
                                                            },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            cursorBrush = SolidColor(BossGold),
                                                            textStyle = TextStyle(
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.End
                                                            ),
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    repeat(3 - rowColors.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSectionDialog) {
        AddCustomSectionDialog(
            onDismiss = { showAddSectionDialog = false },
            isPairModeDefault = true,
            onAdd = { secName, rate ->
                viewModel.addCustomSection(secName, rate)
                showAddSectionDialog = false
            },
            onAddPair = { s1, r1, s2, r2 ->
                viewModel.addCustomSectionPair(s1, r1, s2, r2)
                showAddSectionDialog = false
            }
        )
    }

    if (showAddGlassColorDialog) {
        val activeGlassGauges = state.availableGlassGauges.ifEmpty { listOf("5", "8") }
        var selectedGlassGauge by remember(preselectedGlassGaugeForAdd) {
            mutableStateOf(
                if (activeGlassGauges.contains(preselectedGlassGaugeForAdd)) preselectedGlassGaugeForAdd
                else activeGlassGauges.first()
            )
        }
        var newGlassColorName by remember { mutableStateOf("") }
        var newGlassRateStr by remember(selectedGlassGauge) {
            mutableStateOf(if (selectedGlassGauge == "8") "410" else "250")
        }

        AlertDialog(
            onDismissRequest = { showAddGlassColorDialog = false },
            containerColor = BossCarbonPanel,
            title = {
                Text(
                    text = "Add Custom Glass Color & Rate",
                    color = BossGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Glass Gauge:", color = BossTextDim, fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeGlassGauges.forEach { gCode ->
                            val gLabel = "${gCode}mm Glass"
                            val isSel = selectedGlassGauge == gCode
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedGlassGauge = gCode
                                    newGlassRateStr = if (gCode == "8") "410" else "250"
                                },
                                label = {
                                    Text(
                                        text = gLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BossGold,
                                    selectedLabelColor = Color(0xFF161208),
                                    containerColor = BossCarbonPanel2,
                                    labelColor = BossText
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newGlassColorName,
                        onValueChange = { newGlassColorName = it },
                        label = { Text("Glass Color / Type (e.g. Frosted, Fluted)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newGlassRateStr,
                        onValueChange = { newGlassRateStr = it },
                        label = { Text("${selectedGlassGauge}mm Rate (Rs. / Sq.Ft)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newGlassColorName.trim().uppercase()
                        if (clean.isNotEmpty()) {
                            val rate = newGlassRateStr.toDoubleOrNull() ?: 250.0
                            viewModel.addCustomGlassColor(clean, selectedGlassGauge, rate)
                            showAddGlassColorDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208))
                ) {
                    Text("Add Glass", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGlassColorDialog = false }) {
                    Text("Cancel", color = BossTextDim)
                }
            }
        )
    }

    if (showAddGlassGaugeDialog) {
        var newGlassGaugeText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGlassGaugeDialog = false },
            containerColor = BossCarbonPanel,
            title = {
                Text(
                    text = "Add Glass Gauge (mm)",
                    color = BossGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter glass thickness in mm (e.g. 6, 10, 12):",
                        color = BossTextDim,
                        fontSize = 11.5.sp
                    )
                    OutlinedTextField(
                        value = newGlassGaugeText,
                        onValueChange = { newGlassGaugeText = it },
                        label = { Text("Glass Gauge (e.g. 10, 12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newGlassGaugeText.trim().removeSuffix("mm").removeSuffix("MM").trim()
                        if (clean.isNotEmpty()) {
                            viewModel.addCustomGlassGauge(clean)
                            Toast.makeText(context, "Added ${clean}mm Glass Gauge & saved to defaults!", Toast.LENGTH_SHORT).show()
                            showAddGlassGaugeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208))
                ) {
                    Text("Add Glass Gauge", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGlassGaugeDialog = false }) {
                    Text("Cancel", color = BossTextDim)
                }
            }
        )
    }

    if (showAddGaugeDialog) {
        var newGaugeText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGaugeDialog = false },
            containerColor = BossCarbonPanel,
            title = {
                Text(
                    text = "Add Aluminium Gauge",
                    color = BossGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter gauge thickness in mm (e.g. 1.5, 1.8, 2.2):",
                        color = BossTextDim,
                        fontSize = 11.5.sp
                    )
                    OutlinedTextField(
                        value = newGaugeText,
                        onValueChange = { newGaugeText = it },
                        label = { Text("Gauge (e.g. 1.8)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newGaugeText.trim().removeSuffix("mm").removeSuffix("MM").trim()
                        if (clean.isNotEmpty()) {
                            viewModel.addCustomGauge(clean)
                            Toast.makeText(context, "Added Gauge ${clean}mm & saved to defaults!", Toast.LENGTH_SHORT).show()
                            showAddGaugeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208))
                ) {
                    Text("Add Gauge", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGaugeDialog = false }) {
                    Text("Cancel", color = BossTextDim)
                }
            }
        )
    }

    if (showAddColorDialog) {
        var newColorText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddColorDialog = false },
            containerColor = BossCarbonPanel,
            title = {
                Text(
                    text = "Add Aluminium Color",
                    color = BossGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter Aluminium Color name (e.g. CHAMPAGNE, WOOD GRAIN, GREY):",
                        color = BossTextDim,
                        fontSize = 11.5.sp
                    )
                    OutlinedTextField(
                        value = newColorText,
                        onValueChange = { newColorText = it },
                        label = { Text("Color Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newColorText.trim().uppercase()
                        if (clean.isNotEmpty()) {
                            viewModel.addCustomColor(clean)
                            Toast.makeText(context, "Added Color $clean & saved to defaults!", Toast.LENGTH_SHORT).show()
                            showAddColorDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208))
                ) {
                    Text("Add Color", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddColorDialog = false }) {
                    Text("Cancel", color = BossTextDim)
                }
            }
        )
    }

    if (showMasterAiScanner) {
        MasterAiRateScannerDialog(
            viewModel = viewModel,
            state = state,
            onDismiss = { showMasterAiScanner = false }
        )
    }
}


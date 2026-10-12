@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.components.AddExtraPieceDialog
import com.example.ui.components.ExtraPiecesSection
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class DoorSectionType(val title: String) {
    DOORS("Openable Doors"),
    FIX("Fix Frames"),
    OP_WIN("Openable Windows"),
    SLIDING("Sliding Doors")
}

@Composable
fun DoorsAndFramesScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    var selectedCategory by remember { mutableStateOf(DoorSectionType.DOORS) }
    val baseOuterOptions = listOf("D54A", "D51", "D51A", "D45", "D45A", "D42", "D42F", "D54F", "D51F", "D45F")
        .filter { !state.deletedSections.contains(it) }
    val allOuterOptions = (baseOuterOptions + state.customSections).distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BossCarbonBg)
            .padding(12.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedCategory.ordinal,
            containerColor = BossCarbonPanel,
            contentColor = BossGold,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        ) {
            DoorSectionType.values().forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = {
                        Text(
                            text = cat.title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategory == cat) BossGold else BossTextDim
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (selectedCategory) {
                    DoorSectionType.DOORS -> "Openable Doors (${state.doors.size})"
                    DoorSectionType.FIX -> "Fix Frames (${state.fixframes.size})"
                    DoorSectionType.OP_WIN -> "Openable Windows (${state.openableWindows.size})"
                    DoorSectionType.SLIDING -> "Sliding Doors (${state.slidingDoors.size})"
                },
                color = BossGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        when (selectedCategory) {
                            DoorSectionType.DOORS -> viewModel.addDoor()
                            DoorSectionType.FIX -> viewModel.addFixFrame()
                            DoorSectionType.OP_WIN -> viewModel.addOpenableWindow()
                            DoorSectionType.SLIDING -> viewModel.addSlidingDoor()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedCategory) {
                DoorSectionType.DOORS -> {
                    if (state.doors.isEmpty()) {
                        item { EmptyCategoryNotice("No doors added yet. Tap '+ Add Item' above.") }
                    } else {
                        itemsIndexed(state.doors, key = { _, it -> it.id }) { index, item ->
                            GenericFrameCard(
                                title = "Door #${index + 1}",
                                width = item.width,
                                height = item.height,
                                qty = item.qty,
                                outer = item.outer,
                                outerOptions = allOuterOptions,
                                glassEnabled = item.glassEnabled,
                                sectionGauge = item.sectionGauge,
                                glassGauge = item.glassGauge,
                                glassColor = item.glassColor,
                                availableGauges = state.availableGauges,
                                availableGlassGauges = state.availableGlassGauges,
                                glassColors = state.getGlassColorsForGauge(item.glassGauge),
                                masterGauge = state.ctxGauge,
                                masterGaugeMode = state.ctxGaugeMode,
                                masterFrameType = state.ctxFrameType,
                                masterGlassGauge = state.ctxGlassGauge,
                                masterGlassColor = state.ctxGlassColor,
                                masterGlassMode = state.ctxGlassMode,
                                extras = item.extras,
                                d59Qty = item.d59Qty,
                                onD59QtyChange = { updatedQty -> viewModel.updateDoor(item.copy(d59Qty = updatedQty)) },
                                d59aQty = item.d59aQty,
                                onD59aQtyChange = { updatedQty -> viewModel.updateDoor(item.copy(d59aQty = updatedQty)) },
                                d52Qty = item.d52Qty,
                                onD52QtyChange = { updatedQty -> viewModel.updateDoor(item.copy(d52Qty = updatedQty)) },
                                customSections = state.customSections,
                                unit = state.measurementUnit,
                                onAddNewSection = { sec, rate -> viewModel.addCustomSection(sec, rate) },
                                onAddNewSectionPair = { s1, r1, s2, r2 -> viewModel.addCustomSectionPair(s1, r1, s2, r2) },
                                onRemoveCustomSection = { viewModel.removeCustomSection(it) },
                                onRemoveGlassColor = { col -> viewModel.removeGlassColor(col, item.glassGauge) },
                                onUpdate = { w, h, q, o, g, sg, gg, gc ->
                                    viewModel.updateDoor(
                                        item.copy(
                                            width = w, height = h, qty = q, outer = o,
                                            glassEnabled = g, sectionGauge = sg, glassGauge = gg, glassColor = gc
                                        )
                                    )
                                },
                                onExtrasChange = { updatedExtras ->
                                    viewModel.updateDoor(item.copy(extras = updatedExtras))
                                },
                                onRemove = { viewModel.removeDoor(item.id) }
                            )
                        }
                    }
                }
                DoorSectionType.FIX -> {
                    if (state.fixframes.isEmpty()) {
                        item { EmptyCategoryNotice("No fix frames added yet. Tap '+ Add Item' above.") }
                    } else {
                        itemsIndexed(state.fixframes, key = { _, it -> it.id }) { index, item ->
                            GenericFrameCard(
                                title = "Fix Frame #${index + 1}",
                                width = item.width,
                                height = item.height,
                                qty = item.qty,
                                outer = item.outer,
                                outerOptions = allOuterOptions,
                                glassEnabled = item.glassEnabled,
                                sectionGauge = item.sectionGauge,
                                glassGauge = item.glassGauge,
                                glassColor = item.glassColor,
                                availableGauges = state.availableGauges,
                                availableGlassGauges = state.availableGlassGauges,
                                glassColors = state.getGlassColorsForGauge(item.glassGauge),
                                masterGauge = state.ctxGauge,
                                masterGaugeMode = state.ctxGaugeMode,
                                masterFrameType = state.ctxFrameType,
                                masterGlassGauge = state.ctxGlassGauge,
                                masterGlassColor = state.ctxGlassColor,
                                masterGlassMode = state.ctxGlassMode,
                                extras = item.extras,
                                d59Qty = item.d59Qty,
                                onD59QtyChange = { updatedQty -> viewModel.updateFixFrame(item.copy(d59Qty = updatedQty)) },
                                d59aQty = item.d59aQty,
                                onD59aQtyChange = { updatedQty -> viewModel.updateFixFrame(item.copy(d59aQty = updatedQty)) },
                                d52Qty = item.d52Qty,
                                onD52QtyChange = { updatedQty -> viewModel.updateFixFrame(item.copy(d52Qty = updatedQty)) },
                                customSections = state.customSections,
                                unit = state.measurementUnit,
                                onAddNewSection = { sec, rate -> viewModel.addCustomSection(sec, rate) },
                                onAddNewSectionPair = { s1, r1, s2, r2 -> viewModel.addCustomSectionPair(s1, r1, s2, r2) },
                                onRemoveCustomSection = { viewModel.removeCustomSection(it) },
                                onRemoveGlassColor = { col -> viewModel.removeGlassColor(col, item.glassGauge) },
                                onUpdate = { w, h, q, o, g, sg, gg, gc ->
                                    viewModel.updateFixFrame(
                                        item.copy(
                                            width = w, height = h, qty = q, outer = o,
                                            glassEnabled = g, sectionGauge = sg, glassGauge = gg, glassColor = gc
                                        )
                                    )
                                },
                                onExtrasChange = { updatedExtras ->
                                    viewModel.updateFixFrame(item.copy(extras = updatedExtras))
                                },
                                onRemove = { viewModel.removeFixFrame(item.id) }
                            )
                        }
                    }
                }
                DoorSectionType.OP_WIN -> {
                    if (state.openableWindows.isEmpty()) {
                        item { EmptyCategoryNotice("No openable windows added yet. Tap '+ Add Item' above.") }
                    } else {
                        itemsIndexed(state.openableWindows, key = { _, it -> it.id }) { index, item ->
                            GenericFrameCard(
                                title = "Openable Window #${index + 1}",
                                width = item.width,
                                height = item.height,
                                qty = item.qty,
                                outer = item.outer,
                                outerOptions = allOuterOptions,
                                glassEnabled = item.glassEnabled,
                                sectionGauge = item.sectionGauge,
                                glassGauge = item.glassGauge,
                                glassColor = item.glassColor,
                                availableGauges = state.availableGauges,
                                availableGlassGauges = state.availableGlassGauges,
                                glassColors = state.getGlassColorsForGauge(item.glassGauge),
                                masterGauge = state.ctxGauge,
                                masterGaugeMode = state.ctxGaugeMode,
                                masterFrameType = state.ctxFrameType,
                                masterGlassGauge = state.ctxGlassGauge,
                                masterGlassColor = state.ctxGlassColor,
                                masterGlassMode = state.ctxGlassMode,
                                extras = item.extras,
                                d59Qty = item.d59Qty,
                                onD59QtyChange = { updatedQty -> viewModel.updateOpenableWindow(item.copy(d59Qty = updatedQty)) },
                                d59aQty = item.d59aQty,
                                onD59aQtyChange = { updatedQty -> viewModel.updateOpenableWindow(item.copy(d59aQty = updatedQty)) },
                                d52Qty = item.d52Qty,
                                onD52QtyChange = { updatedQty -> viewModel.updateOpenableWindow(item.copy(d52Qty = updatedQty)) },
                                customSections = state.customSections,
                                unit = state.measurementUnit,
                                onAddNewSection = { sec, rate -> viewModel.addCustomSection(sec, rate) },
                                onAddNewSectionPair = { s1, r1, s2, r2 -> viewModel.addCustomSectionPair(s1, r1, s2, r2) },
                                onRemoveCustomSection = { viewModel.removeCustomSection(it) },
                                onRemoveGlassColor = { col -> viewModel.removeGlassColor(col, item.glassGauge) },
                                onUpdate = { w, h, q, o, g, sg, gg, gc ->
                                    viewModel.updateOpenableWindow(
                                        item.copy(
                                            width = w, height = h, qty = q, outer = o,
                                            glassEnabled = g, sectionGauge = sg, glassGauge = gg, glassColor = gc
                                        )
                                    )
                                },
                                onExtrasChange = { updatedExtras ->
                                    viewModel.updateOpenableWindow(item.copy(extras = updatedExtras))
                                },
                                onRemove = { viewModel.removeOpenableWindow(item.id) }
                            )
                        }
                    }
                }
                DoorSectionType.SLIDING -> {
                    if (state.slidingDoors.isEmpty()) {
                        item { EmptyCategoryNotice("No sliding doors added yet. Tap '+ Add Item' above.") }
                    } else {
                        itemsIndexed(state.slidingDoors, key = { _, it -> it.id }) { index, item ->
                            val sdOuterOptions = (listOf("NORMAL", "F") + state.customSections).distinct()
                            GenericFrameCard(
                                title = "Sliding Door #${index + 1}",
                                isSlidingDoor = true,
                                width = item.width,
                                height = item.height,
                                qty = item.qty,
                                outer = item.outer,
                                outerOptions = sdOuterOptions,
                                glassEnabled = item.glassEnabled,
                                sectionGauge = item.sectionGauge,
                                glassGauge = item.glassGauge,
                                glassColor = item.glassColor,
                                availableGauges = state.availableGauges,
                                availableGlassGauges = state.availableGlassGauges,
                                glassColors = state.getGlassColorsForGauge(item.glassGauge),
                                masterGauge = state.ctxGauge,
                                masterGaugeMode = state.ctxGaugeMode,
                                masterFrameType = state.ctxFrameType,
                                masterGlassGauge = state.ctxGlassGauge,
                                masterGlassColor = state.ctxGlassColor,
                                masterGlassMode = state.ctxGlassMode,
                                extras = item.extras,
                                defaultExtraSection = "M23",
                                suggestedSections = (listOf("M23", "M28", "M24", "M24A", "M26", "M30", "D29") + state.customSections).distinct(),
                                customSections = state.customSections,
                                panelCount = item.panelCount,
                                onPanelCountChange = { newCount ->
                                    val newM24 = newCount * 2
                                    viewModel.updateSlidingDoor(item.copy(panelCount = newCount, m24Qty = newM24))
                                },
                                lockType = item.lockType,
                                onLockTypeChange = { newLock ->
                                    viewModel.updateSlidingDoor(item.copy(lockType = newLock))
                                },
                                m24Qty = item.m24Qty,
                                onM24QtyChange = { updatedQty ->
                                    viewModel.updateSlidingDoor(item.copy(m24Qty = updatedQty))
                                },
                                m24aQty = item.m24aQty,
                                onM24aQtyChange = { updatedQty ->
                                    viewModel.updateSlidingDoor(item.copy(m24aQty = updatedQty))
                                },
                                unit = state.measurementUnit,
                                onAddNewSection = { sec, rate -> viewModel.addCustomSection(sec, rate) },
                                onAddNewSectionPair = { s1, r1, s2, r2 -> viewModel.addCustomSectionPair(s1, r1, s2, r2) },
                                onRemoveCustomSection = { viewModel.removeCustomSection(it) },
                                onRemoveGlassColor = { col -> viewModel.removeGlassColor(col, item.glassGauge) },
                                onUpdate = { w, h, q, o, g, sg, gg, gc ->
                                    viewModel.updateSlidingDoor(
                                        item.copy(
                                            width = w, height = h, qty = q, outer = o,
                                            glassEnabled = g, sectionGauge = sg, glassGauge = gg, glassColor = gc
                                        )
                                    )
                                },
                                onExtrasChange = { updatedExtras ->
                                    viewModel.updateSlidingDoor(item.copy(extras = updatedExtras))
                                },
                                onRemove = { viewModel.removeSlidingDoor(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCategoryNotice(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = msg, color = BossTextDim, fontSize = 13.sp)
    }
}

@Composable
fun GenericFrameCard(
    title: String,
    width: Double,
    height: Double,
    qty: Int,
    outer: String,
    outerOptions: List<String>,
    glassEnabled: Boolean,
    isSlidingDoor: Boolean = false,
    sectionGauge: String = "1.2",
    glassGauge: String = "5",
    glassColor: String = "CLEAR",
    availableGauges: List<String> = BossDefaultRates.GAUGES,
    availableGlassGauges: List<String> = listOf("5", "8"),
    glassColors: List<String> = BossDefaultRates.DEFAULT_GLASS_COLORS,
    masterGauge: String = "1.6",
    masterGaugeMode: String = "OFF",
    masterFrameType: String = "OFF",
    masterGlassGauge: String = "5",
    masterGlassColor: String = "CLEAR",
    masterGlassMode: String = "OFF",
    extras: List<ExtraPiece> = emptyList(),
    defaultExtraSection: String = "D54A",
    suggestedSections: List<String> = listOf("D54A", "D41", "D50", "D52", "D46", "M23"),
    customSections: List<String> = emptyList(),
    panelCount: Int? = null,
    onPanelCountChange: ((Int) -> Unit)? = null,
    lockType: String? = null,
    onLockTypeChange: ((String) -> Unit)? = null,
    m24Qty: Int? = null,
    onM24QtyChange: ((Int) -> Unit)? = null,
    m24aQty: Int? = null,
    onM24aQtyChange: ((Int) -> Unit)? = null,
    d59Qty: Int = 0,
    onD59QtyChange: ((Int) -> Unit)? = null,
    d59aQty: Int = 0,
    onD59aQtyChange: ((Int) -> Unit)? = null,
    d52Qty: Int = 0,
    onD52QtyChange: ((Int) -> Unit)? = null,
    unit: String = "INCH_SUTAR",
    onAddNewSection: ((String, Double) -> Unit)? = null,
    onAddNewSectionPair: ((String, Double, String, Double) -> Unit)? = null,
    onRemoveCustomSection: ((String) -> Unit)? = null,
    onRemoveGlassColor: ((String) -> Unit)? = null,
    onUpdate: (width: Double, height: Double, qty: Int, outer: String, glassEnabled: Boolean, sectionGauge: String, glassGauge: String, glassColor: String) -> Unit,
    onExtrasChange: (List<ExtraPiece>) -> Unit = {},
    onRemove: () -> Unit
) {
    var widthText by remember(width, unit) { mutableStateOf(BossCalculationEngine.formatDimValueOnly(width, unit)) }
    var heightText by remember(height, unit) { mutableStateOf(BossCalculationEngine.formatDimValueOnly(height, unit)) }
    var qtyText by remember(qty) { mutableStateOf(qty.toString()) }
    var outerDropdown by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showAddItemExtraDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val singleSqft = (width * height) / 144.0
    val totalSqft = singleSqft * qty
    val sqftBadgeText = if (qty > 1) {
        "${"%.2f".format(singleSqft)} × $qty = ${"%.2f".format(totalSqft)} sq.ft"
    } else {
        "${"%.2f".format(totalSqft)} sq.ft"
    }

    fun dismissSmoothly(close: () -> Unit) {
        scope.launch {
            kotlinx.coroutines.delay(40)
            close()
        }
    }

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
                Text(title, color = BossGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BossCarbonPanel2,
                        border = BorderStroke(1.dp, BossGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = sqftBadgeText,
                            color = BossGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BossRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (panelCount != null && onPanelCountChange != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Panels:",
                        color = BossTextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(2 to "2 Palay", 3 to "3 Palay", 4 to "4 Palay").forEach { (count, label) ->
                            val isSelected = panelCount == count
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (panelCount != count) {
                                        onPanelCountChange(count)
                                    }
                                },
                                label = {
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BossGold,
                                    selectedLabelColor = Color(0xFF161208),
                                    containerColor = BossCarbonPanel2,
                                    labelColor = BossText
                                ),
                                border = BorderStroke(1.dp, if (isSelected) BossGold else BossCarbonBorder),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            if (lockType != null && onLockTypeChange != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lock / Handle:",
                        color = BossTextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "LATCH" to "Latch Lock",
                            "PUSH_HANDLE" to "Push Handle"
                        ).forEach { (lCode, lLabel) ->
                            val isLockSel = lockType == lCode
                            FilterChip(
                                selected = isLockSel,
                                onClick = {
                                    if (lockType != lCode) {
                                        onLockTypeChange(lCode)
                                    }
                                },
                                label = {
                                    Text(
                                        lLabel,
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isLockSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BossGold,
                                    selectedLabelColor = Color(0xFF161208),
                                    containerColor = BossCarbonPanel2,
                                    labelColor = BossText
                                ),
                                border = BorderStroke(1.dp, if (isLockSel) BossGold else BossCarbonBorder),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            // Piece Counters Row (M24, M24A for Sliding Door; D59, D59A, D52 for all Door/Frame items)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BossCarbonPanel2.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, BossCarbonBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSlidingDoor && m24Qty != null && onM24QtyChange != null && m24aQty != null && onM24aQtyChange != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PieceCounter(
                                label = "M24 (Pcs)",
                                value = m24Qty,
                                onValueChange = onM24QtyChange,
                                modifier = Modifier.weight(1f)
                            )
                            PieceCounter(
                                label = "M24A (Pcs)",
                                value = m24aQty,
                                onValueChange = onM24aQtyChange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (!isSlidingDoor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (onD59QtyChange != null) {
                                PieceCounter(
                                    label = "D59 (Pcs)",
                                    value = d59Qty,
                                    onValueChange = onD59QtyChange,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (onD59aQtyChange != null) {
                                PieceCounter(
                                    label = "D59A (Pcs)",
                                    value = d59aQty,
                                    onValueChange = onD59aQtyChange,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (onD52QtyChange != null) {
                                PieceCounter(
                                    label = "D52 (Pcs)",
                                    value = d52Qty,
                                    onValueChange = onD52QtyChange,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = {
                        widthText = it
                        val parsed = BossCalculationEngine.parseDimToInches(it, unit)
                        if (parsed > 0) onUpdate(parsed, height, qty, outer, glassEnabled, sectionGauge, glassGauge, glassColor)
                    },
                    label = { Text("Width (${BossCalculationEngine.unitLabel(unit)})", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        heightText = it
                        val parsed = BossCalculationEngine.parseDimToInches(it, unit)
                        if (parsed > 0) onUpdate(width, parsed, qty, outer, glassEnabled, sectionGauge, glassGauge, glassColor)
                    },
                    label = { Text("Height (${BossCalculationEngine.unitLabel(unit)})", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = {
                        qtyText = it
                        val q = it.toIntOrNull() ?: 1
                        onUpdate(width, height, q.coerceAtLeast(1), outer, glassEnabled, sectionGauge, glassGauge, glassColor)
                    },
                    label = { Text("Qty", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText
                    ),
                    modifier = Modifier.weight(0.7f)
                )
            }

            val isFrameLocked = isSlidingDoor && masterFrameType != "OFF"
            val displayOuter = if (isFrameLocked) {
                if (masterFrameType == "C") "Normal (Frame C)" else if (masterFrameType == "F") "F-Section (Frame F)" else masterFrameType
            } else {
                if (outer == "NORMAL") "Normal" else if (outer == "F") "F-Section" else outer
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    OutlinedButton(
                        onClick = { if (!isFrameLocked) outerDropdown = true },
                        enabled = !isFrameLocked,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isFrameLocked) BossTextDim else BossText,
                            disabledContainerColor = BossCarbonPanel2.copy(alpha = 0.5f),
                            disabledContentColor = BossTextDim
                        ),
                        border = BorderStroke(1.dp, if (isFrameLocked) BossCarbonBorder.copy(alpha = 0.5f) else BossCarbonBorder),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = displayOuter,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isFrameLocked) Icons.Default.Lock else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isFrameLocked) BossTextDim else BossGoldLight
                        )
                    }

                    DropdownMenu(
                        expanded = outerDropdown,
                        onDismissRequest = { outerDropdown = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                    ) {
                        outerOptions.forEach { opt ->
                            val canDeleteOpt = opt != "NORMAL" && opt != "F" && outerOptions.size > 1
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (opt == "NORMAL") "Normal (M26+M30)" else if (opt == "F") "F-Section (M26F+M30F)" else opt,
                                            fontWeight = if (opt == outer) FontWeight.Bold else FontWeight.Normal,
                                            color = if (opt == outer) BossGoldLight else BossText
                                        )
                                        if (canDeleteOpt) {
                                            IconButton(
                                                onClick = { onRemoveCustomSection?.invoke(opt) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove section $opt",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onUpdate(width, height, qty, opt, glassEnabled, sectionGauge, glassGauge, glassColor)
                                    dismissSmoothly { outerDropdown = false }
                                }
                            )
                        }
                        HorizontalDivider(color = BossCarbonBorder)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = BossGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Add New Section...", color = BossGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                outerDropdown = false
                                showAddSectionDialog = true
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = glassEnabled,
                        onCheckedChange = { onUpdate(width, height, qty, outer, it, sectionGauge, glassGauge, glassColor) },
                        colors = CheckboxDefaults.colors(checkedColor = BossGold)
                    )
                    Text("Glass", color = BossText, fontSize = 11.sp)
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isGaugeLocked = masterGaugeMode == "ON"
                val displaySectionGauge = "Sec: ${if (isGaugeLocked) masterGauge else sectionGauge}mm"
                var showGaugeMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = false,
                        enabled = !isGaugeLocked,
                        onClick = { if (!isGaugeLocked) showGaugeMenu = true },
                        modifier = Modifier.height(28.dp),
                        label = { Text(displaySectionGauge, fontSize = 10.sp, maxLines = 1) },
                        trailingIcon = {
                            Icon(
                                imageVector = if (isGaugeLocked) Icons.Default.Lock else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isGaugeLocked) BossTextDim else BossText
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = BossCarbonPanel2,
                            labelColor = if (isGaugeLocked) BossTextDim else BossText,
                            disabledContainerColor = BossCarbonPanel2.copy(alpha = 0.5f),
                            disabledLabelColor = BossTextDim
                        )
                    )
                    DropdownMenu(
                        expanded = showGaugeMenu,
                        onDismissRequest = { showGaugeMenu = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                    ) {
                        availableGauges.forEach { g ->
                            DropdownMenuItem(
                                text = { Text("${g} mm", fontWeight = if (g == sectionGauge) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onUpdate(width, height, qty, outer, glassEnabled, g, glassGauge, glassColor)
                                    dismissSmoothly { showGaugeMenu = false }
                                }
                            )
                        }
                    }
                }

                if (glassEnabled) {
                    val isGlassLocked = masterGlassMode == "ON"
                    val displayGlassGauge = "Glass: ${if (isGlassLocked) masterGlassGauge else glassGauge}mm"
                    var showGlassGaugeMenu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = false,
                            enabled = !isGlassLocked,
                            onClick = { if (!isGlassLocked) showGlassGaugeMenu = true },
                            modifier = Modifier.height(28.dp),
                            label = { Text(displayGlassGauge, fontSize = 10.sp, maxLines = 1) },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isGlassLocked) Icons.Default.Lock else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isGlassLocked) BossTextDim else BossGoldLight
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = BossCarbonPanel2,
                                labelColor = if (isGlassLocked) BossTextDim else BossGoldLight,
                                disabledContainerColor = BossCarbonPanel2.copy(alpha = 0.5f),
                                disabledLabelColor = BossTextDim
                            )
                        )
                        DropdownMenu(
                            expanded = showGlassGaugeMenu,
                            onDismissRequest = { showGlassGaugeMenu = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                        ) {
                            availableGlassGauges.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text("${g} mm", fontWeight = if (g == glassGauge) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onUpdate(width, height, qty, outer, glassEnabled, sectionGauge, g, glassColor)
                                        dismissSmoothly { showGlassGaugeMenu = false }
                                    }
                                )
                            }
                        }
                    }

                    val displayGlassColor = if (isGlassLocked) masterGlassColor else glassColor
                    var showGlassColorMenu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = false,
                            enabled = !isGlassLocked,
                            onClick = { if (!isGlassLocked) showGlassColorMenu = true },
                            modifier = Modifier.height(28.dp),
                            label = { Text(displayGlassColor, fontSize = 10.sp, maxLines = 1) },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isGlassLocked) Icons.Default.Lock else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isGlassLocked) BossTextDim else BossGoldLight
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = BossCarbonPanel2,
                                labelColor = if (isGlassLocked) BossTextDim else BossGoldLight,
                                disabledContainerColor = BossCarbonPanel2.copy(alpha = 0.5f),
                                disabledLabelColor = BossTextDim
                            )
                        )
                        DropdownMenu(
                            expanded = showGlassColorMenu,
                            onDismissRequest = { showGlassColorMenu = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                        ) {
                            glassColors.forEach { gc ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(gc, fontWeight = if (gc == glassColor) FontWeight.Bold else FontWeight.Normal)
                                            if (glassColors.size > 1) {
                                                IconButton(
                                                    onClick = { onRemoveGlassColor?.invoke(gc) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove glass $gc",
                                                        tint = BossRed,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        onUpdate(width, height, qty, outer, glassEnabled, sectionGauge, glassGauge, gc)
                                        dismissSmoothly { showGlassColorMenu = false }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemExtraDialog) {
        AddExtraPieceDialog(
            initialSection = defaultExtraSection,
            suggestedSections = (suggestedSections + customSections).distinct(),
            onDismiss = { showAddItemExtraDialog = false },
            onAdd = { sec, lenFt, q ->
                val newExtra = ExtraPiece(
                    section = sec,
                    lengthFt = lenFt,
                    qty = q,
                    gauge = sectionGauge
                )
                onExtrasChange(extras + newExtra)
                showAddItemExtraDialog = false
            }
        )
    }

    if (showAddSectionDialog) {
        AddCustomSectionDialog(
            onDismiss = { showAddSectionDialog = false },
            onAdd = { secName, rate ->
                onAddNewSection?.invoke(secName, rate)
                onUpdate(width, height, qty, secName, glassEnabled, sectionGauge, glassGauge, glassColor)
                showAddSectionDialog = false
            },
            onAddPair = { s1, r1, s2, r2 ->
                onAddNewSectionPair?.invoke(s1, r1, s2, r2)
                val pairCode = if (s2.isNotBlank() && s2 != s1) "$s1 / $s2" else s1
                onUpdate(width, height, qty, pairCode, glassEnabled, sectionGauge, glassGauge, glassColor)
                showAddSectionDialog = false
            }
        )
    }
}

@Composable
fun AddCustomSectionDialog(
    onDismiss: () -> Unit,
    onAdd: (sectionName: String, rate: Double) -> Unit,
    onAddPair: ((sec1: String, rate1: Double, sec2: String, rate2: Double) -> Unit)? = null,
    isPairModeDefault: Boolean = false
) {
    var isPairMode by remember { mutableStateOf(isPairModeDefault) }
    var singleName by remember { mutableStateOf("") }
    var singleRateStr by remember { mutableStateOf("0") }
    var sec1Name by remember { mutableStateOf("") }
    var sec1RateStr by remember { mutableStateOf("0") }
    var sec2Name by remember { mutableStateOf("") }
    var sec2RateStr by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = BossGold)
                Text(
                    if (isPairMode) "Add Frame Section Pair (2 in 1)" else "Add Single Custom Section",
                    color = BossGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BossCarbonPanel2, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = isPairMode,
                        onClick = { isPairMode = true },
                        label = { Text("Pair (2 Sections: 2H+1W & 1W)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BossGold,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Transparent,
                            labelColor = BossTextDim
                        ),
                        border = null,
                        modifier = Modifier.weight(1f).height(32.dp)
                    )
                    FilterChip(
                        selected = !isPairMode,
                        onClick = { isPairMode = false },
                        label = { Text("Single Section", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BossGold,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Transparent,
                            labelColor = BossTextDim
                        ),
                        border = null,
                        modifier = Modifier.weight(1f).height(32.dp)
                    )
                }

                if (isPairMode) {
                    OutlinedTextField(
                        value = sec1Name,
                        onValueChange = { sec1Name = it },
                        label = { Text("Section 1: Sides & Top (2H + 1W) *") },
                        placeholder = { Text("e.g. DC30C, D55, DC40") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sec1RateStr,
                        onValueChange = { sec1RateStr = it },
                        label = { Text("Sec 1 Rate per ft (Rs/ft)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sec2Name,
                        onValueChange = { sec2Name = it },
                        label = { Text("Section 2: Bottom Track (1W) *") },
                        placeholder = { Text("e.g. DC26C, D26, DC26") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sec2RateStr,
                        onValueChange = { sec2RateStr = it },
                        label = { Text("Sec 2 Rate per ft (Rs/ft)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = singleName,
                        onValueChange = { singleName = it },
                        label = { Text("Section Name (e.g. D55, DC40)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = singleRateStr,
                        onValueChange = { singleRateStr = it },
                        label = { Text("Rate per foot (Rs/ft for 1.2mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPairMode) {
                        val s1 = sec1Name.trim().uppercase()
                        val s2 = sec2Name.trim().uppercase()
                        if (s1.isNotBlank()) {
                            val r1 = sec1RateStr.toDoubleOrNull() ?: 0.0
                            val r2 = sec2RateStr.toDoubleOrNull() ?: 0.0
                            if (onAddPair != null) {
                                onAddPair(s1, r1, s2, r2)
                            } else {
                                val pair = if (s2.isNotBlank() && s2 != s1) "$s1 / $s2" else s1
                                onAdd(pair, r1)
                            }
                        }
                    } else {
                        val clean = singleName.trim().uppercase()
                        if (clean.isNotBlank()) {
                            val r = singleRateStr.toDoubleOrNull() ?: 0.0
                            onAdd(clean, r)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                enabled = if (isPairMode) sec1Name.trim().isNotBlank() else singleName.trim().isNotBlank()
            ) {
                Text(if (isPairMode) "Add Section Pair" else "Add Section", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BossTextDim)
            }
        },
        containerColor = BossCarbonPanel,
        shape = RoundedCornerShape(12.dp)
    )
}

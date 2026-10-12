@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BossCalculationEngine
import com.example.model.BossDefaultRates
import com.example.model.ExtraPiece
import com.example.model.WindowItem
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.components.AddExtraPieceDialog
import com.example.ui.components.ExtraPiecesSection
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WindowsScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BossCarbonBg)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sliding Windows (${state.windows.size})",
                    color = BossGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.addWindow() },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Window", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.windows.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No windows added yet. Tap '+ Add Window' above.", color = BossTextDim)
                    }
                }
            } else {
                itemsIndexed(state.windows, key = { _, item -> item.id }) { index, win ->
                    WindowCard(
                        index = index,
                        item = win,
                        availableGauges = state.availableGauges,
                        availableGlassGauges = state.availableGlassGauges,
                        glassColors = state.getGlassColorsForGauge(win.glassGauge),
                        customSections = state.customSections,
                        masterGauge = state.ctxGauge,
                        masterGaugeMode = state.ctxGaugeMode,
                        masterFrameType = state.ctxFrameType,
                        masterGlassGauge = state.ctxGlassGauge,
                        masterGlassColor = state.ctxGlassColor,
                        masterGlassMode = state.ctxGlassMode,
                        unit = state.measurementUnit,
                        onAddNewSection = { sec, rate -> viewModel.addCustomSection(sec, rate) },
                        onAddNewSectionPair = { s1, r1, s2, r2 -> viewModel.addCustomSectionPair(s1, r1, s2, r2) },
                        onRemoveCustomSection = { viewModel.removeSection(it) },
                        onRemoveGlassColor = { col, g -> viewModel.removeGlassColor(col, g) },
                        onUpdate = { viewModel.updateWindow(it) },
                        onRemove = { viewModel.removeWindow(win.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WindowCard(
    index: Int,
    item: WindowItem,
    availableGauges: List<String> = BossDefaultRates.GAUGES,
    availableGlassGauges: List<String> = listOf("5", "8"),
    glassColors: List<String> = BossDefaultRates.DEFAULT_GLASS_COLORS,
    customSections: List<String> = emptyList(),
    masterGauge: String = "1.6",
    masterGaugeMode: String = "OFF",
    masterFrameType: String = "OFF",
    masterGlassGauge: String = "5",
    masterGlassColor: String = "CLEAR",
    masterGlassMode: String = "OFF",
    unit: String = "INCH_SUTAR",
    onAddNewSection: ((String, Double) -> Unit)? = null,
    onAddNewSectionPair: ((String, Double, String, Double) -> String)? = null,
    onRemoveCustomSection: ((String) -> Unit)? = null,
    onRemoveGlassColor: ((String, String) -> Unit)? = null,
    onUpdate: (WindowItem) -> Unit,
    onRemove: () -> Unit
) {
    var widthText by remember(item.width, unit) { mutableStateOf(BossCalculationEngine.formatDimValueOnly(item.width, unit)) }
    var heightText by remember(item.height, unit) { mutableStateOf(BossCalculationEngine.formatDimValueOnly(item.height, unit)) }
    var qtyText by remember(item.qty) { mutableStateOf(item.qty.toString()) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showAddItemExtraDialog by remember { mutableStateOf(false) }
    val sqFt = remember(item.width, item.height, item.qty) {
        (item.width / 12.0) * (item.height / 12.0) * item.qty
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Window #${index + 1}",
                        color = BossGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BossCarbonPanel2,
                        border = BorderStroke(0.5.dp, BossGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${"%.2f".format(sqFt)} sq.ft",
                            color = BossGoldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BossRed, modifier = Modifier.size(18.dp))
                }
            }

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
                        val isSelected = item.panelCount == count
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (item.panelCount != count) {
                                    val newM24 = count * 2
                                    val newD29 = if (count >= 3) 4 else 2
                                    onUpdate(item.copy(
                                        panelCount = count,
                                        m24Qty = newM24,
                                        d29HeightQty = newD29,
                                        d29WidthQty = newD29
                                    ))
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
                        "LATCH" to "Latch",
                        "PUSH_HANDLE" to "Push Handle"
                    ).forEach { (lockCode, lockLabel) ->
                        val isSelected = item.lockType == lockCode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (item.lockType != lockCode) {
                                    onUpdate(item.copy(lockType = lockCode))
                                }
                            },
                            label = {
                                Text(
                                    lockLabel,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = {
                        widthText = it
                        val parsed = BossCalculationEngine.parseDimToInches(it, unit)
                        if (parsed > 0) onUpdate(item.copy(width = parsed))
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
                        if (parsed > 0) onUpdate(item.copy(height = parsed))
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
                        onUpdate(item.copy(qty = q.coerceAtLeast(1)))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showFrameTypeMenu by remember { mutableStateOf(false) }
                val isFrameLocked = masterFrameType != "OFF"
                val displayLabel = if (isFrameLocked) {
                    when (masterFrameType) {
                        "C" -> "Section: Frame C (DC30C / DC26C) (Master)"
                        "F" -> "Section: Frame F (DC30F / DC26F) (Master)"
                        else -> "Section: $masterFrameType (Master)"
                    }
                } else {
                    when (item.frameType) {
                        "C" -> "Section: Frame C (DC30C / DC26C)"
                        "F" -> "Section: Frame F (DC30F / DC26F)"
                        else -> "Section: ${item.frameType}"
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { if (!isFrameLocked) showFrameTypeMenu = true },
                        enabled = !isFrameLocked,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BossCarbonPanel2,
                            contentColor = if (isFrameLocked) BossTextDim else if (item.frameType == "F") BossSteel else BossGoldLight,
                            disabledContainerColor = BossCarbonPanel2.copy(alpha = 0.5f),
                            disabledContentColor = BossTextDim
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isFrameLocked) BossCarbonBorder.copy(alpha = 0.5f)
                            else if (item.frameType == "F") BossSteel.copy(alpha = 0.7f) else BossGold.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = displayLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isFrameLocked) Icons.Default.Lock else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isFrameLocked) BossTextDim else (if (item.frameType == "F") BossSteel else BossGoldLight)
                        )
                    }

                    DropdownMenu(
                        expanded = showFrameTypeMenu,
                        onDismissRequest = { showFrameTypeMenu = false },
                        properties = androidx.compose.ui.window.PopupProperties(
                            focusable = true,
                            dismissOnClickOutside = true,
                            dismissOnBackPress = true
                        )
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Frame C (DC30C / DC26C)",
                                    fontWeight = if (item.frameType == "C") FontWeight.Bold else FontWeight.Normal,
                                    color = if (item.frameType == "C") BossGoldLight else BossText
                                )
                            },
                            onClick = {
                                onUpdate(item.copy(frameType = "C", outerSection = "DC30C"))
                                showFrameTypeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Frame F (DC30F / DC26F)",
                                    fontWeight = if (item.frameType == "F") FontWeight.Bold else FontWeight.Normal,
                                    color = if (item.frameType == "F") BossSteel else BossText
                                )
                            },
                            onClick = {
                                onUpdate(item.copy(frameType = "F", outerSection = "DC30F"))
                                showFrameTypeMenu = false
                            }
                        )

                        if (customSections.isNotEmpty()) {
                            HorizontalDivider(color = BossCarbonBorder)
                            customSections.forEach { sec ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                sec,
                                                fontWeight = if (sec == item.frameType) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sec == item.frameType) BossGoldLight else BossText
                                            )
                                            IconButton(
                                                onClick = {
                                                    onRemoveCustomSection?.invoke(sec)
                                                    if (item.frameType == sec) {
                                                        onUpdate(item.copy(frameType = "C", outerSection = "DC30C"))
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove section $sec",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onUpdate(item.copy(frameType = sec, outerSection = sec))
                                        showFrameTypeMenu = false
                                    }
                                )
                            }
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
                                showFrameTypeMenu = false
                                showAddSectionDialog = true
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.glassEnabled,
                        onCheckedChange = { onUpdate(item.copy(glassEnabled = it)) },
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
                val scope = rememberCoroutineScope()
                fun dismissSmoothly(close: () -> Unit) {
                    scope.launch {
                        kotlinx.coroutines.delay(40)
                        close()
                    }
                }

                val isGaugeLocked = masterGaugeMode == "ON"
                val displaySectionGauge = "Sec: ${if (isGaugeLocked) masterGauge else item.sectionGauge}mm"
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
                                text = { Text("${g} mm", fontWeight = if (g == item.sectionGauge) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onUpdate(item.copy(sectionGauge = g))
                                    dismissSmoothly { showGaugeMenu = false }
                                }
                            )
                        }
                    }
                }

                if (item.glassEnabled) {
                    val isGlassLocked = masterGlassMode == "ON"
                    val displayGlassGauge = "Glass: ${if (isGlassLocked) masterGlassGauge else item.glassGauge}mm"
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
                                    text = { Text("${g} mm", fontWeight = if (g == item.glassGauge) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onUpdate(item.copy(glassGauge = g))
                                        dismissSmoothly { showGlassGaugeMenu = false }
                                    }
                                )
                            }
                        }
                    }

                    val displayGlassColor = if (isGlassLocked) masterGlassColor else item.glassColor
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
                                            Text(
                                                gc,
                                                fontWeight = if (gc == item.glassColor) FontWeight.Bold else FontWeight.Normal,
                                                color = if (gc == item.glassColor) BossGoldLight else BossText
                                            )
                                            if (glassColors.size > 1 && onRemoveGlassColor != null) {
                                                IconButton(
                                                    onClick = { onRemoveGlassColor(gc, item.glassGauge) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Remove $gc",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        onUpdate(item.copy(glassColor = gc))
                                        dismissSmoothly { showGlassColorMenu = false }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PieceCounter(
                    label = "M24 Pcs",
                    value = item.m24Qty,
                    onValueChange = { onUpdate(item.copy(m24Qty = it)) },
                    modifier = Modifier.weight(1f)
                )
                PieceCounter(
                    label = "M24A Pcs",
                    value = item.m24aQty,
                    onValueChange = { onUpdate(item.copy(m24aQty = it)) },
                    modifier = Modifier.weight(1f)
                )
                PieceCounter(
                    label = "D29 (H)",
                    value = item.d29HeightQty,
                    onValueChange = { onUpdate(item.copy(d29HeightQty = it)) },
                    modifier = Modifier.weight(1f)
                )
                PieceCounter(
                    label = "D29 (W)",
                    value = item.d29WidthQty,
                    onValueChange = { onUpdate(item.copy(d29WidthQty = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showAddItemExtraDialog) {
        AddExtraPieceDialog(
            initialSection = "M23",
            suggestedSections = (listOf("DC30C", "DC26C", "DC30F", "DC26F", "M23", "M24", "M24A", "M28", "D29", "D31") + customSections).distinct(),
            onDismiss = { showAddItemExtraDialog = false },
            onAdd = { sec, lenFt, q ->
                val newEx = ExtraPiece(
                    section = sec,
                    lengthFt = lenFt,
                    qty = q,
                    gauge = item.sectionGauge
                )
                onUpdate(item.copy(extras = item.extras + newEx))
                showAddItemExtraDialog = false
            }
        )
    }

    if (showAddSectionDialog) {
        AddCustomSectionDialog(
            onDismiss = { showAddSectionDialog = false },
            isPairModeDefault = true,
            onAdd = { secName, rate ->
                onAddNewSection?.invoke(secName, rate)
                onUpdate(item.copy(frameType = secName, outerSection = secName))
                showAddSectionDialog = false
            },
            onAddPair = { s1, r1, s2, r2 ->
                val pair = onAddNewSectionPair?.invoke(s1, r1, s2, r2) ?: (if (s2.isNotBlank() && s2 != s1) "$s1 / $s2" else s1)
                onUpdate(item.copy(frameType = pair, outerSection = pair))
                showAddSectionDialog = false
            }
        )
    }
}

@Composable
fun PieceCounter(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(BossCarbonPanel2, RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = BossTextDim, fontSize = 9.sp, maxLines = 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                modifier = Modifier.size(20.dp)
            ) {
                Text("-", color = BossGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = "$value",
                color = BossText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            IconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(20.dp)
            ) {
                Text("+", color = BossGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

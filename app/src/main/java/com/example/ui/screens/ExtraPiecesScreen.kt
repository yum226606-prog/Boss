package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExtraPiece
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.theme.*

@Composable
fun ExtraPiecesScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val totalPieces = state.projectExtras.sumOf { it.qty }
    val totalFeet = state.projectExtras.sumOf { it.lengthFt * it.qty }
    val uniqueSections = state.projectExtras.map { it.section }.distinct().size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BossCarbonBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
            border = BorderStroke(1.dp, BossCarbonBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = "Extra Pieces",
                        tint = BossGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Extra Pieces",
                        color = BossGold,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (state.projectExtras.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = BossGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BossGold.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$totalPieces",
                                color = BossGoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.projectExtras.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = BossRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add Piece", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        if (state.projectExtras.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatsBadgeCard(
                    title = "PIECES",
                    value = "$totalPieces",
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(1f)
                )
                StatsBadgeCard(
                    title = "LENGTH",
                    value = "${"%.1f".format(totalFeet)} ft",
                    icon = Icons.Default.Straighten,
                    modifier = Modifier.weight(1.2f)
                )
                StatsBadgeCard(
                    title = "SECTIONS",
                    value = "$uniqueSections",
                    icon = Icons.Default.Category,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state.projectExtras.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
                        border = BorderStroke(1.dp, BossCarbonBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BossCarbonPanel2,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = BossGoldLight,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = "No Extra Pieces Added",
                                color = BossGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tap 'Add Piece' to include extra profiles, pipes, or angles.",
                                color = BossTextDim,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Extra Piece", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(state.projectExtras) { idx, ex ->
                    ExtraPieceCard(
                        index = idx,
                        piece = ex,
                        onDelete = { viewModel.removeProjectExtra(idx) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AllSectionsAddDialog(
            state = state,
            onDismiss = { showAddDialog = false },
            onAdd = { sec, lenFt, qty, gauge ->
                viewModel.addProjectExtra(sec, lenFt, qty, gauge)
                showAddDialog = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = BossCarbonPanel,
            title = {
                Text("Clear All Extra Pieces?", color = BossGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Text(
                    "Are you sure you want to remove all $totalPieces extra aluminium pieces?",
                    color = BossText,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearProjectExtras()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossRed, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = BossTextDim, fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun StatsBadgeCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
        border = BorderStroke(1.dp, BossCarbonBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = BossGold, modifier = Modifier.size(13.dp))
            Column {
                Text(title, color = BossTextDim, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(value, color = BossGoldLight, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ExtraPieceCard(
    index: Int,
    piece: ExtraPiece,
    onDelete: () -> Unit
) {
    val totalFt = piece.lengthFt * piece.qty
    val totalInches = (piece.lengthFt * 12.0).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
        border = BorderStroke(1.dp, BossCarbonBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = BossCarbonPanel2,
                    border = BorderStroke(1.dp, BossGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = piece.section,
                        color = BossGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "${piece.lengthFt} ft (${totalInches}\") x ${piece.qty} pcs",
                            color = BossText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (piece.gauge.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = BossSteel.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, BossSteel.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${piece.gauge}mm",
                                    color = BossSteel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Total: ${"%.1f".format(totalFt)} ft profile",
                        color = BossTextDim,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete extra piece",
                    tint = BossRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllSectionsAddDialog(
    state: BossAppState,
    onDismiss: () -> Unit,
    onAdd: (section: String, lengthFt: Double, qty: Int, gauge: String) -> Unit
) {
    val windowSections = listOf("M23", "M24", "M24A", "M28", "M29", "M30", "M35", "M38")
    val doorSections = listOf("D54A", "D51", "D50", "D52", "D46", "D41", "D42", "D45", "D29", "D59", "D55", "DC40")
    val pipeSections = listOf("PIPE", "1x1 PIPE", "1.5x1.5 PIPE", "2x1 PIPE", "3x1.5 PIPE", "ANGLE", "1x1 ANGLE", "BEADING", "CORNER", "U-CHANNEL")
    val customAndRatesSections = (state.customSections + state.rates.values.flatMap { colorMap ->
        colorMap.values.flatMap { it.keys }
    }).distinct()

    val allSections = (windowSections + doorSections + pipeSections + customAndRatesSections)
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Windows (M)", "Doors (D)", "Pipes / Angles", "Custom / Rates")

    var selectedSection by remember { mutableStateOf("M23") }
    var searchQuery by remember { mutableStateOf("") }
    var lengthText by remember { mutableStateOf("10.0") }
    var qtyText by remember { mutableStateOf("1") }
    var selectedGauge by remember { mutableStateOf(state.ctxGauge) }
    val availableGauges = state.availableGauges.ifEmpty { listOf("1.2", "1.4", "1.6", "2.0") }

    val filteredSections = remember(selectedCategory, searchQuery) {
        val list = when (selectedCategory) {
            "Windows (M)" -> windowSections
            "Doors (D)" -> doorSections
            "Pipes / Angles" -> pipeSections
            "Custom / Rates" -> customAndRatesSections
            else -> allSections
        }
        if (searchQuery.isBlank()) list
        else list.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BossCarbonPanel,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Extension, contentDescription = null, tint = BossGold, modifier = Modifier.size(18.dp))
                Text("Add Extra Aluminium Piece", color = BossGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                    containerColor = BossCarbonPanel2,
                    contentColor = BossGold,
                    edgePadding = 2.dp,
                    divider = {},
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                ) {
                    categories.forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            text = {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == cat) BossGold else BossTextDim
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery.ifBlank { selectedSection },
                    onValueChange = {
                        searchQuery = it
                        selectedSection = it.uppercase()
                    },
                    label = { Text("Selected Section / Search", fontSize = 10.5.sp) },
                    placeholder = { Text("e.g. D54A, M23, 2x1 PIPE", fontSize = 10.5.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText,
                        focusedContainerColor = BossCarbonPanel2,
                        unfocusedContainerColor = BossCarbonPanel2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Options (${filteredSections.size}):", color = BossTextDim, fontSize = 10.5.sp)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BossCarbonPanel2, RoundedCornerShape(6.dp))
                        .padding(5.dp)
                ) {
                    items(filteredSections.size) { i ->
                        val sec = filteredSections[i]
                        val isSelected = selectedSection.equals(sec, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (isSelected) BossGold else BossCarbonPanel,
                            border = BorderStroke(1.dp, if (isSelected) BossGold else BossCarbonBorder),
                            modifier = Modifier.clickable {
                                selectedSection = sec
                                searchQuery = ""
                            }
                        ) {
                            Text(
                                text = sec,
                                color = if (isSelected) Color(0xFF161208) else BossText,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gauge:", color = BossTextDim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableGauges.forEach { g ->
                            val isSel = selectedGauge == g
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedGauge = g },
                                label = { Text("${g}mm", fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BossSteel,
                                    selectedLabelColor = Color.White,
                                    containerColor = BossCarbonPanel2,
                                    labelColor = BossText
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = { lengthText = it },
                        label = { Text("Length (Feet)", fontSize = 10.5.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Quantity", fontSize = 10.5.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick:", color = BossTextDim, fontSize = 9.5.sp)
                    listOf("8", "10", "12", "14", "16", "18", "20").forEach { ft ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (lengthText == ft) BossGold.copy(alpha = 0.25f) else BossCarbonPanel2,
                            border = BorderStroke(0.5.dp, if (lengthText == ft) BossGold else BossCarbonBorder),
                            modifier = Modifier.clickable { lengthText = ft }
                        ) {
                            Text(
                                text = "${ft}'",
                                color = if (lengthText == ft) BossGold else BossTextDim,
                                fontSize = 9.5.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sec = selectedSection.trim().uppercase()
                    val len = lengthText.toDoubleOrNull() ?: 10.0
                    val q = qtyText.toIntOrNull() ?: 1
                    if (sec.isNotBlank() && len > 0 && q > 0) {
                        onAdd(sec, len, q, selectedGauge)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                shape = RoundedCornerShape(6.dp),
                enabled = selectedSection.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Add to List", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BossTextDim, fontSize = 11.sp)
            }
        }
    )
}

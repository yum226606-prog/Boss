package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.theme.*

@Composable
fun ExtraPiecesSection(
    viewModel: BossAppViewModel,
    state: BossAppState,
    modifier: Modifier = Modifier,
    suggestedSections: List<String> = emptyList()
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BossCarbonPanel),
        border = BorderStroke(1.dp, BossCarbonBorder)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.projectExtras.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = BossGold.copy(alpha = 0.2f),
                            contentColor = BossGoldLight
                        ) {
                            Text(
                                text = "${state.projectExtras.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Extra Piece", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = BossCarbonBorder)

            if (state.projectExtras.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BossCarbonPanel2, RoundedCornerShape(6.dp))
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No extra pieces added. Tap '+ Add Extra Piece' if you need extra profiles or pipes for this project.",
                        color = BossTextDim,
                        fontSize = 11.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.projectExtras.forEachIndexed { idx, ex ->
                        val totalFt = ex.lengthFt * ex.qty
                        val totalInches = (ex.lengthFt * 12.0).toInt()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BossCarbonPanel2, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BossGold.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, BossGold.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = ex.section,
                                        color = BossGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${ex.lengthFt} ft (${totalInches}\") x ${ex.qty} pcs",
                                    color = BossText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "(Total: ${totalFt} ft)",
                                    color = BossTextDim,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeProjectExtra(idx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete extra piece",
                                    tint = BossRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val quickPicks = (suggestedSections.ifEmpty {
            listOf("D54A", "M23", "D51", "D41", "M24", "D42", "D45", "PIPE", "ANGLE")
        } + state.customSections).distinct()

        AddExtraPieceDialog(
            suggestedSections = quickPicks,
            onDismiss = { showAddDialog = false },
            onAdd = { sec, len, qty ->
                viewModel.addProjectExtra(sec, len, qty)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExtraPieceDialog(
    initialSection: String = "M23",
    suggestedSections: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onAdd: (section: String, lengthFt: Double, qty: Int) -> Unit
) {
    var selectedSection by remember(initialSection) { mutableStateOf(initialSection) }
    var lengthText by remember { mutableStateOf("10.0") }
    var qtyText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BossCarbonPanel,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Extension, contentDescription = null, tint = BossGold, modifier = Modifier.size(20.dp))
                Text("Add Extra Piece", color = BossGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (suggestedSections.isNotEmpty()) {
                    Text("Quick Pick Section:", color = BossTextDim, fontSize = 11.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestedSections.forEach { sec ->
                            val isSelected = selectedSection.equals(sec, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSection = sec },
                                label = { Text(sec, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BossGold,
                                    selectedLabelColor = Color(0xFF161208),
                                    containerColor = BossCarbonPanel2,
                                    labelColor = BossText
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedSection,
                    onValueChange = { selectedSection = it.uppercase() },
                    label = { Text("Section (e.g. D54A, D41, M23, PIPE)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText
                    )
                )
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Length in Feet (e.g. 10.0)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText
                    )
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Quantity", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BossGold,
                        unfocusedBorderColor = BossCarbonBorder,
                        focusedTextColor = BossText,
                        unfocusedTextColor = BossText
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val len = lengthText.toDoubleOrNull() ?: 1.0
                    val q = qtyText.toIntOrNull() ?: 1
                    if (selectedSection.isNotBlank() && len > 0 && q > 0) {
                        onAdd(selectedSection, len, q)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BossGold, contentColor = Color(0xFF161208))
            ) {
                Text("Add to Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BossTextDim)
            }
        }
    )
}

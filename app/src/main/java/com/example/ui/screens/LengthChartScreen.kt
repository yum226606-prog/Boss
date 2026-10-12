package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BossCalculationEngine
import com.example.model.BossDefaultRates
import com.example.model.CutPiece
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.theme.*
import com.example.util.ChartPdfGenerator
import com.example.util.ChartPrintHelper

@Composable
fun LengthChartScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    val context = LocalContext.current
    val allPieces = remember(state) { viewModel.getAllCutPieces() }
    val visiblePieces = remember(allPieces, state.hiddenSections) {
        allPieces.filter { !state.hiddenSections.contains(it.section) }
    }
    val sectionMap = remember(visiblePieces) {
        val map = mutableMapOf<String, MutableList<CutPiece>>()
        visiblePieces.forEach { p ->
            map.getOrPut(p.section) { mutableListOf() }.add(p)
        }
        map
    }

    val currentTheme = LocalBossTheme.current
    val fontScale = currentTheme.fontScale.scale
    val cardShape = RoundedCornerShape(currentTheme.chartCornerRadius)
    val buttonShape = RoundedCornerShape(currentTheme.buttonCornerRadius)
    val spacing = if (currentTheme.compactChartMode) 8.dp else 14.dp
    val padding = if (currentTheme.compactChartMode) 8.dp else 12.dp
    val cardBorder = BorderStroke(
        width = if (currentTheme.chartShadowStyle == com.example.model.ChartShadowStyle.NEON_AMBIENT_GLOW) 1.5.dp else 1.dp,
        color = if (currentTheme.chartShadowStyle == com.example.model.ChartShadowStyle.NEON_AMBIENT_GLOW) currentTheme.primaryColor else currentTheme.dividerColor
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundColor)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        item {
            Surface(
                shape = cardShape,
                color = currentTheme.surfaceColor,
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bar Length Chart & Stock Tally",
                            color = currentTheme.primaryColor,
                            fontSize = (15f * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentTheme.fontFamily
                        )
                        if (state.hiddenSections.isNotEmpty() && sectionMap.isEmpty()) {
                            TextButton(onClick = { viewModel.unhideAllSections() }) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = currentTheme.primaryColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unhide All (${state.hiddenSections.size})", color = currentTheme.primaryColor, fontSize = (11f * fontScale).sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Total stock bars required for the job. Single cuts up to 11ft are shown as exact offcut pieces instead of full bars.",
                        color = currentTheme.textSecondaryColor,
                        fontSize = (11f * fontScale).sp,
                        fontFamily = currentTheme.fontFamily
                    )

                    if (sectionMap.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { ChartPrintHelper.printLengthChart(context, state, sectionMap) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor, contentColor = if (currentTheme.preset == com.example.model.ThemePreset.WHITE) Color.White else Color(0xFF161208)),
                                shape = buttonShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print Length Chart", modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print", fontSize = (12f * fontScale).sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { ChartPdfGenerator.saveAndOpenLengthChartPdf(context, state, sectionMap) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.surfaceVariantColor, contentColor = currentTheme.textPrimaryColor),
                                shape = buttonShape,
                                border = BorderStroke(1.dp, currentTheme.dividerColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Save PDF", modifier = Modifier.size(17.dp), tint = currentTheme.primaryColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF", fontSize = (12f * fontScale).sp, fontWeight = FontWeight.Bold)
                            }
                            if (state.hiddenSections.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.unhideAllSections() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.primaryColor),
                                    border = BorderStroke(1.dp, currentTheme.primaryColor),
                                    shape = buttonShape
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = currentTheme.primaryColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unhide (${state.hiddenSections.size})", fontSize = (11f * fontScale).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sectionMap.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("First add items in the Windows or Doors & Frames tab.", color = BossTextDim, fontSize = 13.sp)
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceColor),
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "TOTAL BARS SUMMARY",
                            color = currentTheme.primaryColor,
                            fontSize = (13f * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentTheme.fontFamily
                        )

                        sectionMap.entries.toList().forEachIndexed { index, (section, pieces) ->
                            val lens = BossCalculationEngine.barLengthsFor(section)
                            val packed = BossCalculationEngine.packSection(pieces, lens)
                            val label = BossDefaultRates.SECTION_LABELS[section]
                            val sectionHeader = if (!label.isNullOrBlank() && !label.equals(section, ignoreCase = true)) {
                                "$section - $label"
                            } else {
                                section
                            }
                            val rowBg = if (currentTheme.showZebraStripes && index % 2 == 1) currentTheme.alternateRowColor else currentTheme.surfaceVariantColor.copy(alpha = 0.5f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = currentTheme.rowHorizontalPadding, vertical = currentTheme.rowVerticalPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sectionHeader,
                                    color = currentTheme.textPrimaryColor,
                                    fontSize = (12f * fontScale).sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    packed.tally.entries.sortedByDescending { it.key }.forEach { (lenFt, tallyInfo) ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (tallyInfo.isCustom) Color.Transparent else currentTheme.primaryColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, if (tallyInfo.isCustom) currentTheme.textSecondaryColor else currentTheme.primaryColor)
                                        ) {
                                            Text(
                                                text = "${lenFt}FT x ${tallyInfo.count}${if (tallyInfo.isCustom) " (offcut)" else ""}",
                                                color = currentTheme.primaryColor,
                                                fontSize = (10f * fontScale).sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (currentTheme.showCellBorders && index < sectionMap.size - 1) {
                                HorizontalDivider(color = currentTheme.dividerColor.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }

            sectionMap.forEach { (section, pieces) ->
                val lens = BossCalculationEngine.barLengthsFor(section)
                val packed = BossCalculationEngine.packSection(pieces, lens)
                val label = BossDefaultRates.SECTION_LABELS[section]
                val sectionHeader = if (!label.isNullOrBlank() && !label.equals(section, ignoreCase = true)) {
                    "$section - $label"
                } else {
                    section
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(currentTheme.surfaceColor, cardShape)
                            .border(cardBorder, cardShape)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$sectionHeader Details",
                            color = currentTheme.primaryColor,
                            fontSize = (14f * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentTheme.fontFamily
                        )

                        packed.bins.forEachIndexed { barIdx, bar ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceVariantColor),
                                border = BorderStroke(1.dp, currentTheme.dividerColor)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(currentTheme.tableHeaderBackground)
                                            .padding(horizontal = currentTheme.rowHorizontalPadding, vertical = currentTheme.rowVerticalPadding),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (bar.isCustom) "${bar.lenFt} ft Offcut - Bar #${barIdx + 1}" else "${bar.lenFt} ft Bar - Bar #${barIdx + 1}",
                                            color = currentTheme.tableHeaderTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (12f * fontScale).sp,
                                            fontFamily = if (currentTheme.tableHeaderStyle == com.example.model.TableHeaderStyle.CYBER_TERMINAL) FontFamily.Monospace else currentTheme.fontFamily
                                        )
                                        Text(
                                            text = "Waste: ${BossCalculationEngine.toDotSutar(bar.remainInches.coerceAtLeast(0.0))}\"",
                                            color = currentTheme.primaryColor,
                                            fontSize = (11f * fontScale).sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (currentTheme.showCellBorders) {
                                        HorizontalDivider(color = currentTheme.dividerColor)
                                    }

                                    bar.pieces.forEachIndexed { pIdx, p ->
                                        val rowBg = if (currentTheme.showZebraStripes && pIdx % 2 == 1) currentTheme.alternateRowColor else Color.Transparent
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(rowBg)
                                                .padding(horizontal = currentTheme.rowHorizontalPadding, vertical = currentTheme.rowVerticalPadding),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${p.source} (${p.label})",
                                                color = currentTheme.textPrimaryColor,
                                                fontSize = (11f * fontScale).sp
                                            )
                                            Text(
                                                text = "${BossCalculationEngine.toDotSutar(p.length)}\"",
                                                color = currentTheme.primaryColor,
                                                fontSize = (12f * fontScale).sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (currentTheme.showCellBorders && pIdx < bar.pieces.size - 1) {
                                            HorizontalDivider(color = currentTheme.dividerColor.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

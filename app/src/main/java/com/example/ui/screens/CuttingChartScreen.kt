@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun CuttingChartScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    val context = LocalContext.current
    val allPieces = remember(state) { viewModel.getAllCutPieces() }
    val glassCuts = remember(state) { viewModel.getGlassCuts() }

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
                            text = "Cutting Chart - Easy Reading",
                            color = currentTheme.primaryColor,
                            fontSize = (15f * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentTheme.fontFamily
                        )
                        if (state.hiddenSections.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.unhideAllSections() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = currentTheme.primaryColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unhide All (${state.hiddenSections.size})", color = currentTheme.primaryColor, fontSize = (11f * fontScale).sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Each stock bar is displayed on its own card. Cut lengths are shown in bold with corresponding Window/Door references.",
                        color = currentTheme.textSecondaryColor,
                        fontSize = (11f * fontScale).sp,
                        fontFamily = currentTheme.fontFamily
                    )

                    if (sectionMap.isNotEmpty() || glassCuts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { ChartPrintHelper.printCuttingChart(context, state, sectionMap, glassCuts) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor, contentColor = if (currentTheme.preset == com.example.model.ThemePreset.WHITE) Color.White else Color(0xFF161208)),
                                shape = buttonShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print Cutting Chart", modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print", fontSize = (12f * fontScale).sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { ChartPdfGenerator.saveAndOpenCuttingChartPdf(context, state, sectionMap, glassCuts) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.surfaceVariantColor, contentColor = currentTheme.textPrimaryColor),
                                shape = buttonShape,
                                border = BorderStroke(1.dp, currentTheme.dividerColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Save PDF", modifier = Modifier.size(17.dp), tint = currentTheme.primaryColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF", fontSize = (12f * fontScale).sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // One-by-one Unhide Section Chips
                    if (state.hiddenSections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = currentTheme.surfaceVariantColor.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, currentTheme.primaryColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
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
                                    Text(
                                        text = "Hidden Sections (Tap any section to unhide one-by-one):",
                                        color = currentTheme.primaryColor,
                                        fontSize = (11f * fontScale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Unhide All",
                                        color = Color(0xFF38BDF8),
                                        fontSize = (11f * fontScale).sp,
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
                                        val secLabel = BossDefaultRates.SECTION_LABELS[hiddenSec]
                                        val chipText = if (!secLabel.isNullOrBlank() && !secLabel.equals(hiddenSec, ignoreCase = true)) {
                                            "$hiddenSec ($secLabel)"
                                        } else {
                                            hiddenSec
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = currentTheme.surfaceColor,
                                            border = BorderStroke(1.dp, currentTheme.primaryColor),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { viewModel.unhideSection(hiddenSec) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Visibility,
                                                    contentDescription = "Unhide $hiddenSec",
                                                    tint = currentTheme.primaryColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = chipText,
                                                    color = currentTheme.textPrimaryColor,
                                                    fontSize = (11f * fontScale).sp,
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
                            .background(BossCarbonPanel, RoundedCornerShape(10.dp))
                            .border(1.dp, BossCarbonBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sectionHeader,
                                color = BossGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${packed.bins.size} bar(s)",
                                    color = BossTextDim,
                                    fontSize = 11.sp
                                )
                                IconButton(
                                    onClick = { viewModel.hideSection(section) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = "Hide section $section",
                                        tint = BossTextDim,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        packed.bins.forEachIndexed { barIdx, bar ->
                            val barTitle = if (bar.isCustom) {
                                "${BossCalculationEngine.formatDim(bar.lenFt * 12.0, state.measurementUnit)} OFFCUT"
                            } else {
                                "${bar.lenFt} FT BAR"
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape,
                                colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceVariantColor),
                                border = cardBorder
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
                                            text = "$barTitle - BAR #${barIdx + 1}",
                                            color = currentTheme.tableHeaderTextColor,
                                            fontSize = (11f * fontScale).sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (currentTheme.tableHeaderStyle == com.example.model.TableHeaderStyle.CYBER_TERMINAL) FontFamily.Monospace else currentTheme.fontFamily
                                        )
                                        Text(
                                            text = "Waste: ${BossCalculationEngine.formatDim(bar.remainInches.coerceAtLeast(0.0), state.measurementUnit)}",
                                            color = currentTheme.primaryColor,
                                            fontSize = (10f * fontScale).sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    val indexedPieceRows = bar.pieces.withIndex().chunked(2)
                                    indexedPieceRows.forEachIndexed { rowIdx, pair ->
                                        val rowBg = if (currentTheme.showZebraStripes && rowIdx % 2 == 1) currentTheme.alternateRowColor else Color.Transparent
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(rowBg)
                                                .height(IntrinsicSize.Min),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            pair.forEachIndexed { colIdx, indexedPiece ->
                                                val pieceIdx = indexedPiece.index
                                                val piece = indexedPiece.value
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = currentTheme.primaryColor.copy(alpha = 0.16f),
                                                                border = BorderStroke(0.6.dp, currentTheme.primaryColor.copy(alpha = 0.6f))
                                                            ) {
                                                                Text(
                                                                    text = "#${pieceIdx + 1} • ${piece.source}",
                                                                    color = currentTheme.primaryColor,
                                                                    fontSize = (9.5f * fontScale).sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = piece.label,
                                                            color = currentTheme.textSecondaryColor,
                                                            fontSize = (9.5f * fontScale).sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Text(
                                                        text = BossCalculationEngine.formatDim(piece.length, state.measurementUnit),
                                                        color = currentTheme.primaryColor,
                                                        fontSize = (13.5f * fontScale).sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                if (colIdx == 0) {
                                                    VerticalDivider(
                                                        color = currentTheme.dividerColor.copy(alpha = 0.6f),
                                                        thickness = 1.dp,
                                                        modifier = Modifier.fillMaxHeight()
                                                    )
                                                }
                                            }
                                            if (pair.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp))
                                            }
                                        }
                                        if (currentTheme.showCellBorders && rowIdx < indexedPieceRows.size - 1) {
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

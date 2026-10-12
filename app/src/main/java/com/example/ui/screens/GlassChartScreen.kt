package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BossCalculationEngine
import com.example.ui.BossAppState
import com.example.ui.BossAppViewModel
import com.example.ui.theme.*
import com.example.util.ChartPdfGenerator
import com.example.util.ChartPrintHelper
import java.util.Locale

@Composable
fun GlassChartScreen(
    viewModel: BossAppViewModel,
    state: BossAppState
) {
    val context = LocalContext.current
    val glassCuts = remember(state) { viewModel.getGlassCuts() }
    val totalSqft = remember(glassCuts) { glassCuts.sumOf { it.sqft } }
    val totalPieces = remember(glassCuts) { glassCuts.sumOf { it.pieces } }
    val groupedByType = remember(glassCuts) {
        glassCuts.groupBy { "${it.glassGauge}mm ${it.glassColor}" }
    }

    val currentTheme = LocalBossTheme.current
    val fontScale = currentTheme.fontScale.scale

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06090F))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Card matching Screenshot 5 (Glass Cutting & Billed Chart)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0B0F17),
                border = BorderStroke(1.dp, Color(0xFF1E2636)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Glass Cutting & Billed Chart",
                            color = Color(0xFFF5B82E),
                            fontSize = (17f * fontScale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Billed size rounds UP to next 6-inch (half-foot) step",
                            color = Color(0xFF94A3B8),
                            fontSize = (11.5f * fontScale).sp
                        )
                    }

                    // 3 Action Buttons Row: Print (Gold), PDF (Sky Blue), Share (Dark Gold Border)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (glassCuts.isEmpty()) {
                                    Toast.makeText(context, "Add windows or doors with glass first", Toast.LENGTH_SHORT).show()
                                } else {
                                    ChartPrintHelper.printGlassChart(context, state, glassCuts, totalSqft)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF5B82E),
                                contentColor = Color(0xFF161208)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Print",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Print",
                                fontSize = (13f * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (glassCuts.isEmpty()) {
                                    Toast.makeText(context, "Add windows or doors with glass first", Toast.LENGTH_SHORT).show()
                                } else {
                                    ChartPdfGenerator.saveAndOpenGlassChartPdf(context, state, glassCuts, totalSqft)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF60A5FA),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PDF",
                                fontSize = (13f * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (glassCuts.isEmpty()) {
                                    Toast.makeText(context, "Add windows or doors with glass first", Toast.LENGTH_SHORT).show()
                                } else {
                                    ChartPdfGenerator.shareGlassChartPdf(context, state, glassCuts, totalSqft)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF0B0F17),
                                contentColor = Color(0xFFF5B82E)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFD9A441)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFFF5B82E),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share",
                                color = Color(0xFFF5B82E),
                                fontSize = (12.5f * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Glass Fitting Type Container (Fix Rubber (Same) vs U(PVC))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0E131D),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Glass Fitting Type",
                                    color = Color(0xFFF5B82E),
                                    fontSize = (12.5f * fontScale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (state.glassFittingType == "UPVC") "U(PVC): Channel" else "Fix Rubber: Standard",
                                    color = Color(0xFF94A3B8),
                                    fontSize = (11f * fontScale).sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isFixRubber = state.glassFittingType != "UPVC"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isFixRubber) Color(0xFFF5B82E) else Color(0xFF070A0F),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isFixRubber) Color(0xFFF5B82E) else Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setGlassFittingType("FIX_RUBBER") }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "Fix Rubber (Same)",
                                            color = if (isFixRubber) Color(0xFF161208) else Color.White,
                                            fontSize = (12f * fontScale).sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val isUpvc = state.glassFittingType == "UPVC"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isUpvc) Color(0xFFF5B82E) else Color(0xFF070A0F),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isUpvc) Color(0xFFF5B82E) else Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setGlassFittingType("UPVC") }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "U(PVC)",
                                            color = if (isUpvc) Color(0xFF161208) else Color.White,
                                            fontSize = (12f * fontScale).sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3 Summary Stat Boxes Row (Total Glass Sq.ft, Total Pieces, Frames with Glass)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0E131D),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Total Glass Sq.ft",
                                    color = Color(0xFF94A3B8),
                                    fontSize = (10f * fontScale).sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2f", totalSqft),
                                    color = Color(0xFFF5B82E),
                                    fontSize = (17f * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0E131D),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Total Pieces",
                                    color = Color(0xFF94A3B8),
                                    fontSize = (10f * fontScale).sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "$totalPieces",
                                    color = Color(0xFF60A5FA),
                                    fontSize = (17f * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0E131D),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Frames with Glass",
                                    color = Color(0xFF94A3B8),
                                    fontSize = (10f * fontScale).sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${glassCuts.size}",
                                    color = Color(0xFFF5B82E),
                                    fontSize = (17f * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        if (glassCuts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B0F17),
                    border = BorderStroke(1.dp, Color(0xFF1E2636)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp)
                    ) {
                        Text(
                            text = "Add windows or doors with glass enabled in the Windows or Doors & Frames tab.",
                            color = Color(0xFFCBD5E1),
                            fontSize = (13f * fontScale).sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B0F17),
                    border = BorderStroke(1.dp, Color(0xFF1E2636)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF111827))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WINDOW / FRAME",
                                color = Color(0xFF94A3B8),
                                fontSize = (10f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                text = "GLASS TYPE",
                                color = Color(0xFF94A3B8),
                                fontSize = (10f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = "CUT SIZE (W x H)",
                                color = Color(0xFF94A3B8),
                                fontSize = (10f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.5f)
                            )
                            Text(
                                text = "BILLED (FT)",
                                color = Color(0xFF94A3B8),
                                fontSize = (10f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.0f)
                            )
                            Text(
                                text = "PCS",
                                color = Color(0xFF94A3B8),
                                fontSize = (10f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.55f)
                            )
                            Text(
                                text = "SQ.FT",
                                color = Color(0xFF94A3B8),
                                fontSize = (10f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.85f)
                            )
                        }

                        HorizontalDivider(color = Color(0xFF1E2636))

                        glassCuts.forEachIndexed { idx, cut ->
                            val rowBg = if (idx % 2 == 1) Color(0xFF0E1420) else Color.Transparent
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = cut.frameLabel,
                                        color = Color.White,
                                        fontSize = (11.5f * fontScale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = BossCalculationEngine.formatDim(
                                            BossCalculationEngine.parseInSutar(cut.frameDim.substringBefore("x").replace("\"", "").trim()),
                                            state.measurementUnit
                                        ) + " x " + BossCalculationEngine.formatDim(
                                            BossCalculationEngine.parseInSutar(cut.frameDim.substringAfter("x").replace("\"", "").trim()),
                                            state.measurementUnit
                                        ),
                                        color = Color(0xFF94A3B8),
                                        fontSize = (9.5f * fontScale).sp
                                    )
                                }

                                Text(
                                    text = "${cut.glassGauge}mm ${cut.glassColor}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = (10.5f * fontScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1.1f)
                                )

                                Text(
                                    text = "${BossCalculationEngine.formatDim(cut.gWpanel, state.measurementUnit)} x ${BossCalculationEngine.formatDim(cut.gH, state.measurementUnit)}",
                                    color = Color(0xFFF5B82E),
                                    fontSize = (11.5f * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1.5f)
                                )

                                Text(
                                    text = "${String.format(Locale.US, "%.1f", cut.billedWFt)}' x ${String.format(Locale.US, "%.1f", cut.billedHFt)}'",
                                    color = Color(0xFF94A3B8),
                                    fontSize = (10.5f * fontScale).sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1.0f)
                                )

                                Text(
                                    text = "${cut.pieces}",
                                    color = Color.White,
                                    fontSize = (11.5f * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.55f)
                                )

                                Text(
                                    text = String.format(Locale.US, "%.2f", cut.sqft),
                                    color = Color(0xFF34D399),
                                    fontSize = (11.5f * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.85f)
                                )
                            }
                            HorizontalDivider(color = Color(0xFF1E2636).copy(alpha = 0.5f))
                        }

                        // Table Bottom Total Footer Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL (${glassCuts.size} Frames)",
                                color = Color(0xFFF5B82E),
                                fontSize = (11.5f * fontScale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(2.2f)
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", glassCuts.sumOf { (it.gWpanel * it.gH / 144.0) * it.pieces })} sq.ft cut",
                                color = Color(0xFF38BDF8),
                                fontSize = (10.5f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(2.2f)
                            )
                            Text(
                                text = "$totalPieces Pcs",
                                color = Color(0xFFF5B82E),
                                fontSize = (12f * fontScale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.65f)
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", totalSqft)} Sq.Ft",
                                color = Color(0xFF34D399),
                                fontSize = (12f * fontScale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.95f)
                            )
                        }
                    }
                }
            }

            // Always show the Bottom Glass Total & Cost Summary Card
            item {
                val totalActualCutSqft = remember(glassCuts) {
                    glassCuts.sumOf { (it.gWpanel * it.gH / 144.0) * it.pieces }
                }
                val totalGlassCost = remember(glassCuts, state.glassRates) {
                    glassCuts.sumOf { c ->
                        val rate = state.glassRates[c.glassGauge]?.get(c.glassColor)
                            ?: if (c.glassGauge == "8") 390.0 else 230.0
                        c.sqft * rate
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0E1525),
                    border = BorderStroke(1.5.dp, Color(0xFFF5B82E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GLASS CHART GRAND TOTAL SUMMARY",
                                color = Color(0xFFF5B82E),
                                fontSize = (13.5f * fontScale).sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF064E3B),
                                border = BorderStroke(1.dp, Color(0xFF34D399))
                            ) {
                                Text(
                                    text = "$totalPieces Total Pcs",
                                    color = Color(0xFF34D399),
                                    fontSize = (11f * fontScale).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B))

                        groupedByType.forEach { (typeLabel, cuts) ->
                            val pcs = cuts.sumOf { it.pieces }
                            val area = cuts.sumOf { it.sqft }
                            val sample = cuts.firstOrNull()
                            val unitRate = if (sample != null) {
                                state.glassRates[sample.glassGauge]?.get(sample.glassColor)
                                    ?: if (sample.glassGauge == "8") 390.0 else 230.0
                            } else 230.0
                            val typeCost = area * unitRate
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = typeLabel,
                                        color = Color.White,
                                        fontSize = (12f * fontScale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Rate: Rs. ${unitRate.toInt()}/Sq.Ft",
                                        color = Color(0xFF94A3B8),
                                        fontSize = (10f * fontScale).sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$pcs Pcs  •  ${String.format(Locale.US, "%.2f", area)} Sq.Ft",
                                        color = Color(0xFF38BDF8),
                                        fontSize = (12f * fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Rs. ${"%,.0f".format(typeCost)}",
                                        color = Color(0xFFF5B82E),
                                        fontSize = (11f * fontScale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Actual Cut Glass Area:",
                                color = Color(0xFF94A3B8),
                                fontSize = (11.5f * fontScale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", totalActualCutSqft)} Sq.Ft",
                                color = Color(0xFF38BDF8),
                                fontSize = (12f * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Billed Glass Area:",
                                color = Color.White,
                                fontSize = (12.5f * fontScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", totalSqft)} Sq.Ft",
                                color = Color(0xFF34D399),
                                fontSize = (13.5f * fontScale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFFF5B82E).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ESTIMATED TOTAL GLASS AMOUNT:",
                                    color = Color(0xFFF5B82E),
                                    fontSize = (12f * fontScale).sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Rs. ${"%,.0f".format(totalGlassCost)}",
                                    color = Color(0xFFF5B82E),
                                    fontSize = (14.5f * fontScale).sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

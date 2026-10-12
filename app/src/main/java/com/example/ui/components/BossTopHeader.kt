package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BossDefaultRates
import com.example.ui.screens.AddCustomSectionDialog
import com.example.ui.theme.*

@Composable
fun BossTopHeader(
    ctxGauge: String,
    ctxGaugeMode: String = "ON",
    ctxColor: String,
    ctxGlassGauge: String,
    ctxGlassColor: String,
    ctxGlassMode: String = "ON",
    ctxFrameType: String,
    availableGauges: List<String> = BossDefaultRates.GAUGES,
    availableColors: List<String>,
    availableGlassGauges: List<String> = listOf("5", "8"),
    availableGlassColors: List<String> = BossDefaultRates.DEFAULT_GLASS_COLORS,
    availableGlassColors5mm: List<String> = BossDefaultRates.DEFAULT_GLASS_COLORS_5MM,
    availableGlassColors8mm: List<String> = BossDefaultRates.DEFAULT_GLASS_COLORS_8MM,
    availableGlassColorsByGauge: Map<String, List<String>> = emptyMap(),
    customSections: List<String> = emptyList(),
    customLogoPath: String? = null,
    companyName: String? = null,
    companyTagline: String? = null,
    isVipOwner: Boolean = false,
    onLogoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onGaugeSelected: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onFrameTypeSelected: (String) -> Unit,
    onGlassSelected: (gauge: String, color: String, mode: String) -> Unit = { _, _, _ -> },
    measurementUnit: String = "INCH_SUTAR",
    onUnitSelected: (String) -> Unit = {},
    onAddCustomGauge: (String) -> Unit = {},
    onRemoveCustomGauge: (String) -> Unit = {},
    onAddCustomColor: (String) -> Unit = {},
    onRemoveCustomColor: (String) -> Unit = {},
    onAddCustomGlassGauge: (String) -> Unit = {},
    onRemoveGlassGauge: (String) -> Unit = {},
    onAddCustomGlassColor: (String, String) -> Unit = { _, _ -> },
    onRemoveGlassColor: (String, String) -> Unit = { _, _ -> },
    onAddNewSection: (String, Double) -> Unit = { _, _ -> },
    onAddNewSectionPair: (String, Double, String, Double) -> String = { s1, _, _, _ -> s1 },
    onRemoveCustomSection: (String) -> Unit = {},
    onThemeClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    val c = currentBossColors()
    var showGaugeMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showFrameMenu by remember { mutableStateOf(false) }
    var showGlassMenu by remember { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }
    var showAddGaugeDialog by remember { mutableStateOf(false) }
    var showAddColorDialog by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showAddGlassGaugeDialog by remember { mutableStateOf(false) }
    var showAddGlassColorGauge by remember { mutableStateOf<String?>(null) }
    var newColorInput by remember { mutableStateOf("") }
    var newGaugeInput by remember { mutableStateOf("") }
    var newGlassGaugeInput by remember { mutableStateOf("") }

    val statusBarInsets = WindowInsets.statusBars.asPaddingValues()
    val topPadding = maxOf(statusBarInsets.calculateTopPadding(), 16.dp) + 2.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = c.panel,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = topPadding, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val customLogoBitmap = remember(customLogoPath) {
                        if (customLogoPath != null) {
                            val file = java.io.File(customLogoPath)
                            if (file.exists()) {
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                            } else null
                        } else null
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable { onLogoClick() }
                            .border(2.dp, c.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customLogoBitmap != null) {
                            Image(
                                bitmap = customLogoBitmap,
                                contentDescription = "App Logo",
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
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        val activeName = if (!companyName.isNullOrBlank() && (isVipOwner || companyName != "BOSS ALUMINIUM & GLASS")) {
                            companyName
                        } else if (isVipOwner) {
                            "BOSS ALUMINIUM & GLASS"
                        } else {
                            "YOUR SHOP / BUSINESS NAME"
                        }
                        val activeTagline = if (!companyTagline.isNullOrBlank()) companyTagline else (if (isVipOwner) "STRONGER | SMARTER | BETTER" else "Aluminium & Glass Fabrication Works")

                        Text(
                            text = activeName,
                            color = c.primary,
                            fontSize = if (activeName.length > 20) 13.5.sp else 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeTagline,
                            color = c.primaryLight.copy(alpha = 0.9f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Button(
                        onClick = onDoneClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.primary,
                            contentColor = Color(0xFF161208)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onThemeClick,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c.panel2)
                            .border(1.dp, c.primary.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Themes & Studio",
                            tint = c.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c.panel2)
                            .border(1.dp, c.primary.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh App",
                            tint = c.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayGauge = if (ctxGaugeMode == "OFF") "OFF" else "${ctxGauge}mm"
                val displayColor = when {
                    ctxColor.contains("H23", ignoreCase = true) || ctxColor.contains("RAL", ignoreCase = true) -> "RAL"
                    ctxColor.contains("SAHARA", ignoreCase = true) -> "SAHARA"
                    ctxColor.contains("BLACK", ignoreCase = true) -> "BLACK"
                    ctxColor.contains("WOOD", ignoreCase = true) -> "WOOD"
                    ctxColor.contains("DULL", ignoreCase = true) -> "DULL"
                    ctxColor.contains("CHAMP", ignoreCase = true) -> "CHAMP"
                    ctxColor.contains("WHITE", ignoreCase = true) -> "WHITE"
                    ctxColor.length > 7 -> ctxColor.take(6) + ".."
                    else -> ctxColor
                }
                val displayFrame = if (ctxFrameType == "OFF") "OFF" else (if (ctxFrameType == "C" || ctxFrameType == "F") "Frame $ctxFrameType" else ctxFrameType)
                val shortGlassColor = when {
                    ctxGlassColor.startsWith("REFL", ignoreCase = true) -> "Refl"
                    ctxGlassColor.startsWith("CLEAR", ignoreCase = true) -> "Clear"
                    ctxGlassColor.startsWith("BRONZ", ignoreCase = true) -> "Brnz"
                    ctxGlassColor.startsWith("GREE", ignoreCase = true) -> "Grn"
                    ctxGlassColor.startsWith("BLUE", ignoreCase = true) -> "Blue"
                    ctxGlassColor.startsWith("GREY", ignoreCase = true) -> "Grey"
                    ctxGlassColor.length > 4 -> ctxGlassColor.take(4)
                    else -> ctxGlassColor
                }
                val displayGlass = if (ctxGlassMode == "OFF") "OFF" else "${ctxGlassGauge}m $shortGlassColor"

                // 1. Gauge chip
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showGaugeMenu = true },
                        color = c.panel2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, c.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayGauge,
                                color = if (ctxGaugeMode == "OFF") c.textDim else c.text,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showGaugeMenu,
                        onDismissRequest = { showGaugeMenu = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                    ) {
                        DropdownMenuItem(
                            text = { Text("OFF - Frame Gauge", color = c.textDim, fontWeight = if (ctxGaugeMode == "OFF") FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showGaugeMenu = false
                                onGaugeSelected("OFF")
                            }
                        )
                        HorizontalDivider()
                        availableGauges.forEach { g ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${g} mm",
                                            fontWeight = if (ctxGaugeMode == "ON" && g == ctxGauge) FontWeight.Bold else FontWeight.Normal,
                                            color = if (ctxGaugeMode == "ON" && g == ctxGauge) c.primary else c.text
                                        )
                                        if (availableGauges.size > 1) {
                                            IconButton(
                                                onClick = { onRemoveCustomGauge(g) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete ${g}mm",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    showGaugeMenu = false
                                    onGaugeSelected(g)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add Custom Gauge...", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            onClick = {
                                showGaugeMenu = false
                                newGaugeInput = ""
                                showAddGaugeDialog = true
                            }
                        )
                    }
                }

                // 2. Color chip
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showColorMenu = true },
                        color = c.panel2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, c.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayColor,
                                color = c.primaryLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showColorMenu,
                        onDismissRequest = { showColorMenu = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                    ) {
                        availableColors.forEach { colorItem ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            colorItem,
                                            fontWeight = if (colorItem == ctxColor) FontWeight.Bold else FontWeight.Normal,
                                            color = if (colorItem == ctxColor) c.primary else c.text
                                        )
                                        if (availableColors.size > 1) {
                                            IconButton(
                                                onClick = { onRemoveCustomColor(colorItem) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete $colorItem",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    showColorMenu = false
                                    onColorSelected(colorItem)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add New Color...", color = c.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showColorMenu = false
                                newColorInput = ""
                                showAddColorDialog = true
                            }
                        )
                    }
                }

                // 3. Frame Type Chip
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showFrameMenu = true },
                        color = c.panel2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, c.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayFrame,
                                color = c.accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFrameMenu,
                        onDismissRequest = { showFrameMenu = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                    ) {
                        DropdownMenuItem(
                            text = { Text("OFF - Frame List", color = c.textDim, fontWeight = if (ctxFrameType == "OFF") FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showFrameMenu = false
                                onFrameTypeSelected("OFF")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("DC30C / DC26C", fontWeight = if (ctxFrameType == "C") FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showFrameMenu = false
                                onFrameTypeSelected("C")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("DC30F / DC26F", fontWeight = if (ctxFrameType == "F") FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showFrameMenu = false
                                onFrameTypeSelected("F")
                            }
                        )
                        if (customSections.isNotEmpty()) {
                            HorizontalDivider()
                            customSections.forEach { sec ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sec, fontWeight = if (ctxFrameType == sec) FontWeight.Bold else FontWeight.Normal)
                                            IconButton(
                                                onClick = { onRemoveCustomSection(sec) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete $sec",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        showFrameMenu = false
                                        onFrameTypeSelected(sec)
                                    }
                                )
                            }
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = c.primary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Add New Section...", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showFrameMenu = false
                                showAddSectionDialog = true
                            }
                        )
                    }
                }

                // 4. Glass Dropdown selector
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showGlassMenu = true },
                        color = c.panel2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, c.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayGlass,
                                color = if (ctxGlassMode == "OFF") c.textDim else c.primaryLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showGlassMenu,
                        onDismissRequest = { showGlassMenu = false },
                        properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
                    ) {
                        DropdownMenuItem(
                            text = { Text("OFF - Frame Glass", color = c.textDim, fontWeight = if (ctxGlassMode == "OFF") FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showGlassMenu = false
                                onGlassSelected("OFF", "", "OFF")
                            }
                        )
                        HorizontalDivider()
                        val effectiveGlassGauges = availableGlassGauges.ifEmpty { listOf("5", "8") }
                        effectiveGlassGauges.forEachIndexed { idx, g ->
                            val colorList = availableGlassColorsByGauge[g]
                                ?: if (g == "8") availableGlassColors8mm else availableGlassColors5mm
                            if (effectiveGlassGauges.size > 1) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${g}mm Glass Gauge",
                                                color = c.primary,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.5.sp
                                            )
                                            IconButton(
                                                onClick = { onRemoveGlassGauge(g) },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete ${g}mm Gauge",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        val firstCol = colorList.firstOrNull() ?: "CLEAR"
                                        showGlassMenu = false
                                        onGlassSelected(g, firstCol, "ON")
                                    }
                                )
                            }
                            colorList.forEach { gc ->
                                val isCur = ctxGlassMode == "ON" && ctxGlassGauge == g && ctxGlassColor == gc
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${g}mm - $gc",
                                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCur) c.primary else c.text
                                            )
                                            if (colorList.size > 1) {
                                                IconButton(
                                                    onClick = { onRemoveGlassColor(gc, g) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete ${g}mm $gc",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        showGlassMenu = false
                                        onGlassSelected(g, gc, "ON")
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Add ${g}mm Glass Color...", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                onClick = {
                                    showGlassMenu = false
                                    newColorInput = ""
                                    showAddGlassColorGauge = g
                                }
                            )
                            if (idx < effectiveGlassGauges.lastIndex) HorizontalDivider()
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add Custom Glass Gauge (mm)...", color = c.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            onClick = {
                                showGlassMenu = false
                                newGlassGaugeInput = ""
                                showAddGlassGaugeDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddGaugeDialog) {
        AlertDialog(
            onDismissRequest = { showAddGaugeDialog = false },
            containerColor = c.panel,
            title = { Text("Add Aluminium Gauge", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newGaugeInput,
                    onValueChange = { newGaugeInput = it },
                    label = { Text("Gauge in mm (e.g. 1.5, 1.8)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newGaugeInput.trim().removeSuffix("mm").removeSuffix("MM").trim()
                        if (trimmed.isNotEmpty()) {
                            onAddCustomGauge(trimmed)
                            onGaugeSelected(trimmed)
                        }
                        showAddGaugeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = Color.Black)
                ) {
                    Text("Add & Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGaugeDialog = false }) {
                    Text("Cancel", color = c.textDim)
                }
            }
        )
    }

    if (showAddColorDialog) {
        AlertDialog(
            onDismissRequest = { showAddColorDialog = false },
            containerColor = c.panel,
            title = { Text("Add Aluminium Color", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newColorInput,
                    onValueChange = { newColorInput = it },
                    label = { Text("Color Name (e.g. Anodized Bronze)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newColorInput.trim().uppercase()
                        if (trimmed.isNotEmpty()) {
                            onAddCustomColor(trimmed)
                            onColorSelected(trimmed)
                        }
                        showAddColorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = Color.Black)
                ) {
                    Text("Add & Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddColorDialog = false }) {
                    Text("Cancel", color = c.textDim)
                }
            }
        )
    }

    if (showAddGlassGaugeDialog) {
        AlertDialog(
            onDismissRequest = { showAddGlassGaugeDialog = false },
            containerColor = c.panel,
            title = { Text("Add Glass Gauge (mm)", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newGlassGaugeInput,
                    onValueChange = { newGlassGaugeInput = it },
                    label = { Text("Glass Gauge in mm (e.g. 6, 10, 12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newGlassGaugeInput.trim().removeSuffix("mm").removeSuffix("MM").trim()
                        if (trimmed.isNotEmpty()) {
                            onAddCustomGlassGauge(trimmed)
                            onGlassSelected(trimmed, ctxGlassColor, "ON")
                        }
                        showAddGlassGaugeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = Color.Black)
                ) {
                    Text("Add & Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGlassGaugeDialog = false }) {
                    Text("Cancel", color = c.textDim)
                }
            }
        )
    }

    if (showAddGlassColorGauge != null) {
        val g = showAddGlassColorGauge!!
        AlertDialog(
            onDismissRequest = { showAddGlassColorGauge = null },
            containerColor = c.panel,
            title = { Text("Add ${g}mm Glass Color", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newColorInput,
                    onValueChange = { newColorInput = it },
                    label = { Text("Glass Color (e.g. Frosted / Mercury)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newColorInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddCustomGlassColor(trimmed, g)
                            onGlassSelected(g, trimmed, "ON")
                        }
                        showAddGlassColorGauge = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = Color.Black)
                ) {
                    Text("Add & Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGlassColorGauge = null }) {
                    Text("Cancel", color = c.textDim)
                }
            }
        )
    }

    if (showAddSectionDialog) {
        AddCustomSectionDialog(
            onDismiss = { showAddSectionDialog = false },
            isPairModeDefault = true,
            onAdd = { secName, rate ->
                onAddNewSection(secName, rate)
                onFrameTypeSelected(secName)
                showAddSectionDialog = false
            },
            onAddPair = { s1, r1, s2, r2 ->
                val pair = onAddNewSectionPair(s1, r1, s2, r2)
                onFrameTypeSelected(pair)
                showAddSectionDialog = false
            }
        )
    }
}

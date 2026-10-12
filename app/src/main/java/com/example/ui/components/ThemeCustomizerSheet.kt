package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.ThemeConfigEngine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeCustomizerSheet(
    currentTheme: ThemeConfig,
    onThemeChange: (ThemeConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onThemeChange(currentTheme.copy(customBackgroundPath = uri.toString()))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = currentTheme.surfaceColor,
        contentColor = currentTheme.textPrimaryColor,
        modifier = modifier.testTag("theme_customizer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = currentTheme.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "THEME & SIZE STUDIO",
                            color = currentTheme.textPrimaryColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Settings, Sizing & Typography Controls",
                            color = currentTheme.textSecondaryColor,
                            fontSize = 11.sp
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val reset = ThemeConfigEngine.getPresetConfig(currentTheme.preset)
                        onThemeChange(reset)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = currentTheme.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        color = currentTheme.primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LiveStudioPreview(currentTheme = currentTheme)
            Spacer(modifier = Modifier.height(14.dp))

            SectionHeader(title = "1. Writing Clarity & Contrast", currentTheme = currentTheme)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(currentTheme.cardCornerRadius))
                    .background(currentTheme.surfaceVariantColor.copy(alpha = 0.65f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ultra-Crisp High Contrast Text",
                            color = currentTheme.textPrimaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(currentTheme.primaryColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                color = currentTheme.primaryColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(
                        text = "Forces 100% sharp pure white / jet black text for maximum readability",
                        color = currentTheme.textSecondaryColor,
                        fontSize = 11.5.sp
                    )
                }
                Switch(
                    checked = currentTheme.isHighContrastText,
                    onCheckedChange = { onThemeChange(currentTheme.copy(isHighContrastText = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentTheme.primaryColor,
                        checkedTrackColor = currentTheme.primaryColor.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Text Thickness:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                FontWeightMode.values().forEach { mode ->
                    val isSel = currentTheme.fontWeightMode == mode
                    StudioOptionChip(
                        selected = isSel,
                        label = mode.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(fontWeightMode = mode)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val currentScale = currentTheme.effectiveFontScale
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Global Text Size: ${(currentScale * 100).toInt()}%",
                    color = currentTheme.textSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (currentTheme.customFontScale != null) {
                    TextButton(
                        onClick = { onThemeChange(currentTheme.copy(customFontScale = null)) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Reset Size", color = currentTheme.primaryColor, fontSize = 11.sp)
                    }
                }
            }

            Slider(
                value = currentScale,
                onValueChange = { newScale ->
                    onThemeChange(currentTheme.copy(customFontScale = newScale))
                },
                valueRange = 0.80f..1.50f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = currentTheme.primaryColor,
                    activeTrackColor = currentTheme.primaryColor,
                    inactiveTrackColor = currentTheme.dividerColor
                )
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                FontScale.values().forEach { scale ->
                    val isSel = currentTheme.customFontScale == null && currentTheme.fontScale == scale
                    StudioOptionChip(
                        selected = isSel,
                        label = scale.label,
                        currentTheme = currentTheme,
                        onClick = {
                            onThemeChange(currentTheme.copy(fontScale = scale, customFontScale = null))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Font Family Style:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                listOf(
                    FontFamily.SansSerif to "Modern Sans",
                    FontFamily.Serif to "Classic Serif",
                    FontFamily.Monospace to "Matrix Mono"
                ).forEach { (ff, label) ->
                    val isSel = currentTheme.fontFamily == ff
                    StudioOptionChip(
                        selected = isSel,
                        label = label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(fontFamily = ff)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = currentTheme.dividerColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "2. Theme Palettes", currentTheme = currentTheme)

            val isVipGoldSelected = currentTheme.preset == ThemePreset.FULL_GOLD_VIP
            Surface(
                onClick = {
                    val vipCfg = ThemeConfigEngine.getPresetConfig(ThemePreset.FULL_GOLD_VIP)
                    onThemeChange(
                        vipCfg.copy(
                            customFontScale = currentTheme.customFontScale,
                            fontWeightMode = currentTheme.fontWeightMode,
                            isHighContrastText = currentTheme.isHighContrastText
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isVipGoldSelected) Color(0xFF2E2007) else currentTheme.surfaceVariantColor,
                border = BorderStroke(
                    width = if (isVipGoldSelected) 2.dp else 1.2.dp,
                    color = if (isVipGoldSelected) Color(0xFFFFD700) else Color(0xFFB88E30)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP",
                            tint = Color(0xFF140F03),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FULL GOLD VIP",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFD700))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ROYAL VIP",
                                    color = Color(0xFF140F03),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "24K Gold gilded luxury panels with crystal clear high-contrast white & black text",
                            color = if (isVipGoldSelected) Color(0xFFFDE68A) else currentTheme.textSecondaryColor,
                            fontSize = 11.5.sp
                        )
                    }
                    if (isVipGoldSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreset.values().filter { it != ThemePreset.FULL_GOLD_VIP }.forEach { preset ->
                    val isSel = currentTheme.preset == preset
                    StudioOptionChip(
                        selected = isSel,
                        label = preset.displayName,
                        currentTheme = currentTheme,
                        onClick = {
                            val presetCfg = ThemeConfigEngine.getPresetConfig(preset)
                            onThemeChange(
                                presetCfg.copy(
                                    customFontScale = currentTheme.customFontScale,
                                    fontWeightMode = currentTheme.fontWeightMode,
                                    isHighContrastText = currentTheme.isHighContrastText
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = currentTheme.dividerColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "3. Button Style & Sizing", currentTheme = currentTheme)

            Text(
                text = "Button Visual Style:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                ButtonStyleType.values().forEach { bStyle ->
                    StudioOptionChip(
                        selected = currentTheme.buttonStyle == bStyle,
                        label = bStyle.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(buttonStyle = bStyle)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Button Size Preset:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                ButtonSize.values().forEach { bSize ->
                    val isSel = currentTheme.customButtonScale == null && currentTheme.buttonSize == bSize
                    StudioOptionChip(
                        selected = isSel,
                        label = bSize.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(buttonSize = bSize, customButtonScale = null)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val btnScale = currentTheme.effectiveButtonScale
            Text(
                text = "Button Scale: ${(btnScale * 100).toInt()}%  |  Corner Radius: ${currentTheme.buttonCornerRadius.value.toInt()}dp",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = btnScale,
                onValueChange = { onThemeChange(currentTheme.copy(customButtonScale = it)) },
                valueRange = 0.75f..1.40f,
                steps = 12,
                colors = SliderDefaults.colors(
                    thumbColor = currentTheme.primaryColor,
                    activeTrackColor = currentTheme.primaryColor,
                    inactiveTrackColor = currentTheme.dividerColor
                )
            )
            Slider(
                value = currentTheme.buttonCornerRadius.value,
                onValueChange = { onThemeChange(currentTheme.copy(buttonCornerRadius = it.dp)) },
                valueRange = 0f..28f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = currentTheme.primaryColor,
                    activeTrackColor = currentTheme.primaryColor,
                    inactiveTrackColor = currentTheme.dividerColor
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Icons Inside Buttons", color = currentTheme.textPrimaryColor, fontSize = 13.sp)
                Switch(
                    checked = currentTheme.showButtonIcons,
                    onCheckedChange = { onThemeChange(currentTheme.copy(showButtonIcons = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentTheme.primaryColor,
                        checkedTrackColor = currentTheme.primaryColor.copy(alpha = 0.35f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = currentTheme.dividerColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "4. Navigation Tab Bar Style & Position", currentTheme = currentTheme)

            Text(
                text = "Tab Bar Position:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                TabBarPosition.values().forEach { pos ->
                    StudioOptionChip(
                        selected = currentTheme.tabPosition == pos,
                        label = pos.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(tabPosition = pos)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tab Indicator Style:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                TabStyleType.values().forEach { tStyle ->
                    StudioOptionChip(
                        selected = currentTheme.tabStyle == tStyle,
                        label = tStyle.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(tabStyle = tStyle)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = currentTheme.dividerColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "5. Cards, Table Headers & Layout Density", currentTheme = currentTheme)

            Text(
                text = "Card Surface Style:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                CardStyleType.values().forEach { cStyle ->
                    StudioOptionChip(
                        selected = currentTheme.cardStyle == cStyle,
                        label = cStyle.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(cardStyle = cStyle)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Chart & Table Header Style:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                TableHeaderStyle.values().forEach { thStyle ->
                    StudioOptionChip(
                        selected = currentTheme.tableHeaderStyle == thStyle,
                        label = thStyle.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(tableHeaderStyle = thStyle)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Layout Spacing Density:",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                LayoutDensity.values().forEach { dens ->
                    StudioOptionChip(
                        selected = currentTheme.densityMode == dens,
                        label = dens.label,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(densityMode = dens)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Card Corner Radius: ${currentTheme.cardCornerRadius.value.toInt()}dp",
                color = currentTheme.textSecondaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = currentTheme.cardCornerRadius.value,
                onValueChange = { newR ->
                    onThemeChange(currentTheme.copy(cardCornerRadius = newR.dp, tableCornerRadius = newR.dp))
                },
                valueRange = 0f..24f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = currentTheme.primaryColor,
                    activeTrackColor = currentTheme.primaryColor,
                    inactiveTrackColor = currentTheme.dividerColor
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Zebra Stripes", color = currentTheme.textPrimaryColor, fontSize = 13.sp)
                Switch(
                    checked = currentTheme.showZebraStripes,
                    onCheckedChange = { onThemeChange(currentTheme.copy(showZebraStripes = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentTheme.primaryColor,
                        checkedTrackColor = currentTheme.primaryColor.copy(alpha = 0.35f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Cell Borders", color = currentTheme.textPrimaryColor, fontSize = 13.sp)
                Switch(
                    checked = currentTheme.showCellBorders,
                    onCheckedChange = { onThemeChange(currentTheme.copy(showCellBorders = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentTheme.primaryColor,
                        checkedTrackColor = currentTheme.primaryColor.copy(alpha = 0.35f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Compact Chart Mode (Khali Space Kam)", color = currentTheme.textPrimaryColor, fontSize = 13.sp)
                Switch(
                    checked = currentTheme.compactChartMode,
                    onCheckedChange = { onThemeChange(currentTheme.copy(compactChartMode = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentTheme.primaryColor,
                        checkedTrackColor = currentTheme.primaryColor.copy(alpha = 0.35f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = currentTheme.dividerColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "6. Custom Accent & Surface Color Overrides", currentTheme = currentTheme)

            val hasAnyCustomColor = currentTheme.customPrimaryColor != null ||
                currentTheme.customSecondaryColor != null ||
                currentTheme.customBackgroundColor != null ||
                currentTheme.customSurfaceColor != null ||
                currentTheme.customTextPrimaryColor != null ||
                currentTheme.customTableHeaderBg != null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Primary Accent Color:", color = currentTheme.textSecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (hasAnyCustomColor) {
                    TextButton(
                        onClick = {
                            onThemeChange(
                                currentTheme.copy(
                                    customPrimaryColor = null,
                                    customSecondaryColor = null,
                                    customBackgroundColor = null,
                                    customSurfaceColor = null,
                                    customTextPrimaryColor = null,
                                    customTableHeaderBg = null
                                )
                            )
                        }
                    ) {
                        Text("Clear Custom Colors", color = currentTheme.effectivePrimaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                listOf(
                    Color(0xFFFFD700) to "24K Gold",
                    Color(0xFFD9A441) to "Boss Gold",
                    Color(0xFF38BDF8) to "Sky Cyan",
                    Color(0xFF10B981) to "Emerald",
                    Color(0xFFF43F5E) to "Crimson",
                    Color(0xFFA855F7) to "Royal Purple",
                    Color(0xFFF97316) to "Amber Orange"
                ).forEach { (col, name) ->
                    StudioOptionChip(
                        selected = currentTheme.customPrimaryColor == col,
                        label = name,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(customPrimaryColor = col)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Card & Panel Surface Color Override:", color = currentTheme.textSecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                listOf(
                    Color(0xFF1A1E24) to "Carbon Dark",
                    Color(0xFF0F172A) to "Midnight Slate",
                    Color(0xFF14241B) to "Emerald Dark",
                    Color(0xFF231815) to "Copper Espresso",
                    Color(0xFFFFFFFF) to "Pure White"
                ).forEach { (col, name) ->
                    StudioOptionChip(
                        selected = currentTheme.customSurfaceColor == col,
                        label = name,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(customSurfaceColor = col)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("App Background Color Override:", color = currentTheme.textSecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                listOf(
                    Color(0xFF0E1013) to "Obsidian Black",
                    Color(0xFF070B14) to "Deep Navy",
                    Color(0xFF09140E) to "Forest Night",
                    Color(0xFF140D0B) to "Dark Bronze",
                    Color(0xFFF4F6F9) to "Daylight Ice"
                ).forEach { (col, name) ->
                    StudioOptionChip(
                        selected = currentTheme.customBackgroundColor == col,
                        label = name,
                        currentTheme = currentTheme,
                        onClick = { onThemeChange(currentTheme.copy(customBackgroundColor = col)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = currentTheme.dividerColor, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "7. Custom Background Wallpaper & Dimming", currentTheme = currentTheme)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        wallpaperLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    border = BorderStroke(1.dp, currentTheme.effectivePrimaryColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.effectivePrimaryColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentTheme.customBackgroundPath != null) "Change Wallpaper" else "Pick Gallery Image",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentTheme.customBackgroundPath != null) {
                    OutlinedButton(
                        onClick = { onThemeChange(currentTheme.copy(customBackgroundPath = null)) },
                        border = BorderStroke(1.dp, currentTheme.dividerColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.textSecondaryColor)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Wallpaper", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }

            if (currentTheme.customBackgroundPath != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Background Dimming Overlay: ${(currentTheme.customBackgroundDim * 100).toInt()}%",
                    color = currentTheme.textSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = currentTheme.customBackgroundDim,
                    onValueChange = { dim ->
                        onThemeChange(currentTheme.copy(customBackgroundDim = dim))
                    },
                    valueRange = 0.1f..0.95f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentTheme.effectivePrimaryColor,
                        activeTrackColor = currentTheme.effectivePrimaryColor,
                        inactiveTrackColor = currentTheme.dividerColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            ThemedButton(
                text = "Apply & Save All Changes",
                onClick = onDismiss,
                theme = currentTheme.copy(buttonSize = ButtonSize.PROMINENT),
                isPrimary = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            )
        }
    }
}

@Composable
fun StudioOptionChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    currentTheme: ThemeConfig,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    sublabel: String? = null
) {
    val primaryColor = currentTheme.primaryColor
    val shape = RoundedCornerShape(8.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) primaryColor.copy(alpha = 0.22f) else currentTheme.surfaceVariantColor.copy(alpha = 0.75f),
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) primaryColor else currentTheme.dividerColor.copy(alpha = 0.8f)
        ),
        modifier = modifier.defaultMinSize(minHeight = 38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = primaryColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = currentTheme.textSecondaryColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    color = if (selected) primaryColor else currentTheme.textPrimaryColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    fontFamily = currentTheme.fontFamily
                )
                if (sublabel != null) {
                    Text(
                        text = sublabel,
                        color = if (selected) primaryColor.copy(alpha = 0.85f) else currentTheme.textSecondaryColor,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStudioPreview(currentTheme: ThemeConfig) {
    val fontScale = currentTheme.effectiveFontScale
    val fontWeight = when (currentTheme.fontWeightMode) {
        FontWeightMode.NORMAL -> FontWeight.Normal
        FontWeightMode.MEDIUM -> FontWeight.SemiBold
        FontWeightMode.BOLD -> FontWeight.Bold
    }

    val previewShape = RoundedCornerShape(currentTheme.cardCornerRadius)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = previewShape,
        colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceColor),
        border = BorderStroke(1.5.dp, currentTheme.primaryColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(currentTheme.primaryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE INTERACTIVE PREVIEW",
                        color = currentTheme.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text = "${currentTheme.preset.displayName} - ${(fontScale * 100).toInt()}%",
                    color = currentTheme.textSecondaryColor,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val headerBg = currentTheme.tableHeaderBackground
            val headerText = currentTheme.tableHeaderTextColor
            val isTerminal = currentTheme.tableHeaderStyle == TableHeaderStyle.CYBER_TERMINAL

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(currentTheme.tableCornerRadius))
                    .border(1.dp, currentTheme.dividerColor, RoundedCornerShape(currentTheme.tableCornerRadius))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTerminal) ">_ SECTION / BAR" else "SECTION / BAR",
                        color = headerText,
                        fontSize = (11f * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isTerminal) FontFamily.Monospace else currentTheme.fontFamily
                    )
                    Text(
                        text = if (isTerminal) ">_ CUT LENGTH" else "CUT LENGTH",
                        color = headerText,
                        fontSize = (11f * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isTerminal) FontFamily.Monospace else currentTheme.fontFamily
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (currentTheme.showZebraStripes) currentTheme.surfaceVariantColor.copy(alpha = 0.45f) else Color.Transparent)
                        .padding(horizontal = currentTheme.rowHorizontalPadding, vertical = currentTheme.rowVerticalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DC30B Bottom (2 Bars)",
                        color = currentTheme.textPrimaryColor,
                        fontSize = (12f * fontScale).sp,
                        fontWeight = fontWeight,
                        fontFamily = currentTheme.fontFamily
                    )
                    Text(
                        text = "47 3/8\"",
                        color = currentTheme.primaryColor,
                        fontSize = (12f * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = currentTheme.fontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, currentTheme: ThemeConfig) {
    Text(
        text = title,
        color = currentTheme.primaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

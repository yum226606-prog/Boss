package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ButtonShapeStyle
import com.example.model.ButtonSize
import com.example.model.TabStyleType
import com.example.model.ThemeConfig

@Composable
fun CustomTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: ThemeConfig,
    isVertical: Boolean = false
) {
    val scale = theme.effectiveFontScale
    val fontSize = (11.5f * scale).sp
    val shape = when (theme.tabStyle) {
        TabStyleType.GOLD_PILL -> RoundedCornerShape(50)
        TabStyleType.NEON_GLOW -> RoundedCornerShape(10.dp)
        TabStyleType.UNDERLINE_BAR -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        TabStyleType.SOLID_BLOCK -> RoundedCornerShape(6.dp)
        TabStyleType.FLOATING_GLASS -> RoundedCornerShape(12.dp)
    }

    val bgColor = when {
        !isSelected -> Color.Transparent
        theme.tabStyle == TabStyleType.GOLD_PILL -> theme.primaryColor
        theme.tabStyle == TabStyleType.SOLID_BLOCK -> theme.primaryColor
        theme.tabStyle == TabStyleType.NEON_GLOW -> theme.primaryColor.copy(alpha = 0.18f)
        theme.tabStyle == TabStyleType.FLOATING_GLASS -> theme.surfaceVariantColor.copy(alpha = 0.85f)
        else -> theme.primaryColor.copy(alpha = 0.12f)
    }

    val contentColor = when {
        !isSelected -> theme.textSecondaryColor
        theme.tabStyle == TabStyleType.GOLD_PILL || theme.tabStyle == TabStyleType.SOLID_BLOCK -> {
            if (theme.primaryColor.red * 0.299 + theme.primaryColor.green * 0.587 + theme.primaryColor.blue * 0.114 > 0.6) Color.Black else Color.White
        }
        else -> theme.primaryColor
    }

    val borderStroke = when {
        !isSelected -> null
        theme.tabStyle == TabStyleType.NEON_GLOW -> BorderStroke(1.5.dp, theme.primaryColor)
        theme.tabStyle == TabStyleType.FLOATING_GLASS -> BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.6f))
        else -> null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = if (isVertical) 4.dp else 3.dp, vertical = 4.dp)
            .clip(shape)
            .background(bgColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
            .clickable { onClick() }
    ) {
        if (isVertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                if (theme.showButtonIcons) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title.split(" ").firstOrNull() ?: title,
                    color = contentColor,
                    fontFamily = theme.fontFamily,
                    fontSize = (9.5f * scale).sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                if (theme.showButtonIcons) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    color = contentColor,
                    fontFamily = theme.fontFamily,
                    fontSize = fontSize,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        if (isSelected && theme.tabStyle == TabStyleType.UNDERLINE_BAR && !isVertical) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(theme.primaryColor)
            )
        }
    }
}

@Composable
fun ThemedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    theme: ThemeConfig,
    testTag: String = "themed_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (minHeight, hPad, vPad, fontSize) = when (theme.buttonSize) {
        ButtonSize.MICRO -> Quad(30.dp, 8.dp, 4.dp, (11 * theme.effectiveFontScale).sp)
        ButtonSize.STANDARD -> Quad(44.dp, 16.dp, 8.dp, (13.5f * theme.effectiveFontScale).sp)
        ButtonSize.PROMINENT -> Quad(52.dp, 20.dp, 12.dp, (15 * theme.effectiveFontScale).sp)
        ButtonSize.JUMBO -> Quad(60.dp, 24.dp, 14.dp, (17 * theme.effectiveFontScale).sp)
    }

    val shape = RoundedCornerShape(theme.buttonCornerRadius)
    val bgColor = if (isPrimary) theme.primaryColor else theme.surfaceVariantColor

    val contentColor = if (isPrimary) {
        if (theme.buttonShapeStyle == ButtonShapeStyle.BRUTALIST_3D) Color.Black
        else if (theme.primaryColor.red * 0.299 + theme.primaryColor.green * 0.587 + theme.primaryColor.blue * 0.114 > 0.6) Color.Black
        else Color.White
    } else {
        theme.textPrimaryColor
    }

    val btnFontWeight = when (theme.fontWeightMode) {
        com.example.model.FontWeightMode.NORMAL -> if (isPrimary) FontWeight.SemiBold else FontWeight.Medium
        com.example.model.FontWeightMode.MEDIUM -> if (isPrimary) FontWeight.Bold else FontWeight.SemiBold
        com.example.model.FontWeightMode.BOLD -> FontWeight.Black
    }

    if (theme.buttonShapeStyle == ButtonShapeStyle.BRUTALIST_3D) {
        val offsetDp = if (isPressed) 1.dp else 4.dp
        Box(
            modifier = modifier
                .padding(bottom = 4.dp, end = 4.dp)
                .testTag(testTag)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = offsetDp, y = offsetDp)
                    .background(Color.Black, shape)
            )
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = minHeight)
                    .clip(shape)
                    .background(bgColor)
                    .border(BorderStroke(theme.buttonBorderWidth, theme.buttonBorderColor), shape)
                    .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                    .padding(horizontal = hPad, vertical = vPad),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size((fontSize.value + 4).dp)
                        )
                    }
                    Text(
                        text = text,
                        color = contentColor,
                        fontFamily = theme.fontFamily,
                        fontSize = fontSize,
                        fontWeight = if (theme.fontWeightMode == com.example.model.FontWeightMode.BOLD) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    } else {
        val elevation = if (theme.buttonElevation > 0.dp && !isPressed) theme.buttonElevation else 0.dp
        val borderStroke = if (theme.buttonBorderWidth > 0.dp) {
            BorderStroke(theme.buttonBorderWidth, theme.buttonBorderColor)
        } else null

        Box(
            modifier = modifier
                .testTag(testTag)
                .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
                .defaultMinSize(minHeight = minHeight)
                .clip(shape)
                .background(bgColor)
                .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .padding(horizontal = hPad, vertical = vPad),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size((fontSize.value + 4).dp)
                    )
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontFamily = theme.fontFamily,
                    fontSize = fontSize,
                    fontWeight = btnFontWeight
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

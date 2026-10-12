package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemePreset(val displayName: String, val description: String) {
    PLATINUM(
        displayName = "Platinum Silver",
        description = "Ultra-premium aerospace platinum silver & titanium styling with crisp cool charcoal panels"
    ),
    BOSS_GOLD(
        displayName = "Gold Metallic",
        description = "Signature luxury 24k gold typography on deep matte carbon panels with amber glow"
    ),
    STEEL(
        displayName = "Steel Blue",
        description = "Executive navy & aerospace steel blue with clean industrial precision"
    ),
    BLACK(
        displayName = "Black Premium",
        description = "Ultra-deep OLED obsidian canvas with razor sharp platinum accents"
    ),
    WHITE(
        displayName = "Clean White",
        description = "High-contrast clean architectural white canvas with dark charcoal text"
    ),
    EMERALD(
        displayName = "Emerald Pro",
        description = "Deep forest green & luminous emerald highlights with natural clarity"
    ),
    ROYAL(
        displayName = "Royal Purple",
        description = "Regal amethyst & imperial violet gradients with royal craftsmanship"
    ),
    OCEAN(
        displayName = "Ocean Cyan",
        description = "Deep maritime slate & electric cyan glow with crisp readability"
    ),
    RUBY(
        displayName = "Ruby Red",
        description = "Dark velvet crimson & vibrant ruby highlights for bold executive presence"
    ),
    COFFEE(
        displayName = "Coffee Bronze",
        description = "Rich roasted espresso & warm architectural bronze with golden undertones"
    ),
    MIDNIGHT(
        displayName = "Midnight Neon",
        description = "Midnight space blue canvas with vibrant electric sapphire neon accents"
    ),
    NEON_VOLT(
        displayName = "Neon Volt",
        description = "High-voltage lime volt highlights on cyber matte panels for maximum visibility"
    ),
    NEON_PINK(
        displayName = "Neon Pink",
        description = "Cyberpunk synthwave magenta & vibrant hot pink borders with dark panels"
    ),
    NEON_MATRIX(
        displayName = "Neon Matrix",
        description = "Classic terminal matrix green & phosphorescent glow for master cutters"
    ),
    FULL_GOLD_VIP(
        displayName = "Full Gold VIP (Regal)",
        description = "Royal 24k gold gilded luxury panels with ultra-sharp crystal clear high-contrast typography"
    )
}

enum class TableHeaderStyle(val label: String) {
    CYBER_TERMINAL("Cyber Terminal"),
    SOLID_CORPORATE("Solid Corporate"),
    BRUTALIST_BOXED("Brutalist Boxed"),
    NORDIC_DOTTED("Nordic Dotted"),
    COMPACT_NAVY("Compact Navy"),
    AURA_TRANSLUCENT("Aura Translucent"),
    GOLD_VIP("Gold VIP")
}

enum class ButtonShapeStyle(val label: String) {
    SHARP_RECT("Sharp Rect"),
    ROUNDED_SQUIRCLE("Rounded Squircle"),
    FULL_PILL("Full Pill"),
    BRUTALIST_3D("Brutalist 3D"),
    MICRO_COMPACT("Micro Compact"),
    NEO_GLOW("Neo Glow")
}

enum class ButtonStyleType(val label: String) {
    SOLID_GOLD("Solid Gold"),
    METALLIC_GLOSS("Metallic Gloss"),
    NEON_OUTLINE("Neon Outline"),
    GLASS_PILL("Glass Pill"),
    SOFT_ROUNDED("Soft Rounded")
}

enum class ButtonSize(val label: String) {
    MICRO("Micro"),
    STANDARD("Standard"),
    PROMINENT("Prominent"),
    JUMBO("Jumbo")
}

enum class TabBarPosition(val label: String) {
    TOP("Top Bar"),
    BOTTOM("Bottom Bar"),
    LEFT("Left Rail"),
    RIGHT("Right Rail")
}

enum class TabStyleType(val label: String) {
    GOLD_PILL("Gold Pill"),
    NEON_GLOW("Neon Glow"),
    UNDERLINE_BAR("Underline"),
    SOLID_BLOCK("Solid Block"),
    FLOATING_GLASS("Floating Glass")
}

enum class CardStyleType(val label: String) {
    GOLD_BORDER("Gold Border"),
    FROSTED_GLASS("Frosted Glass"),
    ELEVATED_SOLID("Elevated Solid"),
    NEON_OUTLINE("Neon Outline"),
    MINIMAL_FLAT("Minimal Flat")
}

enum class LayoutDensity(val label: String) {
    COMPACT("Compact"),
    BALANCED("Balanced"),
    SPACIOUS("Spacious")
}

enum class FontWeightMode(val label: String) {
    NORMAL("Regular (Standard)"),
    MEDIUM("Semi-Bold (Clearer)"),
    BOLD("Bold (Ultra Crisp)")
}

enum class FontFamilyOption(val label: String, val family: FontFamily) {
    SANS_SERIF("Modern Sans", FontFamily.SansSerif),
    SERIF("Classic Serif", FontFamily.Serif),
    MONOSPACE("Matrix Mono", FontFamily.Monospace)
}

enum class ChartBorderStyle {
    NONE,
    SLATE_HAIRLINE,
    BRUTALIST_HEAVY,
    NEON_CYAN_GLOW,
    WARM_STONE,
    GLASS_IRIDESCENT
}

enum class ChartShadowStyle {
    NONE,
    SOFT_DIFFUSE,
    HARD_3D_OFFSET,
    NEON_AMBIENT_GLOW,
    WARM_ORGANIC
}

enum class LegendPosition {
    BOTTOM_CHIPS,
    TOP_INLINE,
    MINIMAL_TAGS
}

enum class FontScale(val scale: Float, val label: String) {
    COMPACT(0.85f, "Compact (85%)"),
    STANDARD(1.0f, "Standard (100%)"),
    LARGE(1.15f, "Large (115%)"),
    EXTRA_LARGE(1.30f, "Extra Large (130%)"),
    JUMBO(1.45f, "Jumbo (145%)")
}

enum class ScreenSpacingMode(val label: String) {
    MOBILE_HANDHELD("Mobile View (Stacked)"),
    DESKTOP_WIDESCREEN("Desktop/Tablet View (Split Pane)")
}

enum class ChartType(val label: String) {
    BAR("Bar Comparison"),
    LINE_AREA("Growth Trend"),
    DONUT("Regional Share")
}

data class ThemeConfig(
    val preset: ThemePreset,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val surfaceVariantColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val dividerColor: Color,
    val gridLineColor: Color,
    val fontFamily: FontFamily,
    val fontScale: FontScale = FontScale.STANDARD,
    val customFontScale: Float? = null,
    val fontWeightMode: FontWeightMode = FontWeightMode.NORMAL,
    val isHighContrastText: Boolean = false,
    val tableHeaderStyle: TableHeaderStyle,
    val tableHeaderBackground: Color,
    val tableHeaderTextColor: Color,
    val rowHeight: Dp,
    val rowVerticalPadding: Dp,
    val rowHorizontalPadding: Dp,
    val alternateRowColor: Color,
    val showZebraStripes: Boolean = true,
    val showCellBorders: Boolean = true,
    val tableCornerRadius: Dp,
    val cardCornerRadius: Dp = 10.dp,
    val buttonShapeStyle: ButtonShapeStyle,
    val buttonStyle: ButtonStyleType = ButtonStyleType.SOLID_GOLD,
    val buttonSize: ButtonSize = ButtonSize.STANDARD,
    val customButtonScale: Float? = null,
    val showButtonIcons: Boolean = true,
    val buttonCornerRadius: Dp,
    val buttonElevation: Dp,
    val buttonBorderWidth: Dp,
    val buttonBorderColor: Color,
    val tabPosition: TabBarPosition = TabBarPosition.TOP,
    val tabStyle: TabStyleType = TabStyleType.GOLD_PILL,
    val cardStyle: CardStyleType = CardStyleType.GOLD_BORDER,
    val densityMode: LayoutDensity = LayoutDensity.BALANCED,
    val customPrimaryColor: Color? = null,
    val customSecondaryColor: Color? = null,
    val customBackgroundColor: Color? = null,
    val customSurfaceColor: Color? = null,
    val customTextPrimaryColor: Color? = null,
    val customTableHeaderBg: Color? = null,
    val customBackgroundPath: String? = null,
    val customBackgroundDim: Float = 0.72f,
    val chartBorderStyle: ChartBorderStyle,
    val chartBorderWidth: Dp,
    val chartBorderColor: Color,
    val chartShadowStyle: ChartShadowStyle,
    val chartCornerRadius: Dp,
    val chartCardElevation: Dp,
    val chartCardBackground: Color,
    val chartHeight: Dp,
    val compactChartMode: Boolean,
    val barCornerRadius: Dp,
    val barBorderWidth: Dp,
    val barBorderColor: Color,
    val chartTitlePrefix: String,
    val legendPosition: LegendPosition,
    val showDataValueLabels: Boolean,
    val screenSpacingMode: ScreenSpacingMode = ScreenSpacingMode.MOBILE_HANDHELD,
    val isPrintLayoutMode: Boolean = false
) {
    val effectiveFontScale: Float
        get() = customFontScale ?: fontScale.scale

    val effectiveButtonScale: Float
        get() = customButtonScale ?: when (buttonSize) {
            ButtonSize.MICRO -> 0.85f
            ButtonSize.STANDARD -> 1.0f
            ButtonSize.PROMINENT -> 1.15f
            ButtonSize.JUMBO -> 1.3f
        }

    val effectivePrimaryColor: Color
        get() = customPrimaryColor ?: primaryColor

    val effectiveSecondaryColor: Color
        get() = customSecondaryColor ?: secondaryColor

    val effectiveBackgroundColor: Color
        get() = customBackgroundColor ?: backgroundColor

    val effectiveSurfaceColor: Color
        get() = customSurfaceColor ?: surfaceColor

    val effectiveTextPrimaryColor: Color
        get() = customTextPrimaryColor ?: textPrimaryColor

    val effectiveTableHeaderBg: Color
        get() = customTableHeaderBg ?: tableHeaderBackground
}

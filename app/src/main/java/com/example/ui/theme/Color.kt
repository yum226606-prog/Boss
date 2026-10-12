package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeConfig
import com.example.model.ThemePreset

val BossGoldDefault = Color(0xFFD9A441)
val BossGoldLightDefault = Color(0xFFF0C766)
val BossGoldDarkDefault = Color(0xFFA67510)
val BossCarbonBgDefault = Color(0xFF0C0F13)
val BossCarbonPanelDefault = Color(0xFF14181F)
val BossCarbonPanel2Default = Color(0xFF1C222B)
val BossCarbonBorderDefault = Color(0xFF2B3441)
val BossSteelDefault = Color(0xFF5B8BB0)
val BossSteelDimDefault = Color(0xFF38556D)
val BossTextDefault = Color(0xFFE9EDF1)
val BossTextDimDefault = Color(0xFF9AA5B1)
val BossGreenDefault = Color(0xFF4CAF7D)
val BossRedDefault = Color(0xFFE05252)

val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

data class BossThemePalette(
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val border: Color,
    val primary: Color,
    val primaryLight: Color,
    val accent: Color,
    val text: Color,
    val textDim: Color,
    val green: Color = Color(0xFF4CAF7D),
    val red: Color = Color(0xFFE05252),
    val config: ThemeConfig? = null
) {
    companion object {
        fun from(cfg: ThemeConfig): BossThemePalette {
            val effBg = cfg.effectiveBackgroundColor
            val effPanel = cfg.effectiveSurfaceColor
            val effPrimary = cfg.effectivePrimaryColor
            val effSecondary = cfg.effectiveSecondaryColor
            val effTextBase = cfg.effectiveTextPrimaryColor
            val isLight = cfg.preset == ThemePreset.WHITE ||
                (effBg.red * 0.299 + effBg.green * 0.587 + effBg.blue * 0.114 > 0.5)
            val effectiveText = if (cfg.isHighContrastText) {
                if (isLight) Color(0xFF000000) else Color(0xFFFFFFFF)
            } else {
                effTextBase
            }
            val effectiveTextDim = if (cfg.isHighContrastText) {
                if (isLight) Color(0xFF1E293B) else Color(0xFFF1F5F9)
            } else {
                cfg.textSecondaryColor
            }
            return BossThemePalette(
                bg = effBg,
                panel = effPanel,
                panel2 = cfg.surfaceVariantColor,
                border = cfg.dividerColor,
                primary = effPrimary,
                primaryLight = effSecondary,
                accent = cfg.accentColor,
                text = effectiveText,
                textDim = effectiveTextDim,
                config = cfg
            )
        }
    }
}

val LocalBossColors = staticCompositionLocalOf {
    BossThemePalette(
        bg = BossCarbonBgDefault,
        panel = BossCarbonPanelDefault,
        panel2 = BossCarbonPanel2Default,
        border = BossCarbonBorderDefault,
        primary = BossGoldDefault,
        primaryLight = BossGoldLightDefault,
        accent = BossSteelDefault,
        text = BossTextDefault,
        textDim = BossTextDimDefault
    )
}

val LocalBossTheme = staticCompositionLocalOf<ThemeConfig> {
    ThemeConfigEngine.getPresetConfig(ThemePreset.BOSS_GOLD)
}

@Composable
fun currentBossColors(): BossThemePalette = LocalBossColors.current

@Composable
fun currentBossTheme(): ThemeConfig = LocalBossTheme.current

val BossGold: Color
    @Composable
    get() = LocalBossColors.current.primary

val BossGoldLight: Color
    @Composable
    get() = LocalBossColors.current.primaryLight

val BossGoldDark: Color
    @Composable
    get() = LocalBossColors.current.primary

val BossCarbonBg: Color
    @Composable
    get() = LocalBossColors.current.bg

val BossCarbonPanel: Color
    @Composable
    get() = LocalBossColors.current.panel

val BossCarbonPanel2: Color
    @Composable
    get() = LocalBossColors.current.panel2

val BossCarbonBorder: Color
    @Composable
    get() = LocalBossColors.current.border

val BossSteel: Color
    @Composable
    get() = LocalBossColors.current.accent

val BossSteelDim: Color
    @Composable
    get() = LocalBossColors.current.accent.copy(alpha = 0.65f)

val BossText: Color
    @Composable
    get() = LocalBossColors.current.text

val BossTextDim: Color
    @Composable
    get() = LocalBossColors.current.textDim

val BossGreen: Color
    @Composable
    get() = LocalBossColors.current.green

val BossRed: Color
    @Composable
    get() = LocalBossColors.current.red

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = BossGoldDefault,
    secondary = BossGoldLightDefault,
    tertiary = BossSteelDefault,
    background = BossCarbonBgDefault,
    surface = BossCarbonPanelDefault,
    surfaceVariant = BossCarbonPanel2Default,
    onPrimary = Color(0xFF120E05),
    onSecondary = Color(0xFF120E05),
    onTertiary = Color.White,
    onBackground = BossTextDefault,
    onSurface = BossTextDefault,
    onSurfaceVariant = BossTextDimDefault,
    outline = BossCarbonBorderDefault
)

private val LightColorScheme = lightColorScheme(
    primary = BossGoldDarkDefault,
    secondary = BossSteelDefault,
    tertiary = BossGoldDefault,
    background = Color(0xFFF3F5F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E9EF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF14181F),
    onSurface = Color(0xFF14181F),
    onSurfaceVariant = Color(0xFF5D6975),
    outline = Color(0xFFCBD2D9)
)

@Composable
fun MyApplicationTheme(
    themeConfig: com.example.model.ThemeConfig? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (themeConfig != null) {
        BossThemePalette.from(themeConfig)
    } else {
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

    val isLight = themeConfig?.preset == com.example.model.ThemePreset.WHITE ||
        (themeConfig != null && (themeConfig.effectiveBackgroundColor.red * 0.299 + themeConfig.effectiveBackgroundColor.green * 0.587 + themeConfig.effectiveBackgroundColor.blue * 0.114 > 0.5))

    val colorScheme = if (themeConfig != null) {
        if (isLight) {
            lightColorScheme(
                primary = themeConfig.effectivePrimaryColor,
                secondary = themeConfig.effectiveSecondaryColor,
                tertiary = themeConfig.accentColor,
                background = themeConfig.effectiveBackgroundColor,
                surface = themeConfig.effectiveSurfaceColor,
                surfaceVariant = themeConfig.surfaceVariantColor,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.Black,
                onBackground = themeConfig.effectiveTextPrimaryColor,
                onSurface = themeConfig.effectiveTextPrimaryColor,
                onSurfaceVariant = themeConfig.textSecondaryColor,
                outline = themeConfig.dividerColor
            )
        } else {
            darkColorScheme(
                primary = themeConfig.effectivePrimaryColor,
                secondary = themeConfig.effectiveSecondaryColor,
                tertiary = themeConfig.accentColor,
                background = themeConfig.effectiveBackgroundColor,
                surface = themeConfig.effectiveSurfaceColor,
                surfaceVariant = themeConfig.surfaceVariantColor,
                onPrimary = if (themeConfig.preset == com.example.model.ThemePreset.FULL_GOLD_VIP) Color(0xFF140F03) else Color.Black,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = themeConfig.effectiveTextPrimaryColor,
                onSurface = themeConfig.effectiveTextPrimaryColor,
                onSurfaceVariant = themeConfig.textSecondaryColor,
                outline = themeConfig.dividerColor
            )
        }
    } else {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val currentActiveConfig = themeConfig ?: ThemeConfigEngine.getPresetConfig(com.example.model.ThemePreset.BOSS_GOLD)
    val fontScale = currentActiveConfig.effectiveFontScale
    val fontFamily = currentActiveConfig.fontFamily

    val baseWeight = when (currentActiveConfig.fontWeightMode) {
        com.example.model.FontWeightMode.NORMAL -> FontWeight.Normal
        com.example.model.FontWeightMode.MEDIUM -> FontWeight.Medium
        com.example.model.FontWeightMode.BOLD -> FontWeight.Bold
    }

    val strongWeight = when (currentActiveConfig.fontWeightMode) {
        com.example.model.FontWeightMode.NORMAL -> FontWeight.Bold
        com.example.model.FontWeightMode.MEDIUM -> FontWeight.ExtraBold
        com.example.model.FontWeightMode.BOLD -> FontWeight.Black
    }

    val dynamicTypography = androidx.compose.material3.Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (57 * fontScale).sp),
        displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (45 * fontScale).sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (36 * fontScale).sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (32 * fontScale).sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (28 * fontScale).sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (24 * fontScale).sp),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = strongWeight, fontSize = (22 * fontScale).sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = strongWeight, fontSize = (16 * fontScale).sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = strongWeight, fontSize = (14 * fontScale).sp),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (16 * fontScale).sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (14 * fontScale).sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = baseWeight, fontSize = (12 * fontScale).sp),
        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = strongWeight, fontSize = (14 * fontScale).sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = strongWeight, fontSize = (12 * fontScale).sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = strongWeight, fontSize = (11 * fontScale).sp)
    )

    val buttonRadius = currentActiveConfig.buttonCornerRadius
    val dynamicShapes = Shapes(
        extraSmall = RoundedCornerShape((buttonRadius / 2).coerceAtLeast(2.dp)),
        small = RoundedCornerShape(buttonRadius),
        medium = RoundedCornerShape(buttonRadius),
        large = RoundedCornerShape(currentActiveConfig.chartCornerRadius),
        extraLarge = RoundedCornerShape(16.dp)
    )

    CompositionLocalProvider(
        LocalBossColors provides palette,
        LocalBossTheme provides currentActiveConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = dynamicTypography,
            shapes = dynamicShapes,
            content = content
        )
    }
}

package com.example.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RemoteLayoutConfig
import com.example.model.TabBarPosition
import com.example.model.ThemeConfig
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.RegistrationValidator
import java.io.File

@Composable
fun BossCalculatorApp(viewModel: BossAppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showThemeStudio by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showRoundLogoDialog by remember { mutableStateOf(false) }
    val bioUser = remember(state.userProfile) { BiometricAuthManager.getBiometricUser(context) }
    var isBiometricLocked by remember {
        mutableStateOf(
            state.userProfile.isRegistered &&
                BiometricAuthManager.isBiometricEnabled(context) &&
                bioUser != null
        )
    }

    // Back handler on non-Windows tabs returns to Windows tab
    if (state.activeTab != ClientTab.WINDOWS) {
        BackHandler {
            viewModel.selectTab(ClientTab.WINDOWS)
        }
    }

    MyApplicationTheme(themeConfig = state.themeConfig) {
        val c = currentBossColors()
        val theme = state.themeConfig

        if (!state.userProfile.isRegistered) {
            UserProfileOnboardingDialog(
                initialProfile = state.userProfile,
                securityManager = viewModel.securityManager,
                onProfileCompleted = { completedProfile ->
                    viewModel.onProfileCompleted(completedProfile)
                }
            )
        } else if (isBiometricLocked) {
            BiometricLockScreen(
                bioUser = bioUser,
                onScanClicked = {
                    val act = context as? FragmentActivity
                    if (act != null) {
                        BiometricAuthManager.showBiometricPrompt(
                            activity = act,
                            onAuthenticated = { isBiometricLocked = false },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        isBiometricLocked = false
                    }
                },
                onPasscodeUnlock = { code ->
                    val ok = code.trim() == RegistrationValidator.SECURITY_MASTER_PIN ||
                        viewModel.securityManager.unlockWithPasscode(code, state.userProfile)
                    if (ok) {
                        isBiometricLocked = false
                    }
                    ok
                },
                onSwitchAccount = {
                    BiometricAuthManager.clearBiometricCredentials(context)
                    isBiometricLocked = false
                    viewModel.logoutUser()
                }
            )
        } else {
            val isHardLocked = state.subscriptionState.isAppLocked(state.userProfile)
            if (isHardLocked || showSubscriptionDialog) {
                AppSubscriptionLockDialog(
                    userProfile = state.userProfile,
                    subscriptionState = state.subscriptionState,
                    securityManager = viewModel.securityManager,
                    onUnlocked = {
                        viewModel.refreshSecurityState()
                        showSubscriptionDialog = false
                    },
                    onDismiss = {
                        if (!isHardLocked) showSubscriptionDialog = false
                    }
                )
            }

            val customBgBitmap = remember(theme.customBackgroundPath) {
                val path = theme.customBackgroundPath
                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } else null
                } else null
            }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(c.bg),
                contentWindowInsets = WindowInsets.navigationBars,
                topBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BossTopHeader(
                            ctxGauge = state.ctxGauge,
                            ctxGaugeMode = state.ctxGaugeMode,
                            ctxColor = state.ctxColor,
                            ctxGlassGauge = state.ctxGlassGauge,
                            ctxGlassColor = state.ctxGlassColor,
                            ctxGlassMode = state.ctxGlassMode,
                            ctxFrameType = state.ctxFrameType,
                            availableGauges = state.availableGauges,
                            availableColors = state.availableColors,
                            availableGlassGauges = state.availableGlassGauges,
                            availableGlassColors5mm = state.availableGlassColors5mm,
                            availableGlassColors8mm = state.availableGlassColors8mm,
                            availableGlassColorsByGauge = state.availableGlassColorsByGauge,
                            customSections = state.customSections,
                            customLogoPath = state.customLogoPath,
                            companyName = state.companyName,
                            companyTagline = state.companyTagline,
                            remoteLayoutConfig = state.remoteLayoutConfig,
                            isVipOwner = state.userProfile.isVipOwner,
                            onLogoClick = {
                                showRoundLogoDialog = true
                            },
                            onSettingsClick = {
                                viewModel.selectTab(ClientTab.RATES_AND_SETTINGS)
                            },
                            onGaugeSelected = { viewModel.setMasterGauge(it) },
                            onColorSelected = { viewModel.setMasterColor(it) },
                            onFrameTypeSelected = { viewModel.setMasterFrameType(it) },
                            onGlassSelected = { g, col, mode -> viewModel.setMasterGlass(g, col, mode) },
                            measurementUnit = state.measurementUnit,
                            onUnitSelected = { viewModel.setMeasurementUnit(it) },
                            onAddCustomGauge = { viewModel.addCustomGauge(it) },
                            onRemoveCustomGauge = { viewModel.removeCustomGauge(it) },
                            onAddCustomColor = { viewModel.addCustomColor(it) },
                            onRemoveCustomColor = { viewModel.removeCustomColor(it) },
                            onAddCustomGlassGauge = { viewModel.addCustomGlassGauge(it) },
                            onRemoveGlassGauge = { viewModel.removeGlassGauge(it) },
                            onAddCustomGlassColor = { col, g -> viewModel.addCustomGlassColor(col, g) },
                            onRemoveGlassColor = { col, g -> viewModel.removeGlassColor(col, g) },
                            onAddNewSection = { sec, rate -> viewModel.addCustomSection(sec, rate) },
                            onAddNewSectionPair = { s1, r1, s2, r2 -> viewModel.addCustomSectionPair(s1, r1, s2, r2) },
                            onRemoveCustomSection = { viewModel.removeCustomSection(it) },
                            onThemeClick = { showThemeStudio = true },
                            onRefresh = {
                                viewModel.syncMasterCloudConfigNow()
                                viewModel.resetCurrentJob()
                                Toast.makeText(context, "Calculator refreshed & synced with Master Server", Toast.LENGTH_SHORT).show()
                            },
                            onDoneClick = {
                                viewModel.selectTab(ClientTab.BILLING)
                            }
                        )

                        TrialStatusHeaderBanner(
                            userProfile = state.userProfile,
                            subscriptionState = state.subscriptionState,
                            activeOfferBanner = state.activeOfferBanner,
                            onSubscribeClick = { showSubscriptionDialog = true },
                            onAccountClick = {
                                viewModel.setSettingsSubTab("ACCOUNT")
                                viewModel.selectTab(ClientTab.RATES_AND_SETTINGS)
                            },
                            onLogoutClick = { viewModel.logoutUser() }
                        )

                        if (theme.tabPosition == TabBarPosition.TOP) {
                            ClientHorizontalTabBar(
                                activeTab = state.activeTab,
                                onTabSelected = { viewModel.selectTab(it) },
                                theme = theme,
                                remoteLayoutConfig = state.remoteLayoutConfig
                            )
                        }
                    }
                },
                bottomBar = {
                    if (theme.tabPosition == TabBarPosition.BOTTOM) {
                        ClientHorizontalTabBar(
                            activeTab = state.activeTab,
                            onTabSelected = { viewModel.selectTab(it) },
                            theme = theme,
                            remoteLayoutConfig = state.remoteLayoutConfig
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(c.bg)
                ) {
                    if (customBgBitmap != null) {
                        Image(
                            bitmap = customBgBitmap,
                            contentDescription = "Custom Wallpaper",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = theme.customBackgroundDim.coerceIn(0.1f, 0.95f)))
                        )
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (theme.tabPosition == TabBarPosition.LEFT) {
                            ClientVerticalTabBar(
                                activeTab = state.activeTab,
                                onTabSelected = { viewModel.selectTab(it) },
                                theme = theme,
                                remoteLayoutConfig = state.remoteLayoutConfig
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (state.activeTab) {
                                ClientTab.WINDOWS -> WindowsScreen(viewModel = viewModel, state = state)
                                ClientTab.DOORS -> DoorsAndFramesScreen(viewModel = viewModel, state = state)
                                ClientTab.EXTRA_PIECES -> ExtraPiecesScreen(viewModel = viewModel, state = state)
                                ClientTab.CUTTING_CHART -> CuttingChartScreen(viewModel = viewModel, state = state)
                                ClientTab.LENGTH_CHART -> LengthChartScreen(viewModel = viewModel, state = state)
                                ClientTab.GLASS_CHART -> GlassChartScreen(viewModel = viewModel, state = state)
                                ClientTab.BILLING -> BillingScreen(viewModel = viewModel, state = state)
                                ClientTab.HISTORY -> HistoryScreen(viewModel = viewModel, state = state)
                                ClientTab.RATES_AND_SETTINGS -> ProfileSettingsScreen(
                                    viewModel = viewModel,
                                    state = state,
                                    onOpenThemeStudio = { showThemeStudio = true },
                                    onOpenSubscription = { showSubscriptionDialog = true }
                                )
                            }
                        }

                        if (theme.tabPosition == TabBarPosition.RIGHT) {
                            ClientVerticalTabBar(
                                activeTab = state.activeTab,
                                onTabSelected = { viewModel.selectTab(it) },
                                theme = theme,
                                remoteLayoutConfig = state.remoteLayoutConfig
                            )
                        }
                    }
                }
            }

            if (showThemeStudio) {
                ThemeCustomizerSheet(
                    currentTheme = state.themeConfig,
                    onThemeChange = { viewModel.updateThemeConfig(it) },
                    onDismiss = { showThemeStudio = false }
                )
            }

            if (showRoundLogoDialog) {
                RoundLogoExtractDialog(
                    onDismiss = { showRoundLogoDialog = false },
                    onOpenLogoSettings = {
                        showRoundLogoDialog = false
                        viewModel.setSettingsSubTab("LOGO_BG")
                        viewModel.selectTab(ClientTab.RATES_AND_SETTINGS)
                    }
                )
            }
        }
    }
}

@Composable
private fun RoundLogoExtractDialog(
    onDismiss: () -> Unit,
    onOpenLogoSettings: () -> Unit
) {
    val context = LocalContext.current
    val activeDrawableRes = com.example.R.drawable.ic_boss_logo_clean

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = Color(0xFF0B0F17),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD9A441)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFFICIAL ROUND 'B' LOGO",
                        color = Color(0xFFF5B82E),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                // Large High-Resolution Round Logo Preview (Original 'B' without square box)
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = activeDrawableRes),
                        contentDescription = "Round B Logo HD",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                    )
                }

                Text(
                    text = "Original 'B' Logo (Round, Square Box Removed)",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelSmall
                )

                // Save to Gallery & Downloads Button
                Button(
                    onClick = {
                        com.example.util.ChartPrintHelper.saveRoundLogoToGalleryAndDownloads(
                            context = context,
                            drawableResId = activeDrawableRes,
                            fileName = "Boss_B_Logo_Round.png"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF5B82E),
                        contentColor = Color(0xFF161208)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save / Download Round Logo (PNG)",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                    )
                }

                // Share / Export Button + Logo Settings Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            com.example.util.ChartPrintHelper.shareRoundLogoPng(
                                context = context,
                                drawableResId = activeDrawableRes,
                                fileName = "Boss_B_Logo_Round.png"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF08101D)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PNG", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenLogoSettings,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFF5B82E), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Logo Settings", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientHorizontalTabBar(
    activeTab: ClientTab,
    onTabSelected: (ClientTab) -> Unit,
    theme: ThemeConfig,
    remoteLayoutConfig: RemoteLayoutConfig = RemoteLayoutConfig()
) {
    val c = currentBossColors()
    val visibleTabs = ClientTab.values().filter { tab ->
        remoteLayoutConfig.isClientTabEnabled(tab.name, tab.title)
    }
    Surface(
        color = c.panel,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleTabs.forEach { tab ->
                CustomTabItem(
                    title = remoteLayoutConfig.getTabTitle(tab.name, tab.title),
                    icon = iconForClientTab(tab),
                    isSelected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    theme = theme,
                    isVertical = false
                )
            }
        }
    }
}

@Composable
private fun ClientVerticalTabBar(
    activeTab: ClientTab,
    onTabSelected: (ClientTab) -> Unit,
    theme: ThemeConfig,
    remoteLayoutConfig: RemoteLayoutConfig = RemoteLayoutConfig()
) {
    val c = currentBossColors()
    val visibleTabs = ClientTab.values().filter { tab ->
        remoteLayoutConfig.isClientTabEnabled(tab.name, tab.title)
    }
    Surface(
        color = c.panel,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(74.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            visibleTabs.forEach { tab ->
                CustomTabItem(
                    title = remoteLayoutConfig.getTabTitle(tab.name, tab.title),
                    icon = iconForClientTab(tab),
                    isSelected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    theme = theme,
                    isVertical = true
                )
            }
        }
    }
}

private fun iconForClientTab(tab: ClientTab): ImageVector = when (tab) {
    ClientTab.WINDOWS -> Icons.Default.Window
    ClientTab.DOORS -> Icons.Default.DoorFront
    ClientTab.EXTRA_PIECES -> Icons.Default.Extension
    ClientTab.CUTTING_CHART -> Icons.Default.ContentCut
    ClientTab.LENGTH_CHART -> Icons.Default.Straighten
    ClientTab.GLASS_CHART -> Icons.Default.CropFree
    ClientTab.BILLING -> Icons.Default.ReceiptLong
    ClientTab.HISTORY -> Icons.Default.History
    ClientTab.RATES_AND_SETTINGS -> Icons.Default.VerifiedUser
}

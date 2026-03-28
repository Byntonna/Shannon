package com.example.shannon.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.titleResId
import com.example.shannon.presentation.components.ConnectivityTestScreen
import com.example.shannon.presentation.components.DnsAnalysisScreen
import com.example.shannon.presentation.components.DiagnosticsHomeScreen
import com.example.shannon.presentation.components.AboutShannonScreen
import com.example.shannon.presentation.components.OverviewDetailsScreen
import com.example.shannon.presentation.components.PortScanScreen
import com.example.shannon.presentation.components.ProtocolAnalysisScreen
import com.example.shannon.presentation.components.PingDiagnosticsScreen
import com.example.shannon.presentation.components.ReportExportScreen
import com.example.shannon.presentation.components.SniMitmAnalysisScreen
import com.example.shannon.presentation.components.TlsAnalysisScreen
import com.example.shannon.presentation.components.TracerouteDiagnosticsScreen
import com.example.shannon.presentation.components.WebsiteAccessibilityScreen
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanTransport
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.model.WebsiteAccessibilityPreset
import com.example.shannon.presentation.model.DiagnosticsDestination
import com.example.shannon.presentation.model.DiagnosticsUiState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NetworkDiagnosticsScreen(
    uiState: DiagnosticsUiState,
    contentPadding: PaddingValues,
    onOpenScreen: (DiagnosticsDestination) -> Unit,
    onNavigateBack: () -> Unit,
    onRunHomeSummaryCheck: () -> Unit,
    onRefreshOverview: () -> Unit,
    onRunConnectivityTest: () -> Unit,
    onSelectTargetPreset: (ConnectivityTargetPreset) -> Unit,
    onRunWebsiteAccessibilityTest: () -> Unit,
    onSelectWebsitePreset: (WebsiteAccessibilityPreset) -> Unit,
    onUpdateCustomWebsiteInput: (String) -> Unit,
    onAddCustomWebsiteTarget: () -> Unit,
    onRemoveCustomWebsiteTarget: (String) -> Unit,
    onUpdateDnsAnalysisDomain: (String) -> Unit,
    onRunDnsAnalysis: () -> Unit,
    onRunProtocolAnalysis: () -> Unit,
    onRunTlsAnalysis: () -> Unit,
    onRunSniMitmAnalysis: () -> Unit,
    onUpdatePortScanHost: (String) -> Unit,
    onUpdatePortScanInput: (String) -> Unit,
    onUpdatePortScanTransport: (PortScanTransport) -> Unit,
    onUpdatePortScanIpVersion: (PortScanIpVersion) -> Unit,
    onRunSinglePortScan: () -> Unit,
    onRunQuickPortScan: () -> Unit,
    onUpdatePingHost: (String) -> Unit,
    onUpdatePingPacketCountInput: (String) -> Unit,
    onRunPing: () -> Unit,
    onUpdateTracerouteHost: (String) -> Unit,
    onRunTraceroute: () -> Unit,
    onExportReport: (ReportFormat) -> Unit,
    onOpenAboutShannon: () -> Unit,
) {
    val homeScrollState = rememberScrollState()
    val previousScreen = remember { mutableStateOf(uiState.currentScreen) }
    val animateHomeChrome = previousScreen.value == uiState.currentScreen &&
        uiState.currentScreen == DiagnosticsDestination.Home
    val titleCollapseFraction = if (uiState.currentScreen != DiagnosticsDestination.Home) {
        0f
    } else {
        (homeScrollState.value / 180f).coerceIn(0f, 1f)
    }
    val animatedTitleCollapseFraction by animateFloatAsState(
        targetValue = titleCollapseFraction,
        animationSpec = if (animateHomeChrome) {
            androidx.compose.animation.core.spring()
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "home-title-collapse",
    )

    SideEffect {
        previousScreen.value = uiState.currentScreen
    }

    BackHandler(enabled = uiState.currentScreen != DiagnosticsDestination.Home) {
        onNavigateBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DiagnosticsTopBar(
                currentScreen = uiState.currentScreen,
                titleCollapseFraction = animatedTitleCollapseFraction,
                animateHomeChrome = animateHomeChrome,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = {
                screenTransition(initialState, targetState)
            },
            label = "diagnostics-screen-transition",
        ) { destination ->
            when (destination) {
                DiagnosticsDestination.Home -> DiagnosticsHomeScreen(
                    uiState = uiState,
                    scrollState = homeScrollState,
                    onRunHomeSummaryCheck = onRunHomeSummaryCheck,
                    onOpenOverview = { onOpenScreen(DiagnosticsDestination.Overview) },
                    onOpenConnectivityTest = { onOpenScreen(DiagnosticsDestination.ConnectivityTest) },
                    onOpenDnsAnalysis = { onOpenScreen(DiagnosticsDestination.DnsAnalysis) },
                    onOpenProtocolAnalysis = {
                        onOpenScreen(DiagnosticsDestination.ProtocolAnalysis)
                    },
                    onOpenTlsAnalysis = {
                        onOpenScreen(DiagnosticsDestination.TlsAnalysis)
                    },
                    onOpenSniMitmAnalysis = {
                        onOpenScreen(DiagnosticsDestination.SniMitmAnalysis)
                    },
                    onOpenPortScan = {
                        onOpenScreen(DiagnosticsDestination.PortScan)
                    },
                    onOpenPingDiagnostics = {
                        onOpenScreen(DiagnosticsDestination.PingDiagnostics)
                    },
                    onOpenTracerouteDiagnostics = {
                        onOpenScreen(DiagnosticsDestination.TracerouteDiagnostics)
                    },
                    onOpenReportExport = {
                        onOpenScreen(DiagnosticsDestination.ReportExport)
                    },
                    onOpenWebsiteAccessibility = {
                        onOpenScreen(DiagnosticsDestination.WebsiteAccessibility)
                    },
                    onOpenAboutShannon = onOpenAboutShannon,
                )
                DiagnosticsDestination.Overview -> OverviewDetailsScreen(
                    overview = uiState.overview,
                    onRefresh = onRefreshOverview,
                )
                DiagnosticsDestination.DnsAnalysis -> DnsAnalysisScreen(
                    domain = uiState.dnsAnalysisDomain,
                    result = uiState.dnsAnalysisResult,
                    isRunning = uiState.isRunningDnsAnalysis,
                    onDomainChange = onUpdateDnsAnalysisDomain,
                    onRunAnalysis = onRunDnsAnalysis,
                )
                DiagnosticsDestination.ProtocolAnalysis -> ProtocolAnalysisScreen(
                    result = uiState.protocolAnalysisResult,
                    isRunning = uiState.isRunningProtocolAnalysis,
                    onRunAnalysis = onRunProtocolAnalysis,
                )
                DiagnosticsDestination.TlsAnalysis -> TlsAnalysisScreen(
                    result = uiState.tlsAnalysisResult,
                    isRunning = uiState.isRunningTlsAnalysis,
                    onRunAnalysis = onRunTlsAnalysis,
                )
                DiagnosticsDestination.SniMitmAnalysis -> SniMitmAnalysisScreen(
                    result = uiState.sniMitmAnalysisResult,
                    isRunning = uiState.isRunningSniMitmAnalysis,
                    onRunAnalysis = onRunSniMitmAnalysis,
                )
                DiagnosticsDestination.PortScan -> PortScanScreen(
                    host = uiState.portScanHost,
                    portInput = uiState.portScanInput,
                    transport = uiState.portScanTransport,
                    ipVersion = uiState.portScanIpVersion,
                    results = uiState.portScanResults,
                    isRunning = uiState.isRunningPortScan,
                    error = uiState.portScanError,
                    onHostChange = onUpdatePortScanHost,
                    onPortInputChange = onUpdatePortScanInput,
                    onTransportChange = onUpdatePortScanTransport,
                    onIpVersionChange = onUpdatePortScanIpVersion,
                    onRunSingleScan = onRunSinglePortScan,
                    onRunQuickScan = onRunQuickPortScan,
                )
                DiagnosticsDestination.PingDiagnostics -> PingDiagnosticsScreen(
                    host = uiState.pingHost,
                    packetCountInput = uiState.pingPacketCountInput,
                    packetStatuses = uiState.pingPacketStatuses,
                    result = uiState.pingResult,
                    isRunning = uiState.isRunningPing,
                    error = uiState.pingError,
                    onHostChange = onUpdatePingHost,
                    onPacketCountInputChange = onUpdatePingPacketCountInput,
                    onRunPing = onRunPing,
                )
                DiagnosticsDestination.TracerouteDiagnostics -> TracerouteDiagnosticsScreen(
                    host = uiState.tracerouteHost,
                    result = uiState.tracerouteResult,
                    isRunning = uiState.isRunningTraceroute,
                    error = uiState.tracerouteError,
                    onHostChange = onUpdateTracerouteHost,
                    onRunTraceroute = onRunTraceroute,
                )
                DiagnosticsDestination.ReportExport -> ReportExportScreen(
                    isExporting = uiState.isExportingReport,
                    lastFormat = uiState.lastReportFormat,
                    exportedReportName = uiState.exportedReportName,
                    error = uiState.reportError,
                    availableSections = listOfNotNull(
                        uiState.overview?.let { stringResource(R.string.report_section_overview) },
                        uiState.testResult?.let { stringResource(R.string.report_section_connectivity) },
                        uiState.dnsAnalysisResult?.let { stringResource(R.string.report_section_dns) },
                        uiState.protocolAnalysisResult?.let { stringResource(R.string.report_section_protocols) },
                        uiState.tlsAnalysisResult?.let { stringResource(R.string.report_section_tls) },
                        uiState.sniMitmAnalysisResult?.let { stringResource(R.string.report_section_sni_mitm) },
                        uiState.pingResult?.let { stringResource(R.string.report_section_ping) },
                        uiState.tracerouteResult?.let { stringResource(R.string.report_section_traceroute) },
                        uiState.websiteAccessibilityResults.takeIf { it.isNotEmpty() }?.let {
                            stringResource(R.string.report_section_websites)
                        },
                    ),
                    onExport = onExportReport,
                )
                DiagnosticsDestination.WebsiteAccessibility -> WebsiteAccessibilityScreen(
                    selectedPreset = uiState.selectedWebsitePreset,
                    customInput = uiState.customWebsiteInput,
                    customTargets = uiState.customWebsiteTargets,
                    results = uiState.websiteAccessibilityResults,
                    isRunning = uiState.isRunningWebsiteAccessibility,
                    onRunTest = onRunWebsiteAccessibilityTest,
                    onSelectPreset = onSelectWebsitePreset,
                    onCustomInputChange = onUpdateCustomWebsiteInput,
                    onAddCustomTarget = onAddCustomWebsiteTarget,
                    onRemoveCustomTarget = onRemoveCustomWebsiteTarget,
                )
                DiagnosticsDestination.ConnectivityTest -> ConnectivityTestScreen(
                    selectedTargetPreset = uiState.selectedTargetPreset,
                    testResult = uiState.testResult,
                    isRunning = uiState.isRunning,
                    onRunTest = onRunConnectivityTest,
                    onRefreshNetwork = onRefreshOverview,
                    onSelectTargetPreset = onSelectTargetPreset,
                )
                DiagnosticsDestination.About -> AboutShannonScreen()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun DiagnosticsTopBar(
    currentScreen: DiagnosticsDestination,
    titleCollapseFraction: Float,
    animateHomeChrome: Boolean,
    onNavigateBack: () -> Unit,
) {
    val isHome = currentScreen == DiagnosticsDestination.Home
    val targetHeight = if (isHome) {
        lerp(120.dp, 64.dp, titleCollapseFraction)
    } else {
        64.dp
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = if (animateHomeChrome) {
            androidx.compose.animation.core.spring()
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "diagnostics-top-bar-height",
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (isHome) 1f - titleCollapseFraction else 0f,
        animationSpec = if (animateHomeChrome) {
            androidx.compose.animation.core.spring()
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "diagnostics-top-bar-subtitle-alpha",
    )
    val subtitleHeight by animateDpAsState(
        targetValue = if (isHome) lerp(20.dp, 0.dp, titleCollapseFraction) else 0.dp,
        animationSpec = if (animateHomeChrome) {
            androidx.compose.animation.core.spring()
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "diagnostics-top-bar-subtitle-height",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(animatedHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isHome) {
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.top_bar_back),
                    )
                }
            }

            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier
                    .weight(1f),
                transitionSpec = {
                    topBarTitleTransition(initialState, targetState)
                },
                label = "diagnostics-top-bar-title-transition",
            ) { screen ->
                if (screen == DiagnosticsDestination.Home) {
                    val expandedTitleStyle = MaterialTheme.typography.headlineMedium
                    val collapsedTitleStyle = MaterialTheme.typography.titleLarge
                    val titleStyle = expandedTitleStyle.copy(
                        fontSize = lerp(
                            expandedTitleStyle.fontSize,
                            collapsedTitleStyle.fontSize,
                            titleCollapseFraction,
                        ),
                        lineHeight = lerp(
                            expandedTitleStyle.lineHeight,
                            collapsedTitleStyle.lineHeight,
                            titleCollapseFraction,
                        ),
                    )
                    Column(
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(screen.titleResId()),
                            style = titleStyle,
                        )
                        Spacer(modifier = Modifier.height(lerp(2.dp, 0.dp, titleCollapseFraction)))
                        if (subtitleHeight > 0.dp || subtitleAlpha > 0.01f) {
                            Text(
                                text = stringResource(R.string.top_bar_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                modifier = Modifier
                                    .height(subtitleHeight)
                                    .alpha(subtitleAlpha),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Text(stringResource(screen.titleResId()))
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun topBarTitleTransition(
    initialState: DiagnosticsDestination,
    targetState: DiagnosticsDestination,
): ContentTransform {
    val openingDetails = initialState == DiagnosticsDestination.Home &&
        targetState != DiagnosticsDestination.Home
    val returningHome = initialState != DiagnosticsDestination.Home &&
        targetState == DiagnosticsDestination.Home

    return when {
        openingDetails -> {
            slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() togetherWith
                slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut()
        }
        returningHome -> {
            slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn() togetherWith
                slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        }
        else -> {
            fadeIn() togetherWith fadeOut()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun screenTransition(
    initialState: DiagnosticsDestination,
    targetState: DiagnosticsDestination,
): ContentTransform {
    val isForward = targetState.ordinal > initialState.ordinal
    return if (isForward) {
        slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn() togetherWith
            slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
    } else {
        slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
            slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut()
    }
}

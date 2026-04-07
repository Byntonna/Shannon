package com.example.shannon.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.millisecondsText
import com.example.shannon.portScanSummaryText
import com.example.shannon.titleResId
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.HomeDashboardStatusKey
import com.example.shannon.domain.model.HomeDashboardStatusTone
import com.example.shannon.domain.model.PortStatus
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.domain.model.WhitelistZoneVerdict
import com.example.shannon.domain.model.WebsiteAccessibilityOutcome
import com.example.shannon.domain.model.toOutcome
import com.example.shannon.presentation.model.DiagnosticsUiState
import com.example.shannon.presentation.model.HomeSummaryState
import com.example.shannon.presentation.model.HomeSummaryTone
import com.example.shannon.presentation.model.homeSummaryState
import com.example.shannon.ui.theme.ShannonTheme
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Icon background color tokens (fixed, per Figma)
// ---------------------------------------------------------------------------
private val IconBlue   = Color(0xFF66D0F8) // Network overview
private val IconPink   = Color(0xFFFBAAD8) // Connectivity test, Website accessibility
private val IconIndigo = Color(0xFF9AC4F9) // DNS analysis
private val IconPurple = Color(0xFFC7A8F5) // Protocol analysis, TLS analysis, SNI filtering and MITM
private val IconPeach  = Color(0xFFF7B97C) // Port scan, Ping diagnostics, Traceroute
private val IconSage   = Color(0xFFD7E4A5) // Report export, About Shannon

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun DiagnosticsHomeScreen(
    uiState: DiagnosticsUiState,
    scrollState: ScrollState,
    onRunHomeSummaryCheck: () -> Unit,
    onOpenSummaryDetails: () -> Unit,
    onOpenOverview: () -> Unit,
    onOpenDnsAnalysis: () -> Unit,
    onOpenConnectivityTest: () -> Unit,
    onOpenProtocolAnalysis: () -> Unit,
    onOpenTlsAnalysis: () -> Unit,
    onOpenSniMitmAnalysis: () -> Unit,
    onOpenPortScan: () -> Unit,
    onOpenPingDiagnostics: () -> Unit,
    onOpenTracerouteDiagnostics: () -> Unit,
    onOpenReportExport: () -> Unit,
    onOpenWebsiteAccessibility: () -> Unit,
    onOpenWhitelistZoneCheck: () -> Unit,
    onOpenAboutShannon: () -> Unit,
) {
    val context = LocalContext.current
    val lanIcon = ImageVector.vectorResource(R.drawable.ic_lan)
    val wifiIcon = ImageVector.vectorResource(R.drawable.ic_wifi)
    val internetIcon = ImageVector.vectorResource(R.drawable.ic_internet)
    val whitelistIcon = ImageVector.vectorResource(R.drawable.ic_whitelist)
    val dnsIcon = ImageVector.vectorResource(R.drawable.ic_dns)
    val hubIcon = ImageVector.vectorResource(R.drawable.ic_hub)
    val lockedIcon = ImageVector.vectorResource(R.drawable.ic_locked)
    val sniIcon = ImageVector.vectorResource(R.drawable.ic_account_group)
    val magnifyIcon = ImageVector.vectorResource(R.drawable.ic_magnify)
    val pingIcon = ImageVector.vectorResource(R.drawable.ic_ping_pong)
    val routesIcon = ImageVector.vectorResource(R.drawable.ic_routes)
    val exportIcon = ImageVector.vectorResource(R.drawable.ic_export_variant)
    val infoIcon = ImageVector.vectorResource(R.drawable.ic_information_outline)
    val connectivityStatus = connectivityStatus(uiState)
    val websiteStatus = websiteStatus(uiState)
    val whitelistStatus = whitelistZoneStatus(uiState)
    val dnsStatus = dnsStatus(uiState)
    val tlsStatus = tlsStatus(uiState)
    val sniStatus = sniStatus(uiState)
    val portScanStatus = portScanStatus(uiState)
    val pingStatus = pingStatus(uiState)
    val tracerouteStatus = tracerouteStatus(uiState)

    val overviewGroup = remember(context, onOpenOverview) {
        listOf(
        HomeMenuEntry(
            icon = lanIcon,
            iconBackground = IconBlue,
            title = context.getString(R.string.home_overview_title),
            subtitle = context.getString(R.string.home_overview_subtitle),
            onClick = onOpenOverview,
        ),
    )
    }
    val coreGroup = remember(
        context,
        connectivityStatus,
        websiteStatus,
        whitelistStatus,
        dnsStatus,
        tlsStatus,
        sniStatus,
        onOpenConnectivityTest,
        onOpenWebsiteAccessibility,
        onOpenWhitelistZoneCheck,
        onOpenDnsAnalysis,
        onOpenProtocolAnalysis,
        onOpenTlsAnalysis,
        onOpenSniMitmAnalysis,
        wifiIcon,
        internetIcon,
        whitelistIcon,
        dnsIcon,
        hubIcon,
        lockedIcon,
        sniIcon,
    ) {
        listOf(
        HomeMenuEntry(
            icon = wifiIcon,
            iconBackground = IconPink,
            title = context.getString(R.string.home_connectivity_title),
            subtitle = context.getString(R.string.home_connectivity_subtitle),
            status = connectivityStatus,
            onClick = onOpenConnectivityTest,
        ),
        HomeMenuEntry(
            icon = internetIcon,
            iconBackground = IconPink,
            title = context.getString(R.string.home_websites_title),
            subtitle = context.getString(R.string.home_websites_subtitle),
            status = websiteStatus,
            onClick = onOpenWebsiteAccessibility,
        ),
        HomeMenuEntry(
            icon = whitelistIcon,
            iconBackground = IconPink,
            title = context.getString(R.string.home_whitelist_title),
            subtitle = context.getString(R.string.home_whitelist_subtitle),
            status = whitelistStatus,
            onClick = onOpenWhitelistZoneCheck,
        ),
        HomeMenuEntry(
            icon = dnsIcon,
            iconBackground = IconIndigo,
            title = context.getString(R.string.home_dns_title),
            subtitle = context.getString(R.string.home_dns_subtitle),
            status = dnsStatus,
            onClick = onOpenDnsAnalysis,
        ),
        HomeMenuEntry(
            icon = hubIcon,
            iconBackground = IconPurple,
            title = context.getString(R.string.home_protocol_title),
            subtitle = context.getString(R.string.home_protocol_subtitle),
            onClick = onOpenProtocolAnalysis,
        ),
        HomeMenuEntry(
            icon = lockedIcon,
            iconBackground = IconPurple,
            title = context.getString(R.string.home_tls_title),
            subtitle = context.getString(R.string.home_tls_subtitle),
            status = tlsStatus,
            onClick = onOpenTlsAnalysis,
        ),
        HomeMenuEntry(
            icon = sniIcon,
            iconBackground = IconPurple,
            title = context.getString(R.string.home_sni_title),
            subtitle = context.getString(R.string.home_sni_subtitle),
            status = sniStatus,
            onClick = onOpenSniMitmAnalysis,
        ),
    )
    }
    val diagnosticsGroup = remember(
        context,
        portScanStatus,
        pingStatus,
        tracerouteStatus,
        onOpenPortScan,
        onOpenPingDiagnostics,
        onOpenTracerouteDiagnostics,
        magnifyIcon,
        pingIcon,
        routesIcon,
    ) {
        listOf(
        HomeMenuEntry(
            icon = magnifyIcon,
            iconBackground = IconPeach,
            title = context.getString(R.string.home_port_scan_title),
            subtitle = context.getString(R.string.home_port_scan_subtitle),
            status = portScanStatus,
            onClick = onOpenPortScan,
        ),
        HomeMenuEntry(
            icon = pingIcon,
            iconBackground = IconPeach,
            title = context.getString(R.string.home_ping_title),
            subtitle = context.getString(R.string.home_ping_subtitle),
            status = pingStatus,
            onClick = onOpenPingDiagnostics,
        ),
        HomeMenuEntry(
            icon = routesIcon,
            iconBackground = IconPeach,
            title = context.getString(R.string.home_traceroute_title),
            subtitle = context.getString(R.string.home_traceroute_subtitle),
            status = tracerouteStatus,
            onClick = onOpenTracerouteDiagnostics,
        ),
    )
    }
    val utilityGroup = remember(context, onOpenReportExport, onOpenAboutShannon, exportIcon, infoIcon) {
        listOf(
        HomeMenuEntry(
            icon = exportIcon,
            iconBackground = IconSage,
            title = context.getString(R.string.home_report_export_title),
            subtitle = context.getString(R.string.home_report_export_subtitle),
            onClick = onOpenReportExport,
        ),
        HomeMenuEntry(
            icon = infoIcon,
            iconBackground = IconSage,
            title = context.getString(R.string.home_about_title),
            subtitle = context.getString(R.string.home_about_subtitle),
            onClick = onOpenAboutShannon,
        ),
    )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val summary = remember(uiState) { uiState.homeSummaryState() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HomeSectionTitle(
                    title = context.getString(R.string.home_summary_section_title),
                )
                NetworkSummaryCard(
                    summary = summary,
                    isRunning = uiState.isRunningHomeSummaryCheck,
                    onRunCheck = onRunHomeSummaryCheck,
                    onOpenDetails = onOpenSummaryDetails,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HomeSectionTitle(
                    title = context.getString(R.string.home_tools_section_title),
                )
                DiagnosticsGroup(entries = overviewGroup)
                DiagnosticsGroup(entries = coreGroup)
                DiagnosticsGroup(entries = diagnosticsGroup)
                DiagnosticsGroup(entries = utilityGroup)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent,
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(24.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
        )
    }
}

// ---------------------------------------------------------------------------
// Group + entry
// ---------------------------------------------------------------------------

@Composable
private fun DiagnosticsGroup(
    entries: List<HomeMenuEntry>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = diagnosticsEntryShape(
                    index = index,
                    size = entries.size,
                ),
            ) {
                DiagnosticsEntryRow(entry = entry)
            }
        }
    }
}

@Composable
private fun diagnosticsEntryShape(index: Int, size: Int) = when {
    size <= 1 -> MaterialTheme.shapes.extraLarge
    index == 0 -> RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )
    index == size - 1 -> RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 20.dp,
        bottomEnd = 20.dp,
    )
    else -> RoundedCornerShape(0.dp)
}

@Composable
private fun DiagnosticsEntryRow(entry: HomeMenuEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(entry.iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF1C1B1F),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = entry.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                entry.status?.let { StatusBadge(it) }
            }
            Text(
                text = entry.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Status badge
// ---------------------------------------------------------------------------

@Composable
private fun StatusBadge(status: HomeEntryStatus) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor(status.tone)),
        )
        Text(
            text = status.text,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor(status.tone),
        )
    }
}

// ---------------------------------------------------------------------------
// Status helpers
// ---------------------------------------------------------------------------

private data class HomeMenuEntry(
    val icon: ImageVector,
    val iconBackground: Color,
    val title: String,
    val subtitle: String,
    val status: HomeEntryStatus? = null,
    val onClick: () -> Unit,
)

private data class HomeEntryStatus(
    val text: String,
    val tone: HomeEntryTone,
)

private enum class HomeEntryTone {
    Positive,
    Warning,
    Error,
    Neutral,
}

@Composable
private fun statusColor(tone: HomeEntryTone): Color = when (tone) {
    HomeEntryTone.Positive -> Color(0xFF1B8F4E)
    HomeEntryTone.Warning  -> Color(0xFFB26A00)
    HomeEntryTone.Error    -> MaterialTheme.colorScheme.error
    HomeEntryTone.Neutral  -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun connectivityStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val result = uiState.testResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.ConnectivityTest)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    val failedStep = result.steps.firstOrNull { !it.success }
    return if (failedStep == null) {
        HomeEntryStatus(context.getString(R.string.status_ok), HomeEntryTone.Positive)
    } else {
        HomeEntryStatus(failedStep.stage, HomeEntryTone.Warning)
    }
}

@Composable
private fun dnsStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val result = uiState.dnsAnalysisResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.DnsAnalysis)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    return when (result.status) {
        DnsAnalysisStatus.Ok           -> HomeEntryStatus(context.getString(R.string.status_ok), HomeEntryTone.Positive)
        DnsAnalysisStatus.CdnVariation -> HomeEntryStatus(context.getString(R.string.dns_status_variation), HomeEntryTone.Neutral)
        DnsAnalysisStatus.Suspicious   -> HomeEntryStatus(context.getString(R.string.status_suspicious), HomeEntryTone.Warning)
        DnsAnalysisStatus.Blocked      -> HomeEntryStatus(context.getString(R.string.status_blocked), HomeEntryTone.Error)
    }
}

@Composable
private fun websiteStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val results = uiState.websiteAccessibilityResults
    if (results.isEmpty()) {
        return persistedStatus(uiState, HomeDashboardStatusKey.WebsiteAccessibility)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    val limitedCount = results.count { it.status.toOutcome() == WebsiteAccessibilityOutcome.Limited }
    val unstableCount = results.count { it.status.toOutcome() == WebsiteAccessibilityOutcome.Unstable }
    return when {
        limitedCount > 0 -> HomeEntryStatus(
            context.getString(R.string.home_websites_status_limited, limitedCount),
            HomeEntryTone.Error,
        )
        unstableCount > 0 -> HomeEntryStatus(
            context.getString(R.string.home_websites_status_unstable, unstableCount),
            HomeEntryTone.Warning,
        )
        else -> HomeEntryStatus(context.getString(R.string.status_ok), HomeEntryTone.Positive)
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun NetworkSummaryCard(
    summary: HomeSummaryState,
    isRunning: Boolean,
    onRunCheck: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val summaryTone = summary.tone.toEntryTone()
    val (containerColor, contentColor) = when (summaryTone) {
        HomeEntryTone.Positive -> {
            if (isDarkTheme) Color(0xFF173526) to Color(0xFF8DD8A8) else Color(0xFFE8F4EA) to Color(0xFF1E7B46)
        }
        HomeEntryTone.Warning -> {
            if (isDarkTheme) Color(0xFF3A2B17) to Color(0xFFF2C27B) else Color(0xFFFFF2E0) to Color(0xFF9A5E00)
        }
        HomeEntryTone.Error -> {
            if (isDarkTheme) Color(0xFF3F1D1E) to Color(0xFFF2B8B5) else Color(0xFFFDECEC) to Color(0xFFB3261E)
        }
        HomeEntryTone.Neutral -> {
            if (isDarkTheme) Color(0xFF23262C) to Color(0xFFE2E2E6) else Color(0xFFF1EFF8) to Color(0xFF4B4F58)
        }
    }
    val icon = when (summaryTone) {
        HomeEntryTone.Positive -> Icons.Filled.CheckCircle
        HomeEntryTone.Warning -> Icons.Filled.Info
        HomeEntryTone.Error -> Icons.Filled.Info
        HomeEntryTone.Neutral -> Icons.Filled.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = context.getString(summary.titleResId),
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                )
                Text(
                    text = context.getString(summary.descriptionResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.9f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = context.getString(R.string.home_summary_open_details),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.9f),
                )
                if (summary.showRunCheckAction) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticPrimaryButton(
                        text = if (isRunning) {
                            context.getString(R.string.home_summary_running_check)
                        } else {
                            context.getString(R.string.home_summary_run_check)
                        },
                        onClick = onRunCheck,
                        enabled = !isRunning,
                    )
                }
            }
        }
    }
}

@Composable
private fun whitelistZoneStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val result = uiState.whitelistZoneCheckResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.WhitelistZoneCheck)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    return when (result.verdict) {
        WhitelistZoneVerdict.InWhitelistZone -> HomeEntryStatus(
            context.getString(R.string.whitelist_home_status_in_zone),
            HomeEntryTone.Error,
        )
        WhitelistZoneVerdict.OutsideWhitelistZone -> HomeEntryStatus(
            context.getString(R.string.whitelist_home_status_outside_zone),
            HomeEntryTone.Positive,
        )
        WhitelistZoneVerdict.NoWhitelistDetectedButReferenceBlocked -> HomeEntryStatus(
            context.getString(R.string.whitelist_home_status_reference_blocked),
            HomeEntryTone.Warning,
        )
        WhitelistZoneVerdict.Inconclusive -> HomeEntryStatus(
            context.getString(R.string.whitelist_home_status_inconclusive),
            HomeEntryTone.Neutral,
        )
    }
}

private fun HomeSummaryTone.toEntryTone(): HomeEntryTone = when (this) {
    HomeSummaryTone.Positive -> HomeEntryTone.Positive
    HomeSummaryTone.Warning -> HomeEntryTone.Warning
    HomeSummaryTone.Error -> HomeEntryTone.Error
    HomeSummaryTone.Neutral -> HomeEntryTone.Neutral
}

@Composable
private fun tlsStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val result = uiState.tlsAnalysisResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.TlsAnalysis)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    return when (result.status) {
        TlsAnalysisHeuristicStatus.NoTlsAnomalies -> HomeEntryStatus(context.getString(R.string.status_ok), HomeEntryTone.Positive)
        TlsAnalysisHeuristicStatus.UnusualCertificateChain -> HomeEntryStatus(context.getString(R.string.home_tls_status_chain), HomeEntryTone.Warning)
        TlsAnalysisHeuristicStatus.TlsDowngradeSuspected -> HomeEntryStatus(context.getString(R.string.home_tls_status_downgrade), HomeEntryTone.Warning)
        TlsAnalysisHeuristicStatus.TlsInterceptionSuspected -> HomeEntryStatus(context.getString(R.string.home_tls_status_interception), HomeEntryTone.Warning)
        TlsAnalysisHeuristicStatus.Inconclusive -> HomeEntryStatus(context.getString(R.string.status_inconclusive), HomeEntryTone.Neutral)
    }
}

@Composable
private fun sniStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val result = uiState.sniMitmAnalysisResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.SniMitmAnalysis)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    return when (result.status) {
        SniAnalysisStatus.Normal -> HomeEntryStatus(context.getString(R.string.status_normal), HomeEntryTone.Positive)
        SniAnalysisStatus.SniFilteringSuspected -> HomeEntryStatus(context.getString(R.string.home_sni_status_filtering), HomeEntryTone.Warning)
        SniAnalysisStatus.TlsInterceptionSuspected -> HomeEntryStatus(context.getString(R.string.home_sni_status_tls_intercept), HomeEntryTone.Warning)
        SniAnalysisStatus.MitmSuspected -> HomeEntryStatus(context.getString(R.string.home_sni_status_mitm), HomeEntryTone.Error)
        SniAnalysisStatus.Inconclusive -> HomeEntryStatus(context.getString(R.string.status_inconclusive), HomeEntryTone.Neutral)
    }
}

@Composable
private fun portScanStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val results = uiState.portScanResults
    if (results.isEmpty()) {
        return persistedStatus(uiState, HomeDashboardStatusKey.PortScan)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    if (results.size == 1) {
        val result = results.single()
        return HomeEntryStatus(
            text = context.getString(result.status.titleResId()),
            tone = when (result.status) {
                PortStatus.OPEN             -> HomeEntryTone.Positive
                PortStatus.CLOSED           -> HomeEntryTone.Error
                PortStatus.FILTERED,
                PortStatus.TIMEOUT          -> HomeEntryTone.Warning
                PortStatus.ERROR            -> HomeEntryTone.Neutral
            },
        )
    }
    val openCount    = results.count { it.status == PortStatus.OPEN }
    val closedCount  = results.count { it.status == PortStatus.CLOSED }
    val warningCount = results.count { it.status == PortStatus.FILTERED || it.status == PortStatus.TIMEOUT }
    val errorCount   = results.count { it.status == PortStatus.ERROR }
    val parts = buildList {
        if (openCount > 0)    add("$openCount open")
        if (closedCount > 0)  add("$closedCount closed")
        if (warningCount > 0) add("$warningCount timeout")
        if (errorCount > 0)   add("$errorCount error")
    }
    val tone = when {
        openCount == results.size   -> HomeEntryTone.Positive
        openCount > 0               -> HomeEntryTone.Warning
        closedCount == results.size -> HomeEntryTone.Error
        else                        -> HomeEntryTone.Neutral
    }
    return HomeEntryStatus(context.portScanSummaryText(openCount, closedCount, warningCount, errorCount), tone)
}

@Composable
private fun pingStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val result = uiState.pingResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.PingDiagnostics)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    val avg = result.avgLatencyMs.roundToInt()
    val tone = when {
        avg <= 60  -> HomeEntryTone.Positive
        avg <= 150 -> HomeEntryTone.Neutral
        else       -> HomeEntryTone.Warning
    }
    return HomeEntryStatus(context.millisecondsText(avg), tone)
}

@Composable
private fun tracerouteStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val context = LocalContext.current
    val result = uiState.tracerouteResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.TracerouteDiagnostics)
            ?: HomeEntryStatus(context.getString(R.string.status_not_run), HomeEntryTone.Neutral)
    }
    return HomeEntryStatus(context.getString(R.string.traceroute_hops_value, result.hops.size), HomeEntryTone.Neutral)
}

private fun persistedStatus(
    uiState: DiagnosticsUiState,
    key: HomeDashboardStatusKey,
): HomeEntryStatus? {
    val status = uiState.persistedHomeStatuses[key] ?: return null
    return HomeEntryStatus(
        text = status.text,
        tone = when (status.tone) {
            HomeDashboardStatusTone.Positive -> HomeEntryTone.Positive
            HomeDashboardStatusTone.Warning -> HomeEntryTone.Warning
            HomeDashboardStatusTone.Error -> HomeEntryTone.Error
            HomeDashboardStatusTone.Neutral -> HomeEntryTone.Neutral
        },
    )
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun DiagnosticsHomeScreenPreview() {
    ShannonTheme {
        DiagnosticsHomeScreen(
            uiState = DiagnosticsUiState(),
            scrollState = rememberScrollState(),
            onRunHomeSummaryCheck = {},
            onOpenSummaryDetails = {},
            onOpenOverview = {},
            onOpenDnsAnalysis = {},
            onOpenConnectivityTest = {},
            onOpenProtocolAnalysis = {},
            onOpenTlsAnalysis = {},
            onOpenSniMitmAnalysis = {},
            onOpenPortScan = {},
            onOpenPingDiagnostics = {},
            onOpenTracerouteDiagnostics = {},
            onOpenReportExport = {},
            onOpenWebsiteAccessibility = {},
            onOpenWhitelistZoneCheck = {},
            onOpenAboutShannon = {},
        )
    }
}

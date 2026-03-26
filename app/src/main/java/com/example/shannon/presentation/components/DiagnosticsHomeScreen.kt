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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.HomeDashboardStatusKey
import com.example.shannon.domain.model.HomeDashboardStatusTone
import com.example.shannon.domain.model.PortStatus
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.presentation.model.DiagnosticsUiState
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
    onOpenAboutShannon: () -> Unit,
) {
    val overviewGroup = listOf(
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_lan),
            iconBackground = IconBlue,
            title = "Network overview",
            subtitle = "Network type, IP addresses, DNS, validated, metered",
            onClick = onOpenOverview,
        ),
    )
    val coreGroup = listOf(
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_wifi),
            iconBackground = IconPink,
            title = "Connectivity test",
            subtitle = "DNS, TCP, TLS, and HTTP checks for a target endpoint",
            status = connectivityStatus(uiState),
            onClick = onOpenConnectivityTest,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_internet),
            iconBackground = IconPink,
            title = "Website accessibility",
            subtitle = "Check popular internet services across DNS, TCP, TLS, and HTTP stages",
            onClick = onOpenWebsiteAccessibility,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_dns),
            iconBackground = IconIndigo,
            title = "DNS analysis",
            subtitle = "Compare system and public DNS resolvers",
            status = dnsStatus(uiState),
            onClick = onOpenDnsAnalysis,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_hub),
            iconBackground = IconPurple,
            title = "Protocol analysis",
            subtitle = "HTTP versions, WebSocket, and DPI heuristics",
            onClick = onOpenProtocolAnalysis,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_locked),
            iconBackground = IconPurple,
            title = "TLS analysis",
            subtitle = "TLS version, cipher suite, ALPN, certificate issuer, fingerprint",
            status = tlsStatus(uiState),
            onClick = onOpenTlsAnalysis,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_account_group),
            iconBackground = IconPurple,
            title = "SNI filtering and MITM",
            subtitle = "Compare normal SNI, alternative SNI, no SNI, and random SNI behavior",
            status = sniStatus(uiState),
            onClick = onOpenSniMitmAnalysis,
        ),
    )
    val diagnosticsGroup = listOf(
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_magnify),
            iconBackground = IconPeach,
            title = "Port scan",
            subtitle = "Single TCP/UDP port checks and quick scans",
            status = portScanStatus(uiState),
            onClick = onOpenPortScan,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_ping_pong),
            iconBackground = IconPeach,
            title = "Ping diagnostics",
            subtitle = "Latency, jitter, packet loss",
            status = pingStatus(uiState),
            onClick = onOpenPingDiagnostics,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_routes),
            iconBackground = IconPeach,
            title = "Traceroute diagnostics",
            subtitle = "Hop path, IP addresses, hostnames, latency, and timeouts",
            status = tracerouteStatus(uiState),
            onClick = onOpenTracerouteDiagnostics,
        ),
    )
    val utilityGroup = listOf(
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_export_variant),
            iconBackground = IconSage,
            title = "Report export",
            subtitle = "Generate and share a diagnostic report",
            onClick = onOpenReportExport,
        ),
        HomeMenuEntry(
            icon = ImageVector.vectorResource(R.drawable.ic_information_outline),
            iconBackground = IconSage,
            title = "About Shannon",
            subtitle = "Application information, version, and project details",
            onClick = onOpenAboutShannon,
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DiagnosticsGroup(entries = overviewGroup)
            DiagnosticsGroup(entries = coreGroup)
            DiagnosticsGroup(entries = diagnosticsGroup)
            DiagnosticsGroup(entries = utilityGroup)
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

private fun connectivityStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val result = uiState.testResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.ConnectivityTest)
            ?: HomeEntryStatus("Not run", HomeEntryTone.Neutral)
    }
    val failedStep = result.steps.firstOrNull { !it.success }
    return if (failedStep == null) {
        HomeEntryStatus("OK", HomeEntryTone.Positive)
    } else {
        HomeEntryStatus(failedStep.stage, HomeEntryTone.Warning)
    }
}

private fun dnsStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val result = uiState.dnsAnalysisResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.DnsAnalysis)
            ?: HomeEntryStatus("Not run", HomeEntryTone.Neutral)
    }
    return when (result.status) {
        DnsAnalysisStatus.Ok           -> HomeEntryStatus("OK", HomeEntryTone.Positive)
        DnsAnalysisStatus.CdnVariation -> HomeEntryStatus("Variation", HomeEntryTone.Neutral)
        DnsAnalysisStatus.Suspicious   -> HomeEntryStatus("Suspicious", HomeEntryTone.Warning)
        DnsAnalysisStatus.Blocked      -> HomeEntryStatus("Blocked", HomeEntryTone.Error)
    }
}

private fun tlsStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val result = uiState.tlsAnalysisResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.TlsAnalysis)
            ?: HomeEntryStatus("Not run", HomeEntryTone.Neutral)
    }
    return when (result.status) {
        TlsAnalysisHeuristicStatus.NoTlsAnomalies           -> HomeEntryStatus("OK", HomeEntryTone.Positive)
        TlsAnalysisHeuristicStatus.UnusualCertificateChain  -> HomeEntryStatus("Chain", HomeEntryTone.Warning)
        TlsAnalysisHeuristicStatus.TlsDowngradeSuspected    -> HomeEntryStatus("Downgrade", HomeEntryTone.Warning)
        TlsAnalysisHeuristicStatus.TlsInterceptionSuspected -> HomeEntryStatus("Interception", HomeEntryTone.Warning)
        TlsAnalysisHeuristicStatus.Inconclusive             -> HomeEntryStatus("Inconclusive", HomeEntryTone.Neutral)
    }
}

private fun sniStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val result = uiState.sniMitmAnalysisResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.SniMitmAnalysis)
            ?: HomeEntryStatus("Not run", HomeEntryTone.Neutral)
    }
    return when (result.status) {
        SniAnalysisStatus.Normal                   -> HomeEntryStatus("Normal", HomeEntryTone.Positive)
        SniAnalysisStatus.SniFilteringSuspected    -> HomeEntryStatus("SNI filtering", HomeEntryTone.Warning)
        SniAnalysisStatus.TlsInterceptionSuspected -> HomeEntryStatus("TLS intercept", HomeEntryTone.Warning)
        SniAnalysisStatus.MitmSuspected            -> HomeEntryStatus("MITM", HomeEntryTone.Error)
        SniAnalysisStatus.Inconclusive             -> HomeEntryStatus("Inconclusive", HomeEntryTone.Neutral)
    }
}

private fun portScanStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val results = uiState.portScanResults
    if (results.isEmpty()) {
        return persistedStatus(uiState, HomeDashboardStatusKey.PortScan)
            ?: HomeEntryStatus("Not run", HomeEntryTone.Neutral)
    }
    if (results.size == 1) {
        val result = results.single()
        return HomeEntryStatus(
            text = result.status.title,
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
    return HomeEntryStatus(parts.joinToString(" / "), tone)
}

private fun pingStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val result = uiState.pingResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.PingDiagnostics)
            ?: HomeEntryStatus("Not run", HomeEntryTone.Neutral)
    }
    val avg = result.avgLatencyMs.roundToInt()
    val tone = when {
        avg <= 60  -> HomeEntryTone.Positive
        avg <= 150 -> HomeEntryTone.Neutral
        else       -> HomeEntryTone.Warning
    }
    return HomeEntryStatus("$avg ms", tone)
}

private fun tracerouteStatus(uiState: DiagnosticsUiState): HomeEntryStatus {
    val result = uiState.tracerouteResult
    if (result == null) {
        return persistedStatus(uiState, HomeDashboardStatusKey.TracerouteDiagnostics)
            ?: HomeEntryStatus("Not run", HomeEntryTone.Neutral)
    }
    return HomeEntryStatus("${result.hops.size} hops", HomeEntryTone.Neutral)
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
            onOpenAboutShannon = {},
        )
    }
}

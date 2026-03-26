package com.example.shannon.presentation

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.FileProvider
import com.example.shannon.domain.model.ConnectivityStepResult
import com.example.shannon.domain.model.ConnectivityTestResult
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.DnsAnalysisResult
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.DnsLookupTransport
import com.example.shannon.domain.model.DnsRecordResult
import com.example.shannon.domain.model.DnsServerResult
import com.example.shannon.domain.model.NetworkOverview
import com.example.shannon.domain.model.PingPacketState
import com.example.shannon.domain.model.PingPacketStatus
import com.example.shannon.domain.model.PingResult
import com.example.shannon.domain.model.ProtocolAnalysisResult
import com.example.shannon.domain.model.ProtocolObservation
import com.example.shannon.domain.model.ProtocolProbeErrorCategory
import com.example.shannon.domain.model.ProtocolProbeKind
import com.example.shannon.domain.model.ProtocolProbeStatus
import com.example.shannon.domain.model.ProtocolTestResult
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.SniObservation
import com.example.shannon.domain.model.SniProbeErrorCategory
import com.example.shannon.domain.model.SniProviderAnalysis
import com.example.shannon.domain.model.SniVariantResult
import com.example.shannon.domain.model.SniVariantType
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.domain.model.TlsAnalysisResult
import com.example.shannon.domain.model.TlsCertificateInfo
import com.example.shannon.domain.model.TlsEndpointAnalysis
import com.example.shannon.domain.model.TlsEndpointStatus
import com.example.shannon.domain.model.TlsObservation
import com.example.shannon.domain.model.TlsVersionSupport
import com.example.shannon.domain.model.TracerouteHop
import com.example.shannon.domain.model.TracerouteResult
import com.example.shannon.domain.model.WebsiteAccessibilityPreset
import com.example.shannon.domain.model.WebsiteAccessibilityResult
import com.example.shannon.domain.model.WebsiteAccessibilityStatus
import com.example.shannon.domain.model.WebsiteAccessibilityTarget
import com.example.shannon.presentation.model.DiagnosticsDestination
import com.example.shannon.presentation.model.DiagnosticsUiState
import com.example.shannon.ui.theme.ShannonTheme
import java.io.File

@Composable
fun NetworkDiagnosticsRoute(
    applicationContext: Context,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val viewModel = viewModel<NetworkDiagnosticsViewModel>(
        factory = NetworkDiagnosticsViewModel.Factory(applicationContext),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.pendingSharedReportPath, uiState.pendingSharedReportFormat) {
        val reportPath = uiState.pendingSharedReportPath
        val reportFormat = uiState.pendingSharedReportFormat
        if (reportPath == null || reportFormat == null) return@LaunchedEffect

        runCatching {
            shareExportedReport(
                context = context,
                file = File(reportPath),
                format = reportFormat,
            )
        }.onSuccess {
            viewModel.onReportShareHandled()
        }.onFailure { error ->
            viewModel.onReportShareHandled(
                error = error.message ?: "Unable to open the share sheet",
            )
        }
    }

    NetworkDiagnosticsScreen(
        uiState = uiState,
        contentPadding = contentPadding,
        onOpenScreen = viewModel::openScreen,
        onNavigateBack = viewModel::goBack,
        onRefreshOverview = viewModel::refreshOverview,
        onRunConnectivityTest = viewModel::launchConnectivityTest,
        onSelectTargetPreset = viewModel::selectTargetPreset,
        onRunWebsiteAccessibilityTest = viewModel::launchWebsiteAccessibilityTest,
        onSelectWebsitePreset = viewModel::selectWebsitePreset,
        onUpdateCustomWebsiteInput = viewModel::updateCustomWebsiteInput,
        onAddCustomWebsiteTarget = viewModel::addCustomWebsiteTarget,
        onRemoveCustomWebsiteTarget = viewModel::removeCustomWebsiteTarget,
        onUpdateDnsAnalysisDomain = viewModel::updateDnsAnalysisDomain,
        onRunDnsAnalysis = viewModel::launchDnsAnalysis,
        onRunProtocolAnalysis = viewModel::launchProtocolAnalysis,
        onRunTlsAnalysis = viewModel::launchTlsAnalysis,
        onRunSniMitmAnalysis = viewModel::launchSniMitmAnalysis,
        onUpdatePortScanHost = viewModel::updatePortScanHost,
        onUpdatePortScanInput = viewModel::updatePortScanInput,
        onUpdatePortScanTransport = viewModel::updatePortScanTransport,
        onUpdatePortScanIpVersion = viewModel::updatePortScanIpVersion,
        onRunSinglePortScan = viewModel::launchSinglePortScan,
        onRunQuickPortScan = viewModel::launchQuickPortScan,
        onUpdatePingHost = viewModel::updatePingHost,
        onUpdatePingPacketCountInput = viewModel::updatePingPacketCountInput,
        onRunPing = viewModel::launchPing,
        onUpdateTracerouteHost = viewModel::updateTracerouteHost,
        onRunTraceroute = viewModel::launchTraceroute,
        onExportReport = viewModel::exportReport,
        onOpenAboutShannon = { viewModel.openScreen(DiagnosticsDestination.About) },
    )
}

@Preview(showBackground = true)
@Composable
private fun NetworkDiagnosticsScreenPreview() {
    ShannonTheme {
        NetworkDiagnosticsScreen(
            uiState = DiagnosticsUiState(
                currentScreen = DiagnosticsDestination.Home,
                overview = NetworkOverview(
                    networkType = "Wi-Fi",
                    internetReachable = true,
                    validated = true,
                    metered = false,
                    downstreamMbps = "250 Mbps",
                    upstreamMbps = "80 Mbps",
                    ssid = "Shannon WiFi",
                    bssid = "AA:BB:CC:DD:EE:FF",
                    signalStrength = "-62 dBm (Good)",
                    privateIpAddress = "192.168.1.24",
                    gatewayAddress = "192.168.1.1",
                    ipv6Address = "2a02::1234",
                    dnsServers = listOf("192.168.1.1", "1.1.1.1"),
                    carrierName = "n/a",
                ),
                testResult = ConnectivityTestResult(
                    steps = listOf(
                        ConnectivityStepResult("DNS", true, "OK, 142.250.74.132 in 24 ms"),
                        ConnectivityStepResult("TCP", true, "Connected to 142.250.74.132:443 in 32 ms"),
                        ConnectivityStepResult("TLS", true, "TLSv1.3 handshake in 41 ms"),
                        ConnectivityStepResult("HTTP", true, "Response 204 in 56 ms"),
                    ),
                    checkedAt = "09:41:03",
                    endpointLabel = "Google Connectivity Check",
                    endpointUrl = "https://connectivitycheck.gstatic.com/generate_204",
                    fallbackUsed = false,
                ),
                selectedTargetPreset = ConnectivityTargetPreset.Standard,
                selectedWebsitePreset = WebsiteAccessibilityPreset.Popular,
                customWebsiteTargets = listOf(
                    WebsiteAccessibilityTarget("openai.com", "https://openai.com/")
                ),
                dnsAnalysisResult = DnsAnalysisResult(
                    domain = "example.com",
                    servers = listOf(
                        DnsServerResult(
                            "System DNS",
                            "system",
                            records = listOf(
                                DnsRecordResult("A", DnsLookupTransport.System, listOf("93.184.216.34")),
                                DnsRecordResult("AAAA", DnsLookupTransport.System, listOf("2606:2800:220:1:248:1893:25c8:1946")),
                            ),
                        ),
                        DnsServerResult(
                            "Google DNS",
                            "8.8.8.8",
                            records = listOf(
                                DnsRecordResult("A", DnsLookupTransport.Udp, listOf("93.184.216.34")),
                                DnsRecordResult(
                                    "AAAA",
                                    DnsLookupTransport.TcpFallback,
                                    emptyList(),
                                    error = "No AAAA records",
                                ),
                            ),
                        ),
                        DnsServerResult(
                            "Cloudflare DNS",
                            "1.1.1.1",
                            records = listOf(
                                DnsRecordResult("A", DnsLookupTransport.Udp, listOf("93.184.216.34")),
                                DnsRecordResult("AAAA", DnsLookupTransport.Udp, listOf("2606:2800:220:1:248:1893:25c8:1946")),
                            ),
                        ),
                        DnsServerResult(
                            "Quad9",
                            "9.9.9.9",
                            records = listOf(
                                DnsRecordResult("A", DnsLookupTransport.TcpFallback, listOf("93.184.216.34")),
                                DnsRecordResult("AAAA", DnsLookupTransport.TcpFallback, listOf("2606:2800:220:1:248:1893:25c8:1946")),
                            ),
                        ),
                    ),
                    status = DnsAnalysisStatus.Ok,
                    summary = "System resolver shares at least one IP with public resolvers.",
                    possibleDnsBlocking = false,
                    possibleDnsPoisoning = false,
                    checkedAt = "09:41:03",
                ),
                websiteAccessibilityResults = listOf(
                    WebsiteAccessibilityResult(
                        serviceName = "GitHub",
                        targetUrl = "https://github.com/",
                        status = WebsiteAccessibilityStatus.Ok,
                        diagnostics = ConnectivityTestResult(
                            steps = listOf(
                                ConnectivityStepResult("DNS", true, "OK, 140.82.121.4 in 16 ms"),
                                ConnectivityStepResult("TCP", true, "Connected to 140.82.121.4:443 in 24 ms"),
                                ConnectivityStepResult("TLS", true, "TLSv1.3 handshake in 41 ms"),
                                ConnectivityStepResult("HTTP", true, "Response 200 in 77 ms"),
                            ),
                            checkedAt = "09:41:03",
                            endpointLabel = "GitHub",
                            endpointUrl = "https://github.com/",
                            fallbackUsed = false,
                        ),
                    )
                ),
                protocolAnalysisResult = ProtocolAnalysisResult(
                    tests = listOf(
                        ProtocolTestResult(
                            protocol = ProtocolProbeKind.Http11,
                            endpointLabel = "Cloudflare",
                            endpointUrl = "https://www.cloudflare.com/cdn-cgi/trace",
                            status = ProtocolProbeStatus.Supported,
                            summary = "Received a valid HTTP/1.1 response.",
                            negotiatedProtocol = "http/1.1",
                            dnsTimeMs = 12,
                            tcpTimeMs = 22,
                            tlsTimeMs = 34,
                            totalTimeMs = 61,
                            httpStatusCode = 200,
                            ipAddress = "104.16.132.229",
                        ),
                        ProtocolTestResult(
                            protocol = ProtocolProbeKind.Http2,
                            endpointLabel = "Google",
                            endpointUrl = "https://www.google.com/generate_204",
                            status = ProtocolProbeStatus.Fallback,
                            summary = "ALPN negotiated HTTP/1.1 instead of h2.",
                            negotiatedProtocol = "http/1.1",
                            dnsTimeMs = 11,
                            tcpTimeMs = 25,
                            tlsTimeMs = 38,
                            totalTimeMs = 74,
                            ipAddress = "142.250.74.132",
                            errorCategory = ProtocolProbeErrorCategory.AlpnFailure,
                        ),
                        ProtocolTestResult(
                            protocol = ProtocolProbeKind.Http3,
                            endpointLabel = "Cloudflare",
                            endpointUrl = "https://www.cloudflare.com/cdn-cgi/trace",
                            status = ProtocolProbeStatus.Supported,
                            summary = "Cronet negotiated h3 successfully.",
                            negotiatedProtocol = "h3",
                            dnsTimeMs = 9,
                            totalTimeMs = 57,
                            httpStatusCode = 200,
                            ipAddress = "104.16.132.229",
                        ),
                    ),
                    observations = listOf(
                        ProtocolObservation(
                            code = "ALPN_FALLBACK_OBSERVED",
                            title = "HTTP/2 fallback observed",
                            summary = "One tested endpoint negotiated HTTP/1.1 instead of HTTP/2.",
                        )
                    ),
                    checkedAt = "09:41:03",
                ),
                tlsAnalysisResult = TlsAnalysisResult(
                    endpoints = listOf(
                        TlsEndpointAnalysis(
                            endpointLabel = "Cloudflare",
                            endpointUrl = "https://www.cloudflare.com/cdn-cgi/trace",
                            status = TlsEndpointStatus.Normal,
                            summary = "TLS handshake completed normally.",
                            ipAddress = "104.16.132.229",
                            tlsVersion = "TLSv1.3",
                            cipherSuite = "TLS_AES_128_GCM_SHA256",
                            alpn = "h2",
                            certificate = TlsCertificateInfo(
                                subject = "*.cloudflare.com",
                                issuer = "Google Trust Services",
                                validFrom = "2026-01-01",
                                validUntil = "2026-04-01",
                                publicKey = "EC 256",
                                fingerprintSha256 = "AA:BB:CC:DD:EE:FF",
                            ),
                            certificateChain = listOf(
                                "*.cloudflare.com",
                                "WE1",
                                "GTS Root R4",
                            ),
                            supportedVersions = listOf(
                                TlsVersionSupport("TLSv1.3", true),
                                TlsVersionSupport("TLSv1.2", true),
                            ),
                            dnsTimeMs = 10,
                            tcpTimeMs = 18,
                            tlsTimeMs = 32,
                            totalTimeMs = 60,
                        )
                    ),
                    status = TlsAnalysisHeuristicStatus.NoTlsAnomalies,
                    observations = listOf(
                        TlsObservation(
                            code = "NO_TLS_ANOMALIES",
                            title = "No TLS anomalies detected",
                            summary = "The certificate chain and negotiated TLS parameters look normal for the tested endpoint.",
                        )
                    ),
                    checkedAt = "09:41:03",
                ),
                sniMitmAnalysisResult = SniMitmAnalysisResult(
                    providers = listOf(
                        SniProviderAnalysis(
                            providerLabel = "Cloudflare",
                            endpointUrl = "https://www.cloudflare.com/cdn-cgi/trace",
                            status = SniAnalysisStatus.Normal,
                            summary = "All tested SNI variants completed without strong signs of filtering.",
                            variants = listOf(
                                SniVariantResult(
                                    variant = SniVariantType.NormalSni,
                                    requestedHost = "www.cloudflare.com",
                                    sniValue = "www.cloudflare.com",
                                    dnsResolved = true,
                                    tcpConnected = true,
                                    tlsHandshakeSucceeded = true,
                                    ipAddress = "104.16.132.229",
                                    tlsVersion = "TLSv1.3",
                                    cipherSuite = "TLS_AES_128_GCM_SHA256",
                                    alpn = "h2",
                                    certificate = TlsCertificateInfo(
                                        subject = "*.cloudflare.com",
                                        issuer = "Google Trust Services",
                                        validFrom = "2026-01-01",
                                        validUntil = "2026-04-01",
                                        publicKey = "EC 256",
                                        fingerprintSha256 = "AA:BB:CC:DD:EE:FF",
                                    ),
                                    certificateChain = listOf("*.cloudflare.com", "WE1", "GTS Root R4"),
                                    certificateMatchesRequestedHost = true,
                                    certificateChainTrustedBySystem = true,
                                    dnsTimeMs = 10,
                                    tcpTimeMs = 18,
                                    tlsTimeMs = 31,
                                    totalTimeMs = 59,
                                ),
                                SniVariantResult(
                                    variant = SniVariantType.NoSni,
                                    requestedHost = "www.cloudflare.com",
                                    sniValue = null,
                                    dnsResolved = true,
                                    tcpConnected = true,
                                    tlsHandshakeSucceeded = true,
                                    ipAddress = "104.16.132.229",
                                    certificateMatchesRequestedHost = false,
                                    certificateChainTrustedBySystem = true,
                                ),
                            ),
                        )
                    ),
                    status = SniAnalysisStatus.Normal,
                    observations = listOf(
                        SniObservation(
                            code = "NO_CLEAR_SNI_INTERFERENCE",
                            title = "No clear SNI interference detected",
                            summary = "The baseline handshake completed normally and no strong MITM indicators were observed in preview data.",
                        )
                    ),
                    checkedAt = "09:41:03",
                ),
                pingResult = PingResult(
                    host = "1.1.1.1",
                    packetsSent = 5,
                    packetsReceived = 5,
                    packetLoss = 0f,
                    minLatencyMs = 12.0,
                    avgLatencyMs = 15.8,
                    maxLatencyMs = 22.4,
                    jitterMs = 3.1,
                ),
                pingPacketCountInput = "",
                pingPacketStatuses = listOf(
                    PingPacketStatus(1, PingPacketState.Reply, latencyMs = 12.0, detail = "12.0 ms"),
                    PingPacketStatus(2, PingPacketState.Reply, latencyMs = 14.8, detail = "14.8 ms"),
                    PingPacketStatus(3, PingPacketState.Reply, latencyMs = 18.1, detail = "18.1 ms"),
                    PingPacketStatus(4, PingPacketState.Lost, detail = "No reply"),
                    PingPacketStatus(5, PingPacketState.Reply, latencyMs = 22.4, detail = "22.4 ms"),
                ),
                tracerouteResult = TracerouteResult(
                    destination = "google.com",
                    hops = listOf(
                        TracerouteHop(1, "192.168.1.1", "router", 1.2, false),
                        TracerouteHop(2, "100.64.0.1", null, 8.7, false),
                        TracerouteHop(3, "142.250.74.132", "google.com", 24.1, false),
                    ),
                ),
                lastReportFormat = ReportFormat.Json,
                exportedReportName = "shannon_report_123.json",
            ),
            contentPadding = PaddingValues(),
            onOpenScreen = {},
            onNavigateBack = {},
            onRefreshOverview = {},
            onRunConnectivityTest = {},
            onSelectTargetPreset = {},
            onRunWebsiteAccessibilityTest = {},
            onSelectWebsitePreset = {},
            onUpdateCustomWebsiteInput = {},
            onAddCustomWebsiteTarget = {},
            onRemoveCustomWebsiteTarget = {},
            onUpdateDnsAnalysisDomain = {},
            onRunDnsAnalysis = {},
            onRunProtocolAnalysis = {},
            onRunTlsAnalysis = {},
            onRunSniMitmAnalysis = {},
            onUpdatePortScanHost = {},
            onUpdatePortScanInput = {},
            onUpdatePortScanTransport = {},
            onUpdatePortScanIpVersion = {},
            onRunSinglePortScan = {},
            onRunQuickPortScan = {},
            onUpdatePingHost = {},
            onUpdatePingPacketCountInput = {},
            onRunPing = {},
            onUpdateTracerouteHost = {},
            onRunTraceroute = {},
            onExportReport = {},
            onOpenAboutShannon = {},
        )
    }
}

private fun shareExportedReport(
    context: Context,
    file: File,
    format: ReportFormat,
) {
    require(file.exists()) { "Exported report file is missing" }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = format.shareMimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, file.name)
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(shareIntent, "Share report").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(chooserIntent)
}

private val ReportFormat.shareMimeType: String
    get() = when (this) {
        ReportFormat.Json -> "application/json"
        ReportFormat.Markdown -> "text/markdown"
        ReportFormat.Text -> "text/plain"
    }

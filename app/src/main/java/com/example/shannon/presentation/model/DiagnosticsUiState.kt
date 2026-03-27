package com.example.shannon.presentation.model

import com.example.shannon.domain.model.ConnectivityTestResult
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.DnsAnalysisResult
import com.example.shannon.domain.model.HomeDashboardStatus
import com.example.shannon.domain.model.HomeDashboardStatusKey
import com.example.shannon.domain.model.PingResult
import com.example.shannon.domain.model.PingPacketStatus
import com.example.shannon.domain.model.NetworkOverview
import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanResult
import com.example.shannon.domain.model.PortScanTransport
import com.example.shannon.domain.model.ProtocolAnalysisResult
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.TlsAnalysisResult
import com.example.shannon.domain.model.TracerouteResult
import com.example.shannon.domain.model.WebsiteAccessibilityPreset
import com.example.shannon.domain.model.WebsiteAccessibilityResult
import com.example.shannon.domain.model.WebsiteAccessibilityTarget

data class DiagnosticsUiState(
    val currentScreen: DiagnosticsDestination = DiagnosticsDestination.Home,
    val persistedHomeStatuses: Map<HomeDashboardStatusKey, HomeDashboardStatus> = emptyMap(),
    val overview: NetworkOverview? = null,
    val testResult: ConnectivityTestResult? = null,
    val isRunning: Boolean = false,
    val selectedTargetPreset: ConnectivityTargetPreset = ConnectivityTargetPreset.Standard,
    val websiteAccessibilityResults: List<WebsiteAccessibilityResult> = emptyList(),
    val isRunningWebsiteAccessibility: Boolean = false,
    val selectedWebsitePreset: WebsiteAccessibilityPreset = WebsiteAccessibilityPreset.Baseline,
    val customWebsiteInput: String = "",
    val customWebsiteTargets: List<WebsiteAccessibilityTarget> = emptyList(),
    val dnsAnalysisDomain: String = "example.com",
    val dnsAnalysisResult: DnsAnalysisResult? = null,
    val isRunningDnsAnalysis: Boolean = false,
    val protocolAnalysisResult: ProtocolAnalysisResult? = null,
    val isRunningProtocolAnalysis: Boolean = false,
    val tlsAnalysisResult: TlsAnalysisResult? = null,
    val isRunningTlsAnalysis: Boolean = false,
    val sniMitmAnalysisResult: SniMitmAnalysisResult? = null,
    val isRunningSniMitmAnalysis: Boolean = false,
    val portScanHost: String = "example.com",
    val portScanInput: String = "443",
    val portScanTransport: PortScanTransport = PortScanTransport.Tcp,
    val portScanIpVersion: PortScanIpVersion = PortScanIpVersion.IPv4,
    val portScanResults: List<PortScanResult> = emptyList(),
    val isRunningPortScan: Boolean = false,
    val portScanError: String? = null,
    val pingHost: String = "1.1.1.1",
    val pingPacketCountInput: String = "",
    val pingPacketStatuses: List<PingPacketStatus> = emptyList(),
    val pingResult: PingResult? = null,
    val isRunningPing: Boolean = false,
    val pingError: String? = null,
    val tracerouteHost: String = "google.com",
    val tracerouteResult: TracerouteResult? = null,
    val isRunningTraceroute: Boolean = false,
    val tracerouteError: String? = null,
    val isExportingReport: Boolean = false,
    val lastReportFormat: ReportFormat? = null,
    val exportedReportName: String? = null,
    val pendingSharedReportPath: String? = null,
    val pendingSharedReportFormat: ReportFormat? = null,
    val reportError: String? = null,
)

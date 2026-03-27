package com.example.shannon.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shannon.R
import com.example.shannon.millisecondsText
import com.example.shannon.portScanSummaryText
import com.example.shannon.titleResId
import com.example.shannon.data.AndroidNetworkDiagnosticsRepository
import com.example.shannon.data.AndroidPortScanRepository
import com.example.shannon.data.HomeDashboardStatusStore
import com.example.shannon.data.ShannonDatabase
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.DnsAnalysisResult
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.HomeDashboardStatus
import com.example.shannon.domain.model.HomeDashboardStatusKey
import com.example.shannon.domain.model.HomeDashboardStatusTone
import com.example.shannon.domain.model.NetworkDiagnosticReport
import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanResult
import com.example.shannon.domain.model.PortScanTransport
import com.example.shannon.domain.model.PortStatus
import com.example.shannon.domain.model.PingPacketState
import com.example.shannon.domain.model.PingPacketStatus
import com.example.shannon.domain.model.PingResult
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.domain.model.TlsAnalysisResult
import com.example.shannon.domain.model.TracerouteResult
import com.example.shannon.domain.model.WebsiteAccessibilityOutcome
import com.example.shannon.domain.model.WebsiteAccessibilityPreset
import com.example.shannon.domain.model.WebsiteAccessibilityResult
import com.example.shannon.domain.model.WebsiteAccessibilityTarget
import com.example.shannon.domain.model.toOutcome
import com.example.shannon.domain.usecase.ExportReportUseCase
import com.example.shannon.domain.usecase.ReadNetworkOverviewUseCase
import com.example.shannon.domain.usecase.RunPingUseCase
import com.example.shannon.domain.usecase.RunConnectivityTestUseCase
import com.example.shannon.domain.usecase.RunDnsAnalysisUseCase
import com.example.shannon.domain.usecase.RunProtocolAnalysisUseCase
import com.example.shannon.domain.usecase.RunSniMitmAnalysisUseCase
import com.example.shannon.domain.usecase.RunTlsAnalysisUseCase
import com.example.shannon.domain.usecase.RunTracerouteUseCase
import com.example.shannon.domain.usecase.RunWebsiteAccessibilityTestUseCase
import com.example.shannon.domain.usecase.ScanPortUseCase
import com.example.shannon.presentation.model.DiagnosticsDestination
import com.example.shannon.presentation.model.DiagnosticsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class NetworkDiagnosticsViewModel(
    private val appContext: Context,
    private val readNetworkOverview: ReadNetworkOverviewUseCase,
    private val runConnectivityTest: RunConnectivityTestUseCase,
    private val runWebsiteAccessibilityTest: RunWebsiteAccessibilityTestUseCase,
    private val runDnsAnalysis: RunDnsAnalysisUseCase,
    private val runProtocolAnalysis: RunProtocolAnalysisUseCase,
    private val runTlsAnalysis: RunTlsAnalysisUseCase,
    private val runSniMitmAnalysis: RunSniMitmAnalysisUseCase,
    private val scanPort: ScanPortUseCase,
    private val runPing: RunPingUseCase,
    private val runTraceroute: RunTracerouteUseCase,
    private val exportDiagnosticReport: ExportReportUseCase,
    private val homeDashboardStatusStore: HomeDashboardStatusStore,
) : ViewModel() {
    companion object {
        private const val MaxPingPacketCount = 50
    }

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refreshOverview()
        viewModelScope.launch {
            val statuses = withContext(Dispatchers.IO) {
                homeDashboardStatusStore.loadStatuses()
            }
            _uiState.update { it.copy(persistedHomeStatuses = statuses) }
        }
    }

    fun openScreen(destination: DiagnosticsDestination) {
        _uiState.update { it.copy(currentScreen = destination) }
        when (destination) {
            DiagnosticsDestination.Home -> Unit
            DiagnosticsDestination.Overview -> refreshOverview()
            DiagnosticsDestination.DnsAnalysis -> {
                if (_uiState.value.dnsAnalysisResult == null && !_uiState.value.isRunningDnsAnalysis) {
                    launchDnsAnalysis()
                }
            }
            DiagnosticsDestination.ProtocolAnalysis -> {
                if (_uiState.value.protocolAnalysisResult == null &&
                    !_uiState.value.isRunningProtocolAnalysis
                ) {
                    launchProtocolAnalysis()
                }
            }
            DiagnosticsDestination.TlsAnalysis -> {
                if (_uiState.value.tlsAnalysisResult == null &&
                    !_uiState.value.isRunningTlsAnalysis
                ) {
                    launchTlsAnalysis()
                }
            }
            DiagnosticsDestination.SniMitmAnalysis -> {
                if (_uiState.value.sniMitmAnalysisResult == null &&
                    !_uiState.value.isRunningSniMitmAnalysis
                ) {
                    launchSniMitmAnalysis()
                }
            }
            DiagnosticsDestination.PortScan -> Unit
            DiagnosticsDestination.PingDiagnostics -> Unit
            DiagnosticsDestination.TracerouteDiagnostics -> Unit
            DiagnosticsDestination.ReportExport -> Unit
            DiagnosticsDestination.About -> Unit
            DiagnosticsDestination.WebsiteAccessibility -> {
                if (_uiState.value.websiteAccessibilityResults.isEmpty() &&
                    !_uiState.value.isRunningWebsiteAccessibility
                ) {
                    launchWebsiteAccessibilityTest()
                }
            }
            DiagnosticsDestination.ConnectivityTest -> {
                if (_uiState.value.testResult == null && !_uiState.value.isRunning) {
                    launchConnectivityTest()
                }
            }
        }
    }

    fun updateDnsAnalysisDomain(domain: String) {
        _uiState.update {
            it.copy(
                dnsAnalysisDomain = domain,
                dnsAnalysisResult = null,
            )
        }
    }

    fun launchDnsAnalysis() {
        if (_uiState.value.isRunningDnsAnalysis) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningDnsAnalysis = true) }
            val result = runDnsAnalysis(_uiState.value.dnsAnalysisDomain)
            _uiState.update {
                it.copy(
                    dnsAnalysisResult = result,
                    isRunningDnsAnalysis = false,
                )
            }
            persistHomeStatus(dnsDashboardStatus(result))
        }
    }

    fun goBack() {
        _uiState.update { it.copy(currentScreen = DiagnosticsDestination.Home) }
    }

    fun launchProtocolAnalysis() {
        if (_uiState.value.isRunningProtocolAnalysis) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningProtocolAnalysis = true) }
            val result = runProtocolAnalysis()
            _uiState.update {
                it.copy(
                    protocolAnalysisResult = result,
                    isRunningProtocolAnalysis = false,
                )
            }
        }
    }

    fun launchTlsAnalysis() {
        if (_uiState.value.isRunningTlsAnalysis) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningTlsAnalysis = true) }
            val result = runTlsAnalysis()
            _uiState.update {
                it.copy(
                    tlsAnalysisResult = result,
                    isRunningTlsAnalysis = false,
                )
            }
            persistHomeStatus(tlsDashboardStatus(result))
        }
    }

    fun launchSniMitmAnalysis() {
        if (_uiState.value.isRunningSniMitmAnalysis) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningSniMitmAnalysis = true) }
            val result = runSniMitmAnalysis()
            _uiState.update {
                it.copy(
                    sniMitmAnalysisResult = result,
                    isRunningSniMitmAnalysis = false,
                )
            }
            persistHomeStatus(sniDashboardStatus(result))
        }
    }

    fun updatePortScanHost(host: String) {
        _uiState.update {
            it.copy(
                portScanHost = host,
                portScanResults = emptyList(),
                portScanError = null,
            )
        }
    }

    fun updatePortScanInput(value: String) {
        _uiState.update {
            it.copy(
                portScanInput = value,
                portScanResults = emptyList(),
                portScanError = null,
            )
        }
    }

    fun updatePortScanTransport(transport: PortScanTransport) {
        _uiState.update {
            it.copy(
                portScanTransport = transport,
                portScanResults = emptyList(),
                portScanError = null,
            )
        }
    }

    fun updatePortScanIpVersion(ipVersion: PortScanIpVersion) {
        _uiState.update {
            it.copy(
                portScanIpVersion = ipVersion,
                portScanResults = emptyList(),
                portScanError = null,
            )
        }
    }

    fun launchSinglePortScan() {
        if (_uiState.value.isRunningPortScan) return
        val host = _uiState.value.portScanHost.trim()
        val port = _uiState.value.portScanInput.trim().toIntOrNull()
        val transport = _uiState.value.portScanTransport
        val ipVersion = _uiState.value.portScanIpVersion
        if (host.isBlank()) {
            _uiState.update { it.copy(portScanError = appContext.getString(R.string.error_host_required)) }
            return
        }
        if (port == null) {
            _uiState.update { it.copy(portScanError = appContext.getString(R.string.error_port_must_be_number)) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningPortScan = true,
                    portScanError = null,
                )
            }
            runCatching {
                scanPort(host, port, transport, ipVersion)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isRunningPortScan = false,
                        portScanResults = listOf(result),
                    )
                }
                persistHomeStatus(portScanDashboardStatus(listOf(result)))
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRunningPortScan = false,
                        portScanError = error.message ?: appContext.getString(R.string.error_port_scan_failed),
                    )
                }
                persistHomeStatus(
                    errorDashboardStatus(
                        key = HomeDashboardStatusKey.PortScan,
                        text = appContext.getString(R.string.protocol_status_failed),
                    )
                )
            }
        }
    }

    fun launchQuickPortScan() {
        if (_uiState.value.isRunningPortScan) return
        val host = _uiState.value.portScanHost.trim()
        val transport = _uiState.value.portScanTransport
        val ipVersion = _uiState.value.portScanIpVersion
        if (host.isBlank()) {
            _uiState.update { it.copy(portScanError = appContext.getString(R.string.error_host_required)) }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningPortScan = true,
                    portScanError = null,
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        QuickScanPorts.map { port ->
                            async { scanPort(host, port, transport, ipVersion) }
                        }.awaitAll().sortedBy { it.port }
                    }
                }
            }.onSuccess { results ->
                _uiState.update {
                    it.copy(
                        isRunningPortScan = false,
                        portScanResults = results,
                    )
                }
                persistHomeStatus(portScanDashboardStatus(results))
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRunningPortScan = false,
                        portScanError = error.message ?: appContext.getString(R.string.error_quick_scan_failed),
                    )
                }
                persistHomeStatus(
                    errorDashboardStatus(
                        key = HomeDashboardStatusKey.PortScan,
                        text = appContext.getString(R.string.protocol_status_failed),
                    )
                )
            }
        }
    }

    fun updatePingHost(host: String) {
        _uiState.update {
            it.copy(
                pingHost = host,
                pingPacketStatuses = emptyList(),
                pingResult = null,
                pingError = null,
            )
        }
    }

    fun updatePingPacketCountInput(value: String) {
        val sanitized = value.filter(Char::isDigit).take(3)
        _uiState.update {
            it.copy(
                pingPacketCountInput = sanitized,
                pingPacketStatuses = emptyList(),
                pingResult = null,
                pingError = null,
            )
        }
    }

    fun launchPing() {
        if (_uiState.value.isRunningPing) return
        val initialState = _uiState.value
        val host = initialState.pingHost
        val packetCount = initialState.pingPacketCountInput.toIntOrNull() ?: 5
        val normalizedHost = host.trim().ifBlank { "1.1.1.1" }
        if (packetCount <= 0) {
            _uiState.update { it.copy(pingError = appContext.getString(R.string.error_packet_count_positive)) }
            return
        }
        if (packetCount > MaxPingPacketCount) {
            _uiState.update {
                it.copy(pingError = appContext.getString(R.string.error_packet_count_max, MaxPingPacketCount))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningPing = true,
                    pingResult = null,
                    pingError = null,
                    pingPacketStatuses = (1..packetCount).map { packetNumber ->
                        PingPacketStatus(
                            sequenceNumber = packetNumber,
                            state = PingPacketState.Pending,
                        )
                    },
                )
            }

            val latencies = mutableListOf<Double>()
            var terminalError: String? = null

            repeat(packetCount) { index ->
                val sequenceNumber = index + 1
                runCatching {
                    runPing(host, 1)
                }.onSuccess { packetResult ->
                    val latency = packetResult.avgLatencyMs
                    latencies += latency
                    _uiState.update { state ->
                        state.copy(
                            pingPacketStatuses = state.pingPacketStatuses.map { packet ->
                                if (packet.sequenceNumber == sequenceNumber) {
                                    packet.copy(
                                        state = PingPacketState.Reply,
                                        latencyMs = latency,
                                        detail = appContext.millisecondsText(latency),
                                    )
                                } else {
                                    packet
                                }
                            }
                        )
                    }
                }.onFailure { error ->
                    val message = error.message ?: appContext.getString(R.string.error_packet_failed)
                    val packetState = if (
                        message.contains("No successful ping replies", ignoreCase = true)
                    ) {
                        PingPacketState.Lost
                    } else {
                        PingPacketState.Error
                    }
                    terminalError = terminalError ?: message
                    _uiState.update { state ->
                        state.copy(
                            pingPacketStatuses = state.pingPacketStatuses.map { packet ->
                                if (packet.sequenceNumber == sequenceNumber) {
                                    packet.copy(
                                        state = packetState,
                                        detail = message,
                                    )
                                } else {
                                    packet
                                }
                            }
                        )
                    }
                }
            }

            if (latencies.isNotEmpty()) {
                val result = PingResult.fromLatencies(
                    host = normalizedHost,
                    packetsSent = packetCount,
                    latencies = latencies,
                )
                _uiState.update {
                    it.copy(
                        pingResult = result,
                        isRunningPing = false,
                    )
                }
                persistHomeStatus(pingDashboardStatus(result))
            } else {
                _uiState.update {
                    it.copy(
                        isRunningPing = false,
                        pingError = terminalError ?: appContext.getString(R.string.error_ping_failed),
                    )
                }
                persistHomeStatus(
                    errorDashboardStatus(
                        key = HomeDashboardStatusKey.PingDiagnostics,
                        text = appContext.getString(R.string.protocol_status_failed),
                    )
                )
            }
        }
    }

    fun updateTracerouteHost(host: String) {
        _uiState.update {
            it.copy(
                tracerouteHost = host,
                tracerouteError = null,
            )
        }
    }

    fun launchTraceroute() {
        if (_uiState.value.isRunningTraceroute) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningTraceroute = true,
                    tracerouteError = null,
                )
            }
            runCatching {
                runTraceroute(_uiState.value.tracerouteHost)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        tracerouteResult = result,
                        isRunningTraceroute = false,
                    )
                }
                persistHomeStatus(tracerouteDashboardStatus(result))
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRunningTraceroute = false,
                        tracerouteError = error.message ?: appContext.getString(R.string.error_traceroute_failed),
                    )
                }
                persistHomeStatus(
                    errorDashboardStatus(
                        key = HomeDashboardStatusKey.TracerouteDiagnostics,
                        text = appContext.getString(R.string.protocol_status_failed),
                    )
                )
            }
        }
    }

    fun exportReport(format: ReportFormat) {
        if (_uiState.value.isExportingReport) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExportingReport = true,
                    reportError = null,
                    pendingSharedReportPath = null,
                    pendingSharedReportFormat = null,
                )
            }
            val report = snapshotReport(_uiState.value)
            runCatching {
                exportDiagnosticReport(report, format)
            }.onSuccess { path ->
                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        lastReportFormat = format,
                        exportedReportName = File(path).name,
                        pendingSharedReportPath = path,
                        pendingSharedReportFormat = format,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isExportingReport = false,
                        reportError = error.message ?: appContext.getString(R.string.error_report_export_failed),
                    )
                }
            }
        }
    }

    fun onReportShareHandled(error: String? = null) {
        _uiState.update {
            it.copy(
                pendingSharedReportPath = null,
                pendingSharedReportFormat = null,
                reportError = error ?: it.reportError,
            )
        }
    }

    fun refreshOverview() {
        viewModelScope.launch {
            val overview = readNetworkOverview()
            _uiState.update { it.copy(overview = overview) }
        }
    }

    fun launchConnectivityTest() {
        if (_uiState.value.isRunning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true) }
            val overview = readNetworkOverview()
            _uiState.update { it.copy(overview = overview) }
            val result = runConnectivityTest(_uiState.value.selectedTargetPreset)
            _uiState.update { it.copy(testResult = result, isRunning = false) }
            persistHomeStatus(connectivityDashboardStatus(result))
        }
    }

    fun selectTargetPreset(targetPreset: ConnectivityTargetPreset) {
        _uiState.update {
            if (it.selectedTargetPreset == targetPreset) {
                it
            } else {
                it.copy(
                    selectedTargetPreset = targetPreset,
                    testResult = null,
                )
            }
        }
    }

    fun launchWebsiteAccessibilityTest() {
        if (_uiState.value.isRunningWebsiteAccessibility) return
        viewModelScope.launch {
            val state = _uiState.value
            val targets = if (state.selectedWebsitePreset == WebsiteAccessibilityPreset.Custom) {
                state.customWebsiteTargets
            } else {
                state.selectedWebsitePreset.targets
            }.distinctBy { it.url }
            if (targets.isEmpty()) {
                _uiState.update {
                    it.copy(
                        websiteAccessibilityResults = emptyList(),
                        isRunningWebsiteAccessibility = false,
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isRunningWebsiteAccessibility = true) }
            val results = runWebsiteAccessibilityTest(targets)
            _uiState.update {
                it.copy(
                    websiteAccessibilityResults = results,
                    isRunningWebsiteAccessibility = false,
                )
            }
            if (results.isNotEmpty()) {
                persistHomeStatus(websiteDashboardStatus(results))
            }
        }
    }

    fun selectWebsitePreset(preset: WebsiteAccessibilityPreset) {
        _uiState.update {
            if (it.selectedWebsitePreset == preset) {
                it
            } else {
                it.copy(
                    selectedWebsitePreset = preset,
                    websiteAccessibilityResults = emptyList(),
                )
            }
        }
    }

    fun updateCustomWebsiteInput(value: String) {
        _uiState.update { it.copy(customWebsiteInput = value) }
    }

    fun addCustomWebsiteTarget() {
        val normalized = normalizeWebsiteTarget(_uiState.value.customWebsiteInput) ?: return
        _uiState.update {
            if (it.customWebsiteTargets.any { target -> target.url == normalized.url }) {
                it.copy(
                    customWebsiteInput = "",
                    selectedWebsitePreset = WebsiteAccessibilityPreset.Custom,
                )
            } else {
                it.copy(
                    customWebsiteInput = "",
                    selectedWebsitePreset = WebsiteAccessibilityPreset.Custom,
                    customWebsiteTargets = it.customWebsiteTargets + normalized,
                    websiteAccessibilityResults = emptyList(),
                )
            }
        }
    }

    fun removeCustomWebsiteTarget(url: String) {
        _uiState.update {
            it.copy(
                customWebsiteTargets = it.customWebsiteTargets.filterNot { target -> target.url == url },
                websiteAccessibilityResults = emptyList(),
            )
        }
    }

    private fun normalizeWebsiteTarget(input: String): WebsiteAccessibilityTarget? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        val normalizedUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return runCatching {
            val parsed = java.net.URL(normalizedUrl)
            val host = parsed.host.takeIf { it.isNotBlank() } ?: return null
            val finalUrl = parsed.toString()
            WebsiteAccessibilityTarget(
                name = host,
                url = finalUrl,
            )
        }.getOrNull()
    }

    private fun snapshotReport(state: DiagnosticsUiState): NetworkDiagnosticReport {
        return NetworkDiagnosticReport(
            overview = state.overview,
            connectivityTest = state.testResult,
            dnsAnalysis = state.dnsAnalysisResult,
            protocolAnalysis = state.protocolAnalysisResult,
            tlsAnalysis = state.tlsAnalysisResult,
            sniMitmAnalysis = state.sniMitmAnalysisResult,
            websiteAccessibilityResults = state.websiteAccessibilityResults,
            pingResult = state.pingResult,
            tracerouteResult = state.tracerouteResult,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun persistHomeStatus(status: HomeDashboardStatus) {
        _uiState.update {
            it.copy(
                persistedHomeStatuses = it.persistedHomeStatuses + (status.key to status),
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            homeDashboardStatusStore.saveStatus(status)
        }
    }

    private fun connectivityDashboardStatus(result: com.example.shannon.domain.model.ConnectivityTestResult): HomeDashboardStatus {
        val failedStep = result.steps.firstOrNull { !it.success }
        return if (failedStep == null) {
            HomeDashboardStatus(
                key = HomeDashboardStatusKey.ConnectivityTest,
                text = appContext.getString(R.string.status_ok),
                tone = HomeDashboardStatusTone.Positive,
            )
        } else {
            HomeDashboardStatus(
                key = HomeDashboardStatusKey.ConnectivityTest,
                text = failedStep.stage,
                tone = HomeDashboardStatusTone.Warning,
            )
        }
    }

    private fun websiteDashboardStatus(results: List<WebsiteAccessibilityResult>): HomeDashboardStatus {
        val limitedCount = results.count { it.status.toOutcome() == WebsiteAccessibilityOutcome.Limited }
        val unstableCount = results.count { it.status.toOutcome() == WebsiteAccessibilityOutcome.Unstable }
        val (text, tone) = when {
            limitedCount > 0 -> {
                appContext.getString(R.string.home_websites_status_limited, limitedCount) to
                    HomeDashboardStatusTone.Error
            }
            unstableCount > 0 -> {
                appContext.getString(R.string.home_websites_status_unstable, unstableCount) to
                    HomeDashboardStatusTone.Warning
            }
            else -> appContext.getString(R.string.status_ok) to HomeDashboardStatusTone.Positive
        }
        return HomeDashboardStatus(
            key = HomeDashboardStatusKey.WebsiteAccessibility,
            text = text,
            tone = tone,
        )
    }

    private fun dnsDashboardStatus(result: DnsAnalysisResult): HomeDashboardStatus {
        val (text, tone) = when (result.status) {
            DnsAnalysisStatus.Ok -> appContext.getString(R.string.status_ok) to HomeDashboardStatusTone.Positive
            DnsAnalysisStatus.CdnVariation -> appContext.getString(R.string.dns_status_variation) to HomeDashboardStatusTone.Neutral
            DnsAnalysisStatus.Suspicious -> appContext.getString(R.string.status_suspicious) to HomeDashboardStatusTone.Warning
            DnsAnalysisStatus.Blocked -> appContext.getString(R.string.status_blocked) to HomeDashboardStatusTone.Error
        }
        return HomeDashboardStatus(
            key = HomeDashboardStatusKey.DnsAnalysis,
            text = text,
            tone = tone,
        )
    }

    private fun tlsDashboardStatus(result: TlsAnalysisResult): HomeDashboardStatus {
        val (text, tone) = when (result.status) {
            TlsAnalysisHeuristicStatus.NoTlsAnomalies -> appContext.getString(R.string.status_ok) to HomeDashboardStatusTone.Positive
            TlsAnalysisHeuristicStatus.UnusualCertificateChain -> appContext.getString(R.string.home_tls_status_chain) to HomeDashboardStatusTone.Warning
            TlsAnalysisHeuristicStatus.TlsDowngradeSuspected -> appContext.getString(R.string.home_tls_status_downgrade) to HomeDashboardStatusTone.Warning
            TlsAnalysisHeuristicStatus.TlsInterceptionSuspected -> appContext.getString(R.string.home_tls_status_interception) to HomeDashboardStatusTone.Warning
            TlsAnalysisHeuristicStatus.Inconclusive -> appContext.getString(R.string.status_inconclusive) to HomeDashboardStatusTone.Neutral
        }
        return HomeDashboardStatus(
            key = HomeDashboardStatusKey.TlsAnalysis,
            text = text,
            tone = tone,
        )
    }

    private fun sniDashboardStatus(result: SniMitmAnalysisResult): HomeDashboardStatus {
        val (text, tone) = when (result.status) {
            SniAnalysisStatus.Normal -> appContext.getString(R.string.status_normal) to HomeDashboardStatusTone.Positive
            SniAnalysisStatus.SniFilteringSuspected -> appContext.getString(R.string.home_sni_status_filtering) to HomeDashboardStatusTone.Warning
            SniAnalysisStatus.TlsInterceptionSuspected -> appContext.getString(R.string.home_sni_status_tls_intercept) to HomeDashboardStatusTone.Warning
            SniAnalysisStatus.MitmSuspected -> appContext.getString(R.string.home_sni_status_mitm) to HomeDashboardStatusTone.Error
            SniAnalysisStatus.Inconclusive -> appContext.getString(R.string.status_inconclusive) to HomeDashboardStatusTone.Neutral
        }
        return HomeDashboardStatus(
            key = HomeDashboardStatusKey.SniMitmAnalysis,
            text = text,
            tone = tone,
        )
    }

    private fun portScanDashboardStatus(results: List<PortScanResult>): HomeDashboardStatus {
        if (results.size == 1) {
            val result = results.single()
            return HomeDashboardStatus(
                key = HomeDashboardStatusKey.PortScan,
                text = appContext.getString(result.status.titleResId()),
                tone = when (result.status) {
                    PortStatus.OPEN -> HomeDashboardStatusTone.Positive
                    PortStatus.CLOSED -> HomeDashboardStatusTone.Error
                    PortStatus.FILTERED,
                    PortStatus.TIMEOUT -> HomeDashboardStatusTone.Warning
                    PortStatus.ERROR -> HomeDashboardStatusTone.Neutral
                },
            )
        }
        val openCount = results.count { it.status == PortStatus.OPEN }
        val closedCount = results.count { it.status == PortStatus.CLOSED }
        val warningCount = results.count { it.status == PortStatus.FILTERED || it.status == PortStatus.TIMEOUT }
        val errorCount = results.count { it.status == PortStatus.ERROR }
        val text = appContext.portScanSummaryText(openCount, closedCount, warningCount, errorCount)
        val tone = when {
            openCount == results.size -> HomeDashboardStatusTone.Positive
            openCount > 0 -> HomeDashboardStatusTone.Warning
            closedCount == results.size -> HomeDashboardStatusTone.Error
            else -> HomeDashboardStatusTone.Neutral
        }
        return HomeDashboardStatus(
            key = HomeDashboardStatusKey.PortScan,
            text = text,
            tone = tone,
        )
    }

    private fun pingDashboardStatus(result: PingResult): HomeDashboardStatus {
        val avg = result.avgLatencyMs.roundToInt()
        val tone = when {
            avg <= 60 -> HomeDashboardStatusTone.Positive
            avg <= 150 -> HomeDashboardStatusTone.Neutral
            else -> HomeDashboardStatusTone.Warning
        }
        return HomeDashboardStatus(
            key = HomeDashboardStatusKey.PingDiagnostics,
            text = appContext.millisecondsText(avg),
            tone = tone,
        )
    }

    private fun tracerouteDashboardStatus(result: TracerouteResult): HomeDashboardStatus {
        return HomeDashboardStatus(
            key = HomeDashboardStatusKey.TracerouteDiagnostics,
            text = appContext.getString(R.string.traceroute_hops_value, result.hops.size),
            tone = HomeDashboardStatusTone.Neutral,
        )
    }

    private fun errorDashboardStatus(
        key: HomeDashboardStatusKey,
        text: String,
    ): HomeDashboardStatus {
        return HomeDashboardStatus(
            key = key,
            text = text,
            tone = HomeDashboardStatusTone.Error,
        )
    }

    class Factory(
        private val applicationContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = AndroidNetworkDiagnosticsRepository(applicationContext)
            val portScanRepository = AndroidPortScanRepository(applicationContext)
            val database = ShannonDatabase.getInstance(applicationContext)
            val homeDashboardStatusStore = HomeDashboardStatusStore(
                context = applicationContext,
                dashboardStatusDao = database.dashboardStatusDao(),
            )
            return NetworkDiagnosticsViewModel(
                appContext = applicationContext,
                readNetworkOverview = ReadNetworkOverviewUseCase(repository),
                runConnectivityTest = RunConnectivityTestUseCase(repository),
                runWebsiteAccessibilityTest = RunWebsiteAccessibilityTestUseCase(repository),
                runDnsAnalysis = RunDnsAnalysisUseCase(repository),
                runProtocolAnalysis = RunProtocolAnalysisUseCase(repository),
                runTlsAnalysis = RunTlsAnalysisUseCase(repository),
                runSniMitmAnalysis = RunSniMitmAnalysisUseCase(repository),
                scanPort = ScanPortUseCase(portScanRepository),
                runPing = RunPingUseCase(repository),
                runTraceroute = RunTracerouteUseCase(repository),
                exportDiagnosticReport = ExportReportUseCase(repository),
                homeDashboardStatusStore = homeDashboardStatusStore,
            ) as T
        }
    }
}

private val QuickScanPorts = listOf(22, 80, 443, 8080, 8443)

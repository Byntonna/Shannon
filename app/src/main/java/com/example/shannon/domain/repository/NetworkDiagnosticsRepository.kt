package com.example.shannon.domain.repository

import com.example.shannon.domain.model.ConnectivityTestResult
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.DnsAnalysisResult
import com.example.shannon.domain.model.NetworkDiagnosticReport
import com.example.shannon.domain.model.NetworkOverview
import com.example.shannon.domain.model.PingResult
import com.example.shannon.domain.model.ProtocolAnalysisResult
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.TlsAnalysisResult
import com.example.shannon.domain.model.TracerouteResult
import com.example.shannon.domain.model.WebsiteAccessibilityTarget
import com.example.shannon.domain.model.WebsiteAccessibilityResult

interface NetworkDiagnosticsRepository {
    suspend fun readNetworkOverview(): NetworkOverview

    suspend fun performConnectivityTest(targetPreset: ConnectivityTargetPreset): ConnectivityTestResult

    suspend fun performWebsiteAccessibilityTest(
        targets: List<WebsiteAccessibilityTarget>,
    ): List<WebsiteAccessibilityResult>

    suspend fun performDnsAnalysis(domain: String): DnsAnalysisResult

    suspend fun performProtocolAnalysis(): ProtocolAnalysisResult

    suspend fun performTlsAnalysis(): TlsAnalysisResult

    suspend fun performSniMitmAnalysis(): SniMitmAnalysisResult

    suspend fun runPing(host: String, count: Int): PingResult

    suspend fun runTraceroute(host: String): TracerouteResult

    suspend fun exportReport(
        report: NetworkDiagnosticReport,
        format: ReportFormat,
    ): String
}

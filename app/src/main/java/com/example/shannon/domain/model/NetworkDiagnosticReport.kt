package com.example.shannon.domain.model

enum class ReportFormat(val extension: String) {
    Json("json"),
    Markdown("md"),
    Text("txt"),
}

data class NetworkDiagnosticReport(
    val overview: NetworkOverview?,
    val connectivityTest: ConnectivityTestResult?,
    val dnsAnalysis: DnsAnalysisResult?,
    val protocolAnalysis: ProtocolAnalysisResult?,
    val tlsAnalysis: TlsAnalysisResult?,
    val sniMitmAnalysis: SniMitmAnalysisResult?,
    val websiteAccessibilityResults: List<WebsiteAccessibilityResult>,
    val pingResult: PingResult?,
    val tracerouteResult: TracerouteResult?,
    val timestamp: Long,
)

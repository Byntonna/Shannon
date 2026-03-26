package com.example.shannon.domain.model

enum class ReportFormat(val title: String, val extension: String) {
    Json("JSON", "json"),
    Markdown("Markdown", "md"),
    Text("Text", "txt"),
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

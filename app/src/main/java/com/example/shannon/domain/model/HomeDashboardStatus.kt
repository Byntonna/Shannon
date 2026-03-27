package com.example.shannon.domain.model

enum class HomeDashboardStatusKey {
    ConnectivityTest,
    WebsiteAccessibility,
    DnsAnalysis,
    TlsAnalysis,
    SniMitmAnalysis,
    PortScan,
    PingDiagnostics,
    TracerouteDiagnostics,
}

enum class HomeDashboardStatusTone {
    Positive,
    Warning,
    Error,
    Neutral,
}

data class HomeDashboardStatus(
    val key: HomeDashboardStatusKey,
    val text: String,
    val tone: HomeDashboardStatusTone,
)

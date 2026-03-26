package com.example.shannon.presentation.model

enum class DiagnosticsDestination(val title: String) {
    Home("Shannon"),
    Overview("Network overview"),
    ConnectivityTest("Connectivity test"),
    DnsAnalysis("DNS analysis"),
    ProtocolAnalysis("Protocol analysis"),
    TlsAnalysis("TLS analysis"),
    SniMitmAnalysis("SNI filtering and MITM"),
    PortScan("Port scan"),
    PingDiagnostics("Ping diagnostics"),
    TracerouteDiagnostics("Traceroute diagnostics"),
    ReportExport("Report export"),
    WebsiteAccessibility("Website accessibility"),
    About("About Shannon"),
}

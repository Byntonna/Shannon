package com.example.shannon.domain.model

enum class ProtocolProbeKind(val title: String) {
    Http11("HTTP/1.1"),
    Http2("HTTP/2"),
    Http3("HTTP/3"),
    WebSocket("WebSocket"),
}

enum class ProtocolProbeStatus(val title: String) {
    Supported("Supported"),
    Failed("Failed"),
    Blocked("Blocked"),
    Fallback("Fallback"),
    Inconclusive("Inconclusive"),
}

enum class ProtocolProbeErrorCategory(val title: String) {
    DnsFailure("DNS failure"),
    TcpFailure("TCP failure"),
    TlsFailure("TLS failure"),
    AlpnFailure("ALPN failure"),
    QuicFailure("QUIC failure"),
    HttpFailure("HTTP failure"),
    WebSocketUpgradeFailure("WebSocket upgrade failure"),
    Timeout("Timeout"),
    Unknown("Unknown"),
}

data class ProtocolTestResult(
    val protocol: ProtocolProbeKind,
    val endpointLabel: String,
    val endpointUrl: String,
    val status: ProtocolProbeStatus,
    val summary: String,
    val negotiatedProtocol: String? = null,
    val dnsTimeMs: Long? = null,
    val tcpTimeMs: Long? = null,
    val tlsTimeMs: Long? = null,
    val totalTimeMs: Long? = null,
    val httpStatusCode: Int? = null,
    val ipAddress: String? = null,
    val errorCategory: ProtocolProbeErrorCategory? = null,
    val errorMessage: String? = null,
)

data class ProtocolObservation(
    val code: String,
    val title: String,
    val summary: String,
)

data class ProtocolAnalysisResult(
    val tests: List<ProtocolTestResult>,
    val observations: List<ProtocolObservation>,
    val checkedAt: String,
)

package com.example.shannon.domain.model

enum class ProtocolProbeKind {
    Http11,
    Http2,
    Http3,
    WebSocket,
}

enum class ProtocolProbeStatus {
    Supported,
    Failed,
    Blocked,
    Fallback,
    Inconclusive,
}

enum class ProtocolProbeErrorCategory {
    DnsFailure,
    TcpFailure,
    TlsFailure,
    AlpnFailure,
    QuicFailure,
    HttpFailure,
    WebSocketUpgradeFailure,
    Timeout,
    Unknown,
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

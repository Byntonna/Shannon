package com.example.shannon.domain.model

enum class DnsLookupTransport(val title: String) {
    System("System resolver"),
    Udp("UDP"),
    TcpFallback("TCP fallback"),
}

enum class DnsAnalysisStatus(val title: String) {
    Ok("DNS OK"),
    CdnVariation("CDN variation"),
    Suspicious("Suspicious"),
    Blocked("Blocked"),
}

data class DnsRecordResult(
    val recordType: String,
    val transport: DnsLookupTransport,
    val addresses: List<String>,
    val responseCode: Int? = null,
    val error: String? = null,
)

data class DnsServerResult(
    val serverName: String,
    val serverAddress: String,
    val records: List<DnsRecordResult>,
)

data class DnsAnalysisResult(
    val domain: String,
    val servers: List<DnsServerResult>,
    val status: DnsAnalysisStatus,
    val summary: String,
    val possibleDnsBlocking: Boolean,
    val possibleDnsPoisoning: Boolean,
    val checkedAt: String,
)

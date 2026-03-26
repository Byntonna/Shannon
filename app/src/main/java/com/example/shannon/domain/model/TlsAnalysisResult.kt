package com.example.shannon.domain.model

enum class TlsEndpointStatus {
    Normal,
    Failed,
    Inconclusive,
}

enum class TlsAnalysisHeuristicStatus {
    NoTlsAnomalies,
    TlsInterceptionSuspected,
    UnusualCertificateChain,
    TlsDowngradeSuspected,
    Inconclusive,
}

data class TlsVersionSupport(
    val version: String,
    val supported: Boolean,
)

data class TlsCertificateInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validUntil: String,
    val publicKey: String,
    val fingerprintSha256: String,
)

data class TlsEndpointAnalysis(
    val endpointLabel: String,
    val endpointUrl: String,
    val status: TlsEndpointStatus,
    val summary: String,
    val ipAddress: String? = null,
    val tlsVersion: String? = null,
    val cipherSuite: String? = null,
    val alpn: String? = null,
    val certificate: TlsCertificateInfo? = null,
    val certificateChain: List<String> = emptyList(),
    val supportedVersions: List<TlsVersionSupport> = emptyList(),
    val dnsTimeMs: Long? = null,
    val tcpTimeMs: Long? = null,
    val tlsTimeMs: Long? = null,
    val totalTimeMs: Long? = null,
    val errorMessage: String? = null,
)

data class TlsObservation(
    val code: String,
    val title: String,
    val summary: String,
)

data class TlsAnalysisResult(
    val endpoints: List<TlsEndpointAnalysis>,
    val status: TlsAnalysisHeuristicStatus,
    val observations: List<TlsObservation>,
    val checkedAt: String,
)

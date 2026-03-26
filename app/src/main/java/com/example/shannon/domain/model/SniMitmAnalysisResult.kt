package com.example.shannon.domain.model

enum class SniVariantType {
    NormalSni,
    AlternativeSni,
    NoSni,
    RandomSni,
}

enum class SniAnalysisStatus {
    Normal,
    SniFilteringSuspected,
    TlsInterceptionSuspected,
    MitmSuspected,
    Inconclusive,
}

enum class SniProbeErrorCategory {
    DnsFailure,
    TcpFailure,
    TlsFailure,
    RstDetected,
    Timeout,
    Unknown,
}

data class SniVariantResult(
    val variant: SniVariantType,
    val requestedHost: String,
    val sniValue: String?,
    val dnsResolved: Boolean,
    val tcpConnected: Boolean,
    val tlsHandshakeSucceeded: Boolean,
    val ipAddress: String? = null,
    val tlsVersion: String? = null,
    val cipherSuite: String? = null,
    val alpn: String? = null,
    val certificate: TlsCertificateInfo? = null,
    val certificateChain: List<String> = emptyList(),
    val certificateMatchesRequestedHost: Boolean? = null,
    val certificateChainTrustedBySystem: Boolean? = null,
    val systemTrustErrorMessage: String? = null,
    val dnsTimeMs: Long? = null,
    val tcpTimeMs: Long? = null,
    val tlsTimeMs: Long? = null,
    val totalTimeMs: Long? = null,
    val errorCategory: SniProbeErrorCategory? = null,
    val errorMessage: String? = null,
)

data class SniProviderProbeResult(
    val providerLabel: String,
    val endpointUrl: String,
    val variants: List<SniVariantResult>,
)

data class SniProviderAnalysis(
    val providerLabel: String,
    val endpointUrl: String,
    val status: SniAnalysisStatus,
    val summary: String,
    val observations: List<SniObservation> = emptyList(),
    val variants: List<SniVariantResult>,
)

data class SniObservation(
    val code: String,
    val title: String,
    val summary: String,
)

data class SniMitmAnalysisResult(
    val providers: List<SniProviderAnalysis>,
    val status: SniAnalysisStatus,
    val observations: List<SniObservation>,
    val checkedAt: String,
)

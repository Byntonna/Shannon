package com.example.shannon.domain.analysis

import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniProviderProbeResult
import com.example.shannon.domain.model.SniVariantResult
import com.example.shannon.domain.model.SniVariantType
import com.example.shannon.domain.model.TlsCertificateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SniMitmAnalysisEngineTest {
    private val engine = SniMitmAnalysisEngine { resId, _ -> "res-$resId" }

    @Test
    fun normalRunStaysNormalWhenOnlyExpectedVariantDifferencesExist() {
        val result = engine.analyze(
            probeResults = listOf(
                provider(
                    label = "Cloudflare",
                    variants = listOf(
                        successfulVariant(SniVariantType.NormalSni),
                        successfulVariant(
                            SniVariantType.RandomSni,
                            sniValue = "random-123.invalid",
                            hostnameMatch = false,
                        ),
                    ),
                )
            ),
            checkedAt = "12:00:00",
        )

        assertEquals(SniAnalysisStatus.Normal, result.status)
        assertEquals(SniAnalysisStatus.Normal, result.providers.single().status)
        assertTrue(result.observations.any { it.code == "NO_CLEAR_SNI_INTERFERENCE" })
    }

    @Test
    fun selectiveNormalSniFailureIsReportedAsPossibleSniFiltering() {
        val result = engine.analyze(
            probeResults = listOf(
                provider(
                    label = "Wikipedia",
                    variants = listOf(
                        failedVariant(SniVariantType.NormalSni, dnsResolved = true, tcpConnected = true),
                        successfulVariant(SniVariantType.AlternativeSni, sniValue = "example.com"),
                        successfulVariant(SniVariantType.NoSni, sniValue = null, hostnameMatch = false),
                        successfulVariant(SniVariantType.RandomSni, sniValue = "random-123.invalid", hostnameMatch = false),
                    ),
                )
            ),
            checkedAt = "12:00:00",
        )

        assertEquals(SniAnalysisStatus.SniFilteringSuspected, result.status)
        assertEquals(SniAnalysisStatus.SniFilteringSuspected, result.providers.single().status)
        assertTrue(result.observations.any { it.code == "SNI_FILTERING_SUSPECTED" })
    }

    @Test
    fun repeatedUntrustedIssuerAcrossProvidersEscalatesToMitmSuspected() {
        val result = engine.analyze(
            probeResults = listOf(
                provider(
                    label = "Google",
                    variants = listOf(
                        successfulVariant(
                            variant = SniVariantType.NormalSni,
                            fingerprint = "AA:BB:CC",
                            issuer = "Local Proxy CA",
                            trustedBySystem = false,
                        )
                    ),
                ),
                provider(
                    label = "GitHub",
                    variants = listOf(
                        successfulVariant(
                            variant = SniVariantType.NormalSni,
                            fingerprint = "DD:EE:FF",
                            issuer = "Local Proxy CA",
                            trustedBySystem = false,
                        )
                    ),
                ),
            ),
            checkedAt = "12:00:00",
        )

        assertEquals(SniAnalysisStatus.MitmSuspected, result.status)
        assertTrue(result.providers.all { it.status == SniAnalysisStatus.TlsInterceptionSuspected })
        assertTrue(result.observations.any { it.code == "MITM_SUSPECTED" })
    }

    private fun provider(
        label: String,
        variants: List<SniVariantResult>,
    ): SniProviderProbeResult {
        return SniProviderProbeResult(
            providerLabel = label,
            endpointUrl = "https://$label.example",
            variants = variants,
        )
    }

    private fun successfulVariant(
        variant: SniVariantType,
        sniValue: String? = "target.example",
        hostnameMatch: Boolean = true,
        trustedBySystem: Boolean = true,
        fingerprint: String = "11:22:33",
        issuer: String = "Public CA",
    ): SniVariantResult {
        return SniVariantResult(
            variant = variant,
            requestedHost = "target.example",
            sniValue = sniValue,
            dnsResolved = true,
            tcpConnected = true,
            tlsHandshakeSucceeded = true,
            certificate = TlsCertificateInfo(
                subject = "target.example",
                issuer = issuer,
                validFrom = "2026-01-01",
                validUntil = "2026-04-01",
                publicKey = "EC 256",
                fingerprintSha256 = fingerprint,
            ),
            certificateChain = listOf("target.example", issuer),
            certificateMatchesRequestedHost = hostnameMatch,
            certificateChainTrustedBySystem = trustedBySystem,
            dnsTimeMs = 10,
            tcpTimeMs = 20,
            tlsTimeMs = 30,
            totalTimeMs = 60,
        )
    }

    private fun failedVariant(
        variant: SniVariantType,
        dnsResolved: Boolean,
        tcpConnected: Boolean,
    ): SniVariantResult {
        return SniVariantResult(
            variant = variant,
            requestedHost = "target.example",
            sniValue = "target.example",
            dnsResolved = dnsResolved,
            tcpConnected = tcpConnected,
            tlsHandshakeSucceeded = false,
            dnsTimeMs = 10,
            tcpTimeMs = if (tcpConnected) 20 else null,
            tlsTimeMs = if (tcpConnected) 30 else null,
            totalTimeMs = 60,
            errorMessage = "TLS handshake failed",
        )
    }
}

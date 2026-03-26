package com.example.shannon.domain.analysis

import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.SniObservation
import com.example.shannon.domain.model.SniProviderAnalysis
import com.example.shannon.domain.model.SniProviderProbeResult
import com.example.shannon.domain.model.SniVariantResult
import com.example.shannon.domain.model.SniVariantType
import java.util.Locale

class SniMitmAnalysisEngine {
    fun analyze(
        probeResults: List<SniProviderProbeResult>,
        checkedAt: String,
    ): SniMitmAnalysisResult {
        val crossProviderSignals = buildCrossProviderSignals(probeResults)
        val providers = probeResults.map { analyzeProvider(it, crossProviderSignals) }
        val observations = buildOverallObservations(providers, crossProviderSignals)
        val status = when {
            crossProviderSignals.mitmSuspected -> SniAnalysisStatus.MitmSuspected
            providers.any { it.status == SniAnalysisStatus.TlsInterceptionSuspected } ->
                SniAnalysisStatus.TlsInterceptionSuspected
            providers.any { it.status == SniAnalysisStatus.SniFilteringSuspected } ->
                SniAnalysisStatus.SniFilteringSuspected
            providers.any { it.status == SniAnalysisStatus.Inconclusive } ->
                SniAnalysisStatus.Inconclusive
            else -> SniAnalysisStatus.Normal
        }

        return SniMitmAnalysisResult(
            providers = providers,
            status = status,
            observations = observations,
            checkedAt = checkedAt,
        )
    }

    private fun analyzeProvider(
        provider: SniProviderProbeResult,
        crossProviderSignals: CrossProviderSignals,
    ): SniProviderAnalysis {
        val normal = provider.variant(SniVariantType.NormalSni)
        val alternative = provider.variant(SniVariantType.AlternativeSni)
        val noSni = provider.variant(SniVariantType.NoSni)
        val random = provider.variant(SniVariantType.RandomSni)

        val observations = mutableListOf<SniObservation>()

        val normalTlsFailedAfterTcp = normal?.dnsResolved == true &&
            normal.tcpConnected &&
            !normal.tlsHandshakeSucceeded
        val successfulAlternatives = listOfNotNull(alternative, noSni, random)
            .count { it.tlsHandshakeSucceeded }
        val strongSniFilteringPattern = normalTlsFailedAfterTcp &&
            (
                (alternative?.tlsHandshakeSucceeded == true && noSni?.tlsHandshakeSucceeded == true) ||
                    (noSni?.tlsHandshakeSucceeded == true && random?.tlsHandshakeSucceeded == true) ||
                    successfulAlternatives >= 3
                )
        val mixedSniPattern = normalTlsFailedAfterTcp && successfulAlternatives > 0

        if (strongSniFilteringPattern) {
            observations += SniObservation(
                code = "SNI_FILTERING_PATTERN",
                title = "Selective TLS failure on the normal SNI",
                summary = "DNS and TCP succeeded for the baseline connection, but TLS failed only for the normal SNI while other variants still completed.",
            )
        } else if (mixedSniPattern) {
            observations += SniObservation(
                code = "MIXED_SNI_BEHAVIOR",
                title = "Mixed SNI behavior",
                summary = "The baseline TLS handshake failed after TCP, but non-default SNI variants produced mixed outcomes that are not conclusive on their own.",
            )
        }

        val baselineSucceeded = normal?.tlsHandshakeSucceeded == true
        val baselineUntrusted = normal?.certificateChainTrustedBySystem == false
        val baselineHostnameMismatch = normal?.certificateMatchesRequestedHost == false
        val baselineIssuer = normal?.certificate?.issuer?.normalize()
        val baselineFingerprint = normal?.certificate?.fingerprintSha256

        if (baselineSucceeded && baselineUntrusted) {
            observations += SniObservation(
                code = "BASELINE_UNTRUSTED_CHAIN",
                title = "Baseline certificate chain was not trusted",
                summary = "The normal SNI handshake completed, but the certificate chain did not validate against the Android system trust store.",
            )
        }
        if (baselineSucceeded && baselineHostnameMismatch) {
            observations += SniObservation(
                code = "BASELINE_HOSTNAME_MISMATCH",
                title = "Baseline certificate did not match the hostname",
                summary = "The normal SNI handshake completed, but the presented certificate did not match the requested hostname.",
            )
        }
        if (
            baselineSucceeded &&
            baselineUntrusted &&
            baselineIssuer != null &&
            baselineIssuer in crossProviderSignals.repeatedUntrustedIssuers
        ) {
            observations += SniObservation(
                code = "REPEATED_UNTRUSTED_ISSUER",
                title = "Same untrusted issuer seen across providers",
                summary = "Multiple providers returned baseline certificates issued by the same untrusted authority, which is consistent with TLS interception.",
            )
        }
        if (
            baselineSucceeded &&
            baselineFingerprint != null &&
            baselineFingerprint in crossProviderSignals.repeatedSuspiciousFingerprints
        ) {
            observations += SniObservation(
                code = "REPEATED_SUSPICIOUS_FINGERPRINT",
                title = "Same suspicious certificate fingerprint seen across providers",
                summary = "Multiple providers returned the same baseline certificate fingerprint together with other unusual TLS signals.",
            )
        }

        val status = when {
            strongSniFilteringPattern -> SniAnalysisStatus.SniFilteringSuspected
            baselineSucceeded && baselineUntrusted -> SniAnalysisStatus.TlsInterceptionSuspected
            baselineSucceeded &&
                baselineHostnameMismatch &&
                (crossProviderSignals.multipleBaselineHostnameMismatches ||
                    baselineFingerprint in crossProviderSignals.repeatedSuspiciousFingerprints) ->
                SniAnalysisStatus.TlsInterceptionSuspected
            baselineSucceeded && baselineHostnameMismatch -> SniAnalysisStatus.Inconclusive
            mixedSniPattern -> SniAnalysisStatus.Inconclusive
            baselineSucceeded -> SniAnalysisStatus.Normal
            provider.variants.any { it.tlsHandshakeSucceeded } -> SniAnalysisStatus.Inconclusive
            else -> SniAnalysisStatus.Inconclusive
        }

        val summary = when (status) {
            SniAnalysisStatus.SniFilteringSuspected ->
                "The normal SNI failed after TCP while non-default SNI variants still completed, which matches a selective SNI filtering pattern."
            SniAnalysisStatus.TlsInterceptionSuspected ->
                "The baseline TLS behavior showed certificate validation anomalies that are more consistent with interception than with routine CDN variance."
            SniAnalysisStatus.MitmSuspected ->
                "Baseline certificates across providers matched a strong multi-signal MITM pattern."
            SniAnalysisStatus.Inconclusive ->
                "This provider produced mixed TLS/SNI behavior that is notable, but not strong enough for a confident interference claim."
            SniAnalysisStatus.Normal ->
                "The baseline handshake completed normally and the variant differences stayed within expected server behavior."
        }

        return SniProviderAnalysis(
            providerLabel = provider.providerLabel,
            endpointUrl = provider.endpointUrl,
            status = status,
            summary = summary,
            observations = observations,
            variants = provider.variants,
        )
    }

    private fun buildOverallObservations(
        providers: List<SniProviderAnalysis>,
        crossProviderSignals: CrossProviderSignals,
    ): List<SniObservation> {
        val observations = mutableListOf<SniObservation>()

        if (providers.any { it.status == SniAnalysisStatus.SniFilteringSuspected }) {
            observations += SniObservation(
                code = "SNI_FILTERING_SUSPECTED",
                title = "Possible SNI filtering detected",
                summary = "At least one provider failed the TLS handshake only on the normal SNI while other SNI variants still completed.",
            )
        }
        if (providers.any { it.status == SniAnalysisStatus.TlsInterceptionSuspected }) {
            observations += SniObservation(
                code = "TLS_INTERCEPTION_SUSPECTED",
                title = "Possible TLS interception detected",
                summary = "At least one baseline handshake completed with certificate validation anomalies that should be reviewed alongside the raw variant details.",
            )
        }
        if (crossProviderSignals.mitmSuspected) {
            val summary = when {
                crossProviderSignals.repeatedUntrustedIssuers.isNotEmpty() ->
                    "Multiple providers returned baseline certificates issued by the same untrusted authority."
                crossProviderSignals.repeatedSuspiciousFingerprints.isNotEmpty() ->
                    "Multiple providers returned the same suspicious baseline certificate fingerprint."
                else ->
                    "Multiple providers showed the same uncommon baseline certificate pattern."
            }
            observations += SniObservation(
                code = "MITM_SUSPECTED",
                title = "MITM suspected",
                summary = summary,
            )
        }
        if (observations.isEmpty() && providers.any { it.status == SniAnalysisStatus.Inconclusive }) {
            observations += SniObservation(
                code = "INCONCLUSIVE",
                title = "Inconclusive",
                summary = "Some providers showed mixed SNI or certificate behavior, but the overall pattern was not strong enough for a reliable interference conclusion.",
            )
        }
        if (observations.isEmpty()) {
            observations += SniObservation(
                code = "NO_CLEAR_SNI_INTERFERENCE",
                title = "No clear SNI interference detected",
                summary = "Baseline handshakes completed normally and the collected variant differences did not form a strong filtering or interception pattern.",
            )
        }

        return observations
    }

    private fun buildCrossProviderSignals(probeResults: List<SniProviderProbeResult>): CrossProviderSignals {
        val baselineFacts = probeResults.mapNotNull { provider ->
            provider.variant(SniVariantType.NormalSni)
                ?.takeIf { it.tlsHandshakeSucceeded }
                ?.let { variant ->
                    BaselineFact(
                        providerLabel = provider.providerLabel,
                        issuer = variant.certificate?.issuer?.normalize(),
                        fingerprint = variant.certificate?.fingerprintSha256,
                        trustedBySystem = variant.certificateChainTrustedBySystem == true,
                        hostnameMatch = variant.certificateMatchesRequestedHost == true,
                    )
                }
        }

        val repeatedUntrustedIssuers = baselineFacts
            .filter { !it.trustedBySystem }
            .groupBy { it.issuer }
            .filterKeys { !it.isNullOrBlank() }
            .filterValues { facts -> facts.map { it.providerLabel }.distinct().size >= 2 }
            .keys
            .filterNotNull()
            .toSet()

        val repeatedSuspiciousFingerprints = baselineFacts
            .groupBy { it.fingerprint }
            .filterKeys { !it.isNullOrBlank() }
            .filterValues { facts ->
                facts.map { it.providerLabel }.distinct().size >= 2 &&
                    facts.any { !it.trustedBySystem || !it.hostnameMatch }
            }
            .keys
            .filterNotNull()
            .toSet()

        val multipleBaselineHostnameMismatches = baselineFacts.count { !it.hostnameMatch } >= 2
        val mitmSuspected = repeatedUntrustedIssuers.isNotEmpty() ||
            repeatedSuspiciousFingerprints.isNotEmpty() ||
            baselineFacts.count { !it.trustedBySystem && !it.hostnameMatch } >= 2

        return CrossProviderSignals(
            repeatedUntrustedIssuers = repeatedUntrustedIssuers,
            repeatedSuspiciousFingerprints = repeatedSuspiciousFingerprints,
            multipleBaselineHostnameMismatches = multipleBaselineHostnameMismatches,
            mitmSuspected = mitmSuspected,
        )
    }

    private fun SniProviderProbeResult.variant(type: SniVariantType): SniVariantResult? {
        return variants.firstOrNull { it.variant == type }
    }

    private fun String.normalize(): String {
        return trim().lowercase(Locale.US)
    }

    private data class BaselineFact(
        val providerLabel: String,
        val issuer: String?,
        val fingerprint: String?,
        val trustedBySystem: Boolean,
        val hostnameMatch: Boolean,
    )

    private data class CrossProviderSignals(
        val repeatedUntrustedIssuers: Set<String>,
        val repeatedSuspiciousFingerprints: Set<String>,
        val multipleBaselineHostnameMismatches: Boolean,
        val mitmSuspected: Boolean,
    )
}

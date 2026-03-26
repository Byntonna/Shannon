package com.example.shannon.domain.analysis

import androidx.annotation.StringRes
import com.example.shannon.R
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.SniObservation
import com.example.shannon.domain.model.SniProviderAnalysis
import com.example.shannon.domain.model.SniProviderProbeResult
import com.example.shannon.domain.model.SniVariantResult
import com.example.shannon.domain.model.SniVariantType
import java.util.Locale

class SniMitmAnalysisEngine(
    private val stringResolver: (Int, List<Any>) -> String,
) {
    private fun text(@StringRes resId: Int, vararg args: Any): String = stringResolver(resId, args.toList())

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
                title = text(R.string.sni_observation_selective_tls_failure_title),
                summary = text(R.string.sni_observation_selective_tls_failure_summary),
            )
        } else if (mixedSniPattern) {
            observations += SniObservation(
                code = "MIXED_SNI_BEHAVIOR",
                title = text(R.string.sni_observation_mixed_behavior_title),
                summary = text(R.string.sni_observation_mixed_behavior_summary),
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
                title = text(R.string.sni_observation_untrusted_chain_title),
                summary = text(R.string.sni_observation_untrusted_chain_summary),
            )
        }
        if (baselineSucceeded && baselineHostnameMismatch) {
            observations += SniObservation(
                code = "BASELINE_HOSTNAME_MISMATCH",
                title = text(R.string.sni_observation_hostname_mismatch_title),
                summary = text(R.string.sni_observation_hostname_mismatch_summary),
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
                title = text(R.string.sni_observation_repeated_untrusted_issuer_title),
                summary = text(R.string.sni_observation_repeated_untrusted_issuer_summary),
            )
        }
        if (
            baselineSucceeded &&
            baselineFingerprint != null &&
            baselineFingerprint in crossProviderSignals.repeatedSuspiciousFingerprints
        ) {
            observations += SniObservation(
                code = "REPEATED_SUSPICIOUS_FINGERPRINT",
                title = text(R.string.sni_observation_repeated_fingerprint_title),
                summary = text(R.string.sni_observation_repeated_fingerprint_summary),
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
                text(R.string.sni_provider_summary_filtering)
            SniAnalysisStatus.TlsInterceptionSuspected ->
                text(R.string.sni_provider_summary_tls_interception)
            SniAnalysisStatus.MitmSuspected ->
                text(R.string.sni_provider_summary_mitm)
            SniAnalysisStatus.Inconclusive ->
                text(R.string.sni_provider_summary_inconclusive)
            SniAnalysisStatus.Normal ->
                text(R.string.sni_provider_summary_normal)
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
                title = text(R.string.sni_overall_filtering_title),
                summary = text(R.string.sni_overall_filtering_summary),
            )
        }
        if (providers.any { it.status == SniAnalysisStatus.TlsInterceptionSuspected }) {
            observations += SniObservation(
                code = "TLS_INTERCEPTION_SUSPECTED",
                title = text(R.string.sni_overall_tls_interception_title),
                summary = text(R.string.sni_overall_tls_interception_summary),
            )
        }
        if (crossProviderSignals.mitmSuspected) {
            val summary = when {
                crossProviderSignals.repeatedUntrustedIssuers.isNotEmpty() ->
                    text(R.string.sni_overall_mitm_summary_untrusted_issuer)
                crossProviderSignals.repeatedSuspiciousFingerprints.isNotEmpty() ->
                    text(R.string.sni_overall_mitm_summary_fingerprint)
                else ->
                    text(R.string.sni_overall_mitm_summary_pattern)
            }
            observations += SniObservation(
                code = "MITM_SUSPECTED",
                title = text(R.string.sni_status_mitm),
                summary = summary,
            )
        }
        if (observations.isEmpty() && providers.any { it.status == SniAnalysisStatus.Inconclusive }) {
            observations += SniObservation(
                code = "INCONCLUSIVE",
                title = text(R.string.status_inconclusive),
                summary = text(R.string.sni_overall_inconclusive_summary),
            )
        }
        if (observations.isEmpty()) {
            observations += SniObservation(
                code = "NO_CLEAR_SNI_INTERFERENCE",
                title = text(R.string.sni_overall_no_clear_interference_title),
                summary = text(R.string.sni_overall_no_clear_interference_summary),
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

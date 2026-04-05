package com.example.shannon.presentation.model

import androidx.annotation.StringRes
import com.example.shannon.R
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.domain.model.WebsiteAccessibilityOutcome
import com.example.shannon.domain.model.toOutcome

data class HomeSummaryState(
    @param:StringRes val titleResId: Int,
    @param:StringRes val descriptionResId: Int,
    @param:StringRes val explanationResId: Int,
    val tone: HomeSummaryTone,
    val showRunCheckAction: Boolean = false,
    val reasons: List<HomeSummaryReason> = emptyList(),
    val nextSteps: List<DiagnosticsDestination> = emptyList(),
)

data class HomeSummaryReason(
    @param:StringRes val titleResId: Int,
    @param:StringRes val descriptionResId: Int,
    val destination: DiagnosticsDestination? = null,
)

enum class HomeSummaryTone {
    Positive,
    Warning,
    Error,
    Neutral,
}

fun DiagnosticsUiState.homeSummaryState(): HomeSummaryState {
    val hasConnectivityFailure = testResult?.steps?.any { !it.success } == true
    val hasLimitedWebsites = websiteAccessibilityResults.any {
        it.status.toOutcome() == WebsiteAccessibilityOutcome.Limited
    }
    val hasUnstableWebsites = websiteAccessibilityResults.any {
        it.status.toOutcome() == WebsiteAccessibilityOutcome.Unstable
    }
    val hasKeyResults = testResult != null ||
        dnsAnalysisResult != null ||
        websiteAccessibilityResults.isNotEmpty() ||
        tlsAnalysisResult != null ||
        sniMitmAnalysisResult != null
    val reasons = mutableListOf<HomeSummaryReason>()
    val nextSteps = linkedSetOf<DiagnosticsDestination>()

    fun addReason(
        @StringRes titleResId: Int,
        @StringRes descriptionResId: Int,
        destination: DiagnosticsDestination? = null,
    ) {
        reasons += HomeSummaryReason(
            titleResId = titleResId,
            descriptionResId = descriptionResId,
            destination = destination,
        )
        if (destination != null) {
            nextSteps += destination
        }
    }

    val state = when {
        sniMitmAnalysisResult?.status == SniAnalysisStatus.MitmSuspected ||
            tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.TlsInterceptionSuspected -> {
            if (sniMitmAnalysisResult?.status == SniAnalysisStatus.MitmSuspected) {
                addReason(
                    titleResId = R.string.home_summary_reason_mitm_title,
                    descriptionResId = R.string.home_summary_reason_mitm_message,
                    destination = DiagnosticsDestination.SniMitmAnalysis,
                )
            }
            if (tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.TlsInterceptionSuspected) {
                addReason(
                    titleResId = R.string.home_summary_reason_tls_interception_title,
                    descriptionResId = R.string.home_summary_reason_tls_interception_message,
                    destination = DiagnosticsDestination.TlsAnalysis,
                )
            }

            HomeSummaryState(
                titleResId = R.string.home_summary_tampering_title,
                descriptionResId = R.string.home_summary_tampering_message,
                explanationResId = R.string.home_summary_details_error_meaning,
                tone = HomeSummaryTone.Error,
            )
        }

        sniMitmAnalysisResult?.status == SniAnalysisStatus.SniFilteringSuspected ||
            sniMitmAnalysisResult?.status == SniAnalysisStatus.TlsInterceptionSuspected ||
            tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.UnusualCertificateChain ||
            tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.TlsDowngradeSuspected ||
            dnsAnalysisResult?.status == DnsAnalysisStatus.Blocked ||
            hasLimitedWebsites ||
            hasConnectivityFailure -> {
            if (sniMitmAnalysisResult?.status == SniAnalysisStatus.SniFilteringSuspected) {
                addReason(
                    titleResId = R.string.home_summary_reason_sni_filtering_title,
                    descriptionResId = R.string.home_summary_reason_sni_filtering_message,
                    destination = DiagnosticsDestination.SniMitmAnalysis,
                )
            }
            if (sniMitmAnalysisResult?.status == SniAnalysisStatus.TlsInterceptionSuspected) {
                addReason(
                    titleResId = R.string.home_summary_reason_sni_tls_interception_title,
                    descriptionResId = R.string.home_summary_reason_sni_tls_interception_message,
                    destination = DiagnosticsDestination.SniMitmAnalysis,
                )
            }
            if (tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.UnusualCertificateChain) {
                addReason(
                    titleResId = R.string.home_summary_reason_tls_chain_title,
                    descriptionResId = R.string.home_summary_reason_tls_chain_message,
                    destination = DiagnosticsDestination.TlsAnalysis,
                )
            }
            if (tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.TlsDowngradeSuspected) {
                addReason(
                    titleResId = R.string.home_summary_reason_tls_downgrade_title,
                    descriptionResId = R.string.home_summary_reason_tls_downgrade_message,
                    destination = DiagnosticsDestination.TlsAnalysis,
                )
            }
            if (dnsAnalysisResult?.status == DnsAnalysisStatus.Blocked) {
                addReason(
                    titleResId = R.string.home_summary_reason_dns_blocked_title,
                    descriptionResId = R.string.home_summary_reason_dns_blocked_message,
                    destination = DiagnosticsDestination.DnsAnalysis,
                )
            }
            if (hasLimitedWebsites) {
                addReason(
                    titleResId = R.string.home_summary_reason_websites_limited_title,
                    descriptionResId = R.string.home_summary_reason_websites_limited_message,
                    destination = DiagnosticsDestination.WebsiteAccessibility,
                )
            }
            if (hasConnectivityFailure) {
                addReason(
                    titleResId = R.string.home_summary_reason_connectivity_failed_title,
                    descriptionResId = R.string.home_summary_reason_connectivity_failed_message,
                    destination = DiagnosticsDestination.ConnectivityTest,
                )
            }

            HomeSummaryState(
                titleResId = R.string.home_summary_limited_title,
                descriptionResId = R.string.home_summary_limited_message,
                explanationResId = R.string.home_summary_details_warning_meaning,
                tone = HomeSummaryTone.Warning,
            )
        }

        !hasKeyResults ||
            tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.Inconclusive ||
            sniMitmAnalysisResult?.status == SniAnalysisStatus.Inconclusive ||
            hasUnstableWebsites -> {
            if (!hasKeyResults) {
                addReason(
                    titleResId = R.string.home_summary_reason_not_enough_data_title,
                    descriptionResId = R.string.home_summary_reason_not_enough_data_message,
                )
            }
            if (tlsAnalysisResult?.status == TlsAnalysisHeuristicStatus.Inconclusive) {
                addReason(
                    titleResId = R.string.home_summary_reason_tls_inconclusive_title,
                    descriptionResId = R.string.home_summary_reason_tls_inconclusive_message,
                    destination = DiagnosticsDestination.TlsAnalysis,
                )
            }
            if (sniMitmAnalysisResult?.status == SniAnalysisStatus.Inconclusive) {
                addReason(
                    titleResId = R.string.home_summary_reason_sni_inconclusive_title,
                    descriptionResId = R.string.home_summary_reason_sni_inconclusive_message,
                    destination = DiagnosticsDestination.SniMitmAnalysis,
                )
            }
            if (hasUnstableWebsites) {
                addReason(
                    titleResId = R.string.home_summary_reason_websites_unstable_title,
                    descriptionResId = R.string.home_summary_reason_websites_unstable_message,
                    destination = DiagnosticsDestination.WebsiteAccessibility,
                )
            }

            HomeSummaryState(
                titleResId = R.string.home_summary_attention_title,
                descriptionResId = R.string.home_summary_attention_message,
                explanationResId = R.string.home_summary_details_neutral_meaning,
                tone = HomeSummaryTone.Neutral,
                showRunCheckAction = true,
            )
        }

        else -> {
            addReason(
                titleResId = R.string.home_summary_reason_everything_ok_title,
                descriptionResId = R.string.home_summary_reason_everything_ok_message,
            )

            HomeSummaryState(
                titleResId = R.string.home_summary_ok_title,
                descriptionResId = R.string.home_summary_ok_message,
                explanationResId = R.string.home_summary_details_positive_meaning,
                tone = HomeSummaryTone.Positive,
            )
        }
    }

    val fallbackNextSteps = when (state.tone) {
        HomeSummaryTone.Error -> listOf(
            DiagnosticsDestination.SniMitmAnalysis,
            DiagnosticsDestination.TlsAnalysis,
            DiagnosticsDestination.WebsiteAccessibility,
        )
        HomeSummaryTone.Warning -> listOf(
            DiagnosticsDestination.WebsiteAccessibility,
            DiagnosticsDestination.DnsAnalysis,
            DiagnosticsDestination.ConnectivityTest,
            DiagnosticsDestination.TlsAnalysis,
        )
        HomeSummaryTone.Neutral -> listOf(
            DiagnosticsDestination.ConnectivityTest,
            DiagnosticsDestination.WebsiteAccessibility,
            DiagnosticsDestination.DnsAnalysis,
            DiagnosticsDestination.TlsAnalysis,
        )
        HomeSummaryTone.Positive -> listOf(
            DiagnosticsDestination.Overview,
            DiagnosticsDestination.ReportExport,
        )
    }

    return state.copy(
        reasons = reasons,
        nextSteps = (nextSteps + fallbackNextSteps).take(4),
    )
}

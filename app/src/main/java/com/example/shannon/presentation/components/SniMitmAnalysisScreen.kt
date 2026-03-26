package com.example.shannon.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.titleResId
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.SniProviderAnalysis
import com.example.shannon.domain.model.SniVariantResult

@Composable
fun SniMitmAnalysisScreen(
    result: SniMitmAnalysisResult?,
    isRunning: Boolean,
    onRunAnalysis: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiagnosticsSectionSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = context.getString(R.string.sni_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRunAnalysis, enabled = !isRunning) {
                Text(context.getString(if (isRunning) R.string.action_running else R.string.sni_run_analysis))
            }
            if (isRunning && result == null) {
                CircularProgressIndicator()
            }
        }

        result?.let {
            SniSummaryCard(it)
            it.providers.forEach { provider ->
                SniProviderCard(provider)
            }
        }
    }
}

@Composable
private fun SniSummaryCard(result: SniMitmAnalysisResult) {
    val context = LocalContext.current
    DiagnosticsSectionSurface {
        Text(context.getString(R.string.interpretation), style = MaterialTheme.typography.titleMedium)
        SniStatusBadge(result.status)
        if (result.observations.isEmpty()) {
            Text(
                text = context.getString(R.string.sni_no_observations),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(context.getString(R.string.observations), style = MaterialTheme.typography.titleMedium)
            result.observations.forEachIndexed { index, observation ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(observation.title, style = MaterialTheme.typography.titleMedium)
                    Text(observation.summary, style = MaterialTheme.typography.bodyMedium)
                }
                if (index != result.observations.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
        Text(
            text = context.getString(R.string.checked_at_value, result.checkedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SniProviderCard(provider: SniProviderAnalysis) {
    var detailsExpanded by rememberSaveable(provider.providerLabel, provider.endpointUrl) {
        mutableStateOf(false)
    }
    DiagnosticsSectionSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(provider.providerLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = provider.endpointUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SniStatusBadge(provider.status)
        }
        Text(provider.summary, style = MaterialTheme.typography.bodyMedium)
        CollapsibleSectionHeader(
            title = LocalContext.current.getString(R.string.sni_provider_details),
            subtitle = LocalContext.current.getString(R.string.sni_variants_count, provider.variants.size),
            expanded = detailsExpanded,
            onToggle = { detailsExpanded = !detailsExpanded },
        )
        AnimatedVisibility(
            visible = detailsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                provider.observations.forEach { observation ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(observation.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = observation.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                provider.variants.forEachIndexed { index, variant ->
                    VariantCard(variant)
                    if (index != provider.variants.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantCard(result: SniVariantResult) {
    val context = LocalContext.current
    var detailsExpanded by rememberSaveable(result.variant.name, result.sniValue, result.ipAddress) {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(variantOutcomeSummary(result), style = MaterialTheme.typography.bodyMedium)
        CollapsibleSectionHeader(
            title = context.getString(result.variant.titleResId()),
            subtitle = context.getString(if (detailsExpanded) R.string.action_tap_to_collapse else R.string.action_tap_to_expand),
            expanded = detailsExpanded,
            onToggle = { detailsExpanded = !detailsExpanded },
        )
        AnimatedVisibility(
            visible = detailsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.sniValue?.let {
                    TechnicalLine(context.getString(R.string.sni_label), it)
                } ?: TechnicalLine(context.getString(R.string.sni_label), context.getString(R.string.sni_not_sent))
                result.ipAddress?.let { TechnicalLine(context.getString(R.string.overview_private_ip_short), it) }
                TechnicalLine(context.getString(R.string.stage_dns), if (result.dnsResolved) context.getString(R.string.sni_dns_resolved) else context.getString(R.string.protocol_status_failed))
                TechnicalLine(
                    context.getString(R.string.stage_tcp),
                    when {
                        result.tcpConnected -> context.getString(R.string.sni_tcp_connected)
                        result.dnsResolved -> context.getString(R.string.protocol_status_failed)
                        else -> context.getString(R.string.sni_not_attempted)
                    },
                )
                TechnicalLine(
                    context.getString(R.string.stage_tls),
                    when {
                        result.tlsHandshakeSucceeded -> context.getString(R.string.sni_tls_handshake_succeeded)
                        result.tcpConnected -> context.getString(R.string.sni_tls_handshake_failed)
                        else -> context.getString(R.string.sni_not_attempted)
                    },
                )
                result.tlsVersion?.let { TechnicalLine(context.getString(R.string.tls_version), it) }
                result.cipherSuite?.let { TechnicalLine(context.getString(R.string.tls_cipher), it) }
                result.alpn?.let { TechnicalLine(context.getString(R.string.tls_alpn), it) }
                result.certificate?.let { cert ->
                    TechnicalLine(context.getString(R.string.tls_certificate_subject), cert.subject)
                    TechnicalLine(context.getString(R.string.tls_issuer), cert.issuer)
                    TechnicalLine(context.getString(R.string.tls_sha256), cert.fingerprintSha256)
                }
                result.certificateMatchesRequestedHost?.let {
                    TechnicalLine(context.getString(R.string.sni_hostname_match), context.getString(if (it) R.string.sni_hostname_matched else R.string.sni_hostname_not_matched))
                }
                result.certificateChainTrustedBySystem?.let {
                    TechnicalLine(context.getString(R.string.sni_system_trust), context.getString(if (it) R.string.sni_trusted_by_android else R.string.sni_not_trusted_by_android))
                }
                val timings = listOfNotNull(
                    result.dnsTimeMs?.let { context.getString(R.string.metric_dns_ms, it) },
                    result.tcpTimeMs?.let { context.getString(R.string.metric_tcp_ms, it) },
                    result.tlsTimeMs?.let { context.getString(R.string.metric_tls_ms, it) },
                    result.totalTimeMs?.let { context.getString(R.string.metric_total_ms, it) },
                )
                if (timings.isNotEmpty()) {
                    Text(
                        text = timings.joinToString("  |  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                result.errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnicalLine(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = LocalContext.current.getString(R.string.label_with_colon, label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SniStatusBadge(status: SniAnalysisStatus) {
    DiagnosticsStatusChip(
        text = LocalContext.current.getString(status.titleResId()),
        tone = when (status) {
            SniAnalysisStatus.Normal -> DiagnosticsChipTone.Positive
            SniAnalysisStatus.Inconclusive -> DiagnosticsChipTone.Neutral
            SniAnalysisStatus.MitmSuspected,
            SniAnalysisStatus.SniFilteringSuspected,
            SniAnalysisStatus.TlsInterceptionSuspected -> DiagnosticsChipTone.Caution
        },
    )
}

@Composable
private fun variantOutcomeSummary(result: SniVariantResult): String {
    val context = LocalContext.current
    return when {
        result.tlsHandshakeSucceeded -> context.getString(R.string.sni_variant_outcome_success)
        result.tcpConnected -> result.errorCategory?.let { context.getString(it.titleResId()) }
            ?: context.getString(R.string.sni_variant_outcome_tcp_connected_tls_failed)
        result.dnsResolved -> result.errorCategory?.let { context.getString(it.titleResId()) }
            ?: context.getString(R.string.sni_variant_outcome_dns_resolved_tcp_failed)
        else -> result.errorCategory?.let { context.getString(it.titleResId()) }
            ?: context.getString(R.string.sni_variant_outcome_dns_failed)
    }
}

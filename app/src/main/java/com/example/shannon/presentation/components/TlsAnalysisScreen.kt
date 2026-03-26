package com.example.shannon.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.domain.model.TlsAnalysisResult
import com.example.shannon.domain.model.TlsEndpointAnalysis
import com.example.shannon.domain.model.TlsEndpointStatus

@Composable
fun TlsAnalysisScreen(
    result: TlsAnalysisResult?,
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
                text = context.getString(R.string.tls_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRunAnalysis, enabled = !isRunning) {
                Text(context.getString(if (isRunning) R.string.action_running else R.string.tls_run_analysis))
            }
            if (isRunning && result == null) {
                CircularProgressIndicator()
            }
        }

        result?.let {
            TlsSummaryCard(result = it)
            it.endpoints.forEach { endpoint ->
                TlsEndpointCard(endpoint = endpoint)
            }
        }
    }
}

@Composable
private fun TlsSummaryCard(result: TlsAnalysisResult) {
    val context = LocalContext.current
    DiagnosticsSectionSurface {
        Text(context.getString(R.string.interpretation), style = MaterialTheme.typography.titleMedium)
        HeuristicBadge(result.status)
        if (result.observations.isEmpty()) {
            Text(
                text = context.getString(R.string.tls_no_observations),
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
private fun TlsEndpointCard(endpoint: TlsEndpointAnalysis) {
    var detailsExpanded by rememberSaveable(endpoint.endpointUrl) { mutableStateOf(false) }
    DiagnosticsSectionSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(endpoint.endpointLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = endpoint.endpointUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    EndpointBadge(endpoint.status)
                }
                Text(endpoint.summary, style = MaterialTheme.typography.bodyMedium)
                CollapsibleSectionHeader(
                    title = LocalContext.current.getString(R.string.tls_endpoint_details),
                    subtitle = LocalContext.current.getString(if (detailsExpanded) R.string.action_tap_to_collapse else R.string.action_tap_to_expand),
                    expanded = detailsExpanded,
                    onToggle = { detailsExpanded = !detailsExpanded },
                )
                AnimatedVisibility(
                    visible = detailsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        endpoint.ipAddress?.let { TechnicalLine(LocalContext.current.getString(R.string.overview_private_ip_short), it) }
                        endpoint.tlsVersion?.let { TechnicalLine(LocalContext.current.getString(R.string.tls_version), it) }
                        endpoint.cipherSuite?.let { TechnicalLine(LocalContext.current.getString(R.string.tls_cipher), it) }
                        endpoint.alpn?.let { TechnicalLine(LocalContext.current.getString(R.string.tls_alpn), it) }

                        endpoint.certificate?.let { certificate ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(LocalContext.current.getString(R.string.tls_certificate), style = MaterialTheme.typography.titleMedium)
                                TechnicalLine(LocalContext.current.getString(R.string.tls_subject), certificate.subject)
                                TechnicalLine(LocalContext.current.getString(R.string.tls_issuer), certificate.issuer)
                                TechnicalLine(LocalContext.current.getString(R.string.tls_valid_from), certificate.validFrom)
                                TechnicalLine(LocalContext.current.getString(R.string.tls_valid_until), certificate.validUntil)
                                TechnicalLine(LocalContext.current.getString(R.string.tls_public_key), certificate.publicKey)
                                TechnicalLine(LocalContext.current.getString(R.string.tls_sha256), certificate.fingerprintSha256)
                            }
                        }

                        if (endpoint.certificateChain.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(LocalContext.current.getString(R.string.tls_chain), style = MaterialTheme.typography.titleMedium)
                                endpoint.certificateChain.forEach { item ->
                                    Text(item, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (endpoint.supportedVersions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(LocalContext.current.getString(R.string.tls_version_support), style = MaterialTheme.typography.titleMedium)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    endpoint.supportedVersions.forEach { support ->
                                        VersionChip(
                                            label = LocalContext.current.getString(
                                                if (support.supported) R.string.tls_version_supported else R.string.tls_version_disabled,
                                                support.version,
                                            ),
                                            enabled = support.supported,
                                        )
                                    }
                                }
                            }
                        }

                        val metrics = listOfNotNull(
                            endpoint.dnsTimeMs?.let { LocalContext.current.getString(R.string.metric_dns_ms, it) },
                            endpoint.tcpTimeMs?.let { LocalContext.current.getString(R.string.metric_tcp_ms, it) },
                            endpoint.tlsTimeMs?.let { LocalContext.current.getString(R.string.metric_tls_ms, it) },
                            endpoint.totalTimeMs?.let { LocalContext.current.getString(R.string.metric_total_ms, it) },
                        )
                        if (metrics.isNotEmpty()) {
                            Text(
                                text = metrics.joinToString("  |  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        endpoint.errorMessage?.let {
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
private fun HeuristicBadge(status: TlsAnalysisHeuristicStatus) {
    DiagnosticsStatusChip(
        text = LocalContext.current.getString(status.titleResId()),
        tone = when (status) {
            TlsAnalysisHeuristicStatus.NoTlsAnomalies -> DiagnosticsChipTone.Positive
            TlsAnalysisHeuristicStatus.Inconclusive -> DiagnosticsChipTone.Neutral
            TlsAnalysisHeuristicStatus.TlsInterceptionSuspected,
            TlsAnalysisHeuristicStatus.TlsDowngradeSuspected,
            TlsAnalysisHeuristicStatus.UnusualCertificateChain -> DiagnosticsChipTone.Caution
        },
    )
}

@Composable
private fun EndpointBadge(status: TlsEndpointStatus) {
    DiagnosticsStatusChip(
        text = LocalContext.current.getString(status.titleResId()),
        tone = when (status) {
            TlsEndpointStatus.Normal -> DiagnosticsChipTone.Positive
            TlsEndpointStatus.Failed -> DiagnosticsChipTone.Negative
            TlsEndpointStatus.Inconclusive -> DiagnosticsChipTone.Neutral
        },
    )
}

@Composable
private fun VersionChip(
    label: String,
    enabled: Boolean,
) {
    DiagnosticsStatusChip(
        text = label,
        tone = if (enabled) DiagnosticsChipTone.Positive else DiagnosticsChipTone.Neutral,
    )
}

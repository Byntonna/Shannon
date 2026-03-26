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
import androidx.compose.ui.unit.dp
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
                text = "Inspect TLS handshakes, negotiated versions, cipher suites, ALPN, and certificate metadata across multiple providers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRunAnalysis, enabled = !isRunning) {
                Text(if (isRunning) "Running..." else "Run TLS analysis")
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
    DiagnosticsSectionSurface {
        Text("Interpretation", style = MaterialTheme.typography.titleMedium)
        HeuristicBadge(result.status)
        if (result.observations.isEmpty()) {
            Text(
                text = "No TLS observations were generated for this run.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text("Observations", style = MaterialTheme.typography.titleMedium)
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
            text = "Checked at ${result.checkedAt}",
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
                    title = "Endpoint details",
                    subtitle = if (detailsExpanded) "Tap to collapse" else "Tap to expand",
                    expanded = detailsExpanded,
                    onToggle = { detailsExpanded = !detailsExpanded },
                )
                AnimatedVisibility(
                    visible = detailsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        endpoint.ipAddress?.let { TechnicalLine("IP", it) }
                        endpoint.tlsVersion?.let { TechnicalLine("TLS version", it) }
                        endpoint.cipherSuite?.let { TechnicalLine("Cipher", it) }
                        endpoint.alpn?.let { TechnicalLine("ALPN", it) }

                        endpoint.certificate?.let { certificate ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Certificate", style = MaterialTheme.typography.titleMedium)
                                TechnicalLine("Subject", certificate.subject)
                                TechnicalLine("Issuer", certificate.issuer)
                                TechnicalLine("Valid from", certificate.validFrom)
                                TechnicalLine("Valid until", certificate.validUntil)
                                TechnicalLine("Public key", certificate.publicKey)
                                TechnicalLine("SHA-256", certificate.fingerprintSha256)
                            }
                        }

                        if (endpoint.certificateChain.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Chain", style = MaterialTheme.typography.titleMedium)
                                endpoint.certificateChain.forEach { item ->
                                    Text(item, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (endpoint.supportedVersions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Version support", style = MaterialTheme.typography.titleMedium)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    endpoint.supportedVersions.forEach { support ->
                                        VersionChip(
                                            label = "${support.version} ${if (support.supported) "supported" else "disabled"}",
                                            enabled = support.supported,
                                        )
                                    }
                                }
                            }
                        }

                        val metrics = listOfNotNull(
                            endpoint.dnsTimeMs?.let { "DNS ${it} ms" },
                            endpoint.tcpTimeMs?.let { "TCP ${it} ms" },
                            endpoint.tlsTimeMs?.let { "TLS ${it} ms" },
                            endpoint.totalTimeMs?.let { "Total ${it} ms" },
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
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HeuristicBadge(status: TlsAnalysisHeuristicStatus) {
    DiagnosticsStatusChip(
        text = status.title,
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
        text = status.title,
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

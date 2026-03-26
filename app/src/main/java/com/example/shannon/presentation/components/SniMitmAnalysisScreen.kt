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
import androidx.compose.ui.unit.dp
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
                text = "Compare normal SNI, alternative SNI, no SNI, and random SNI handshakes to look for selective TLS interference and MITM signals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRunAnalysis, enabled = !isRunning) {
                Text(if (isRunning) "Running..." else "Run SNI and MITM analysis")
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
    DiagnosticsSectionSurface {
        Text("Interpretation", style = MaterialTheme.typography.titleMedium)
        SniStatusBadge(result.status)
        if (result.observations.isEmpty()) {
            Text(
                text = "No high-confidence SNI or MITM observations were produced.",
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
            title = "Provider details",
            subtitle = "${provider.variants.size} variant(s)",
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
    var detailsExpanded by rememberSaveable(result.variant.title, result.sniValue, result.ipAddress) {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(variantOutcomeSummary(result), style = MaterialTheme.typography.bodyMedium)
        CollapsibleSectionHeader(
            title = result.variant.title,
            subtitle = if (detailsExpanded) "Tap to collapse" else "Tap to expand",
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
                    TechnicalLine("SNI", it)
                } ?: TechnicalLine("SNI", "Not sent")
                result.ipAddress?.let { TechnicalLine("IP", it) }
                TechnicalLine("DNS", if (result.dnsResolved) "Resolved" else "Failed")
                TechnicalLine(
                    "TCP",
                    when {
                        result.tcpConnected -> "Connected"
                        result.dnsResolved -> "Failed"
                        else -> "Not attempted"
                    },
                )
                TechnicalLine(
                    "TLS",
                    when {
                        result.tlsHandshakeSucceeded -> "Handshake succeeded"
                        result.tcpConnected -> "Handshake failed"
                        else -> "Not attempted"
                    },
                )
                result.tlsVersion?.let { TechnicalLine("TLS version", it) }
                result.cipherSuite?.let { TechnicalLine("Cipher", it) }
                result.alpn?.let { TechnicalLine("ALPN", it) }
                result.certificate?.let { cert ->
                    TechnicalLine("Certificate subject", cert.subject)
                    TechnicalLine("Issuer", cert.issuer)
                    TechnicalLine("SHA-256", cert.fingerprintSha256)
                }
                result.certificateMatchesRequestedHost?.let {
                    TechnicalLine("Hostname match", if (it) "Matched requested host" else "Did not match requested host")
                }
                result.certificateChainTrustedBySystem?.let {
                    TechnicalLine("System trust", if (it) "Trusted by Android" else "Not trusted by Android")
                }
                val timings = listOfNotNull(
                    result.dnsTimeMs?.let { "DNS ${it} ms" },
                    result.tcpTimeMs?.let { "TCP ${it} ms" },
                    result.tlsTimeMs?.let { "TLS ${it} ms" },
                    result.totalTimeMs?.let { "Total ${it} ms" },
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
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SniStatusBadge(status: SniAnalysisStatus) {
    DiagnosticsStatusChip(
        text = status.title,
        tone = when (status) {
            SniAnalysisStatus.Normal -> DiagnosticsChipTone.Positive
            SniAnalysisStatus.Inconclusive -> DiagnosticsChipTone.Neutral
            SniAnalysisStatus.MitmSuspected,
            SniAnalysisStatus.SniFilteringSuspected,
            SniAnalysisStatus.TlsInterceptionSuspected -> DiagnosticsChipTone.Caution
        },
    )
}

private fun variantOutcomeSummary(result: SniVariantResult): String = when {
    result.tlsHandshakeSucceeded -> "TLS handshake succeeded for this variant."
    result.tcpConnected -> result.errorCategory?.title ?: "TCP connected, but TLS handshake failed."
    result.dnsResolved -> result.errorCategory?.title ?: "DNS resolved, but TCP connection failed."
    else -> result.errorCategory?.title ?: "DNS resolution failed for this variant."
}

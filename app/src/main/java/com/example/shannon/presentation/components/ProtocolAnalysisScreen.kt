package com.example.shannon.presentation.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shannon.domain.model.ProtocolAnalysisResult
import com.example.shannon.domain.model.ProtocolProbeKind
import com.example.shannon.domain.model.ProtocolProbeStatus
import com.example.shannon.domain.model.ProtocolTestResult

@Composable
fun ProtocolAnalysisScreen(
    result: ProtocolAnalysisResult?,
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
                text = "Active probes for HTTP/1.1, HTTP/2, HTTP/3 over QUIC, and WebSocket upgrades.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRunAnalysis, enabled = !isRunning) {
                Text(if (isRunning) "Running..." else "Run protocol analysis")
            }
            if (isRunning && result == null) {
                CircularProgressIndicator()
            }
        }

        result?.let { analysis ->
            ObservationCard(result = analysis)
            ProtocolProbeKind.entries.forEach { protocol ->
                val tests = analysis.tests.filter { it.protocol == protocol }
                if (tests.isNotEmpty()) {
                    ProtocolGroupCard(protocol = protocol, tests = tests)
                }
            }
        }
    }
}

@Composable
private fun ObservationCard(result: ProtocolAnalysisResult) {
    DiagnosticsSectionSurface {
        Text("Interpretation", style = MaterialTheme.typography.titleMedium)
        if (result.observations.isEmpty()) {
            Text(
                text = "No clear interference patterns were detected in this run.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
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
private fun ProtocolGroupCard(
    protocol: ProtocolProbeKind,
    tests: List<ProtocolTestResult>,
) {
    DiagnosticsSectionSurface {
        Text(protocol.title, style = MaterialTheme.typography.titleMedium)
        tests.forEachIndexed { index, test ->
            ProtocolTestCard(test = test)
            if (index != tests.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProtocolTestCard(test: ProtocolTestResult) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(test.endpointLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = test.endpointUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DiagnosticsStatusChip(
                text = test.status.title,
                tone = when (test.status) {
                    ProtocolProbeStatus.Supported -> DiagnosticsChipTone.Positive
                    ProtocolProbeStatus.Fallback -> DiagnosticsChipTone.Caution
                    ProtocolProbeStatus.Failed,
                    ProtocolProbeStatus.Blocked -> DiagnosticsChipTone.Negative
                    ProtocolProbeStatus.Inconclusive -> DiagnosticsChipTone.Neutral
                },
            )
        }
        Text(test.summary, style = MaterialTheme.typography.bodyMedium)
        test.negotiatedProtocol?.let {
            TechnicalLine(label = "Negotiated", value = it)
        }
        test.ipAddress?.let {
            TechnicalLine(label = "IP", value = it)
        }
        MetricsLine(test = test)
        test.httpStatusCode?.let {
            TechnicalLine(label = "HTTP status", value = it.toString())
        }
        test.errorCategory?.let {
            TechnicalLine(label = "Error category", value = it.title)
        }
        test.errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun MetricsLine(test: ProtocolTestResult) {
    val parts = listOfNotNull(
        test.dnsTimeMs?.let { "DNS ${it} ms" },
        test.tcpTimeMs?.let { "TCP ${it} ms" },
        test.tlsTimeMs?.let { "TLS ${it} ms" },
        test.totalTimeMs?.let { "Total ${it} ms" },
    )
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString("  |  "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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

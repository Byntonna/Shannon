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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.titleResId
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
                text = context.getString(R.string.protocol_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRunAnalysis, enabled = !isRunning) {
                Text(context.getString(if (isRunning) R.string.action_running else R.string.protocol_run_analysis))
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
    val context = LocalContext.current
    DiagnosticsSectionSurface {
        Text(context.getString(R.string.interpretation), style = MaterialTheme.typography.titleMedium)
        if (result.observations.isEmpty()) {
            Text(
                text = context.getString(R.string.protocol_no_observations),
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
            text = context.getString(R.string.checked_at_value, result.checkedAt),
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
        Text(LocalContext.current.getString(protocol.titleResId()), style = MaterialTheme.typography.titleMedium)
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
                text = LocalContext.current.getString(test.status.titleResId()),
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
            TechnicalLine(label = LocalContext.current.getString(R.string.protocol_negotiated), value = it)
        }
        test.ipAddress?.let {
            TechnicalLine(label = LocalContext.current.getString(R.string.overview_private_ip_short), value = it)
        }
        MetricsLine(test = test)
        test.httpStatusCode?.let {
            TechnicalLine(label = LocalContext.current.getString(R.string.protocol_http_status), value = it.toString())
        }
        test.errorCategory?.let {
            TechnicalLine(label = LocalContext.current.getString(R.string.protocol_error_category), value = LocalContext.current.getString(it.titleResId()))
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
        test.dnsTimeMs?.let { LocalContext.current.getString(R.string.metric_dns_ms, it) },
        test.tcpTimeMs?.let { LocalContext.current.getString(R.string.metric_tcp_ms, it) },
        test.tlsTimeMs?.let { LocalContext.current.getString(R.string.metric_tls_ms, it) },
        test.totalTimeMs?.let { LocalContext.current.getString(R.string.metric_total_ms, it) },
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
            text = LocalContext.current.getString(R.string.label_with_colon, label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

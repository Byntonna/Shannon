package com.example.shannon.presentation.components

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanResult
import com.example.shannon.domain.model.PortScanTransport
import com.example.shannon.domain.model.PortStatus

private val QuickPortsLabel = "22, 80, 443, 8080, 8443"

@Composable
fun PortScanScreen(
    host: String,
    portInput: String,
    transport: PortScanTransport,
    ipVersion: PortScanIpVersion,
    results: List<PortScanResult>,
    isRunning: Boolean,
    error: String?,
    onHostChange: (String) -> Unit,
    onPortInputChange: (String) -> Unit,
    onTransportChange: (PortScanTransport) -> Unit,
    onIpVersionChange: (PortScanIpVersion) -> Unit,
    onRunSingleScan: () -> Unit,
    onRunQuickScan: () -> Unit,
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
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Host") },
                singleLine = true,
            )
            OutlinedTextField(
                value = portInput,
                onValueChange = onPortInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Single port") },
                singleLine = true,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PortScanTransport.entries.forEach { entry ->
                    FilterChip(
                        selected = transport == entry,
                        onClick = { onTransportChange(entry) },
                        label = { Text(entry.title) },
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PortScanIpVersion.entries.forEach { entry ->
                    FilterChip(
                        selected = ipVersion == entry,
                        onClick = { onIpVersionChange(entry) },
                        label = { Text(entry.title) },
                    )
                }
            }
            Text(
                text = "Quick scan ports: $QuickPortsLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRunSingleScan, enabled = !isRunning) {
                    Text(if (isRunning) "Running..." else "Scan port")
                }
                Button(onClick = onRunQuickScan, enabled = !isRunning) {
                    Text("Quick scan")
                }
            }
            if (isRunning && results.isEmpty()) {
                CircularProgressIndicator()
            }
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (results.isEmpty()) {
            PlaceholderCard("Run a single port scan or a quick scan to see results.")
        } else {
            DiagnosticsSectionSurface {
                Text("Port scan", style = MaterialTheme.typography.titleMedium)
                ScanMetaLine("Host", results.first().host)
                ScanMetaLine("Transport", results.first().transport.title)
                ScanMetaLine("Address family", results.first().ipVersion.title)
                results.first().resolvedAddress?.let {
                    ScanMetaLine("Resolved address", it)
                }
                results.sortedBy { it.port }.forEachIndexed { index, result ->
                    PortResultRow(result)
                    if (index != results.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanMetaLine(
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
private fun PortResultRow(result: PortScanResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = result.port.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
            result.latencyMs?.let {
                Text(
                    text = "${it} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            PortStatusBadge(result.status)
            result.resolvedAddress?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            result.error?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PortStatusBadge(status: PortStatus) {
    DiagnosticsStatusChip(
        text = status.title,
        tone = when (status) {
            PortStatus.OPEN -> DiagnosticsChipTone.Positive
            PortStatus.CLOSED -> DiagnosticsChipTone.Negative
            PortStatus.FILTERED,
            PortStatus.TIMEOUT -> DiagnosticsChipTone.Caution
            PortStatus.ERROR -> DiagnosticsChipTone.Neutral
        },
    )
}

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.millisecondsText
import com.example.shannon.titleResId
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
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.host)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = portInput,
                onValueChange = onPortInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.port_scan_single_port)) },
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
                        label = { Text(context.getString(entry.titleResId())) },
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
                        label = { Text(context.getString(entry.titleResId())) },
                    )
                }
            }
            Text(
                text = context.getString(R.string.port_scan_quick_ports_value, QuickPortsLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRunSingleScan, enabled = !isRunning) {
                    Text(context.getString(if (isRunning) R.string.action_running else R.string.port_scan_run_single))
                }
                Button(onClick = onRunQuickScan, enabled = !isRunning) {
                    Text(context.getString(R.string.port_scan_run_quick))
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
            PlaceholderCard(context.getString(R.string.port_scan_placeholder))
        } else {
            DiagnosticsSectionSurface {
                Text(context.getString(R.string.screen_port_scan), style = MaterialTheme.typography.titleMedium)
                ScanMetaLine(context.getString(R.string.host), results.first().host)
                ScanMetaLine(context.getString(R.string.port_scan_transport), context.getString(results.first().transport.titleResId()))
                ScanMetaLine(context.getString(R.string.port_scan_address_family), context.getString(results.first().ipVersion.titleResId()))
                results.first().resolvedAddress?.let {
                    ScanMetaLine(context.getString(R.string.port_scan_resolved_address), it)
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
            text = LocalContext.current.getString(R.string.label_with_colon, label),
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
                                        text = LocalContext.current.millisecondsText(it),
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
        text = LocalContext.current.getString(status.titleResId()),
        tone = when (status) {
            PortStatus.OPEN -> DiagnosticsChipTone.Positive
            PortStatus.CLOSED -> DiagnosticsChipTone.Negative
            PortStatus.FILTERED,
            PortStatus.TIMEOUT -> DiagnosticsChipTone.Caution
            PortStatus.ERROR -> DiagnosticsChipTone.Neutral
        },
    )
}

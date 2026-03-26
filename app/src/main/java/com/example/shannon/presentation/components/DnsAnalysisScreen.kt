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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.titleResId
import com.example.shannon.domain.model.DnsAnalysisResult
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.DnsRecordResult
import com.example.shannon.domain.model.DnsServerResult

@Composable
fun DnsAnalysisScreen(
    domain: String,
    result: DnsAnalysisResult?,
    isRunning: Boolean,
    onDomainChange: (String) -> Unit,
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
            OutlinedTextField(
                value = domain,
                onValueChange = onDomainChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.dns_domain)) },
                singleLine = true,
            )
            Button(onClick = onRunAnalysis, enabled = !isRunning) {
                Text(context.getString(if (isRunning) R.string.action_running else R.string.dns_run_analysis))
            }
            if (isRunning && result == null) {
                CircularProgressIndicator()
            }
        }

        result?.let {
            FindingsCard(result = it)
            it.servers.forEach { server ->
                DnsServerCard(server = server)
            }
        }
    }
}

@Composable
private fun FindingsCard(result: DnsAnalysisResult) {
    val context = LocalContext.current
    DiagnosticsSectionSurface {
        Text(context.getString(R.string.findings), style = MaterialTheme.typography.titleMedium)
        Text(context.getString(R.string.dns_domain_value, result.domain), style = MaterialTheme.typography.bodyMedium)
        DiagnosticsStatusChip(
            text = context.getString(result.status.titleResId()),
            tone = when (result.status) {
                DnsAnalysisStatus.Ok -> DiagnosticsChipTone.Positive
                DnsAnalysisStatus.Blocked -> DiagnosticsChipTone.Negative
                DnsAnalysisStatus.Suspicious -> DiagnosticsChipTone.Caution
                DnsAnalysisStatus.CdnVariation -> DiagnosticsChipTone.Neutral
            },
        )
        Text(result.summary, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = context.getString(R.string.checked_at_value, result.checkedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DnsServerCard(server: DnsServerResult) {
    DiagnosticsSectionSurface {
        Text(server.serverName, style = MaterialTheme.typography.titleMedium)
        Text(
            text = server.serverAddress,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        server.records.forEach { record ->
            DnsRecordCard(record = record)
        }
    }
}

@Composable
private fun DnsRecordCard(record: DnsRecordResult) {
    val context = LocalContext.current
    DiagnosticsSectionSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(record.recordType, style = MaterialTheme.typography.titleMedium)
            Text(
                text = context.getString(record.transport.titleResId()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (record.error != null) {
            Text(
                text = record.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            record.addresses.forEach { address ->
                Text(address, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

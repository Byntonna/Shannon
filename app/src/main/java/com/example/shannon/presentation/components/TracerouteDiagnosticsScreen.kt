package com.example.shannon.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.millisecondsText
import com.example.shannon.domain.model.TracerouteResult

@Composable
fun TracerouteDiagnosticsScreen(
    host: String,
    result: TracerouteResult?,
    isRunning: Boolean,
    error: String?,
    onHostChange: (String) -> Unit,
    onRunTraceroute: () -> Unit,
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
                label = { Text(context.getString(R.string.traceroute_destination)) },
                singleLine = true,
            )
            Button(onClick = onRunTraceroute, enabled = !isRunning) {
                Text(context.getString(if (isRunning) R.string.action_running else R.string.traceroute_run))
            }
            if (isRunning && result == null) {
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

        result?.let {
            DiagnosticsSectionSurface {
                Text(context.getString(R.string.traceroute_destination_value, it.destination), style = MaterialTheme.typography.titleMedium)
                it.hops.forEachIndexed { index, hop ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(context.getString(R.string.traceroute_hop_value, hop.hopNumber), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (hop.timeout) {
                                context.getString(R.string.port_status_timeout)
                            } else {
                                listOfNotNull(
                                    hop.hostname,
                                    hop.ipAddress,
                    hop.latencyMs?.let { latency -> context.millisecondsText(latency) },
                                ).joinToString("  |  ")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (index != it.hops.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

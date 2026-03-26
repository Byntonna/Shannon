package com.example.shannon.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shannon.domain.model.PingPacketState
import com.example.shannon.domain.model.PingPacketStatus
import com.example.shannon.domain.model.PingResult

@Composable
fun PingDiagnosticsScreen(
    host: String,
    packetCountInput: String,
    packetStatuses: List<PingPacketStatus>,
    result: PingResult?,
    isRunning: Boolean,
    error: String?,
    onHostChange: (String) -> Unit,
    onPacketCountInputChange: (String) -> Unit,
    onRunPing: () -> Unit,
) {
    var isPacketStatusExpanded by rememberSaveable { mutableStateOf(false) }
    val completedPackets = packetStatuses.count { it.state != PingPacketState.Pending }
    val totalPackets = packetStatuses.size
    val currentPacket = when {
        totalPackets == 0 -> null
        completedPackets >= totalPackets -> totalPackets
        else -> completedPackets + 1
    }

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
                value = packetCountInput,
                onValueChange = onPacketCountInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Packet count") },
                placeholder = { Text("5") },
                singleLine = true,
                enabled = !isRunning,
            )
            Button(onClick = onRunPing, enabled = !isRunning) {
                Text(if (isRunning) "Running..." else "Run ping")
            }
            if (isRunning) {
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
                Text("Ping result", style = MaterialTheme.typography.titleMedium)
                MetricLine("Host", it.host)
                MetricLine("Packets", "${it.packetsReceived}/${it.packetsSent}")
                MetricLine("Packet loss", "${it.packetLoss}%")
                MetricLine("Min", "${it.minLatencyMs} ms")
                MetricLine("Avg", "${it.avgLatencyMs} ms")
                MetricLine("Max", "${it.maxLatencyMs} ms")
                MetricLine("Jitter", "${it.jitterMs} ms")
            }
        }

        if (packetStatuses.isNotEmpty()) {
            DiagnosticsSectionSurface {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPacketStatusExpanded = !isPacketStatusExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Packet status", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                currentPacket == null -> "No packets yet"
                                isRunning -> "Current packet: $currentPacket / $totalPackets"
                                else -> "Completed: $completedPackets / $totalPackets"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = if (isPacketStatusExpanded) {
                            Icons.Filled.KeyboardArrowDown
                        } else {
                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                        },
                        contentDescription = null,
                    )
                }
                AnimatedVisibility(
                    visible = isPacketStatusExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        packetStatuses.forEach { packet ->
                            PacketStatusLine(packet = packet)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PacketStatusLine(packet: PingPacketStatus) {
    val statusText = when (packet.state) {
        PingPacketState.Pending -> "Waiting"
        PingPacketState.Reply -> packet.detail ?: "${packet.latencyMs ?: 0.0} ms"
        PingPacketState.Lost -> "No reply"
        PingPacketState.Error -> packet.detail ?: "Error"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiagnosticsStatusChip(
            text = "#${packet.sequenceNumber}",
            tone = when (packet.state) {
                PingPacketState.Pending -> DiagnosticsChipTone.Neutral
                PingPacketState.Reply -> when {
                    (packet.latencyMs ?: Double.MAX_VALUE) < 5.0 -> DiagnosticsChipTone.Positive
                    (packet.latencyMs ?: Double.MAX_VALUE) <= 50.0 -> DiagnosticsChipTone.Caution
                    else -> DiagnosticsChipTone.Negative
                }
                PingPacketState.Lost,
                PingPacketState.Error -> DiagnosticsChipTone.Negative
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricLine(
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

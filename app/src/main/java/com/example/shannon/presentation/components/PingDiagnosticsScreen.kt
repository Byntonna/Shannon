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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.millisecondsText
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
    val context = LocalContext.current
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
                label = { Text(context.getString(R.string.host)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = packetCountInput,
                onValueChange = onPacketCountInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.ping_packet_count)) },
                placeholder = { Text(context.getString(R.string.ping_packet_count_placeholder)) },
                singleLine = true,
                enabled = !isRunning,
            )
            Button(onClick = onRunPing, enabled = !isRunning) {
                Text(context.getString(if (isRunning) R.string.action_running else R.string.ping_run))
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
                Text(context.getString(R.string.ping_result), style = MaterialTheme.typography.titleMedium)
                MetricLine(context.getString(R.string.host), it.host)
                MetricLine(context.getString(R.string.ping_packets), context.getString(R.string.ping_packets_value, it.packetsReceived, it.packetsSent))
                MetricLine(context.getString(R.string.ping_packet_loss), context.getString(R.string.percentage_value, it.packetLoss))
                    MetricLine(context.getString(R.string.ping_min), context.millisecondsText(it.minLatencyMs))
                    MetricLine(context.getString(R.string.ping_avg), context.millisecondsText(it.avgLatencyMs))
                    MetricLine(context.getString(R.string.ping_max), context.millisecondsText(it.maxLatencyMs))
                    MetricLine(context.getString(R.string.ping_jitter), context.millisecondsText(it.jitterMs))
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
                        Text(context.getString(R.string.ping_packet_status), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                currentPacket == null -> context.getString(R.string.ping_no_packets_yet)
                                isRunning -> context.getString(R.string.ping_current_packet_value, currentPacket, totalPackets)
                                else -> context.getString(R.string.ping_completed_packets_value, completedPackets, totalPackets)
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
    val context = LocalContext.current
    val statusText = when (packet.state) {
        PingPacketState.Pending -> context.getString(R.string.ping_waiting)
                    PingPacketState.Reply -> packet.detail ?: context.millisecondsText(packet.latencyMs ?: 0.0)
        PingPacketState.Lost -> context.getString(R.string.ping_no_reply)
        PingPacketState.Error -> packet.detail ?: context.getString(R.string.port_status_error)
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
            text = LocalContext.current.getString(R.string.label_with_colon, label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

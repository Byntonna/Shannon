package com.example.shannon.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.subtitleResId
import com.example.shannon.titleResId
import com.example.shannon.domain.model.ConnectivityStepResult
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.ConnectivityTestResult

private val ConnectivityStages = listOf("DNS", "TCP", "TLS", "HTTP")

@Composable
fun ConnectivityTestScreen(
    selectedTargetPreset: ConnectivityTargetPreset,
    testResult: ConnectivityTestResult?,
    isRunning: Boolean,
    onRunTest: () -> Unit,
    onRefreshNetwork: () -> Unit,
    onSelectTargetPreset: (ConnectivityTargetPreset) -> Unit,
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
            TargetPresetCard(
                selectedTargetPreset = selectedTargetPreset,
                testResult = testResult,
                onSelectTargetPreset = onSelectTargetPreset,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRunTest,
                    enabled = !isRunning,
                ) {
                    Text(context.getString(if (isRunning) R.string.action_running else R.string.connectivity_run_test))
                }
                Button(
                    onClick = onRefreshNetwork,
                    enabled = !isRunning,
                ) {
                    Text(context.getString(R.string.action_refresh_network))
                }
            }
            when {
                isRunning && testResult == null -> CircularProgressIndicator()
                testResult != null || isRunning -> ResultList(result = testResult, isRunning = isRunning)
                else -> PlaceholderCard(text = context.getString(R.string.connectivity_placeholder))
            }
        }
    }
}

@Composable
private fun TargetPresetCard(
    selectedTargetPreset: ConnectivityTargetPreset,
    testResult: ConnectivityTestResult?,
    onSelectTargetPreset: (ConnectivityTargetPreset) -> Unit,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    DiagnosticsSectionSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = context.getString(R.string.connectivity_target_preset),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = context.getString(selectedTargetPreset.titleResId()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = testResult?.let {
                        context.getString(R.string.connectivity_last_run_used_value, it.endpointLabel)
                    } ?: context.getString(R.string.connectivity_no_runs_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(context.getString(if (expanded) R.string.action_hide else R.string.action_show))
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = context.getString(selectedTargetPreset.subtitleResId()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ConnectivityTargetPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = preset == selectedTargetPreset,
                            onClick = { onSelectTargetPreset(preset) },
                            label = { Text(context.getString(preset.titleResId())) },
                        )
                    }
                }
                selectedTargetPreset.endpoints.forEachIndexed { index, endpoint ->
                    EndpointLine(
                        label = context.getString(if (index == 0) R.string.connectivity_primary else R.string.connectivity_fallback),
                        title = endpoint.label,
                        url = endpoint.url,
                    )
                }
                testResult?.let { result ->
                    HorizontalDivider()
                    EndpointLine(
                        label = context.getString(R.string.connectivity_last_run_used),
                        title = result.endpointLabel,
                        url = result.endpointUrl,
                    )
                    if (result.fallbackUsed) {
                        Text(
                            text = result.fallbackReason ?: context.getString(R.string.connectivity_fallback_used),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EndpointLine(
    label: String,
    title: String,
    url: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultList(result: ConnectivityTestResult?, isRunning: Boolean) {
    val context = LocalContext.current
    val stepMap = result?.steps.orEmpty().associateBy { it.stage }
    val hasCompletedRun = result != null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isRunning) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(context.getString(R.string.connectivity_updating_results), style = MaterialTheme.typography.bodyMedium)
            }
        }
        ConnectivityStages.forEachIndexed { index, stage ->
            StepRow(
                stage = stage,
                step = stepMap[stage],
                isRunning = isRunning,
                hasCompletedRun = hasCompletedRun,
            )
            if (index != ConnectivityStages.lastIndex) {
                HorizontalDivider()
            }
        }
        result?.let {
            Text(
                text = context.getString(R.string.checked_at_value, it.checkedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepRow(
    stage: String,
    step: ConnectivityStepResult?,
    isRunning: Boolean,
    hasCompletedRun: Boolean,
) {
    val context = LocalContext.current
    val statusText = when {
        step != null -> step.summary
        isRunning -> context.getString(R.string.status_pending)
        hasCompletedRun -> context.getString(R.string.connectivity_skipped_after_failure)
        else -> context.getString(R.string.status_not_run_yet)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusDot(
            success = step?.success,
            isPending = step == null && !hasCompletedRun,
            isSkipped = step == null && hasCompletedRun,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stage, style = MaterialTheme.typography.titleMedium)
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (step == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun StatusDot(success: Boolean?, isPending: Boolean, isSkipped: Boolean) {
    val color = when {
        isPending -> MaterialTheme.colorScheme.outline
        isSkipped -> MaterialTheme.colorScheme.secondary
        success == true -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .size(10.dp)
            .background(color = color, shape = RoundedCornerShape(50))
    )
}

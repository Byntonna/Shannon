package com.example.shannon.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.subtitleResId
import com.example.shannon.titleResId
import com.example.shannon.domain.model.WebsiteAccessibilityPreset
import com.example.shannon.domain.model.WebsiteAccessibilityResult
import com.example.shannon.domain.model.WebsiteAccessibilityStatus
import com.example.shannon.domain.model.WebsiteAccessibilityTarget

@Composable
fun WebsiteAccessibilityScreen(
    selectedPreset: WebsiteAccessibilityPreset,
    customInput: String,
    customTargets: List<WebsiteAccessibilityTarget>,
    results: List<WebsiteAccessibilityResult>,
    isRunning: Boolean,
    onRunTest: () -> Unit,
    onSelectPreset: (WebsiteAccessibilityPreset) -> Unit,
    onCustomInputChange: (String) -> Unit,
    onAddCustomTarget: () -> Unit,
    onRemoveCustomTarget: (String) -> Unit,
) {
    val context = LocalContext.current
    val displayedTargets = if (customTargets.isNotEmpty()) {
        customTargets
    } else {
        selectedPreset.targets
    }
    val showingCustomTargets = customTargets.isNotEmpty()

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
                text = context.getString(R.string.website_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WebsiteAccessibilityPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = preset == selectedPreset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(context.getString(preset.titleResId())) },
                    )
                }
            }
            Text(
                text = context.getString(selectedPreset.subtitleResId()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = customInput,
                onValueChange = onCustomInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.website_custom_site)) },
                placeholder = { Text(context.getString(R.string.website_custom_site_placeholder)) },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAddCustomTarget, enabled = !isRunning) {
                    Text(context.getString(R.string.website_add_site))
                }
                Button(onClick = onRunTest, enabled = !isRunning) {
                    Text(context.getString(if (isRunning) R.string.action_running else R.string.website_run_test))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = context.getString(if (showingCustomTargets) R.string.website_custom_targets else R.string.website_sites_to_test),
                    style = MaterialTheme.typography.titleMedium,
                )
                displayedTargets.forEach { target ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(target.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                target.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (showingCustomTargets) {
                            TextButton(onClick = { onRemoveCustomTarget(target.url) }) {
                                Text(context.getString(R.string.action_remove))
                            }
                        }
                    }
                }
            }
            when {
                isRunning && results.isEmpty() -> CircularProgressIndicator()
                results.isEmpty() -> Text(
                    text = context.getString(R.string.website_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        results.forEach { result ->
            WebsiteResultCard(result = result)
        }
    }
}

@Composable
private fun WebsiteResultCard(result: WebsiteAccessibilityResult) {
    var detailsExpanded by rememberSaveable(result.serviceName, result.targetUrl) {
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
                Text(
                    text = result.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = result.targetUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(status = result.status)
        }

        CollapsibleSectionHeader(
            title = LocalContext.current.getString(R.string.website_diagnostics_details),
            subtitle = LocalContext.current.getString(if (detailsExpanded) R.string.action_tap_to_collapse else R.string.action_tap_to_expand),
            expanded = detailsExpanded,
            onToggle = { detailsExpanded = !detailsExpanded },
        )

        AnimatedVisibility(
            visible = detailsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                result.diagnostics.steps.forEachIndexed { index, step ->
                    StepLine(stage = step.stage, summary = step.summary, success = step.success)
                    if (index != result.diagnostics.steps.lastIndex) {
                        HorizontalDivider()
                    }
                }

                Text(
                    text = LocalContext.current.getString(R.string.checked_at_value, result.diagnostics.checkedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: WebsiteAccessibilityStatus) {
    DiagnosticsStatusChip(
        text = LocalContext.current.getString(status.titleResId()),
        tone = when (status) {
            WebsiteAccessibilityStatus.Ok -> DiagnosticsChipTone.Positive
            WebsiteAccessibilityStatus.DnsBlocked,
            WebsiteAccessibilityStatus.TcpBlocked,
            WebsiteAccessibilityStatus.TlsError,
            WebsiteAccessibilityStatus.HttpError -> DiagnosticsChipTone.Negative
        },
    )
}

@Composable
private fun StepLine(
    stage: String,
    summary: String,
    success: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(
                    color = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(50),
                )
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stage, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WebsiteAccessibilityScreenPreview() {
    WebsiteAccessibilityScreen(
        selectedPreset = WebsiteAccessibilityPreset.Popular,
        customInput = "",
        customTargets = emptyList(),
        results = emptyList(),
        isRunning = false,
        onRunTest = {},
        onSelectPreset = {},
        onCustomInputChange = {},
        onAddCustomTarget = {},
        onRemoveCustomTarget = {},
    )
}

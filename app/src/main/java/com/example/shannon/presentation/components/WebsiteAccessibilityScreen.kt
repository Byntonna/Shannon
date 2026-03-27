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
import com.example.shannon.domain.model.WebsiteAccessibilityCategory
import com.example.shannon.domain.model.WebsiteAccessibilityOutcome
import com.example.shannon.domain.model.WebsiteAccessibilityPreset
import com.example.shannon.domain.model.WebsiteAccessibilityResult
import com.example.shannon.domain.model.WebsiteAccessibilityStatus
import com.example.shannon.domain.model.WebsiteAccessibilityTarget
import com.example.shannon.domain.model.toOutcome

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
    val activeTargets = when (selectedPreset) {
        WebsiteAccessibilityPreset.Custom -> customTargets
        else -> selectedPreset.targets
    }
    val targetIndex = activeTargets.associateBy { it.url }
    val groupedTargets = activeTargets
        .groupBy { it.category }
        .toList()
        .sortedBy { (category, _) -> category.sortOrder() }
    val sortedResults = results.sortedWith(
        compareBy<WebsiteAccessibilityResult> { it.status.toOutcome().sortOrder() }
            .thenBy { targetIndex[it.targetUrl]?.category?.sortOrder() ?: WebsiteAccessibilityCategory.Custom.sortOrder() }
            .thenBy { it.serviceName.lowercase() },
    )
    val availableCount = results.count { it.status.toOutcome() == WebsiteAccessibilityOutcome.Available }
    val unstableCount = results.count { it.status.toOutcome() == WebsiteAccessibilityOutcome.Unstable }
    val limitedCount = results.count { it.status.toOutcome() == WebsiteAccessibilityOutcome.Limited }
    val canRun = !isRunning && activeTargets.isNotEmpty()

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
            Text(
                text = context.getString(R.string.website_summary_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (results.isEmpty()) {
                Text(
                    text = context.getString(R.string.website_summary_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DiagnosticsStatusChip(
                        text = context.getString(R.string.website_summary_available_count, availableCount),
                        tone = DiagnosticsChipTone.Positive,
                    )
                    DiagnosticsStatusChip(
                        text = context.getString(R.string.website_summary_unstable_count, unstableCount),
                        tone = DiagnosticsChipTone.Caution,
                    )
                    DiagnosticsStatusChip(
                        text = context.getString(R.string.website_summary_limited_count, limitedCount),
                        tone = DiagnosticsChipTone.Negative,
                    )
                }
                results.firstOrNull()?.diagnostics?.checkedAt?.let { checkedAt ->
                    Text(
                        text = context.getString(R.string.checked_at_value, checkedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = context.getString(R.string.website_summary_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DiagnosticsSectionSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = context.getString(R.string.website_preset_title),
                style = MaterialTheme.typography.titleMedium,
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
            Text(
                text = context.getString(R.string.website_custom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAddCustomTarget, enabled = !isRunning) {
                    Text(context.getString(R.string.website_add_site))
                }
                Button(onClick = onRunTest, enabled = canRun) {
                    Text(context.getString(if (isRunning) R.string.action_running else R.string.website_run_test))
                }
            }
        }

        DiagnosticsSectionSurface {
            Text(
                text = context.getString(R.string.website_targets_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (groupedTargets.isEmpty()) {
                Text(
                    text = context.getString(R.string.website_empty_custom),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                groupedTargets.forEachIndexed { index, (category, targets) ->
                    TargetGroup(
                        category = category,
                        targets = targets,
                        showRemoveAction = selectedPreset == WebsiteAccessibilityPreset.Custom,
                        onRemoveTarget = onRemoveCustomTarget,
                    )
                    if (index != groupedTargets.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

        when {
            isRunning && results.isEmpty() -> DiagnosticsSectionSurface {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = context.getString(R.string.action_running),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            results.isEmpty() -> DiagnosticsSectionSurface {
                Text(
                    text = if (selectedPreset == WebsiteAccessibilityPreset.Custom && customTargets.isEmpty()) {
                        context.getString(R.string.website_empty_custom)
                    } else {
                        context.getString(R.string.website_placeholder)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Text(
                    text = context.getString(R.string.website_results_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                sortedResults.forEach { result ->
                    WebsiteResultCard(
                        result = result,
                        category = targetIndex[result.targetUrl]?.category ?: WebsiteAccessibilityCategory.Custom,
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetGroup(
    category: WebsiteAccessibilityCategory,
    targets: List<WebsiteAccessibilityTarget>,
    showRemoveAction: Boolean,
    onRemoveTarget: (String) -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = context.getString(category.titleResId()),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = context.getString(category.subtitleResId()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        targets.forEach { target ->
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
                if (showRemoveAction) {
                    TextButton(onClick = { onRemoveTarget(target.url) }) {
                        Text(context.getString(R.string.action_remove))
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsiteResultCard(
    result: WebsiteAccessibilityResult,
    category: WebsiteAccessibilityCategory,
) {
    val context = LocalContext.current
    val outcome = result.status.toOutcome()
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
            DiagnosticsStatusChip(
                text = context.getString(outcome.titleResId()),
                tone = outcome.tone(),
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DiagnosticsStatusChip(
                text = context.getString(category.titleResId()),
                tone = DiagnosticsChipTone.Neutral,
            )
            DiagnosticsStatusChip(
                text = context.getString(result.status.titleResId()),
                tone = rawStatusTone(result.status),
            )
        }

        Text(
            text = context.getString(result.summaryResId()),
            style = MaterialTheme.typography.bodyMedium,
        )

        result.contextNote(category)?.let { noteResId ->
            Text(
                text = context.getString(noteResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        CollapsibleSectionHeader(
            title = context.getString(R.string.website_diagnostics_details),
            subtitle = context.getString(if (detailsExpanded) R.string.action_tap_to_collapse else R.string.action_tap_to_expand),
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
                    text = context.getString(R.string.checked_at_value, result.diagnostics.checkedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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

private fun WebsiteAccessibilityCategory.sortOrder(): Int = when (this) {
    WebsiteAccessibilityCategory.LocalControl -> 0
    WebsiteAccessibilityCategory.ForeignControl -> 1
    WebsiteAccessibilityCategory.Restricted -> 2
    WebsiteAccessibilityCategory.Sensitive -> 3
    WebsiteAccessibilityCategory.Custom -> 4
}

private fun WebsiteAccessibilityOutcome.sortOrder(): Int = when (this) {
    WebsiteAccessibilityOutcome.Limited -> 0
    WebsiteAccessibilityOutcome.Unstable -> 1
    WebsiteAccessibilityOutcome.Available -> 2
}

private fun WebsiteAccessibilityOutcome.tone(): DiagnosticsChipTone = when (this) {
    WebsiteAccessibilityOutcome.Available -> DiagnosticsChipTone.Positive
    WebsiteAccessibilityOutcome.Unstable -> DiagnosticsChipTone.Caution
    WebsiteAccessibilityOutcome.Limited -> DiagnosticsChipTone.Negative
}

private fun rawStatusTone(status: WebsiteAccessibilityStatus): DiagnosticsChipTone = when (status) {
    WebsiteAccessibilityStatus.Ok -> DiagnosticsChipTone.Positive
    WebsiteAccessibilityStatus.HttpError -> DiagnosticsChipTone.Caution
    WebsiteAccessibilityStatus.DnsBlocked,
    WebsiteAccessibilityStatus.TcpBlocked,
    WebsiteAccessibilityStatus.TlsError -> DiagnosticsChipTone.Negative
}

private fun WebsiteAccessibilityResult.summaryResId(): Int = when (status) {
    WebsiteAccessibilityStatus.Ok -> R.string.website_result_available_summary
    WebsiteAccessibilityStatus.HttpError -> R.string.website_result_unstable_summary
    WebsiteAccessibilityStatus.DnsBlocked -> R.string.website_result_limited_dns
    WebsiteAccessibilityStatus.TcpBlocked -> R.string.website_result_limited_tcp
    WebsiteAccessibilityStatus.TlsError -> R.string.website_result_limited_tls
}

private fun WebsiteAccessibilityResult.contextNote(
    category: WebsiteAccessibilityCategory,
): Int? {
    val normalizedUrl = targetUrl.lowercase()
    return when {
        "youtube.com" in normalizedUrl || "youtu.be" in normalizedUrl -> R.string.website_context_youtube
        "telegram.org" in normalizedUrl -> R.string.website_context_telegram
        category == WebsiteAccessibilityCategory.LocalControl && status.toOutcome() != WebsiteAccessibilityOutcome.Available ->
            R.string.website_context_control_failure
        category == WebsiteAccessibilityCategory.ForeignControl && status.toOutcome() != WebsiteAccessibilityOutcome.Available ->
            R.string.website_context_control_failure
        else -> null
    }
}

@Preview(showBackground = true)
@Composable
private fun WebsiteAccessibilityScreenPreview() {
    WebsiteAccessibilityScreen(
        selectedPreset = WebsiteAccessibilityPreset.Baseline,
        customInput = "",
        customTargets = emptyList(),
        results = listOf(
            WebsiteAccessibilityResult(
                serviceName = "YouTube",
                targetUrl = "https://www.youtube.com/",
                status = WebsiteAccessibilityStatus.HttpError,
                diagnostics = com.example.shannon.domain.model.ConnectivityTestResult(
                    steps = emptyList(),
                    checkedAt = "09:41:03",
                    endpointLabel = "YouTube",
                    endpointUrl = "https://www.youtube.com/",
                    fallbackUsed = false,
                ),
            )
        ),
        isRunning = false,
        onRunTest = {},
        onSelectPreset = {},
        onCustomInputChange = {},
        onAddCustomTarget = {},
        onRemoveCustomTarget = {},
    )
}

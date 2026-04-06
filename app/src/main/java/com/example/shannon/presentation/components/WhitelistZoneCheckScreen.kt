package com.example.shannon.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.example.shannon.subtitleResId
import com.example.shannon.titleResId
import com.example.shannon.domain.model.WhitelistZoneCheckResult
import com.example.shannon.domain.model.WhitelistZoneTargetGroup
import com.example.shannon.domain.model.WhitelistZoneTargetResult
import com.example.shannon.domain.model.WhitelistZoneVerdict

@Composable
fun WhitelistZoneCheckScreen(
    result: WhitelistZoneCheckResult?,
    isRunning: Boolean,
    onRunCheck: () -> Unit,
) {
    val context = LocalContext.current
    val groupedResults = result?.results
        ?.groupBy { it.target.group }
        ?.toList()
        ?.sortedBy { it.first.sortOrder() }
        .orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DiagnosticsSectionSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = context.getString(R.string.whitelist_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DiagnosticPrimaryButton(
                text = context.getString(if (isRunning) R.string.action_running else R.string.whitelist_run_check),
                onClick = onRunCheck,
                enabled = !isRunning,
            )
        }

        DiagnosticsSectionSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = context.getString(R.string.whitelist_summary_title),
                style = MaterialTheme.typography.titleMedium,
            )
            when {
                isRunning && result == null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(context.getString(R.string.action_running))
                    }
                }
                result == null -> {
                    Text(
                        text = context.getString(R.string.whitelist_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    DiagnosticsStatusChip(
                        text = context.getString(result.verdict.titleResId()),
                        tone = result.verdict.tone(),
                    )
                    Text(
                        text = context.getString(result.verdict.summaryResId()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        result.reasonItems().forEach { (reasonResId, tone) ->
                            DiagnosticsStatusChip(
                                text = context.getString(reasonResId),
                                tone = tone,
                            )
                        }
                    }
                    Text(
                        text = context.getString(R.string.checked_at_value, result.checkedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (groupedResults.isNotEmpty()) {
            DiagnosticsSectionHeader(
                title = context.getString(R.string.whitelist_results_title),
                subtitle = context.getString(R.string.whitelist_results_subtitle),
            )
            groupedResults.forEach { (group, targets) ->
                DiagnosticsSectionSurface {
                    Text(
                        text = context.getString(group.titleResId()),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = context.getString(group.subtitleResId()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    targets.forEachIndexed { index, item ->
                        WhitelistTargetCard(item)
                        if (index != targets.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhitelistTargetCard(
    result: WhitelistZoneTargetResult,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(result.target.url) { mutableStateOf(false) }

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
                Text(result.target.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    result.target.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DiagnosticsStatusChip(
                text = context.getString(
                    if (result.accessible) R.string.whitelist_target_accessible else R.string.whitelist_target_unavailable
                ),
                tone = if (result.accessible) DiagnosticsChipTone.Positive else DiagnosticsChipTone.Negative,
            )
        }

        CollapsibleSectionHeader(
            title = context.getString(R.string.whitelist_diagnostics_details),
            subtitle = context.getString(if (expanded) R.string.action_tap_to_collapse else R.string.action_tap_to_expand),
            expanded = expanded,
            onToggle = { expanded = !expanded },
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                result.diagnostics.steps.forEachIndexed { index, step ->
                    WhitelistStepLine(stage = step.stage, summary = step.summary, success = step.success)
                    if (index != result.diagnostics.steps.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun WhitelistStepLine(
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
                    color = if (success) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    shape = RoundedCornerShape(50),
                ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stage, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun WhitelistZoneTargetGroup.sortOrder(): Int = when (this) {
    WhitelistZoneTargetGroup.LocalControl -> 0
    WhitelistZoneTargetGroup.ForeignControl -> 1
    WhitelistZoneTargetGroup.BlockedReference -> 2
}

private fun WhitelistZoneVerdict.tone(): DiagnosticsChipTone = when (this) {
    WhitelistZoneVerdict.InWhitelistZone -> DiagnosticsChipTone.Negative
    WhitelistZoneVerdict.OutsideWhitelistZone -> DiagnosticsChipTone.Positive
    WhitelistZoneVerdict.NoWhitelistDetectedButReferenceBlocked -> DiagnosticsChipTone.Caution
    WhitelistZoneVerdict.Inconclusive -> DiagnosticsChipTone.Neutral
}

private fun WhitelistZoneVerdict.summaryResId(): Int = when (this) {
    WhitelistZoneVerdict.InWhitelistZone -> R.string.whitelist_verdict_in_zone_summary
    WhitelistZoneVerdict.OutsideWhitelistZone -> R.string.whitelist_verdict_outside_zone_summary
    WhitelistZoneVerdict.NoWhitelistDetectedButReferenceBlocked -> {
        R.string.whitelist_verdict_no_whitelist_but_reference_blocked_summary
    }
    WhitelistZoneVerdict.Inconclusive -> R.string.whitelist_verdict_inconclusive_summary
}

private fun WhitelistZoneCheckResult.reasonResIds(): List<Int> {
    val localAccessible = results
        .filter { it.target.group == WhitelistZoneTargetGroup.LocalControl }
        .all { it.accessible }
    val foreignBlocked = results
        .count { it.target.group == WhitelistZoneTargetGroup.ForeignControl && !it.accessible }
    val blockedUnavailable = results
        .count { it.target.group == WhitelistZoneTargetGroup.BlockedReference && !it.accessible }

    return reasonItems().map { it.first }
}

private fun WhitelistZoneCheckResult.reasonItems(): List<Pair<Int, DiagnosticsChipTone>> {
    val localAccessible = results
        .filter { it.target.group == WhitelistZoneTargetGroup.LocalControl }
        .all { it.accessible }
    val foreignBlocked = results
        .count { it.target.group == WhitelistZoneTargetGroup.ForeignControl && !it.accessible }
    val blockedUnavailable = results
        .count { it.target.group == WhitelistZoneTargetGroup.BlockedReference && !it.accessible }

    return buildList {
        add(
            if (localAccessible) {
                R.string.whitelist_reason_local_ok to DiagnosticsChipTone.Positive
            } else {
                R.string.whitelist_reason_local_failed to DiagnosticsChipTone.Negative
            }
        )
        add(
            if (foreignBlocked >= 2) {
                R.string.whitelist_reason_foreign_blocked to DiagnosticsChipTone.Negative
            } else {
                R.string.whitelist_reason_foreign_reachable to DiagnosticsChipTone.Positive
            }
        )
        add(
            if (blockedUnavailable >= 1) {
                R.string.whitelist_reason_blocked_refs_unavailable to DiagnosticsChipTone.Caution
            } else {
                R.string.whitelist_reason_blocked_refs_reachable to DiagnosticsChipTone.Positive
            }
        )
    }
}

package com.example.shannon.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.titleResId
import com.example.shannon.presentation.model.DiagnosticsDestination
import com.example.shannon.presentation.model.DiagnosticsUiState
import com.example.shannon.presentation.model.HomeSummaryReason
import com.example.shannon.presentation.model.HomeSummaryState
import com.example.shannon.presentation.model.HomeSummaryTone
import com.example.shannon.presentation.model.homeSummaryState

@Composable
fun HomeSummaryDetailsScreen(
    uiState: DiagnosticsUiState,
    onRunHomeSummaryCheck: () -> Unit,
    onOpenScreen: (DiagnosticsDestination) -> Unit,
) {
    val summary = uiState.homeSummaryState()
    val scrollState = rememberScrollState()
    val (containerColor, contentColor) = summaryCardColors(
        tone = summary.tone,
        isDarkTheme = isSystemInDarkTheme(),
    )
    val icon = when (summary.tone) {
        HomeSummaryTone.Positive -> Icons.Filled.CheckCircle
        HomeSummaryTone.Warning,
        HomeSummaryTone.Error,
        HomeSummaryTone.Neutral -> Icons.Filled.Info
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(summary.titleResId),
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor,
                    )
                    Text(
                        text = stringResource(summary.descriptionResId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.9f),
                    )
                    if (summary.showRunCheckAction) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRunHomeSummaryCheck,
                            enabled = !uiState.isRunningHomeSummaryCheck,
                        ) {
                            Text(
                                text = if (uiState.isRunningHomeSummaryCheck) {
                                    stringResource(R.string.home_summary_running_check)
                                } else {
                                    stringResource(R.string.home_summary_run_check)
                                }
                            )
                        }
                    }
                }
            }
        }

        DiagnosticsSectionSurface {
            Text(
                text = stringResource(R.string.home_summary_details_meaning_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(summary.explanationResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DiagnosticsSectionSurface {
            Text(
                text = stringResource(R.string.home_summary_details_reasons_title),
                style = MaterialTheme.typography.titleMedium,
            )
            summary.reasons.forEach { reason ->
                SummaryReasonCard(reason = reason)
            }
        }

        DiagnosticsSectionSurface {
            Text(
                text = stringResource(R.string.home_summary_details_next_steps_title),
                style = MaterialTheme.typography.titleMedium,
            )
            summary.nextSteps.forEach { destination ->
                Button(
                    onClick = { onOpenScreen(destination) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(destination.titleResId()))
                }
            }
        }
    }
}

@Composable
private fun SummaryReasonCard(
    reason: HomeSummaryReason,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(reason.titleResId),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = stringResource(reason.descriptionResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun summaryCardColors(
    tone: HomeSummaryTone,
    isDarkTheme: Boolean,
): Pair<Color, Color> = when (tone) {
    HomeSummaryTone.Positive -> {
        if (isDarkTheme) Color(0xFF173526) to Color(0xFF8DD8A8) else Color(0xFFE8F4EA) to Color(0xFF1E7B46)
    }
    HomeSummaryTone.Warning -> {
        if (isDarkTheme) Color(0xFF3A2B17) to Color(0xFFF2C27B) else Color(0xFFFFF2E0) to Color(0xFF9A5E00)
    }
    HomeSummaryTone.Error -> {
        if (isDarkTheme) Color(0xFF3F1D1E) to Color(0xFFF2B8B5) else Color(0xFFFDECEC) to Color(0xFFB3261E)
    }
    HomeSummaryTone.Neutral -> {
        if (isDarkTheme) Color(0xFF23262C) to Color(0xFFE2E2E6) else Color(0xFFF1EFF8) to Color(0xFF4B4F58)
    }
}

package com.example.shannon.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shannon.R
import com.example.shannon.sectionTitleResId
import com.example.shannon.titleResId
import com.example.shannon.domain.model.ReportFormat

@Composable
fun ReportExportScreen(
    isExporting: Boolean,
    lastFormat: ReportFormat?,
    exportedReportName: String?,
    error: String?,
    availableSections: List<String>,
    onExport: (ReportFormat) -> Unit,
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
            Text(
                text = context.getString(R.string.report_export_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = context.getString(
                    R.string.report_export_available_sections,
                    availableSections.joinToString { section ->
                        sectionTitleResId(section)?.let(context::getString) ?: section
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportFormat.entries.forEach { format ->
                    Button(
                        onClick = { onExport(format) },
                        enabled = !isExporting,
                    ) {
                        Text(context.getString(R.string.report_export_share_format, context.getString(format.titleResId())))
                    }
                }
            }
            if (isExporting) {
                CircularProgressIndicator()
            }
            lastFormat?.let { format ->
                Text(
                    text = context.getString(R.string.report_export_last_export, context.getString(format.titleResId())),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            exportedReportName?.let { reportName ->
                Text(
                    text = context.getString(R.string.report_export_prepared_file, reportName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

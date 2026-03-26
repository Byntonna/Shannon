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
import androidx.compose.ui.unit.dp
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
                text = "Export the latest diagnostics snapshot and open Android's share sheet as JSON, Markdown, or plain text.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Available sections: ${availableSections.joinToString()}",
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
                        Text("Share ${format.title}")
                    }
                }
            }
            if (isExporting) {
                CircularProgressIndicator()
            }
            lastFormat?.let { format ->
                Text(
                    text = "Last export: ${format.title}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            exportedReportName?.let { reportName ->
                Text(
                    text = "Prepared file: $reportName",
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

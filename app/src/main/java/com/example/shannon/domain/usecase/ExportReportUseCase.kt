package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.NetworkDiagnosticReport
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class ExportReportUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(
        report: NetworkDiagnosticReport,
        format: ReportFormat,
    ): String {
        return repository.exportReport(report, format)
    }
}

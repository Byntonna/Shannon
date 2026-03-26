package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.TlsAnalysisResult
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunTlsAnalysisUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(): TlsAnalysisResult {
        return repository.performTlsAnalysis()
    }
}

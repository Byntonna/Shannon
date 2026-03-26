package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunSniMitmAnalysisUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(): SniMitmAnalysisResult {
        return repository.performSniMitmAnalysis()
    }
}

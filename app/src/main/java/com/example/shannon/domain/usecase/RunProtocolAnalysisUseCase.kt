package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.ProtocolAnalysisResult
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunProtocolAnalysisUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(): ProtocolAnalysisResult {
        return repository.performProtocolAnalysis()
    }
}

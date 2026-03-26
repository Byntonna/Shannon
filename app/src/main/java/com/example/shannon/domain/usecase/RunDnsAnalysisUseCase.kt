package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.DnsAnalysisResult
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunDnsAnalysisUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(domain: String): DnsAnalysisResult {
        return repository.performDnsAnalysis(domain)
    }
}

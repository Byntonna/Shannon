package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.TracerouteResult
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunTracerouteUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(host: String): TracerouteResult {
        return repository.runTraceroute(host)
    }
}

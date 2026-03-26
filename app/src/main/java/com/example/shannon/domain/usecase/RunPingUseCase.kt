package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.PingResult
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunPingUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(
        host: String,
        count: Int = 5,
    ): PingResult {
        return repository.runPing(host, count)
    }
}

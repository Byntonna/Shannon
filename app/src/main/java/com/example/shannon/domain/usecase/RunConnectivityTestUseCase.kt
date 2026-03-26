package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.ConnectivityTestResult
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunConnectivityTestUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(targetPreset: ConnectivityTargetPreset): ConnectivityTestResult {
        return repository.performConnectivityTest(targetPreset)
    }
}

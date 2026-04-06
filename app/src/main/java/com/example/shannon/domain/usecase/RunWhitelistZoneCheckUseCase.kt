package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.WhitelistZoneCheckResult
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunWhitelistZoneCheckUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(): WhitelistZoneCheckResult {
        return repository.performWhitelistZoneCheck()
    }
}


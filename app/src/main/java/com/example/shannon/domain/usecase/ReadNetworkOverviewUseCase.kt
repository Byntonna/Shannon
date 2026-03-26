package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.NetworkOverview
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class ReadNetworkOverviewUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(): NetworkOverview = repository.readNetworkOverview()
}

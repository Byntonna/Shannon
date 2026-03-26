package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.WebsiteAccessibilityResult
import com.example.shannon.domain.model.WebsiteAccessibilityTarget
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository

class RunWebsiteAccessibilityTestUseCase(
    private val repository: NetworkDiagnosticsRepository,
) {
    suspend operator fun invoke(
        targets: List<WebsiteAccessibilityTarget>,
    ): List<WebsiteAccessibilityResult> {
        return repository.performWebsiteAccessibilityTest(targets)
    }
}

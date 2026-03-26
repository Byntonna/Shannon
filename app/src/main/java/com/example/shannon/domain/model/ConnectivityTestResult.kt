package com.example.shannon.domain.model

data class ConnectivityTestResult(
    val steps: List<ConnectivityStepResult>,
    val checkedAt: String,
    val endpointLabel: String,
    val endpointUrl: String,
    val fallbackUsed: Boolean,
    val fallbackReason: String? = null,
)

data class ConnectivityStepResult(
    val stage: String,
    val success: Boolean,
    val summary: String,
)

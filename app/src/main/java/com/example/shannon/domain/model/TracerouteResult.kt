package com.example.shannon.domain.model

data class TracerouteHop(
    val hopNumber: Int,
    val ipAddress: String?,
    val hostname: String?,
    val latencyMs: Double?,
    val timeout: Boolean,
)

data class TracerouteResult(
    val destination: String,
    val hops: List<TracerouteHop>,
)

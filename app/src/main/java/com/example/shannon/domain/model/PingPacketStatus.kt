package com.example.shannon.domain.model

enum class PingPacketState {
    Pending,
    Reply,
    Lost,
    Error,
}

data class PingPacketStatus(
    val sequenceNumber: Int,
    val state: PingPacketState,
    val latencyMs: Double? = null,
    val detail: String? = null,
)

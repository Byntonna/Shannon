package com.example.shannon.domain.model

enum class PingPacketState(val title: String) {
    Pending("Pending"),
    Reply("Reply"),
    Lost("Lost"),
    Error("Error"),
}

data class PingPacketStatus(
    val sequenceNumber: Int,
    val state: PingPacketState,
    val latencyMs: Double? = null,
    val detail: String? = null,
)

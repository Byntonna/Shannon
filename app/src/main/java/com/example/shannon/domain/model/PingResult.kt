package com.example.shannon.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt

data class PingResult(
    val host: String,
    val packetsSent: Int,
    val packetsReceived: Int,
    val packetLoss: Float,
    val minLatencyMs: Double,
    val avgLatencyMs: Double,
    val maxLatencyMs: Double,
    val jitterMs: Double,
) {
    companion object {
        fun fromLatencies(
            host: String,
            packetsSent: Int,
            latencies: List<Double>,
        ): PingResult {
            require(latencies.isNotEmpty()) { "No successful ping replies were received" }

            val packetsReceived = latencies.size
            val avg = latencies.average()
            val jitter = latencies.zipWithNext { first, second -> abs(second - first) }
                .average()
                .takeIf { !it.isNaN() }
                ?: 0.0
            val packetLoss =
                ((packetsSent - packetsReceived).toFloat() / packetsSent.toFloat()) * 100f

            return PingResult(
                host = host,
                packetsSent = packetsSent,
                packetsReceived = packetsReceived,
                packetLoss = ((packetLoss * 10f).roundToInt() / 10f),
                minLatencyMs = latencies.minOrNull() ?: 0.0,
                avgLatencyMs = ((avg * 10.0).roundToInt() / 10.0),
                maxLatencyMs = latencies.maxOrNull() ?: 0.0,
                jitterMs = ((jitter * 10.0).roundToInt() / 10.0),
            )
        }
    }
}

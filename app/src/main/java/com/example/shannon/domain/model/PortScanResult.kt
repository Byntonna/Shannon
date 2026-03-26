package com.example.shannon.domain.model

data class PortScanResult(
    val host: String,
    val port: Int,
    val transport: PortScanTransport,
    val ipVersion: PortScanIpVersion,
    val resolvedAddress: String?,
    val status: PortStatus,
    val latencyMs: Long?,
    val error: String?
)

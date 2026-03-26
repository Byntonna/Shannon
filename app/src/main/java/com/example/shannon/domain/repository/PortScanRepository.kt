package com.example.shannon.domain.repository

import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanResult
import com.example.shannon.domain.model.PortScanTransport

interface PortScanRepository {
    suspend fun scanPort(
        host: String,
        port: Int,
        transport: PortScanTransport,
        ipVersion: PortScanIpVersion,
    ): PortScanResult
}

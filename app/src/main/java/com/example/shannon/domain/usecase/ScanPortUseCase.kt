package com.example.shannon.domain.usecase

import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanResult
import com.example.shannon.domain.model.PortScanTransport
import com.example.shannon.domain.repository.PortScanRepository

class ScanPortUseCase(
    private val repository: PortScanRepository
) {
    suspend operator fun invoke(
        host: String,
        port: Int,
        transport: PortScanTransport,
        ipVersion: PortScanIpVersion,
    ): PortScanResult {
        return repository.scanPort(host, port, transport, ipVersion)
    }
}

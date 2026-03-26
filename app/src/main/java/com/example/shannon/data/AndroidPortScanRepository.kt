package com.example.shannon.data

import android.content.Context
import com.example.shannon.R
import com.example.shannon.titleResId
import com.example.shannon.domain.model.PortScanIpVersion
import com.example.shannon.domain.model.PortScanResult
import com.example.shannon.domain.model.PortScanTransport
import com.example.shannon.domain.model.PortStatus
import com.example.shannon.domain.repository.PortScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.system.measureNanoTime

class AndroidPortScanRepository(
    private val context: Context,
) : PortScanRepository {
    override suspend fun scanPort(
        host: String,
        port: Int,
        transport: PortScanTransport,
        ipVersion: PortScanIpVersion,
    ): PortScanResult = withContext(Dispatchers.IO) {
        val normalizedHost = host.trim()
        if (normalizedHost.isBlank()) {
            return@withContext errorResult(
                host = host,
                port = port,
                transport = transport,
                ipVersion = ipVersion,
                message = context.getString(R.string.error_host_required),
            )
        }
        if (port !in 1..65535) {
            return@withContext errorResult(
                host = normalizedHost,
                port = port,
                transport = transport,
                ipVersion = ipVersion,
                message = context.getString(R.string.error_port_range),
            )
        }

        val address = runCatching { resolveAddress(normalizedHost, ipVersion) }.getOrElse { error ->
            return@withContext errorResult(
                host = normalizedHost,
                port = port,
                transport = transport,
                ipVersion = ipVersion,
                message = error.message ?: error.javaClass.simpleName,
            )
        }

        if (address == null) {
            return@withContext errorResult(
                host = normalizedHost,
                port = port,
                transport = transport,
                ipVersion = ipVersion,
                message = context.getString(R.string.error_no_ip_for_host, context.getString(ipVersion.titleResId())),
            )
        }

        when (transport) {
            PortScanTransport.Tcp -> scanTcpPort(normalizedHost, port, ipVersion, address)
            PortScanTransport.Udp -> scanUdpPort(normalizedHost, port, ipVersion, address)
        }
    }

    private fun scanTcpPort(
        host: String,
        port: Int,
        ipVersion: PortScanIpVersion,
        address: InetAddress,
    ): PortScanResult {
        val socket = Socket()
        return try {
            val latencyMs = measureNanoTime {
                socket.connect(InetSocketAddress(address, port), TCP_TIMEOUT_MS)
            } / 1_000_000
            PortScanResult(
                host = host,
                port = port,
                transport = PortScanTransport.Tcp,
                ipVersion = ipVersion,
                resolvedAddress = address.hostAddress,
                status = PortStatus.OPEN,
                latencyMs = latencyMs,
                error = null,
            )
        } catch (error: Throwable) {
            PortScanResult(
                host = host,
                port = port,
                transport = PortScanTransport.Tcp,
                ipVersion = ipVersion,
                resolvedAddress = address.hostAddress,
                status = when (error) {
                    is ConnectException -> PortStatus.CLOSED
                    is SocketTimeoutException, is NoRouteToHostException -> PortStatus.FILTERED
                    is UnknownHostException -> PortStatus.ERROR
                    else -> PortStatus.ERROR
                },
                latencyMs = null,
                error = error.message ?: error.javaClass.simpleName,
            )
        } finally {
            socket.close()
        }
    }

    private fun scanUdpPort(
        host: String,
        port: Int,
        ipVersion: PortScanIpVersion,
        address: InetAddress,
    ): PortScanResult {
        val socket = DatagramSocket(null)
        return try {
            socket.soTimeout = UDP_TIMEOUT_MS
            socket.connect(address, port)

            val payload = byteArrayOf(0x00)
            val sendPacket = DatagramPacket(payload, payload.size, address, port)
            val responseBuffer = ByteArray(32)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)

            val latencyMs = measureNanoTime {
                socket.send(sendPacket)
                socket.receive(responsePacket)
            } / 1_000_000

            PortScanResult(
                host = host,
                port = port,
                transport = PortScanTransport.Udp,
                ipVersion = ipVersion,
                resolvedAddress = address.hostAddress,
                status = PortStatus.OPEN,
                latencyMs = latencyMs,
                error = null,
            )
        } catch (error: Throwable) {
            PortScanResult(
                host = host,
                port = port,
                transport = PortScanTransport.Udp,
                ipVersion = ipVersion,
                resolvedAddress = address.hostAddress,
                status = when (error) {
                    is PortUnreachableException -> PortStatus.CLOSED
                    is SocketTimeoutException -> PortStatus.TIMEOUT
                    is NoRouteToHostException -> PortStatus.FILTERED
                    is UnknownHostException -> PortStatus.ERROR
                    else -> PortStatus.ERROR
                },
                latencyMs = null,
                error = when (error) {
                    is SocketTimeoutException -> context.getString(R.string.error_no_udp_response, UDP_TIMEOUT_MS)
                    else -> error.message ?: error.javaClass.simpleName
                },
            )
        } finally {
            socket.close()
        }
    }

    private fun resolveAddress(
        host: String,
        ipVersion: PortScanIpVersion,
    ): InetAddress? {
        return InetAddress.getAllByName(host).firstOrNull { address ->
            when (ipVersion) {
                PortScanIpVersion.IPv4 -> address is Inet4Address
                PortScanIpVersion.IPv6 -> address is Inet6Address
            }
        }
    }

    private fun errorResult(
        host: String,
        port: Int,
        transport: PortScanTransport,
        ipVersion: PortScanIpVersion,
        message: String,
    ): PortScanResult {
        return PortScanResult(
            host = host,
            port = port,
            transport = transport,
            ipVersion = ipVersion,
            resolvedAddress = null,
            status = PortStatus.ERROR,
            latencyMs = null,
            error = message,
        )
    }
}

private const val TCP_TIMEOUT_MS = 2_500
private const val UDP_TIMEOUT_MS = 2_000

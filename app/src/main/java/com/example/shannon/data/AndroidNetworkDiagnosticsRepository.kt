package com.example.shannon.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Base64
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.example.shannon.R
import com.example.shannon.titleResId
import com.example.shannon.domain.analysis.SniMitmAnalysisEngine
import com.example.shannon.domain.model.ConnectivityStepResult
import com.example.shannon.domain.model.ConnectivityTestResult
import com.example.shannon.domain.model.ConnectivityTarget
import com.example.shannon.domain.model.ConnectivityTargetPreset
import com.example.shannon.domain.model.DnsAnalysisResult
import com.example.shannon.domain.model.DnsAnalysisStatus
import com.example.shannon.domain.model.DnsLookupTransport
import com.example.shannon.domain.model.DnsRecordResult
import com.example.shannon.domain.model.DnsServerResult
import com.example.shannon.domain.model.NetworkDiagnosticReport
import com.example.shannon.domain.model.NetworkOverview
import com.example.shannon.domain.model.PingResult
import com.example.shannon.domain.model.ProtocolAnalysisResult
import com.example.shannon.domain.model.ProtocolObservation
import com.example.shannon.domain.model.ProtocolProbeErrorCategory
import com.example.shannon.domain.model.ProtocolProbeKind
import com.example.shannon.domain.model.ProtocolProbeStatus
import com.example.shannon.domain.model.ProtocolTestResult
import com.example.shannon.domain.model.ReportFormat
import com.example.shannon.domain.model.SniAnalysisStatus
import com.example.shannon.domain.model.SniMitmAnalysisResult
import com.example.shannon.domain.model.SniObservation
import com.example.shannon.domain.model.SniProbeErrorCategory
import com.example.shannon.domain.model.SniProviderAnalysis
import com.example.shannon.domain.model.SniProviderProbeResult
import com.example.shannon.domain.model.SniVariantResult
import com.example.shannon.domain.model.SniVariantType
import com.example.shannon.domain.model.TlsAnalysisHeuristicStatus
import com.example.shannon.domain.model.TlsAnalysisResult
import com.example.shannon.domain.model.TlsCertificateInfo
import com.example.shannon.domain.model.TlsEndpointAnalysis
import com.example.shannon.domain.model.TlsEndpointStatus
import com.example.shannon.domain.model.TlsObservation
import com.example.shannon.domain.model.TlsVersionSupport
import com.example.shannon.domain.model.TracerouteHop
import com.example.shannon.domain.model.TracerouteResult
import com.example.shannon.domain.model.WebsiteAccessibilityResult
import com.example.shannon.domain.model.WebsiteAccessibilityStatus
import com.example.shannon.domain.model.WebsiteAccessibilityTarget
import com.example.shannon.domain.repository.NetworkDiagnosticsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Connection
import okhttp3.ConnectionPool
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.system.measureNanoTime
import org.json.JSONArray
import org.json.JSONObject

private data class ProtocolEndpoint(
    val label: String,
    val url: String,
)

private data class WifiSnapshot(
    val ssid: String,
    val bssid: String,
    val signalStrength: String,
)

private data class ResolvedHost(
    val address: InetAddress,
    val dnsTimeMs: Long,
)

private data class OkHttpProbeMetrics(
    var dnsTimeMs: Long? = null,
    var dnsResolvedIp: String? = null,
    var tcpTimeMs: Long? = null,
    var tcpResolvedIp: String? = null,
    var tlsTimeMs: Long? = null,
    var tlsProtocol: String? = null,
    var httpTimeMs: Long? = null,
    var httpStatusCode: Int? = null,
    var httpProtocol: String? = null,
    var failure: Throwable? = null,
    var dnsStarted: Boolean = false,
    var dnsFinished: Boolean = false,
    var connectStarted: Boolean = false,
    var connectFinished: Boolean = false,
    var secureConnectStarted: Boolean = false,
    var secureConnectFinished: Boolean = false,
) {
    fun resolvedIpOrNull(): String? = tcpResolvedIp ?: dnsResolvedIp
}

private data class OpenTlsProbe(
    val socket: SSLSocket,
    val resolvedIp: String,
    val dnsTimeMs: Long,
    val tcpTimeMs: Long,
    val tlsTimeMs: Long,
) : AutoCloseable {
    override fun close() {
        socket.close()
    }
}

private val http11ProtocolEndpoints = listOf(
    ProtocolEndpoint("Cloudflare", "https://www.cloudflare.com/cdn-cgi/trace"),
    ProtocolEndpoint("Google", "https://www.google.com/generate_204"),
)

private val http2ProtocolEndpoints = listOf(
    ProtocolEndpoint("Cloudflare", "https://www.cloudflare.com/cdn-cgi/trace"),
    ProtocolEndpoint("Google", "https://www.google.com/generate_204"),
    ProtocolEndpoint("GitHub", "https://github.com/"),
)

private val http3ProtocolEndpoints = listOf(
    ProtocolEndpoint("Cloudflare", "https://www.cloudflare.com/cdn-cgi/trace"),
    ProtocolEndpoint("Google", "https://www.google.com/generate_204"),
)

private val webSocketProtocolEndpoints = listOf(
    ProtocolEndpoint("Postman Echo", "wss://ws.postman-echo.com/raw"),
)

private val tlsAnalysisEndpoints = listOf(
    ProtocolEndpoint("Cloudflare", "https://www.cloudflare.com/cdn-cgi/trace"),
    ProtocolEndpoint("Google", "https://www.google.com/generate_204"),
    ProtocolEndpoint("GitHub", "https://github.com/"),
    ProtocolEndpoint("Fastly", "https://www.fastly.com/"),
)

private val sniAnalysisEndpoints = listOf(
    ProtocolEndpoint("Cloudflare", "https://www.cloudflare.com/cdn-cgi/trace"),
    ProtocolEndpoint("Google", "https://www.google.com/generate_204"),
    ProtocolEndpoint("GitHub", "https://github.com/"),
    ProtocolEndpoint("Fastly", "https://www.fastly.com/"),
    ProtocolEndpoint("Wikipedia", "https://www.wikipedia.org/"),
)

private val latencyRegex = Regex("""time[=<]([0-9.]+)""")
private val tracerouteIpRegex = Regex("""(?i)\bfrom ([0-9a-f:.]+)""")

private class OkHttpConnectivityEventListener(
    private val metrics: OkHttpProbeMetrics,
) : EventListener() {
    private var dnsStartedAtNs: Long? = null
    private var connectStartedAtNs: Long? = null
    private var secureConnectStartedAtNs: Long? = null
    private var requestHeadersStartedAtNs: Long? = null

    override fun dnsStart(
        call: Call,
        domainName: String,
    ) {
        metrics.dnsStarted = true
        dnsStartedAtNs = System.nanoTime()
    }

    override fun dnsEnd(
        call: Call,
        domainName: String,
        inetAddressList: List<InetAddress>,
    ) {
        metrics.dnsFinished = true
        metrics.dnsTimeMs = elapsedMillis(dnsStartedAtNs)
        metrics.dnsResolvedIp = inetAddressList.firstOrNull()?.hostAddress
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: java.net.Proxy,
    ) {
        metrics.connectStarted = true
        connectStartedAtNs = System.nanoTime()
        metrics.tcpResolvedIp = inetSocketAddress.address?.hostAddress ?: inetSocketAddress.hostString
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: java.net.Proxy,
        protocol: Protocol?,
    ) {
        metrics.connectFinished = true
        metrics.tcpTimeMs = elapsedMillis(connectStartedAtNs)
        metrics.tcpResolvedIp = inetSocketAddress.address?.hostAddress ?: inetSocketAddress.hostString
        metrics.httpProtocol = protocol?.toString() ?: metrics.httpProtocol
    }

    override fun secureConnectStart(call: Call) {
        metrics.secureConnectStarted = true
        secureConnectStartedAtNs = System.nanoTime()
    }

    override fun secureConnectEnd(
        call: Call,
        handshake: Handshake?,
    ) {
        metrics.secureConnectFinished = true
        metrics.tlsTimeMs = elapsedMillis(secureConnectStartedAtNs)
        metrics.tlsProtocol = handshake?.tlsVersion?.javaName
    }

    override fun requestHeadersStart(call: Call) {
        requestHeadersStartedAtNs = System.nanoTime()
    }

    override fun responseHeadersEnd(
        call: Call,
        response: Response,
    ) {
        metrics.httpTimeMs = elapsedMillis(requestHeadersStartedAtNs)
        metrics.httpStatusCode = response.code
        metrics.httpProtocol = response.protocol.toString()
        metrics.tlsProtocol = metrics.tlsProtocol ?: response.handshake?.tlsVersion?.javaName
    }

    override fun callFailed(
        call: Call,
        ioe: IOException,
    ) {
        metrics.failure = ioe
        if (metrics.connectStarted && !metrics.connectFinished) {
            metrics.tcpTimeMs = elapsedMillis(connectStartedAtNs)
        }
        if (metrics.secureConnectStarted && !metrics.secureConnectFinished) {
            metrics.tlsTimeMs = elapsedMillis(secureConnectStartedAtNs)
        }
        if (requestHeadersStartedAtNs != null && metrics.httpTimeMs == null) {
            metrics.httpTimeMs = elapsedMillis(requestHeadersStartedAtNs)
        }
    }

    private fun elapsedMillis(startedAtNs: Long?): Long? {
        return startedAtNs?.let { (System.nanoTime() - it).toMillis() }
    }
}

class AndroidNetworkDiagnosticsRepository(
    private val context: Context,
) : NetworkDiagnosticsRepository {
    private val sniMitmAnalysisEngine = SniMitmAnalysisEngine { resId, args ->
        context.getString(resId, *args.toTypedArray())
    }
    private val http3Probe by lazy { CronetHttp3Probe(context) }
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .build()
    }

    private val systemTrustManager: X509TrustManager by lazy {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as java.security.KeyStore?)
        trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()
    }

    private val permissiveSslSocketFactory: SSLSocketFactory by lazy {
        val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        }.socketFactory
    }

    override suspend fun readNetworkOverview(): NetworkOverview = withContext(Dispatchers.IO) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        val linkProperties = network?.let(connectivityManager::getLinkProperties)
        val wifiSnapshot = readWifiSnapshot(capabilities)

        NetworkOverview(
            networkType = capabilities.toNetworkType(),
            internetReachable = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            metered = connectivityManager.isActiveNetworkMetered,
            downstreamMbps = capabilities?.linkDownstreamBandwidthKbps.toMbps(),
            upstreamMbps = capabilities?.linkUpstreamBandwidthKbps.toMbps(),
            ssid = wifiSnapshot.ssid,
            bssid = wifiSnapshot.bssid,
            signalStrength = wifiSnapshot.signalStrength,
            privateIpAddress = linkProperties.findAddress(isIpv4 = true),
            gatewayAddress = linkProperties.findGatewayAddress(),
            ipv6Address = linkProperties.findAddress(isIpv4 = false),
            dnsServers = linkProperties?.dnsServers
                ?.map { it.hostAddress ?: it.toString() }
                .orEmpty()
                .ifEmpty { listOf("n/a") },
            carrierName = readCarrierName(capabilities),
        )
    }

    override suspend fun performConnectivityTest(targetPreset: ConnectivityTargetPreset): ConnectivityTestResult =
        withContext(Dispatchers.IO) {
            var lastResult: ConnectivityTestResult? = null

            targetPreset.endpoints.forEachIndexed { index, target ->
                val fallbackReason = lastResult?.steps
                    ?.lastOrNull { !it.success }
                ?.let {
                    context.getString(
                        R.string.connectivity_fallback_reason,
                        it.stage,
                        targetPreset.endpoints[index - 1].label,
                    )
                }

                val result = runSingleEndpointTest(
                    target = target,
                    fallbackUsed = index > 0,
                    fallbackReason = fallbackReason,
                )
                val httpStep = result.steps.lastOrNull { it.stage == "HTTP" }
                if (httpStep?.success == true) {
                    return@withContext result
                }
                lastResult = result
            }

            lastResult ?: runSingleEndpointTest(
                target = targetPreset.endpoints.first(),
                fallbackUsed = false,
                fallbackReason = null,
            )
        }

    override suspend fun performWebsiteAccessibilityTest(
        targets: List<WebsiteAccessibilityTarget>,
    ): List<WebsiteAccessibilityResult> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                targets.map { target ->
                    async {
                        val diagnostics = runSingleEndpointTest(
                            target = ConnectivityTarget(
                                label = target.name,
                                url = target.url,
                            ),
                            fallbackUsed = false,
                            fallbackReason = null,
                        )
                        WebsiteAccessibilityResult(
                            serviceName = target.name,
                            targetUrl = target.url,
                            status = diagnostics.toWebsiteAccessibilityStatus(),
                            diagnostics = diagnostics,
                        )
                    }
                }.awaitAll()
            }
        }

    override suspend fun performDnsAnalysis(domain: String): DnsAnalysisResult = withContext(Dispatchers.IO) {
        val normalizedDomain = domain.trim().ifBlank { "example.com" }
        val results = coroutineScope {
            listOf(
                async { resolveWithSystemDns(normalizedDomain) },
                async { resolveWithDnsServer(normalizedDomain, "Google DNS", "8.8.8.8") },
                async { resolveWithDnsServer(normalizedDomain, "Cloudflare DNS", "1.1.1.1") },
                async { resolveWithDnsServer(normalizedDomain, "Quad9", "9.9.9.9") },
            ).awaitAll()
        }

        val systemServer = results.firstOrNull()
        val publicServers = results.drop(1)

        val systemAddressesByType = systemServer
            ?.records
            ?.associate { it.recordType to it.addresses.toSet() }
            .orEmpty()

        val publicAddressesByType = buildMap<String, Set<String>> {
            listOf("A", "AAAA").forEach { recordType ->
                put(
                    recordType,
                    publicServers
                        .flatMap { server -> server.records.filter { it.recordType == recordType } }
                        .flatMap { it.addresses }
                        .toSet(),
                )
            }
        }

        val anySharedAddress = systemAddressesByType.any { (recordType, systemSet) ->
            systemSet.intersect(publicAddressesByType[recordType].orEmpty()).isNotEmpty()
        }

        val anyPrivateOrReserved = results.any { server ->
            server.records.any { record ->
                record.addresses.any(::isPrivateOrReservedIp)
            }
        }

        val systemNoValidAnswers = systemServer
            ?.records
            ?.all { it.addresses.isEmpty() } == true
        val systemLikelyNxdomain = systemServer
            ?.records
            ?.all { it.responseCode == 3 || (it.error?.contains("NXDOMAIN", ignoreCase = true) == true) } == true
        val publicHasValidAnswers = publicServers.any { server ->
            server.records.any { it.addresses.isNotEmpty() }
        }
        val publicConsistentAmongThemselves = listOf("A", "AAAA").any { recordType ->
            val publicSets = publicServers
                .mapNotNull { server ->
                    server.records.firstOrNull { it.recordType == recordType }?.addresses?.toSet()
                }
                .filter { it.isNotEmpty() }
            publicSets.size > 1 && publicSets.reduce { acc, next -> acc.intersect(next) }.isNotEmpty()
        }

        val status = when {
            anyPrivateOrReserved -> DnsAnalysisStatus.Blocked
            systemLikelyNxdomain && publicHasValidAnswers -> DnsAnalysisStatus.Blocked
            systemNoValidAnswers && publicHasValidAnswers -> DnsAnalysisStatus.Blocked
            anySharedAddress -> DnsAnalysisStatus.Ok
            publicConsistentAmongThemselves -> DnsAnalysisStatus.Suspicious
            else -> DnsAnalysisStatus.CdnVariation
        }

        val summary = when {
            anyPrivateOrReserved -> context.getString(R.string.dns_summary_private_reserved)
            systemLikelyNxdomain && publicHasValidAnswers ->
                context.getString(R.string.dns_summary_nxdomain_public_answers)
            systemNoValidAnswers && publicHasValidAnswers ->
                context.getString(R.string.dns_summary_no_answers_public_succeeded)
            anySharedAddress ->
                context.getString(R.string.dns_summary_shared_ip)
            publicConsistentAmongThemselves ->
                context.getString(R.string.dns_summary_no_shared_ips)
            else ->
                context.getString(R.string.dns_summary_cdn_variation)
        }

        DnsAnalysisResult(
            domain = normalizedDomain,
            servers = results,
            status = status,
            summary = summary,
            possibleDnsBlocking = status == DnsAnalysisStatus.Blocked,
            possibleDnsPoisoning = status == DnsAnalysisStatus.Suspicious,
            checkedAt = nowTimestamp(),
        )
    }

    override suspend fun performProtocolAnalysis(): ProtocolAnalysisResult = withContext(Dispatchers.IO) {
        val tests = coroutineScope {
            val http11Tests = http11ProtocolEndpoints.map { endpoint ->
                async { runHttp11Probe(endpoint) }
            }
            val http2Tests = http2ProtocolEndpoints.map { endpoint ->
                async { runHttp2Probe(endpoint) }
            }
            val http3Tests = http3ProtocolEndpoints.map { endpoint ->
                async { runHttp3Probe(endpoint) }
            }
            val webSocketTests = webSocketProtocolEndpoints.map { endpoint ->
                async { runWebSocketProbe(endpoint) }
            }

            (http11Tests + http2Tests + http3Tests + webSocketTests).awaitAll()
        }

        ProtocolAnalysisResult(
            tests = tests,
            observations = buildProtocolObservations(tests),
            checkedAt = nowTimestamp(),
        )
    }

    override suspend fun performTlsAnalysis(): TlsAnalysisResult = withContext(Dispatchers.IO) {
        val endpoints = coroutineScope {
            tlsAnalysisEndpoints.map { endpoint ->
                async { runTlsAnalysisProbe(endpoint) }
            }.awaitAll()
        }
        val observations = buildTlsObservations(endpoints)
        val status = when {
            endpoints.all { it.status == TlsEndpointStatus.Failed } -> TlsAnalysisHeuristicStatus.Inconclusive
            observations.any { it.code == "TLS_INTERCEPTION_SUSPECTED" } ->
                TlsAnalysisHeuristicStatus.TlsInterceptionSuspected
            observations.any { it.code == "TLS_DOWNGRADE_SUSPECTED" } ->
                TlsAnalysisHeuristicStatus.TlsDowngradeSuspected
            observations.any { it.code == "UNUSUAL_CERTIFICATE_CHAIN" } ->
                TlsAnalysisHeuristicStatus.UnusualCertificateChain
            else -> TlsAnalysisHeuristicStatus.NoTlsAnomalies
        }

        TlsAnalysisResult(
            endpoints = endpoints,
            status = status,
            observations = observations,
            checkedAt = nowTimestamp(),
        )
    }

    override suspend fun performSniMitmAnalysis(): SniMitmAnalysisResult = withContext(Dispatchers.IO) {
        val probeResults = coroutineScope {
            sniAnalysisEndpoints.map { endpoint ->
                async { runSniProviderAnalysis(endpoint) }
            }.awaitAll()
        }
        sniMitmAnalysisEngine.analyze(
            probeResults = probeResults,
            checkedAt = nowTimestamp(),
        )
    }

    override suspend fun runPing(host: String, count: Int): PingResult = withContext(Dispatchers.IO) {
        val normalizedHost = host.trim().ifBlank { "1.1.1.1" }
        runCatching {
            runSystemPing(normalizedHost, count)
        }.recoverCatching {
            runTcpPing(normalizedHost, count)
        }.getOrThrow()
    }

    override suspend fun runTraceroute(host: String): TracerouteResult = withContext(Dispatchers.IO) {
        val normalizedHost = host.trim().ifBlank { "google.com" }
        runPingTraceroute(normalizedHost, maxHops = 12)
    }

    override suspend fun exportReport(
        report: NetworkDiagnosticReport,
        format: ReportFormat,
    ): String = withContext(Dispatchers.IO) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "shannon_report_${report.timestamp}.${format.extension}")
        file.writeText(formatReport(report, format))
        file.absolutePath
    }

    private fun runSingleEndpointTest(
        target: ConnectivityTarget,
        fallbackUsed: Boolean,
        fallbackReason: String?,
    ): ConnectivityTestResult {
        val metrics = OkHttpProbeMetrics()
        val steps = mutableListOf<ConnectivityStepResult>()
        runCatching {
            executeOkHttpConnectivityProbe(target.url, metrics).use { response ->
                metrics.httpStatusCode = response.code
                metrics.httpProtocol = response.protocol.toString()
                metrics.tlsProtocol = metrics.tlsProtocol ?: response.handshake?.tlsVersion?.javaName
            }
        }.onFailure { metrics.failure = it }

        val dnsStep = buildDnsConnectivityStep(metrics)
        steps += dnsStep
        if (!dnsStep.success) {
            return connectivityResult(
                target = target,
                steps = steps,
                fallbackUsed = fallbackUsed,
                fallbackReason = fallbackReason,
            )
        }

        val tcpStep = buildTcpConnectivityStep(metrics)
        steps += tcpStep
        if (!tcpStep.success) {
            return connectivityResult(
                target = target,
                steps = steps,
                fallbackUsed = fallbackUsed,
                fallbackReason = fallbackReason,
            )
        }

        val tlsStep = buildTlsConnectivityStep(metrics)
        steps += tlsStep
        if (!tlsStep.success) {
            return connectivityResult(
                target = target,
                steps = steps,
                fallbackUsed = fallbackUsed,
                fallbackReason = fallbackReason,
            )
        }

        steps += buildHttpConnectivityStep(metrics)
        return connectivityResult(
            target = target,
            steps = steps,
            fallbackUsed = fallbackUsed,
            fallbackReason = fallbackReason,
        )
    }

    private fun executeOkHttpConnectivityProbe(
        targetUrl: String,
        metrics: OkHttpProbeMetrics,
    ): Response {
        val listener = OkHttpConnectivityEventListener(metrics)
        val client = okHttpClient.newBuilder()
            .eventListener(listener)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.MILLISECONDS))
            .build()
        val request = Request.Builder()
            .url(targetUrl)
            .get()
            .header("Connection", "close")
            .build()
        return client.newCall(request).execute()
    }

    private fun buildDnsConnectivityStep(metrics: OkHttpProbeMetrics): ConnectivityStepResult {
        val message = metrics.failure?.message ?: context.getString(R.string.connectivity_dns_failed)
        return if (metrics.dnsFinished && metrics.dnsTimeMs != null) {
            ConnectivityStepResult(
                stage = context.getString(R.string.stage_dns),
                success = true,
                summary = context.getString(
                    R.string.connectivity_dns_success,
                    metrics.dnsResolvedIp ?: context.getString(R.string.connectivity_dns_resolved),
                    metrics.dnsTimeMs ?: 0,
                ),
            )
        } else {
            ConnectivityStepResult(
                stage = context.getString(R.string.stage_dns),
                success = false,
                summary = message,
            )
        }
    }

    private fun buildTcpConnectivityStep(metrics: OkHttpProbeMetrics): ConnectivityStepResult {
        val message = metrics.failure?.message ?: context.getString(R.string.connectivity_tcp_failed)
        return if (metrics.connectFinished && metrics.tcpTimeMs != null) {
            ConnectivityStepResult(
                stage = context.getString(R.string.stage_tcp),
                success = true,
                summary = context.getString(
                    R.string.connectivity_tcp_success,
                    metrics.resolvedIpOrNull() ?: context.getString(R.string.connectivity_dns_resolved),
                    metrics.tcpTimeMs ?: 0,
                ),
            )
        } else {
            ConnectivityStepResult(
                stage = context.getString(R.string.stage_tcp),
                success = false,
                summary = message,
            )
        }
    }

    private fun buildTlsConnectivityStep(metrics: OkHttpProbeMetrics): ConnectivityStepResult {
        val message = metrics.failure?.message ?: context.getString(R.string.connectivity_tls_failed)
        return if (metrics.secureConnectFinished) {
            ConnectivityStepResult(
                stage = context.getString(R.string.stage_tls),
                success = true,
                summary = context.getString(
                    R.string.connectivity_tls_success,
                    metrics.tlsProtocol ?: context.getString(R.string.stage_tls),
                    metrics.tlsTimeMs ?: 0,
                ),
            )
        } else {
            ConnectivityStepResult(
                stage = context.getString(R.string.stage_tls),
                success = false,
                summary = message,
            )
        }
    }

    private fun buildHttpConnectivityStep(metrics: OkHttpProbeMetrics): ConnectivityStepResult {
        val responseCode = metrics.httpStatusCode
        return if (responseCode != null) {
            ConnectivityStepResult(
                stage = "HTTP",
                success = responseCode in 200..399,
                summary = context.getString(
                    R.string.connectivity_http_success,
                    responseCode,
                    metrics.httpTimeMs ?: 0,
                ),
            )
        } else {
            ConnectivityStepResult(
                stage = "HTTP",
                success = false,
                summary = metrics.failure?.message ?: context.getString(R.string.connectivity_http_failed),
            )
        }
    }

    private fun connectivityResult(
        target: ConnectivityTarget,
        steps: List<ConnectivityStepResult>,
        fallbackUsed: Boolean,
        fallbackReason: String?,
    ): ConnectivityTestResult {
        return ConnectivityTestResult(
            steps = steps,
            checkedAt = nowTimestamp(),
            endpointLabel = target.label,
            endpointUrl = target.url,
            fallbackUsed = fallbackUsed,
            fallbackReason = fallbackReason,
        )
    }

    private fun runSystemPing(
        host: String,
        count: Int,
    ): PingResult {
        val output = runCommandCandidates(
            listOf("/system/bin/ping", "-c", count.toString(), "-W", "2", host),
            listOf("ping", "-c", count.toString(), "-W", "2", host),
        )
        val latencies = latencyRegex.findAll(output)
            .mapNotNull { it.groupValues.getOrNull(1)?.toDoubleOrNull() }
            .toList()
        return buildPingResult(
            host = host,
            packetsSent = count,
            latencies = latencies,
        )
    }

    private fun runTcpPing(
        host: String,
        count: Int,
    ): PingResult {
        val latencies = mutableListOf<Double>()
        repeat(count) {
            val latency = runCatching {
                measureNanoTime {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, 443), 2_000)
                    }
                }.toMillis().toDouble()
            }.getOrNull()
            if (latency != null) {
                latencies += latency
            }
            Thread.sleep(150)
        }
        return buildPingResult(
            host = host,
            packetsSent = count,
            latencies = latencies,
        )
    }

    private fun buildPingResult(
        host: String,
        packetsSent: Int,
        latencies: List<Double>,
    ): PingResult {
        return PingResult.fromLatencies(
            host = host,
            packetsSent = packetsSent,
            latencies = latencies,
        )
    }

    private fun runPingTraceroute(
        host: String,
        maxHops: Int,
    ): TracerouteResult {
        val destinationAddress = InetAddress.getByName(host).hostAddress ?: host
        val hops = mutableListOf<TracerouteHop>()
        for (ttl in 1..maxHops) {
            val output = runCommandCandidates(
                listOf("/system/bin/ping", "-c", "1", "-W", "1", "-t", ttl.toString(), host),
                listOf("ping", "-c", "1", "-W", "1", "-t", ttl.toString(), host),
            )
            val ip = extractTracerouteIp(output)
            val latency = latencyRegex.find(output)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val timeout = ip == null
            val hostname = ip?.let(::reverseLookup)
            hops += TracerouteHop(
                hopNumber = ttl,
                ipAddress = ip,
                hostname = hostname,
                latencyMs = latency,
                timeout = timeout,
            )
            val reachedDestination = ip != null &&
                (ip == destinationAddress || !output.contains("Time to live exceeded", ignoreCase = true))
            if (reachedDestination) {
                break
            }
        }
        return TracerouteResult(
            destination = host,
            hops = hops,
        )
    }

    private fun runCommandCandidates(vararg commands: List<String>): String {
        var lastError: Throwable? = null
        commands.forEach { command ->
            runCatching {
                return runCommand(command)
            }.onFailure { error ->
                lastError = error
            }
        }
        throw IllegalStateException(lastError?.message ?: "No supported command variant was available")
    }

    private fun runCommand(command: List<String>): String {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw SocketTimeoutException("Command timed out: ${command.firstOrNull()}")
        }
        return process.inputStream.bufferedReader().use { it.readText() }
    }

    private fun extractTracerouteIp(output: String): String? {
        return tracerouteIpRegex.findAll(output)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .firstOrNull()
    }

    private fun reverseLookup(ip: String): String? {
        return runCatching {
            InetAddress.getByName(ip).canonicalHostName
                .takeUnless { it == ip || it.isBlank() }
        }.getOrNull()
    }

    private fun formatReport(
        report: NetworkDiagnosticReport,
        format: ReportFormat,
    ): String {
        return when (format) {
            ReportFormat.Json -> buildJsonReport(report).toString(2)
            ReportFormat.Markdown -> buildMarkdownReport(report)
            ReportFormat.Text -> buildTextReport(report)
        }
    }

    private fun buildJsonReport(report: NetworkDiagnosticReport): JSONObject {
        return JSONObject().apply {
            put("timestamp", report.timestamp)
            report.overview?.let { overview ->
                put(
                    "overview",
                    JSONObject().apply {
                        put("networkType", overview.networkType)
                        put("internetReachable", overview.internetReachable)
                        put("validated", overview.validated)
                        put("metered", overview.metered)
                        put("ssid", overview.ssid)
                        put("bssid", overview.bssid)
                        put("signalStrength", overview.signalStrength)
                        put("privateIpAddress", overview.privateIpAddress)
                        put("gatewayAddress", overview.gatewayAddress)
                        put("ipv6Address", overview.ipv6Address)
                        put("dnsServers", JSONArray(overview.dnsServers))
                        put("carrierName", overview.carrierName)
                    }
                )
            }
            report.connectivityTest?.let { connectivity ->
                put(
                    "connectivityTest",
                    JSONObject().apply {
                        put("endpointLabel", connectivity.endpointLabel)
                        put("endpointUrl", connectivity.endpointUrl)
                        put("checkedAt", connectivity.checkedAt)
                        put(
                            "steps",
                            JSONArray().apply {
                                connectivity.steps.forEach { step ->
                                    put(
                                        JSONObject().apply {
                                            put("stage", step.stage)
                                            put("success", step.success)
                                            put("summary", step.summary)
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
            report.dnsAnalysis?.let { dns ->
                put(
                    "dnsAnalysis",
                    JSONObject().apply {
                        put("domain", dns.domain)
                    put("status", context.getString(dns.status.titleResId()))
                        put("summary", dns.summary)
                    }
                )
            }
            report.protocolAnalysis?.let { protocol ->
                put(
                    "protocolAnalysis",
                    JSONObject().apply {
                        put(
                            "observations",
                            JSONArray(protocol.observations.map { it.title })
                        )
                    }
                )
            }
            report.tlsAnalysis?.let { tls ->
                put("tlsAnalysis", JSONObject().apply {
                    put("status", context.getString(tls.status.titleResId()))
                    put("observations", JSONArray(tls.observations.map { it.title }))
                })
            }
            report.sniMitmAnalysis?.let { sni ->
                put("sniMitmAnalysis", JSONObject().apply {
                    put("status", context.getString(sni.status.titleResId()))
                    put("observations", JSONArray(sni.observations.map { it.title }))
                })
            }
            if (report.websiteAccessibilityResults.isNotEmpty()) {
                put(
                    "websiteAccessibility",
                    JSONArray().apply {
                        report.websiteAccessibilityResults.forEach { result ->
                            put(
                                JSONObject().apply {
                                    put("service", result.serviceName)
                    put("status", context.getString(result.status.titleResId()))
                                    put("url", result.targetUrl)
                                }
                            )
                        }
                    }
                )
            }
            report.pingResult?.let { ping ->
                put(
                    "ping",
                    JSONObject().apply {
                        put("host", ping.host)
                        put("packetsSent", ping.packetsSent)
                        put("packetsReceived", ping.packetsReceived)
                        put("packetLoss", ping.packetLoss)
                        put("minLatencyMs", ping.minLatencyMs)
                        put("avgLatencyMs", ping.avgLatencyMs)
                        put("maxLatencyMs", ping.maxLatencyMs)
                        put("jitterMs", ping.jitterMs)
                    }
                )
            }
            report.tracerouteResult?.let { traceroute ->
                put(
                    "traceroute",
                    JSONObject().apply {
                        put("destination", traceroute.destination)
                        put(
                            "hops",
                            JSONArray().apply {
                                traceroute.hops.forEach { hop ->
                                    put(
                                        JSONObject().apply {
                                            put("hopNumber", hop.hopNumber)
                                            put("ipAddress", hop.ipAddress)
                                            put("hostname", hop.hostname)
                                            put("latencyMs", hop.latencyMs)
                                            put("timeout", hop.timeout)
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    private fun buildMarkdownReport(report: NetworkDiagnosticReport): String {
        return buildString {
            appendLine("# Shannon Diagnostic Report")
            appendLine()
            appendLine("Generated at: ${report.timestamp}")
            report.overview?.let {
                appendLine()
                appendLine("## Network overview")
                appendLine("- Network type: ${it.networkType}")
                appendLine("- SSID: ${it.ssid}")
                appendLine("- BSSID: ${it.bssid}")
                appendLine("- Signal strength: ${it.signalStrength}")
                appendLine("- Private IP: ${it.privateIpAddress}")
                appendLine("- Gateway: ${it.gatewayAddress}")
                appendLine("- IPv6: ${it.ipv6Address}")
                appendLine("- DNS: ${it.dnsServers.joinToString()}")
                appendLine("- Carrier: ${it.carrierName}")
            }
            report.pingResult?.let {
                appendLine()
                appendLine("## Ping")
                appendLine("- Host: ${it.host}")
                appendLine("- Packets: ${it.packetsReceived}/${it.packetsSent}")
                appendLine("- Packet loss: ${it.packetLoss}%")
                appendLine("- Min / Avg / Max: ${it.minLatencyMs} / ${it.avgLatencyMs} / ${it.maxLatencyMs} ms")
                appendLine("- Jitter: ${it.jitterMs} ms")
            }
            report.tracerouteResult?.let {
                appendLine()
                appendLine("## Traceroute")
                appendLine("- Destination: ${it.destination}")
                it.hops.forEach { hop ->
                    appendLine("- Hop ${hop.hopNumber}: ${hop.hostname ?: "-"} ${hop.ipAddress ?: "*"} ${hop.latencyMs?.let { latency -> "${latency} ms" } ?: "timeout"}")
                }
            }
            report.connectivityTest?.let {
                appendLine()
                appendLine("## Connectivity test")
                appendLine("- Endpoint: ${it.endpointLabel}")
                it.steps.forEach { step ->
                    appendLine("- ${step.stage}: ${step.summary}")
                }
            }
            report.dnsAnalysis?.let {
                appendLine()
                appendLine("## DNS analysis")
                appendLine("- Domain: ${it.domain}")
            appendLine("- Status: ${context.getString(it.status.titleResId())}")
                appendLine("- Summary: ${it.summary}")
            }
            report.protocolAnalysis?.let {
                appendLine()
                appendLine("## Protocol analysis")
                it.observations.forEach { observation ->
                    appendLine("- ${observation.title}: ${observation.summary}")
                }
            }
            report.tlsAnalysis?.let {
                appendLine()
                appendLine("## TLS analysis")
            appendLine("- Status: ${context.getString(it.status.titleResId())}")
                it.observations.forEach { observation ->
                    appendLine("- ${observation.title}: ${observation.summary}")
                }
            }
            report.sniMitmAnalysis?.let {
                appendLine()
                appendLine("## SNI filtering and MITM")
            appendLine("- Status: ${context.getString(it.status.titleResId())}")
                it.observations.forEach { observation ->
                    appendLine("- ${observation.title}: ${observation.summary}")
                }
            }
            if (report.websiteAccessibilityResults.isNotEmpty()) {
                appendLine()
                appendLine("## Website accessibility")
                report.websiteAccessibilityResults.forEach { result ->
            appendLine("- ${result.serviceName}: ${context.getString(result.status.titleResId())}")
                }
            }
        }
    }

    private fun buildTextReport(report: NetworkDiagnosticReport): String {
        return buildString {
            appendLine("Shannon Diagnostic Report")
            appendLine("Timestamp: ${report.timestamp}")
            report.overview?.let {
                appendLine()
                appendLine("Network overview")
                appendLine("Network type: ${it.networkType}")
                appendLine("SSID: ${it.ssid}")
                appendLine("BSSID: ${it.bssid}")
                appendLine("Signal strength: ${it.signalStrength}")
                appendLine("Private IP: ${it.privateIpAddress}")
                appendLine("Gateway: ${it.gatewayAddress}")
                appendLine("IPv6: ${it.ipv6Address}")
                appendLine("DNS: ${it.dnsServers.joinToString()}")
                appendLine("Carrier: ${it.carrierName}")
            }
            report.pingResult?.let {
                appendLine()
                appendLine("Ping")
                appendLine("Host: ${it.host}")
                appendLine("Packets: ${it.packetsReceived}/${it.packetsSent}")
                appendLine("Packet loss: ${it.packetLoss}%")
                appendLine("Min/Avg/Max: ${it.minLatencyMs}/${it.avgLatencyMs}/${it.maxLatencyMs} ms")
                appendLine("Jitter: ${it.jitterMs} ms")
            }
            report.tracerouteResult?.let {
                appendLine()
                appendLine("Traceroute to ${it.destination}")
                it.hops.forEach { hop ->
                    appendLine("Hop ${hop.hopNumber}: ${hop.hostname ?: "-"} ${hop.ipAddress ?: "*"} ${hop.latencyMs?.let { latency -> "${latency} ms" } ?: "timeout"}")
                }
            }
            report.dnsAnalysis?.let {
                appendLine()
            appendLine("DNS status: ${context.getString(it.status.titleResId())}")
                appendLine(it.summary)
            }
            report.tlsAnalysis?.let {
                appendLine()
            appendLine("TLS status: ${context.getString(it.status.titleResId())}")
            }
            report.sniMitmAnalysis?.let {
                appendLine()
            appendLine("SNI status: ${context.getString(it.status.titleResId())}")
            }
        }
    }

    private fun runHttp11Probe(endpoint: ProtocolEndpoint): ProtocolTestResult {
        val url = URL(endpoint.url)
        return runCatching {
            openTlsProbe(
                host = url.host,
                port = effectivePort(url.port, 443),
                alpn = listOf("http/1.1"),
            ).use { probe ->
                val requestStart = System.nanoTime()
                val request = buildString {
                    append("HEAD ${buildRequestPath(url)} HTTP/1.1\r\n")
                    append("Host: ${url.host}\r\n")
                    append("Connection: close\r\n")
                    append("User-Agent: Shannon/1.0\r\n")
                    append("Accept: */*\r\n")
                    append("\r\n")
                }
                probe.socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                probe.socket.getOutputStream().flush()

                val reader = probe.socket.inputStream.bufferedReader(Charsets.ISO_8859_1)
                val statusLine = reader.readLine() ?: throw IllegalStateException("No HTTP status line received")
                drainHeaders(reader)
                val httpTimeMs = (System.nanoTime() - requestStart).toMillis()
                val httpStatus = parseHttpStatusCode(statusLine)
                val negotiated = probe.socket.applicationProtocol.takeIf { it.isNotBlank() } ?: "http/1.1"

                ProtocolTestResult(
                    protocol = ProtocolProbeKind.Http11,
                    endpointLabel = endpoint.label,
                    endpointUrl = endpoint.url,
                    status = ProtocolProbeStatus.Supported,
                    summary = httpStatus?.let { "Received HTTP $it over HTTP/1.1." }
                        ?: "Received a valid HTTP/1.1 response.",
                    negotiatedProtocol = negotiated,
                    dnsTimeMs = probe.dnsTimeMs,
                    tcpTimeMs = probe.tcpTimeMs,
                    tlsTimeMs = probe.tlsTimeMs,
                    totalTimeMs = probe.dnsTimeMs + probe.tcpTimeMs + probe.tlsTimeMs + httpTimeMs,
                    httpStatusCode = httpStatus,
                    ipAddress = probe.resolvedIp,
                    errorMessage = httpStatus?.takeIf { it >= 400 }?.let {
                        "Endpoint returned HTTP $it, but the HTTP/1.1 exchange completed successfully."
                    },
                )
            }
        }.getOrElse { error ->
            protocolFailureResult(
                protocol = ProtocolProbeKind.Http11,
                endpoint = endpoint,
                error = error,
                defaultCategory = ProtocolProbeErrorCategory.HttpFailure,
            )
        }
    }

    private fun runHttp2Probe(endpoint: ProtocolEndpoint): ProtocolTestResult {
        val url = URL(endpoint.url)
        return runCatching {
            openTlsProbe(
                host = url.host,
                port = effectivePort(url.port, 443),
                alpn = listOf("h2", "http/1.1"),
            ).use { probe ->
                val negotiated = probe.socket.applicationProtocol.takeIf { it.isNotBlank() }
                when (negotiated) {
                    "h2" -> ProtocolTestResult(
                        protocol = ProtocolProbeKind.Http2,
                        endpointLabel = endpoint.label,
                        endpointUrl = endpoint.url,
                        status = ProtocolProbeStatus.Supported,
                        summary = "ALPN negotiated HTTP/2 successfully.",
                        negotiatedProtocol = negotiated,
                        dnsTimeMs = probe.dnsTimeMs,
                        tcpTimeMs = probe.tcpTimeMs,
                        tlsTimeMs = probe.tlsTimeMs,
                        totalTimeMs = probe.dnsTimeMs + probe.tcpTimeMs + probe.tlsTimeMs,
                        ipAddress = probe.resolvedIp,
                    )
                    "http/1.1" -> ProtocolTestResult(
                        protocol = ProtocolProbeKind.Http2,
                        endpointLabel = endpoint.label,
                        endpointUrl = endpoint.url,
                        status = ProtocolProbeStatus.Fallback,
                        summary = "ALPN negotiated HTTP/1.1 instead of h2.",
                        negotiatedProtocol = negotiated,
                        dnsTimeMs = probe.dnsTimeMs,
                        tcpTimeMs = probe.tcpTimeMs,
                        tlsTimeMs = probe.tlsTimeMs,
                        totalTimeMs = probe.dnsTimeMs + probe.tcpTimeMs + probe.tlsTimeMs,
                        ipAddress = probe.resolvedIp,
                        errorCategory = ProtocolProbeErrorCategory.AlpnFailure,
                        errorMessage = "The endpoint completed TLS but did not negotiate HTTP/2.",
                    )
                    else -> ProtocolTestResult(
                        protocol = ProtocolProbeKind.Http2,
                        endpointLabel = endpoint.label,
                        endpointUrl = endpoint.url,
                        status = ProtocolProbeStatus.Inconclusive,
                        summary = "TLS succeeded, but the ALPN result was unavailable.",
                        dnsTimeMs = probe.dnsTimeMs,
                        tcpTimeMs = probe.tcpTimeMs,
                        tlsTimeMs = probe.tlsTimeMs,
                        totalTimeMs = probe.dnsTimeMs + probe.tcpTimeMs + probe.tlsTimeMs,
                        ipAddress = probe.resolvedIp,
                        errorCategory = ProtocolProbeErrorCategory.AlpnFailure,
                    )
                }
            }
        }.getOrElse { error ->
            protocolFailureResult(
                protocol = ProtocolProbeKind.Http2,
                endpoint = endpoint,
                error = error,
                defaultCategory = ProtocolProbeErrorCategory.AlpnFailure,
            )
        }
    }

    private fun runHttp3Probe(endpoint: ProtocolEndpoint): ProtocolTestResult {
        val url = URL(endpoint.url)
        return runCatching {
            val resolvedHost = resolveHost(url.host)
            val probe = http3Probe.execute(endpoint.url)
            val negotiated = probe.negotiatedProtocol
            val status = when {
                negotiated?.startsWith("h3", ignoreCase = true) == true -> ProtocolProbeStatus.Supported
                negotiated == "h2" || negotiated == "http/1.1" -> ProtocolProbeStatus.Fallback
                else -> ProtocolProbeStatus.Inconclusive
            }
            val summary = when (status) {
                ProtocolProbeStatus.Supported ->
                    "Cronet negotiated ${negotiated ?: "HTTP/3"} successfully."
                ProtocolProbeStatus.Fallback ->
                    "The request completed, but Cronet negotiated ${negotiated ?: "another protocol"} instead of HTTP/3."
                ProtocolProbeStatus.Inconclusive ->
                    "The request completed, but Cronet did not expose a definitive HTTP/3 negotiation result."
                else -> "HTTP/3 probe completed."
            }

            ProtocolTestResult(
                protocol = ProtocolProbeKind.Http3,
                endpointLabel = endpoint.label,
                endpointUrl = endpoint.url,
                status = status,
                summary = summary,
                negotiatedProtocol = negotiated,
                dnsTimeMs = resolvedHost.dnsTimeMs,
                totalTimeMs = probe.totalTimeMs,
                httpStatusCode = probe.httpStatusCode,
                ipAddress = resolvedHost.address.hostAddress,
                errorMessage = probe.httpStatusCode?.takeIf { it >= 400 }?.let {
                    "Endpoint returned HTTP $it, but the HTTP/3-capable request completed successfully."
                },
            )
        }.getOrElse { error ->
            val rootError = error.rootCause()
            when (rootError) {
                is GooglePlayServicesNotAvailableException,
                is GooglePlayServicesRepairableException,
                is TimeoutException -> ProtocolTestResult(
                    protocol = ProtocolProbeKind.Http3,
                    endpointLabel = endpoint.label,
                    endpointUrl = endpoint.url,
                    status = ProtocolProbeStatus.Inconclusive,
                    summary = "Cronet could not be initialized on this device, so Shannon could not run a real HTTP/3 probe.",
                    errorCategory = ProtocolProbeErrorCategory.QuicFailure,
                    errorMessage = rootError.message ?: rootError.javaClass.simpleName,
                )
                is InterruptedException -> {
                    Thread.currentThread().interrupt()
                    ProtocolTestResult(
                        protocol = ProtocolProbeKind.Http3,
                        endpointLabel = endpoint.label,
                        endpointUrl = endpoint.url,
                        status = ProtocolProbeStatus.Inconclusive,
                        summary = "HTTP/3 initialization was interrupted before Cronet could start.",
                        errorCategory = ProtocolProbeErrorCategory.QuicFailure,
                        errorMessage = rootError.message ?: rootError.javaClass.simpleName,
                    )
                }
                is ExecutionException -> ProtocolTestResult(
                    protocol = ProtocolProbeKind.Http3,
                    endpointLabel = endpoint.label,
                    endpointUrl = endpoint.url,
                    status = ProtocolProbeStatus.Inconclusive,
                    summary = "Cronet setup failed before Shannon could complete an HTTP/3 request.",
                    errorCategory = ProtocolProbeErrorCategory.QuicFailure,
                    errorMessage = rootError.message ?: rootError.javaClass.simpleName,
                )
                else -> protocolFailureResult(
                    protocol = ProtocolProbeKind.Http3,
                    endpoint = endpoint,
                    error = rootError,
                    defaultCategory = ProtocolProbeErrorCategory.QuicFailure,
                )
            }
        }
    }

    private fun runWebSocketProbe(endpoint: ProtocolEndpoint): ProtocolTestResult {
        val uri = URI(endpoint.url)
        return runCatching {
            openTlsProbe(
                host = uri.host,
                port = effectivePort(uri.port, 443),
                alpn = listOf("http/1.1"),
            ).use { probe ->
                val requestStart = System.nanoTime()
                val key = generateWebSocketKey()
                val request = buildString {
                    append("GET ${buildRequestPath(uri)} HTTP/1.1\r\n")
                    append("Host: ${uri.host}\r\n")
                    append("Upgrade: websocket\r\n")
                    append("Connection: Upgrade\r\n")
                    append("Sec-WebSocket-Key: $key\r\n")
                    append("Sec-WebSocket-Version: 13\r\n")
                    append("Origin: https://${uri.host}\r\n")
                    append("User-Agent: Shannon/1.0\r\n")
                    append("\r\n")
                }
                probe.socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
                probe.socket.getOutputStream().flush()

                val reader = probe.socket.inputStream.bufferedReader(Charsets.ISO_8859_1)
                val statusLine = reader.readLine() ?: throw IllegalStateException("No WebSocket handshake response")
                val headers = readHeaders(reader)
                val handshakeTimeMs = (System.nanoTime() - requestStart).toMillis()
                val httpStatus = parseHttpStatusCode(statusLine)
                val expectedAccept = expectedWebSocketAccept(key)
                val actualAccept = headers["sec-websocket-accept"]
                val isUpgrade = httpStatus == 101 &&
                    headers["upgrade"]?.equals("websocket", ignoreCase = true) == true &&
                    headers["connection"]?.contains("upgrade", ignoreCase = true) == true &&
                    actualAccept == expectedAccept

                if (isUpgrade) {
                    ProtocolTestResult(
                        protocol = ProtocolProbeKind.WebSocket,
                        endpointLabel = endpoint.label,
                        endpointUrl = endpoint.url,
                        status = ProtocolProbeStatus.Supported,
                        summary = "Received a valid 101 Switching Protocols response.",
                        negotiatedProtocol = "websocket",
                        dnsTimeMs = probe.dnsTimeMs,
                        tcpTimeMs = probe.tcpTimeMs,
                        tlsTimeMs = probe.tlsTimeMs,
                        totalTimeMs = probe.dnsTimeMs + probe.tcpTimeMs + probe.tlsTimeMs + handshakeTimeMs,
                        httpStatusCode = httpStatus,
                        ipAddress = probe.resolvedIp,
                    )
                } else {
                    ProtocolTestResult(
                        protocol = ProtocolProbeKind.WebSocket,
                        endpointLabel = endpoint.label,
                        endpointUrl = endpoint.url,
                        status = ProtocolProbeStatus.Failed,
                        summary = "WebSocket upgrade did not complete successfully.",
                        dnsTimeMs = probe.dnsTimeMs,
                        tcpTimeMs = probe.tcpTimeMs,
                        tlsTimeMs = probe.tlsTimeMs,
                        totalTimeMs = probe.dnsTimeMs + probe.tcpTimeMs + probe.tlsTimeMs + handshakeTimeMs,
                        httpStatusCode = httpStatus,
                        ipAddress = probe.resolvedIp,
                        errorCategory = ProtocolProbeErrorCategory.WebSocketUpgradeFailure,
                        errorMessage = "Expected HTTP 101 with a valid Sec-WebSocket-Accept header.",
                    )
                }
            }
        }.getOrElse { error ->
            protocolFailureResult(
                protocol = ProtocolProbeKind.WebSocket,
                endpoint = endpoint,
                error = error,
                defaultCategory = ProtocolProbeErrorCategory.WebSocketUpgradeFailure,
            )
        }
    }

    private fun runTlsAnalysisProbe(endpoint: ProtocolEndpoint): TlsEndpointAnalysis {
        val url = URL(endpoint.url)
        return runCatching {
            openTlsProbe(
                host = url.host,
                port = effectivePort(url.port, 443),
                alpn = listOf("h2", "http/1.1"),
            ).use { probe ->
                val session = probe.socket.session
                val certificates = session.peerCertificates
                    .mapNotNull { it as? X509Certificate }
                val leaf = certificates.firstOrNull()
                    ?: throw SSLHandshakeException("Peer certificate chain is empty")
                val issuer = principalLabel(leaf.issuerX500Principal.name)
                val tlsVersion = session.protocol ?: "Unknown"
                val supportedVersions = listOf(
                    TlsVersionSupport(
                        version = "TLSv1.3",
                        supported = probeTlsVersionSupport(
                            host = url.host,
                            port = effectivePort(url.port, 443),
                            version = "TLSv1.3",
                        ),
                    ),
                    TlsVersionSupport(
                        version = "TLSv1.2",
                        supported = probeTlsVersionSupport(
                            host = url.host,
                            port = effectivePort(url.port, 443),
                            version = "TLSv1.2",
                        ),
                    ),
                )

                TlsEndpointAnalysis(
                    endpointLabel = endpoint.label,
                    endpointUrl = endpoint.url,
                    status = TlsEndpointStatus.Normal,
                    summary = "TLS handshake completed normally.",
                    ipAddress = probe.resolvedIp,
                    tlsVersion = tlsVersion,
                    cipherSuite = session.cipherSuite ?: "Unknown",
                    alpn = probe.socket.applicationProtocol.takeIf { it.isNotBlank() },
                    certificate = TlsCertificateInfo(
                        subject = principalLabel(leaf.subjectX500Principal.name),
                        issuer = issuer,
                        validFrom = certificateDate(leaf.notBefore.time),
                        validUntil = certificateDate(leaf.notAfter.time),
                        publicKey = describePublicKey(leaf.publicKey),
                        fingerprintSha256 = sha256Fingerprint(leaf.encoded),
                    ),
                    certificateChain = certificates.map { principalLabel(it.subjectX500Principal.name) },
                    supportedVersions = supportedVersions,
                    dnsTimeMs = probe.dnsTimeMs,
                    tcpTimeMs = probe.tcpTimeMs,
                    tlsTimeMs = probe.tlsTimeMs,
                    totalTimeMs = probe.dnsTimeMs + probe.tcpTimeMs + probe.tlsTimeMs,
                )
            }
        }.getOrElse { error ->
            val category = classifyProtocolError(
                error = error,
                defaultCategory = ProtocolProbeErrorCategory.TlsFailure,
            )
            TlsEndpointAnalysis(
                endpointLabel = endpoint.label,
                endpointUrl = endpoint.url,
                status = if (category == ProtocolProbeErrorCategory.Timeout) {
                    TlsEndpointStatus.Inconclusive
                } else {
                    TlsEndpointStatus.Failed
                },
                summary = error.message ?: "TLS handshake failed.",
                errorMessage = error.message ?: error.javaClass.simpleName,
            )
        }
    }

    private fun runSniProviderAnalysis(endpoint: ProtocolEndpoint): SniProviderProbeResult {
        val url = URL(endpoint.url)
        val requestedHost = url.host
        val alternativeSni = "example.com"
        val randomSni = "random-${Random.nextInt(100_000, 999_999)}.invalid"
        val resolved = runCatching { resolveHost(requestedHost) }.getOrElse { error ->
            return SniProviderProbeResult(
                providerLabel = endpoint.label,
                endpointUrl = endpoint.url,
                variants = listOf(
                    failedSniVariant(
                        variant = SniVariantType.NormalSni,
                        requestedHost = requestedHost,
                        sniValue = requestedHost,
                        dnsResolved = false,
                        tcpConnected = false,
                        error = error,
                    ),
                    failedSniVariant(
                        variant = SniVariantType.AlternativeSni,
                        requestedHost = requestedHost,
                        sniValue = alternativeSni,
                        dnsResolved = false,
                        tcpConnected = false,
                        error = error,
                    ),
                    failedSniVariant(
                        variant = SniVariantType.NoSni,
                        requestedHost = requestedHost,
                        sniValue = null,
                        dnsResolved = false,
                        tcpConnected = false,
                        error = error,
                    ),
                    failedSniVariant(
                        variant = SniVariantType.RandomSni,
                        requestedHost = requestedHost,
                        sniValue = randomSni,
                        dnsResolved = false,
                        tcpConnected = false,
                        error = error,
                    ),
                ),
            )
        }

        val variants = listOf(
            runSniVariant(
                variant = SniVariantType.NormalSni,
                requestedHost = requestedHost,
                port = effectivePort(url.port, 443),
                resolved = resolved,
                explicitSni = requestedHost,
                includeSni = true,
            ),
            runSniVariant(
                variant = SniVariantType.AlternativeSni,
                requestedHost = requestedHost,
                port = effectivePort(url.port, 443),
                resolved = resolved,
                explicitSni = alternativeSni,
                includeSni = true,
            ),
            runSniVariant(
                variant = SniVariantType.NoSni,
                requestedHost = requestedHost,
                port = effectivePort(url.port, 443),
                resolved = resolved,
                explicitSni = null,
                includeSni = false,
            ),
            runSniVariant(
                variant = SniVariantType.RandomSni,
                requestedHost = requestedHost,
                port = effectivePort(url.port, 443),
                resolved = resolved,
                explicitSni = randomSni,
                includeSni = true,
            ),
        )

        return SniProviderProbeResult(
            providerLabel = endpoint.label,
            endpointUrl = endpoint.url,
            variants = variants,
        )
    }

    private fun runSniVariant(
        variant: SniVariantType,
        requestedHost: String,
        port: Int,
        resolved: ResolvedHost,
        explicitSni: String?,
        includeSni: Boolean,
    ): SniVariantResult {
        val rawSocket = Socket()
        val tcpStart = System.nanoTime()
        val tcpTimeMs = try {
            rawSocket.connect(InetSocketAddress(resolved.address, port), 5_000)
            (System.nanoTime() - tcpStart).toMillis()
        } catch (error: Throwable) {
            val elapsed = (System.nanoTime() - tcpStart).toMillis()
            rawSocket.close()
            return failedSniVariant(
                variant = variant,
                requestedHost = requestedHost,
                sniValue = if (includeSni) explicitSni else null,
                dnsResolved = true,
                tcpConnected = false,
                error = error,
                ipAddress = resolved.address.hostAddress,
                dnsTimeMs = resolved.dnsTimeMs,
                tcpTimeMs = elapsed,
            )
        }

        val sslSocket = try {
            (permissiveSslSocketFactory.createSocket(
                rawSocket,
                if (includeSni) requestedHost else (resolved.address.hostAddress ?: requestedHost),
                port,
                true,
            ) as SSLSocket).apply {
                soTimeout = 5_000
                useClientMode = true
                val parameters = sslParameters
                parameters.endpointIdentificationAlgorithm = null
                parameters.applicationProtocols = arrayOf("h2", "http/1.1")
                parameters.serverNames = when {
                    !includeSni -> emptyList()
                    explicitSni != null -> listOf(SNIHostName(explicitSni))
                    else -> null
                }
                sslParameters = parameters
            }
        } catch (error: Throwable) {
            rawSocket.close()
            return failedSniVariant(
                variant = variant,
                requestedHost = requestedHost,
                sniValue = if (includeSni) explicitSni else null,
                dnsResolved = true,
                tcpConnected = true,
                error = error,
                ipAddress = resolved.address.hostAddress,
                dnsTimeMs = resolved.dnsTimeMs,
                tcpTimeMs = tcpTimeMs,
            )
        }

        val tlsStart = System.nanoTime()
        try {
            sslSocket.startHandshake()
        } catch (error: Throwable) {
            val tlsTimeMs = (System.nanoTime() - tlsStart).toMillis()
            sslSocket.close()
            return failedSniVariant(
                variant = variant,
                requestedHost = requestedHost,
                sniValue = if (includeSni) explicitSni else null,
                dnsResolved = true,
                tcpConnected = true,
                error = error,
                ipAddress = resolved.address.hostAddress,
                dnsTimeMs = resolved.dnsTimeMs,
                tcpTimeMs = tcpTimeMs,
                tlsTimeMs = tlsTimeMs,
            )
        }

        sslSocket.use { socket ->
            val tlsTimeMs = (System.nanoTime() - tlsStart).toMillis()
            val session = socket.session
            val certificates = session.peerCertificates.mapNotNull { it as? X509Certificate }
            val leaf = certificates.firstOrNull()
            val hostnameMatch = leaf?.let { hostMatchesCertificate(requestedHost, it) }
            val trustValidation = validateAgainstSystemTrustStore(certificates)

            return SniVariantResult(
                variant = variant,
                requestedHost = requestedHost,
                sniValue = if (includeSni) explicitSni else null,
                dnsResolved = true,
                tcpConnected = true,
                tlsHandshakeSucceeded = true,
                ipAddress = resolved.address.hostAddress,
                tlsVersion = session.protocol ?: "Unknown",
                cipherSuite = session.cipherSuite ?: "Unknown",
                alpn = socket.applicationProtocol.takeIf { it.isNotBlank() },
                certificate = leaf?.let {
                    TlsCertificateInfo(
                        subject = principalLabel(it.subjectX500Principal.name),
                        issuer = principalLabel(it.issuerX500Principal.name),
                        validFrom = certificateDate(it.notBefore.time),
                        validUntil = certificateDate(it.notAfter.time),
                        publicKey = describePublicKey(it.publicKey),
                        fingerprintSha256 = sha256Fingerprint(it.encoded),
                    )
                },
                certificateChain = certificates.map { principalLabel(it.subjectX500Principal.name) },
                certificateMatchesRequestedHost = hostnameMatch,
                certificateChainTrustedBySystem = trustValidation?.isTrusted,
                systemTrustErrorMessage = trustValidation?.errorMessage,
                dnsTimeMs = resolved.dnsTimeMs,
                tcpTimeMs = tcpTimeMs,
                tlsTimeMs = tlsTimeMs,
                totalTimeMs = resolved.dnsTimeMs + tcpTimeMs + tlsTimeMs,
            )
        }
    }

    private fun failedSniVariant(
        variant: SniVariantType,
        requestedHost: String,
        sniValue: String?,
        dnsResolved: Boolean,
        tcpConnected: Boolean,
        error: Throwable,
        ipAddress: String? = null,
        dnsTimeMs: Long? = null,
        tcpTimeMs: Long? = null,
        tlsTimeMs: Long? = null,
    ): SniVariantResult {
        val category = classifySniError(error)
        return SniVariantResult(
            variant = variant,
            requestedHost = requestedHost,
            sniValue = sniValue,
            dnsResolved = dnsResolved,
            tcpConnected = tcpConnected,
            tlsHandshakeSucceeded = false,
            ipAddress = ipAddress,
            dnsTimeMs = dnsTimeMs,
            tcpTimeMs = tcpTimeMs,
            tlsTimeMs = tlsTimeMs,
            totalTimeMs = listOfNotNull(dnsTimeMs, tcpTimeMs, tlsTimeMs).sum().takeIf { it > 0L },
            errorCategory = category,
            errorMessage = error.message ?: error.javaClass.simpleName,
        )
    }

    private fun probeTlsVersionSupport(
        host: String,
        port: Int,
        version: String,
    ): Boolean {
        return runCatching {
            openTlsProbe(
                host = host,
                port = port,
                alpn = listOf("h2", "http/1.1"),
                enabledProtocols = arrayOf(version),
            ).use { true }
        }.getOrDefault(false)
    }

    private fun openTlsProbe(
        host: String,
        port: Int,
        alpn: List<String>,
        enabledProtocols: Array<String>? = null,
        resolvedHost: ResolvedHost? = null,
        tlsPeerHost: String = host,
        explicitSni: String? = host,
        includeSni: Boolean = true,
        verifyHostname: Boolean = true,
    ): OpenTlsProbe {
        val resolved = resolvedHost ?: resolveHost(host)
        val rawSocket = Socket()
        val tcpTimeMs = try {
            measureNanoTime {
                rawSocket.connect(InetSocketAddress(resolved.address, port), 5_000)
            }.toMillis()
        } catch (error: Throwable) {
            rawSocket.close()
            throw error
        }

        val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val sslSocket = try {
            (sslFactory.createSocket(rawSocket, tlsPeerHost, port, true) as SSLSocket).apply {
                soTimeout = 5_000
                useClientMode = true
                if (enabledProtocols != null) {
                    this.enabledProtocols = enabledProtocols
                }
                val parameters = sslParameters
                parameters.endpointIdentificationAlgorithm = if (verifyHostname) "HTTPS" else null
                parameters.applicationProtocols = alpn.toTypedArray()
                parameters.serverNames = when {
                    !includeSni -> emptyList()
                    explicitSni != null -> listOf(SNIHostName(explicitSni))
                    else -> null
                }
                sslParameters = parameters
            }
        } catch (error: Throwable) {
            rawSocket.close()
            throw error
        }

        val tlsTimeMs = try {
            measureNanoTime {
                sslSocket.startHandshake()
            }.toMillis()
        } catch (error: Throwable) {
            sslSocket.close()
            throw error
        }

        return OpenTlsProbe(
            socket = sslSocket,
            resolvedIp = resolved.address.hostAddress ?: host,
            dnsTimeMs = resolved.dnsTimeMs,
            tcpTimeMs = tcpTimeMs,
            tlsTimeMs = tlsTimeMs,
        )
    }

    private fun resolveHost(host: String): ResolvedHost {
        var address: InetAddress? = null
        val dnsTimeMs = measureNanoTime {
            address = InetAddress.getByName(host)
        }.toMillis()
        return ResolvedHost(
            address = requireNotNull(address),
            dnsTimeMs = dnsTimeMs,
        )
    }

    private fun buildTlsObservations(endpoints: List<TlsEndpointAnalysis>): List<TlsObservation> {
        val observations = mutableListOf<TlsObservation>()
        val successful = endpoints.filter { it.status == TlsEndpointStatus.Normal }
        if (successful.isEmpty()) {
            return listOf(
                TlsObservation(
                    code = "INCONCLUSIVE",
                    title = context.getString(R.string.tls_observation_inconclusive_title),
                    summary = context.getString(R.string.tls_observation_inconclusive_summary),
                )
            )
        }

        val suspiciousIssuerKeywords = listOf(
            "proxy",
            "inspection",
            "filter",
            "fortinet",
            "zscaler",
            "netskope",
            "kaspersky",
            "eset",
            "avast",
            "secure web gateway",
        )
        val suspiciousIssuers = successful.filter { endpoint ->
            val issuer = endpoint.certificate?.issuer.orEmpty().lowercase(Locale.US)
            suspiciousIssuerKeywords.any { keyword -> issuer.contains(keyword) }
        }
        if (suspiciousIssuers.isNotEmpty()) {
            observations += TlsObservation(
                code = "TLS_INTERCEPTION_SUSPECTED",
                title = context.getString(R.string.observation_tls_interception_title),
                summary = context.getString(R.string.observation_tls_interception_summary),
            )
        }

        val downgradeEndpoints = successful.filter { endpoint ->
            endpoint.tlsVersion == "TLSv1.2" &&
                endpoint.supportedVersions.any { it.version == "TLSv1.3" && it.supported }
        }
        if (downgradeEndpoints.isNotEmpty()) {
            observations += TlsObservation(
                code = "TLS_DOWNGRADE_SUSPECTED",
                title = context.getString(R.string.observation_tls_downgrade_title),
                summary = context.getString(R.string.observation_tls_downgrade_summary),
            )
        }

        val unusualChains = successful.filter { endpoint ->
            endpoint.certificateChain.size < 2
        }
        if (unusualChains.isNotEmpty()) {
            observations += TlsObservation(
                code = "UNUSUAL_CERTIFICATE_CHAIN",
                title = context.getString(R.string.observation_unusual_chain_title),
                summary = context.getString(R.string.observation_unusual_chain_summary),
            )
        }

        if (observations.isEmpty()) {
            observations += TlsObservation(
                code = "NO_TLS_ANOMALIES",
                title = context.getString(R.string.observation_no_tls_anomalies_title),
                summary = context.getString(R.string.observation_no_tls_anomalies_summary),
            )
        }

        return observations
    }

    private fun buildProtocolObservations(tests: List<ProtocolTestResult>): List<ProtocolObservation> {
        val observations = mutableListOf<ProtocolObservation>()
        val http11Supported = tests.filter {
            it.protocol == ProtocolProbeKind.Http11 && it.status == ProtocolProbeStatus.Supported
        }
        val http2Supported = tests.filter {
            it.protocol == ProtocolProbeKind.Http2 && it.status == ProtocolProbeStatus.Supported
        }
        val http2Fallbacks = tests.filter {
            it.protocol == ProtocolProbeKind.Http2 && it.status == ProtocolProbeStatus.Fallback
        }
        val http3Supported = tests.filter {
            it.protocol == ProtocolProbeKind.Http3 && it.status == ProtocolProbeStatus.Supported
        }
        val http3Fallbacks = tests.filter {
            it.protocol == ProtocolProbeKind.Http3 && it.status == ProtocolProbeStatus.Fallback
        }
        val http3Blocked = tests.filter {
            it.protocol == ProtocolProbeKind.Http3 &&
                (it.status == ProtocolProbeStatus.Failed ||
                    it.status == ProtocolProbeStatus.Blocked)
        }
        val http3Inconclusive = tests.filter {
            it.protocol == ProtocolProbeKind.Http3 && it.status == ProtocolProbeStatus.Inconclusive
        }
        val webSocketFailures = tests.filter {
            it.protocol == ProtocolProbeKind.WebSocket && it.status != ProtocolProbeStatus.Supported
        }

        if (http2Fallbacks.size >= 2 && http11Supported.isNotEmpty()) {
            observations += ProtocolObservation(
                code = "ALPN_FALLBACK_OBSERVED",
                title = context.getString(R.string.observation_alpn_fallback_title),
                summary = context.getString(R.string.observation_alpn_fallback_summary),
            )
        }

        if (http3Blocked.size >= 2 && http2Supported.isNotEmpty()) {
            observations += ProtocolObservation(
                code = "QUIC_BLOCKING_SUSPECTED",
                title = context.getString(R.string.observation_quic_blocking_title),
                summary = context.getString(R.string.observation_quic_blocking_summary),
            )
        } else if (http3Fallbacks.isNotEmpty() && http2Supported.isNotEmpty()) {
            observations += ProtocolObservation(
                code = "HTTP3_FALLBACK_OBSERVED",
                title = context.getString(R.string.observation_http3_fallback_title),
                summary = context.getString(R.string.observation_http3_fallback_summary),
            )
        } else if (http3Inconclusive.size >= 2 && http3Supported.isEmpty() && http2Supported.isNotEmpty()) {
            observations += ProtocolObservation(
                code = "QUIC_OR_HTTP3_LIMITED",
                title = context.getString(R.string.observation_http3_unconfirmed_title),
                summary = context.getString(R.string.observation_http3_unconfirmed_summary),
            )
        }

        if (webSocketFailures.isNotEmpty() && (http11Supported.isNotEmpty() || http2Supported.isNotEmpty())) {
            observations += ProtocolObservation(
                code = "WEBSOCKET_FILTERING_SUSPECTED",
                title = context.getString(R.string.observation_websocket_failed_title),
                summary = context.getString(R.string.observation_websocket_failed_summary),
            )
        }

        val mixedProviders = tests
            .groupBy { it.endpointLabel }
            .filterValues { providerTests ->
                providerTests.any { it.status == ProtocolProbeStatus.Supported } &&
                    providerTests.any { it.status != ProtocolProbeStatus.Supported }
            }
        if (mixedProviders.isNotEmpty()) {
            observations += ProtocolObservation(
                code = "PROVIDER_SPECIFIC_BEHAVIOR",
                title = context.getString(R.string.observation_provider_specific_title),
                summary = context.getString(R.string.observation_provider_specific_summary),
            )
        }

        if (observations.isEmpty()) {
            observations += ProtocolObservation(
                code = "NO_CLEAR_INTERFERENCE",
                title = context.getString(R.string.observation_no_clear_interference_title),
                summary = context.getString(R.string.observation_no_clear_interference_summary),
            )
        }

        return observations
    }

    private fun protocolFailureResult(
        protocol: ProtocolProbeKind,
        endpoint: ProtocolEndpoint,
        error: Throwable,
        defaultCategory: ProtocolProbeErrorCategory,
    ): ProtocolTestResult {
        val category = classifyProtocolError(error, defaultCategory)
        return ProtocolTestResult(
            protocol = protocol,
            endpointLabel = endpoint.label,
            endpointUrl = endpoint.url,
            status = when (category) {
                ProtocolProbeErrorCategory.TcpFailure,
                ProtocolProbeErrorCategory.DnsFailure,
                ProtocolProbeErrorCategory.Timeout -> ProtocolProbeStatus.Blocked
                else -> ProtocolProbeStatus.Failed
            },
            summary = error.message ?: context.getString(R.string.protocol_probe_failed, context.getString(protocol.titleResId())),
            errorCategory = category,
            errorMessage = error.message ?: error.javaClass.simpleName,
        )
    }

    private fun classifyProtocolError(
        error: Throwable,
        defaultCategory: ProtocolProbeErrorCategory,
    ): ProtocolProbeErrorCategory {
        return when (error) {
            is UnknownHostException -> ProtocolProbeErrorCategory.DnsFailure
            is NoRouteToHostException -> ProtocolProbeErrorCategory.TcpFailure
            is java.net.ConnectException -> ProtocolProbeErrorCategory.TcpFailure
            is SSLHandshakeException -> ProtocolProbeErrorCategory.TlsFailure
            is SSLException -> ProtocolProbeErrorCategory.TlsFailure
            is SocketTimeoutException -> ProtocolProbeErrorCategory.Timeout
            else -> defaultCategory
        }
    }

    private fun Throwable.rootCause(): Throwable {
        var current = this
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
    }

    private fun buildRequestPath(url: URL): String {
        return url.file.takeIf { it.isNotBlank() } ?: "/"
    }

    private fun buildRequestPath(uri: URI): String {
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        return uri.rawQuery?.let { "$path?$it" } ?: path
    }

    private fun parseHttpStatusCode(statusLine: String): Int? {
        return statusLine.split(" ").getOrNull(1)?.toIntOrNull()
    }

    private fun drainHeaders(reader: BufferedReader) {
        while (true) {
            val line = reader.readLine() ?: return
            if (line.isEmpty()) return
        }
    }

    private fun readHeaders(reader: BufferedReader): Map<String, String> {
        val headers = linkedMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                val key = line.substring(0, separator).trim().lowercase(Locale.US)
                val value = line.substring(separator + 1).trim()
                headers[key] = value
            }
        }
        return headers
    }

    private fun generateWebSocketKey(): String {
        val keyBytes = ByteArray(16)
        Random.Default.nextBytes(keyBytes)
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }

    private fun expectedWebSocketAccept(key: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun sha256Fingerprint(encoded: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(encoded)
            .joinToString(":") { byte -> "%02X".format(byte) }
    }

    private fun principalLabel(name: String): String {
        return name.split(",")
            .map { it.trim() }
            .firstOrNull { it.startsWith("CN=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim()
            ?: name
    }

    private fun describePublicKey(publicKey: java.security.PublicKey): String {
        return when (publicKey) {
            is RSAPublicKey -> "RSA ${publicKey.modulus.bitLength()}"
            is ECPublicKey -> "EC ${publicKey.params.order.bitLength()}"
            else -> publicKey.algorithm
        }
    }

    private fun certificateDate(epochMillis: Long): String {
        return java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

    private fun validateAgainstSystemTrustStore(
        certificates: List<X509Certificate>,
    ): TrustValidationResult? {
        if (certificates.isEmpty()) return null
        return try {
            systemTrustManager.checkServerTrusted(
                certificates.toTypedArray(),
                certificates.first().publicKey.algorithm,
            )
            TrustValidationResult(isTrusted = true, errorMessage = null)
        } catch (error: CertificateException) {
            TrustValidationResult(
                isTrusted = false,
                errorMessage = error.message ?: error.javaClass.simpleName,
            )
        }
    }

    private fun hostMatchesCertificate(host: String, certificate: X509Certificate): Boolean {
        val normalizedHost = host.lowercase(Locale.US)
        val sanMatches = certificate.subjectAlternativeNames
            ?.mapNotNull { entry ->
                val type = entry.getOrNull(0) as? Int
                if (type == 2) entry.getOrNull(1) as? String else null
            }
            .orEmpty()
            .any { pattern -> hostMatchesPattern(normalizedHost, pattern.lowercase(Locale.US)) }
        if (sanMatches) return true
        return hostMatchesPattern(
            normalizedHost,
            principalLabel(certificate.subjectX500Principal.name).lowercase(Locale.US),
        )
    }

    private fun hostMatchesPattern(host: String, pattern: String): Boolean {
        return when {
            pattern.startsWith("*.") -> {
                val suffix = pattern.removePrefix("*.")
                host.endsWith(".$suffix") && host.count { it == '.' } > suffix.count { it == '.' }
            }
            else -> host == pattern
        }
    }

    private fun classifySniError(error: Throwable): SniProbeErrorCategory {
        return when (error) {
            is UnknownHostException -> SniProbeErrorCategory.DnsFailure
            is NoRouteToHostException -> SniProbeErrorCategory.TcpFailure
            is java.net.ConnectException -> SniProbeErrorCategory.TcpFailure
            is SocketTimeoutException -> SniProbeErrorCategory.Timeout
            is SSLHandshakeException -> {
                val message = error.message.orEmpty()
                when {
                    message.contains("reset", ignoreCase = true) ->
                        SniProbeErrorCategory.RstDetected
                    else -> SniProbeErrorCategory.TlsFailure
                }
            }
            is SSLException -> SniProbeErrorCategory.TlsFailure
            else -> SniProbeErrorCategory.Unknown
        }
    }

    private data class TrustValidationResult(
        val isTrusted: Boolean,
        val errorMessage: String?,
    )

    private fun effectivePort(port: Int, defaultPort: Int): Int {
        return if (port == -1) defaultPort else port
    }

    private fun readCarrierName(capabilities: NetworkCapabilities?): String {
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) != true) {
            return "n/a"
        }
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() } ?: "n/a"
    }

    private fun readWifiSnapshot(capabilities: NetworkCapabilities?): WifiSnapshot {
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
            return WifiSnapshot(
                ssid = "n/a",
                bssid = "n/a",
                signalStrength = "n/a",
            )
        }
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiInfo = wifiManager?.connectionInfo

        val ssid = wifiInfo?.ssid
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && !it.equals("<unknown ssid>", ignoreCase = true) }
            ?: "n/a"
        val bssid = wifiInfo?.bssid
            ?.takeIf { it.isNotBlank() && !it.equals("02:00:00:00:00:00", ignoreCase = true) }
            ?: "n/a"
        val rssi = wifiInfo?.rssi ?: Int.MIN_VALUE
        val signalStrength = if (rssi == Int.MIN_VALUE || rssi <= -127) {
            "n/a"
        } else {
            "$rssi dBm (${rssi.toSignalQualityLabel()})"
        }

        return WifiSnapshot(
            ssid = ssid,
            bssid = bssid,
            signalStrength = signalStrength,
        )
    }

    private fun nowTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalTime.now().withNano(0).toString()
        } else {
            System.currentTimeMillis().toString()
        }
    }

    private fun resolveWithSystemDns(domain: String): DnsServerResult {
        return runCatching {
            val addresses = InetAddress.getAllByName(domain).mapNotNull { it.hostAddress }.distinct()
            val ipv4 = addresses.filter { !it.contains(":") }
            val ipv6 = addresses.filter { it.contains(":") }
            DnsServerResult(
            serverName = context.getString(R.string.dns_server_system),
            serverAddress = context.getString(R.string.dns_server_system_address),
            records = listOf(
                    DnsRecordResult(
                        recordType = "A",
                        transport = DnsLookupTransport.System,
                        addresses = ipv4,
                        responseCode = if (ipv4.isEmpty()) 0 else 0,
                        error = if (ipv4.isEmpty()) context.getString(R.string.dns_no_records, "A") else null,
                    ),
                    DnsRecordResult(
                        recordType = "AAAA",
                        transport = DnsLookupTransport.System,
                        addresses = ipv6,
                        responseCode = if (ipv6.isEmpty()) 0 else 0,
                        error = if (ipv6.isEmpty()) context.getString(R.string.dns_no_records, "AAAA") else null,
                    ),
                ),
            )
        }.getOrElse { error ->
            val isNxdomain = error.javaClass.simpleName.contains("UnknownHost", ignoreCase = true)
            DnsServerResult(
                serverName = context.getString(R.string.dns_server_system),
                serverAddress = context.getString(R.string.dns_server_system_address),
                records = listOf(
                    DnsRecordResult(
                        recordType = "A",
                        transport = DnsLookupTransport.System,
                        addresses = emptyList(),
                        responseCode = if (isNxdomain) 3 else null,
                        error = if (isNxdomain) context.getString(R.string.dns_nxdomain) else (error.message ?: context.getString(R.string.dns_system_lookup_failed)),
                    ),
                    DnsRecordResult(
                        recordType = "AAAA",
                        transport = DnsLookupTransport.System,
                        addresses = emptyList(),
                        responseCode = if (isNxdomain) 3 else null,
                        error = if (isNxdomain) context.getString(R.string.dns_nxdomain) else (error.message ?: context.getString(R.string.dns_system_lookup_failed)),
                    ),
                ),
            )
        }
    }

    private fun resolveWithDnsServer(
        domain: String,
        serverName: String,
        serverIp: String,
    ): DnsServerResult {
        return DnsServerResult(
            serverName = serverName,
            serverAddress = serverIp,
            records = listOf(
                resolveDnsRecord(domain, serverIp, "A", 0x0001),
                resolveDnsRecord(domain, serverIp, "AAAA", 0x001C),
            ),
        )
    }

    private fun resolveDnsRecord(
        domain: String,
        serverIp: String,
        recordType: String,
        queryType: Int,
    ): DnsRecordResult {
        return runCatching {
            val addresses = resolveDnsOverUdp(domain, serverIp, queryType)
            DnsRecordResult(
                recordType = recordType,
                transport = DnsLookupTransport.Udp,
                addresses = addresses,
                responseCode = 0,
                error = if (addresses.isEmpty()) context.getString(R.string.dns_no_records, recordType) else null,
            )
        }.recoverCatching { error ->
            val addresses = resolveDnsOverTcp(domain, serverIp, queryType)
            DnsRecordResult(
                recordType = recordType,
                transport = DnsLookupTransport.TcpFallback,
                addresses = addresses,
                responseCode = 0,
                error = if (addresses.isEmpty()) {
                    context.getString(
                        R.string.dns_tcp_fallback_no_records,
                        error.message ?: context.getString(R.string.dns_udp_query_failed),
                        recordType,
                    )
                } else {
                    null
                },
            )
        }.getOrElse { error ->
            DnsRecordResult(
                recordType = recordType,
                transport = DnsLookupTransport.TcpFallback,
                addresses = emptyList(),
                responseCode = extractDnsResponseCode(error.message),
                error = error.message ?: context.getString(R.string.dns_lookup_failed, recordType),
            )
        }
    }

    private fun resolveDnsOverUdp(
        domain: String,
        serverIp: String,
        queryType: Int,
    ): List<String> {
        val query = buildDnsQuery(domain, queryType)
        val response = ByteArray(512)
        DatagramSocket().use { socket ->
            socket.soTimeout = 3_000
            socket.send(
                DatagramPacket(
                    query,
                    query.size,
                    InetSocketAddress(serverIp, 53)
                )
            )
            val packet = DatagramPacket(response, response.size)
            socket.receive(packet)
            return parseDnsResponse(packet.data, packet.length, queryType)
        }
    }

    private fun resolveDnsOverTcp(
        domain: String,
        serverIp: String,
        queryType: Int,
    ): List<String> {
        val query = buildDnsQuery(domain, queryType)
        Socket().use { socket ->
            socket.soTimeout = 3_000
            socket.connect(InetSocketAddress(serverIp, 53), 3_000)
            val output = socket.getOutputStream()
            output.write(query.size shr 8)
            output.write(query.size and 0xFF)
            output.write(query)
            output.flush()

            val input = socket.getInputStream()
            val high = input.read()
            val low = input.read()
            if (high < 0 || low < 0) {
                throw SocketTimeoutException("DNS TCP response length not received")
            }
            val length = (high shl 8) or low
            val response = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val read = input.read(response, offset, length - offset)
                if (read < 0) break
                offset += read
            }
            if (offset != length) {
                throw SocketTimeoutException("Incomplete DNS TCP response")
            }
            return parseDnsResponse(response, length, queryType)
        }
    }
}

private fun ConnectivityTestResult.toWebsiteAccessibilityStatus(): WebsiteAccessibilityStatus {
    val dnsStep = steps.firstOrNull { it.stage == "DNS" }
    if (dnsStep?.success == false) return WebsiteAccessibilityStatus.DnsBlocked

    val tcpStep = steps.firstOrNull { it.stage == "TCP" }
    if (tcpStep?.success == false) return WebsiteAccessibilityStatus.TcpBlocked

    val tlsStep = steps.firstOrNull { it.stage == "TLS" }
    if (tlsStep?.success == false) return WebsiteAccessibilityStatus.TlsError

    val httpStep = steps.firstOrNull { it.stage == "HTTP" }
    return if (httpStep?.success == true) {
        WebsiteAccessibilityStatus.Ok
    } else {
        WebsiteAccessibilityStatus.HttpError
    }
}

private fun NetworkCapabilities?.toNetworkType(): String {
    if (this == null) return "n/a"
    return when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
        else -> "Other"
    }
}

private fun LinkProperties?.findAddress(isIpv4: Boolean): String {
    val address = this?.linkAddresses
        ?.mapNotNull { it.address }
        ?.firstOrNull { candidate ->
            val host = candidate.hostAddress.orEmpty()
            if (isIpv4) !host.contains(":") else host.contains(":")
        }
    return address?.hostAddress ?: "n/a"
}

private fun LinkProperties?.findGatewayAddress(): String {
    return this?.routes
        ?.mapNotNull { it.gateway?.hostAddress }
        ?.firstOrNull { !it.contains(":") }
        ?: "n/a"
}

private fun Int?.toMbps(): String {
    val kbps = this ?: return "n/a"
    if (kbps <= 0) return "n/a"
    return "${(kbps / 1000f).roundToInt()} Mbps"
}

private fun Int.toSignalQualityLabel(): String {
    return when {
        this >= -55 -> "Excellent"
        this >= -67 -> "Good"
        this >= -75 -> "Fair"
        else -> "Weak"
    }
}

private fun Long.toMillis(): Long = this / 1_000_000

private fun buildDnsQuery(domain: String, queryType: Int): ByteArray {
    val output = ByteArrayOutputStream()
    output.write(
        byteArrayOf(
            0x12, 0x34, 0x01, 0x00,
            0x00, 0x01, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
        )
    )
    domain.split(".").forEach { label ->
        output.write(label.length)
        output.write(label.toByteArray(Charsets.US_ASCII))
    }
    output.write(0x00)
    output.write(byteArrayOf((queryType shr 8).toByte(), (queryType and 0xFF).toByte(), 0x00, 0x01))
    return output.toByteArray()
}

private fun parseDnsResponse(data: ByteArray, length: Int, queryType: Int): List<String> {
    val buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
    val flags = buffer.getShort(2).toInt() and 0xFFFF
    val responseCode = flags and 0x000F
    if (responseCode != 0) {
        throw IllegalStateException(dnsResponseCodeMessage(responseCode))
    }
    buffer.position(4)
    val questionCount = buffer.short.toInt() and 0xFFFF
    val answerCount = buffer.short.toInt() and 0xFFFF
    buffer.position(12)

    repeat(questionCount) {
        skipDnsName(buffer)
        buffer.position(buffer.position() + 4)
    }

    val addresses = mutableListOf<String>()
    repeat(answerCount) {
        skipDnsName(buffer)
        val type = buffer.short.toInt() and 0xFFFF
        buffer.short
        buffer.int
        val dataLength = buffer.short.toInt() and 0xFFFF
        if (type == 1 && queryType == 1 && dataLength == 4) {
            val octets = ByteArray(4)
            buffer.get(octets)
            addresses += octets.joinToString(".") { (it.toInt() and 0xFF).toString() }
        } else if (type == 28 && queryType == 28 && dataLength == 16) {
            val octets = ByteArray(16)
            buffer.get(octets)
            InetAddress.getByAddress(octets).hostAddress?.let(addresses::add)
        } else {
            buffer.position(buffer.position() + dataLength)
        }
    }

    return addresses.distinct()
}

private fun skipDnsName(buffer: ByteBuffer) {
    while (buffer.hasRemaining()) {
        val length = buffer.get().toInt() and 0xFF
        when {
            length == 0 -> return
            length and 0xC0 == 0xC0 -> {
                buffer.get()
                return
            }
            else -> buffer.position(buffer.position() + length)
        }
    }
}

private fun extractDnsResponseCode(message: String?): Int? {
    return when {
        message?.contains("NXDOMAIN", ignoreCase = true) == true -> 3
        else -> null
    }
}

private fun dnsResponseCodeMessage(code: Int): String {
    return when (code) {
        3 -> "NXDOMAIN"
        2 -> "SERVFAIL"
        5 -> "REFUSED"
        else -> "DNS error code $code"
    }
}

private fun isPrivateOrReservedIp(ip: String): Boolean {
    if (ip == "0.0.0.0") return true
    if (ip.startsWith("127.")) return true
    if (ip.startsWith("10.")) return true
    if (ip.startsWith("192.168.")) return true
    if (ip.matches(Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\..*"))) return true
    return false
}

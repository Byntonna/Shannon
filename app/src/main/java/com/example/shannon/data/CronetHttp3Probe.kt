package com.example.shannon.data

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.google.android.gms.tasks.Tasks
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo

internal data class CronetHttp3Result(
    val negotiatedProtocol: String?,
    val httpStatusCode: Int?,
    val totalTimeMs: Long,
)

internal class CronetHttp3Probe(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    private var engine: CronetEngine? = null

    fun execute(url: String): CronetHttp3Result {
        ensureProviderInstalled()
        val requestCallback = BlockingUrlRequestCallback()
        val requestStart = System.nanoTime()
        val request = cronetEngine()
            .newUrlRequestBuilder(url, requestCallback, executor)
            .setHttpMethod("GET")
            .addHeader("User-Agent", "Shannon/1.0")
            .addHeader("Accept", "*/*")
            .disableCache()
            .build()
        requestCallback.bind(request)
        request.start()

        val info = requestCallback.await()
        return CronetHttp3Result(
            negotiatedProtocol = info.negotiatedProtocol?.takeIf { it.isNotBlank() },
            httpStatusCode = info.httpStatusCode,
            totalTimeMs = (System.nanoTime() - requestStart).toMillis(),
        )
    }

    private fun ensureProviderInstalled() {
        if (CronetProviderInstaller.isInstalled()) return
        Tasks.await(
            CronetProviderInstaller.installProvider(appContext),
            10,
            TimeUnit.SECONDS,
        )
    }

    private fun cronetEngine(): CronetEngine {
        engine?.let { return it }
        return synchronized(this) {
            engine?.let { return@synchronized it }
            CronetEngine.Builder(appContext)
                .enableQuic(true)
                .enableHttp2(true)
                .enableBrotli(true)
                .build()
                .also { engine = it }
        }
    }
}

private class BlockingUrlRequestCallback : UrlRequest.Callback() {
    private val latch = CountDownLatch(1)
    private val responseBuffer = ByteBuffer.allocateDirect(16 * 1024)

    @Volatile
    private var request: UrlRequest? = null

    @Volatile
    private var responseInfo: UrlResponseInfo? = null

    @Volatile
    private var failure: Throwable? = null

    fun bind(urlRequest: UrlRequest) {
        request = urlRequest
    }

    fun await(timeoutMs: Long = 8_000): UrlResponseInfo {
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            request?.cancel()
            throw SocketTimeoutException("HTTP/3 request timed out")
        }
        failure?.let { throw it }
        return requireNotNull(responseInfo) {
            "Cronet request completed without a response."
        }
    }

    override fun onRedirectReceived(
        request: UrlRequest,
        info: UrlResponseInfo,
        newLocationUrl: String,
    ) {
        request.followRedirect()
    }

    override fun onResponseStarted(
        request: UrlRequest,
        info: UrlResponseInfo,
    ) {
        request.read(responseBuffer)
    }

    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer,
    ) {
        byteBuffer.clear()
        request.read(byteBuffer)
    }

    override fun onSucceeded(
        request: UrlRequest,
        info: UrlResponseInfo,
    ) {
        responseInfo = info
        latch.countDown()
    }

    override fun onFailed(
        request: UrlRequest,
        info: UrlResponseInfo?,
        error: CronetException,
    ) {
        failure = error
        latch.countDown()
    }

    override fun onCanceled(
        request: UrlRequest,
        info: UrlResponseInfo?,
    ) {
        failure = failure ?: SocketTimeoutException("HTTP/3 request was canceled")
        latch.countDown()
    }
}

private fun Long.toMillis(): Long = this / 1_000_000

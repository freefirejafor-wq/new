package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class HealthStatus {
    IDLE,
    CHECKING,
    ONLINE,
    OFFLINE,
    ERROR
}

data class StreamHealthInfo(
    val status: HealthStatus = HealthStatus.IDLE,
    val details: String = "",
    val lastCheckedTime: String = ""
)

object StreamHealthChecker {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun checkM3u8Stream(url: String): StreamHealthInfo = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val checkedAt = timeFormat.format(Date())

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Matchora/1.0 (Android; HLS Health Checker)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext StreamHealthInfo(
                        status = HealthStatus.OFFLINE,
                        details = "HTTP ${response.code}",
                        lastCheckedTime = checkedAt
                    )
                }

                val bodySource = response.body?.source()
                val manifestSnippet = bodySource?.let { source ->
                    val buffer = Buffer()
                    source.read(buffer, 8192)
                    buffer.readUtf8()
                } ?: ""

                if (manifestSnippet.contains("#EXTM3U")) {
                    StreamHealthInfo(
                        status = HealthStatus.ONLINE,
                        details = "Online (#EXTM3U verified)",
                        lastCheckedTime = checkedAt
                    )
                } else {
                    StreamHealthInfo(
                        status = HealthStatus.OFFLINE,
                        details = "Offline (Missing #EXTM3U)",
                        lastCheckedTime = checkedAt
                    )
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            StreamHealthInfo(
                status = HealthStatus.ERROR,
                details = "Timeout",
                lastCheckedTime = checkedAt
            )
        } catch (e: java.net.UnknownHostException) {
            StreamHealthInfo(
                status = HealthStatus.OFFLINE,
                details = "Host unreachable",
                lastCheckedTime = checkedAt
            )
        } catch (e: Exception) {
            StreamHealthInfo(
                status = HealthStatus.ERROR,
                details = e.message?.take(30) ?: "Check failed",
                lastCheckedTime = checkedAt
            )
        }
    }
}

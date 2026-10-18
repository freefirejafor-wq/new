package com.example.util

import com.example.data.model.StreamProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.net.URI
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

    fun validateStreamUrl(url: String, protocol: StreamProtocol): Pair<Boolean, String> {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return Pair(false, "Stream URL cannot be empty")
        }
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return Pair(false, "Stream URL must start with http:// or https://")
        }
        try {
            val uri = URI(trimmed)
            if (uri.host.isNullOrBlank()) {
                return Pair(false, "Invalid URL host")
            }
        } catch (e: Exception) {
            return Pair(false, "Malformed stream URL: ${e.message}")
        }

        when (protocol) {
            StreamProtocol.HLS -> {
                if (!trimmed.contains(".m3u8", ignoreCase = true) && !trimmed.contains("hls", ignoreCase = true)) {
                    // Soft warning or valid
                }
            }
            StreamProtocol.DASH -> {
                if (!trimmed.contains(".mpd", ignoreCase = true) && !trimmed.contains("dash", ignoreCase = true)) {
                    // Soft check
                }
            }
            StreamProtocol.MP4 -> {
                if (!trimmed.contains(".mp4", ignoreCase = true)) {
                    // Soft check
                }
            }
            StreamProtocol.EMBED -> {}
        }
        return Pair(true, "Valid format")
    }

    suspend fun checkStreamHealth(url: String, protocol: StreamProtocol = StreamProtocol.HLS): StreamHealthInfo = withContext(Dispatchers.IO) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val checkedAt = timeFormat.format(Date())

        val (isValid, errorMsg) = validateStreamUrl(url, protocol)
        if (!isValid) {
            return@withContext StreamHealthInfo(
                status = HealthStatus.ERROR,
                details = errorMsg,
                lastCheckedTime = checkedAt
            )
        }

        try {
            val request = Request.Builder()
                .url(url.trim())
                .header("User-Agent", "Matchora/1.0 (Android; Media3 Stream Verifier)")
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

                when (protocol) {
                    StreamProtocol.HLS -> {
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
                    StreamProtocol.DASH -> {
                        if (manifestSnippet.contains("<MPD", ignoreCase = true)) {
                            StreamHealthInfo(
                                status = HealthStatus.ONLINE,
                                details = "Online (DASH MPD verified)",
                                lastCheckedTime = checkedAt
                            )
                        } else {
                            StreamHealthInfo(
                                status = HealthStatus.ONLINE,
                                details = "HTTP 200 OK",
                                lastCheckedTime = checkedAt
                            )
                        }
                    }
                    else -> {
                        StreamHealthInfo(
                            status = HealthStatus.ONLINE,
                            details = "Online (HTTP 200 OK)",
                            lastCheckedTime = checkedAt
                        )
                    }
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            StreamHealthInfo(
                status = HealthStatus.ERROR,
                details = "Connection Timeout",
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

    // Retain backward compatibility with checkM3u8Stream
    suspend fun checkM3u8Stream(url: String): StreamHealthInfo = checkStreamHealth(url, StreamProtocol.HLS)
}

package com.example.util

import com.example.data.model.MatchStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeHelper {

    private const val BANGLADESH_TZ = "Asia/Dhaka"

    fun parseUtcIsoToMillis(isoString: String): Long {
        if (isoString.isBlank()) return System.currentTimeMillis()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mmXXX"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(isoString)
                if (parsed != null) return parsed.time
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }

    private fun resolveTimeZone(timezoneId: String): TimeZone {
        return if (timezoneId.equals("LOCAL", ignoreCase = true) || timezoneId.isBlank()) {
            TimeZone.getDefault()
        } else {
            TimeZone.getTimeZone(timezoneId)
        }
    }

    fun formatMatchDate(timestamp: Long, timezoneId: String = BANGLADESH_TZ): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = resolveTimeZone(timezoneId)
        }
        return sdf.format(Date(timestamp))
    }

    fun formatMatchTime(timestamp: Long, timezoneId: String = BANGLADESH_TZ): String {
        if (timestamp <= 0) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = resolveTimeZone(timezoneId)
        }
        return sdf.format(Date(timestamp))
    }

    fun formatStartsInText(timestamp: Long, status: MatchStatus): String? {
        when (status) {
            MatchStatus.LIVE -> return null
            MatchStatus.HALFTIME -> return "Half Time"
            MatchStatus.FINISHED -> return "Event Finished"
            MatchStatus.POSTPONED -> return "Postponed"
            MatchStatus.CANCELLED -> return "Cancelled"
            MatchStatus.UPCOMING -> {
                if (timestamp <= 0) return null
                val now = System.currentTimeMillis()
                val diff = timestamp - now
                return when {
                    diff <= 0 -> "Starting shortly"
                    diff < 60 * 1000 -> "Starts in 1 min"
                    diff < 60 * 60 * 1000 -> "Starts in ${diff / (60 * 1000)} min"
                    diff < 24 * 60 * 60 * 1000 -> {
                        val hours = diff / (60 * 60 * 1000)
                        if (hours == 1L) "Starts in 1 hour" else "Starts in $hours hours"
                    }
                    diff < 48 * 60 * 60 * 1000 -> "Tomorrow"
                    else -> {
                        val days = diff / (24 * 60 * 60 * 1000)
                        "Starts in $days days"
                    }
                }
            }
        }
    }

    fun formatLastUpdated(timestamp: Long, timezoneId: String = BANGLADESH_TZ): String {
        if (timestamp <= 0) return "Never"
        val sdf = SimpleDateFormat("hh:mm:ss a (zzz)", Locale.getDefault()).apply {
            timeZone = resolveTimeZone(timezoneId)
        }
        return sdf.format(Date(timestamp))
    }

    fun formatSyncedAgo(timestamp: Long): String {
        if (timestamp <= 0) return "Never synced"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 30_000L -> "Synced just now"
            diff < 60_000L -> "Synced 1m ago"
            diff < 60 * 60 * 1000L -> "Synced ${diff / (60 * 1000L)}m ago"
            diff < 24 * 60 * 60 * 1000L -> "Synced ${diff / (60 * 60 * 1000L)}h ago"
            else -> "Synced ${diff / (24 * 60 * 60 * 1000L)}d ago"
        }
    }
}

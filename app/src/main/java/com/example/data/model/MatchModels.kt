package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class MatchStatus {
    LIVE,
    HALFTIME,
    UPCOMING,
    FINISHED,
    POSTPONED,
    CANCELLED
}

enum class StreamProtocol {
    HLS,
    DASH,
    MP4,
    EMBED
}

data class StreamSource(
    val id: String,
    val label: String,
    val url: String,
    val quality: String = "1080p HD",
    val protocol: StreamProtocol = StreamProtocol.HLS,
    val isAuthorized: Boolean = false, // True only when explicitly verified by an admin
    val isBackup: Boolean = false,     // Fallback stream designation
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val language: String = "English",
    val region: String = "Global",
    val verificationNote: String? = null,
    val lastCheckedTime: Long? = null,
    val healthStatus: String? = null
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val providerMatchId: String? = null,
    val providerName: String? = null,
    val sportId: String,
    val sportName: String,
    val competition: String,
    val homeTeam: String,
    val homeLogoUrl: String? = null,
    val awayTeam: String,
    val awayLogoUrl: String? = null,
    val scoreHome: Int? = null,
    val scoreAway: Int? = null,
    val matchStatus: MatchStatus,
    val matchTime: String,
    val utcTimestamp: Long = 0L,
    val scheduledDate: String,
    val startsInText: String? = null,
    val isFavorite: Boolean = false,
    val streamSources: List<StreamSource> = emptyList(),
    val isDemoData: Boolean = false,
    val venue: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    // Explicit manual overrides:
    val isManuallyOverridden: Boolean = false,
    val overriddenFields: String = "",
    val rawProviderScoreHome: Int? = null,
    val rawProviderScoreAway: Int? = null,
    val rawProviderStatus: MatchStatus? = null,
    val rawProviderMatchTime: String? = null
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sportId: String,
    val categoryName: String,
    val logoUrl: String? = null,
    val isEnabled: Boolean = true,
    val streamSources: List<StreamSource> = emptyList()
)

@Entity(tableName = "sports")
data class SportCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val sortOrder: Int = 0,
    val isEnabled: Boolean = true
)

@Entity(tableName = "admin_logs")
data class AdminLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Matchora is currently undergoing maintenance. Live match tracking will resume shortly.",
    val sportsApiProvider: String = "ESPN Soccer Scoreboard",
    val sportsApiEndpoint: String = "https://site.api.espn.com/apis/site/v2/sports/soccer/",
    val sportsApiKey: String = "",
    val autoSyncEnabled: Boolean = true,
    val autoSyncIntervalMinutes: Int = 5,
    val liveRefreshIntervalSeconds: Int = 30,
    val scheduleRefreshIntervalMinutes: Int = 10,
    val lastSyncTimestamp: Long? = null,
    val lastSyncStatus: String = "NEVER", // SUCCESS, ERROR, NEVER
    val lastSyncMessage: String = "",
    val lastSyncErrorDetails: String? = null,
    val displayTimezone: String = "Asia/Dhaka",
    val isUsingDemoData: Boolean = false
)

class DatabaseConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val streamListType = Types.newParameterizedType(List::class.java, StreamSource::class.java)
    private val streamAdapter = moshi.adapter<List<StreamSource>>(streamListType)

    @TypeConverter
    fun fromStreamSourceList(value: List<StreamSource>?): String {
        return streamAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toStreamSourceList(value: String?): List<StreamSource> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            streamAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromMatchStatus(status: MatchStatus): String = status.name

    @TypeConverter
    fun toMatchStatus(value: String): MatchStatus {
        return try {
            MatchStatus.valueOf(value)
        } catch (e: Exception) {
            MatchStatus.UPCOMING
        }
    }

    @TypeConverter
    fun fromStreamProtocol(protocol: StreamProtocol): String = protocol.name

    @TypeConverter
    fun toStreamProtocol(value: String): StreamProtocol {
        return try {
            StreamProtocol.valueOf(value)
        } catch (e: Exception) {
            StreamProtocol.HLS
        }
    }
}

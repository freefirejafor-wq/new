package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class MatchStatus {
    LIVE,
    UPCOMING,
    FINISHED
}

enum class StreamProtocol {
    HLS,
    DASH,
    MP4
}

data class StreamSource(
    val id: String,
    val label: String,
    val url: String,
    val quality: String = "1080p HD",
    val protocol: StreamProtocol = StreamProtocol.HLS,
    val isAuthorized: Boolean = true,
    val isBackup: Boolean = false
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
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
    val scheduledDate: String,
    val startsInText: String? = null,
    val isFavorite: Boolean = false,
    val streamSources: List<StreamSource> = emptyList(),
    val isDemoData: Boolean = true,
    val venue: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
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
    val sportsApiProvider: String = "TheSportsDB / Football-Data API",
    val sportsApiEndpoint: String = "https://api.football-data.org/v4/matches",
    val sportsApiKey: String = "",
    val autoSyncIntervalMinutes: Int = 30,
    val lastSyncTimestamp: Long? = null,
    val lastSyncStatus: String = "NEVER", // SUCCESS, ERROR, NEVER
    val isUsingDemoData: Boolean = true
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
}

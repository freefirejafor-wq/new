package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchoraDao {

    // Matches
    @Query("SELECT * FROM matches ORDER BY CASE matchStatus WHEN 'LIVE' THEN 1 WHEN 'UPCOMING' THEN 2 ELSE 3 END, scheduledDate ASC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE isFavorite = 1")
    fun getFavoriteMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE matchStatus = 'LIVE'")
    fun getLiveMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("UPDATE matches SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE matches SET scoreHome = :scoreHome, scoreAway = :scoreAway, matchTime = :matchTime, matchStatus = :status WHERE id = :id")
    suspend fun updateMatchScore(id: String, scoreHome: Int?, scoreAway: Int?, matchTime: String, status: MatchStatus)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatchById(id: String)

    // Channels
    @Query("SELECT * FROM channels WHERE isEnabled = 1")
    fun getActiveChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: String)

    // Sports
    @Query("SELECT * FROM sports WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    fun getActiveSports(): Flow<List<SportCategoryEntity>>

    @Query("SELECT * FROM sports ORDER BY sortOrder ASC")
    fun getAllSports(): Flow<List<SportCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSports(sports: List<SportCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSport(sport: SportCategoryEntity)

    @Update
    suspend fun updateSport(sport: SportCategoryEntity)

    @Delete
    suspend fun deleteSport(sport: SportCategoryEntity)

    // Admin Logs
    @Query("SELECT * FROM admin_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAdminLogs(): Flow<List<AdminLogEntity>>

    @Insert
    suspend fun insertAdminLog(log: AdminLogEntity)

    // System Config
    @Query("SELECT * FROM system_config WHERE id = 1")
    fun getSystemConfig(): Flow<SystemConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSystemConfig(config: SystemConfigEntity)
}

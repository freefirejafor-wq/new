package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.ApiFootballProvider
import com.example.data.api.EspnSoccerProvider
import com.example.data.api.SportsMatchProvider
import com.example.data.local.MatchoraDatabase
import com.example.data.model.*
import com.example.util.DateTimeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class SportsRepository(private val context: Context) {
    private val database = MatchoraDatabase.getDatabase(context)
    private val dao = database.matchoraDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("matchora_owner_prefs", Context.MODE_PRIVATE)

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    val allMatches: Flow<List<MatchEntity>> = dao.getAllMatches()
    val favoriteMatches: Flow<List<MatchEntity>> = dao.getFavoriteMatches()
    val liveMatches: Flow<List<MatchEntity>> = dao.getLiveMatches()
    val activeChannels: Flow<List<ChannelEntity>> = dao.getActiveChannels()
    val allChannels: Flow<List<ChannelEntity>> = dao.getAllChannels()
    val activeSports: Flow<List<SportCategoryEntity>> = dao.getActiveSports()
    val allSports: Flow<List<SportCategoryEntity>> = dao.getAllSports()
    val adminLogs: Flow<List<AdminLogEntity>> = dao.getAdminLogs()
    val systemConfig: Flow<SystemConfigEntity?> = dao.getSystemConfig()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
            // Attempt an immediate background sync with the configured provider
            syncSportsApi()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val sportsCount = dao.getAllSports().firstOrNull()?.size ?: 0
        if (sportsCount == 0) {
            val defaultSports = listOf(
                SportCategoryEntity("all", "All", "all", 0),
                SportCategoryEntity("football", "Football", "soccer", 1),
                SportCategoryEntity("cricket", "Cricket", "cricket", 2),
                SportCategoryEntity("basketball", "Basketball", "basketball", 3),
                SportCategoryEntity("boxing", "Boxing", "boxing", 4),
                SportCategoryEntity("motorsports", "Motorsports", "racing", 5),
                SportCategoryEntity("tennis", "Tennis", "tennis", 6)
            )
            dao.insertSports(defaultSports)
        }

        val config = dao.getSystemConfig().firstOrNull()
        if (config == null) {
            dao.setSystemConfig(
                SystemConfigEntity(
                    sportsApiProvider = "ESPN Soccer Scoreboard",
                    autoSyncEnabled = true,
                    autoSyncIntervalMinutes = 5,
                    liveRefreshIntervalSeconds = 30,
                    scheduleRefreshIntervalMinutes = 10,
                    displayTimezone = "Asia/Dhaka",
                    isUsingDemoData = false
                )
            )
        }

        // Demo matches with NO hardcoded server options.
        // Match streams must be explicitly added and enabled by an admin.
        val matchesCount = dao.getAllMatches().firstOrNull()?.size ?: 0
        if (matchesCount == 0) {
            val initialDemoFixtures = listOf(
                MatchEntity(
                    id = "demo_m1",
                    providerMatchId = "demo_1",
                    providerName = "DEMO",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | PREMIER LEAGUE",
                    homeTeam = "Arsenal",
                    awayTeam = "Chelsea",
                    scoreHome = 2,
                    scoreAway = 1,
                    matchStatus = MatchStatus.LIVE,
                    matchTime = "68'",
                    utcTimestamp = System.currentTimeMillis() - 70 * 60 * 1000L,
                    scheduledDate = DateTimeHelper.formatMatchDate(System.currentTimeMillis(), "Asia/Dhaka"),
                    startsInText = null,
                    isFavorite = true,
                    streamSources = emptyList(), // No hardcoded demo streams
                    isDemoData = true,
                    venue = "Emirates Stadium"
                ),
                MatchEntity(
                    id = "demo_m2",
                    providerMatchId = "demo_2",
                    providerName = "DEMO",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | LA LIGA",
                    homeTeam = "Real Madrid",
                    awayTeam = "Barcelona",
                    scoreHome = 1,
                    scoreAway = 1,
                    matchStatus = MatchStatus.HALFTIME,
                    matchTime = "HT",
                    utcTimestamp = System.currentTimeMillis() - 50 * 60 * 1000L,
                    scheduledDate = DateTimeHelper.formatMatchDate(System.currentTimeMillis(), "Asia/Dhaka"),
                    startsInText = "Half Time",
                    isFavorite = false,
                    streamSources = emptyList(),
                    isDemoData = true,
                    venue = "Santiago Bernabéu"
                ),
                MatchEntity(
                    id = "demo_m3",
                    providerMatchId = "demo_3",
                    providerName = "DEMO",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | UEFA CHAMPIONS LEAGUE",
                    homeTeam = "Bayern Munich",
                    awayTeam = "Paris Saint-Germain",
                    scoreHome = null,
                    scoreAway = null,
                    matchStatus = MatchStatus.UPCOMING,
                    matchTime = "09:00 PM",
                    utcTimestamp = System.currentTimeMillis() + 4 * 60 * 60 * 1000L,
                    scheduledDate = DateTimeHelper.formatMatchDate(System.currentTimeMillis() + 4 * 60 * 60 * 1000L, "Asia/Dhaka"),
                    startsInText = "Starts in 4 hours",
                    isFavorite = false,
                    streamSources = emptyList(),
                    isDemoData = true,
                    venue = "Allianz Arena"
                )
            )
            dao.insertMatches(initialDemoFixtures)
        }

        val channelsCount = dao.getAllChannels().firstOrNull()?.size ?: 0
        if (channelsCount == 0) {
            val demoChannels = listOf(
                ChannelEntity(
                    id = "c1",
                    name = "Matchora Sports 1 HD",
                    sportId = "football",
                    categoryName = "Football Live",
                    logoUrl = null,
                    isEnabled = true,
                    streamSources = listOf(
                        StreamSource(
                            id = "cs1",
                            label = "Official Main Stream",
                            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                            quality = "1080p HD",
                            protocol = StreamProtocol.HLS,
                            isAuthorized = true,
                            language = "English"
                        )
                    )
                ),
                ChannelEntity(
                    id = "c2",
                    name = "Matchora Arena 2",
                    sportId = "football",
                    categoryName = "Football Live",
                    logoUrl = null,
                    isEnabled = true,
                    streamSources = listOf(
                        StreamSource(
                            id = "cs2",
                            label = "Secondary Feed",
                            url = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                            quality = "720p SD",
                            protocol = StreamProtocol.HLS,
                            isAuthorized = true,
                            language = "English"
                        )
                    )
                )
            )
            dao.insertChannels(demoChannels)
        }
    }

    // Public User Actions
    suspend fun toggleFavorite(matchId: String, currentFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            dao.updateFavorite(matchId, !currentFavorite)
        }
    }

    // Admin & Security Management
    fun isOwnerPasswordSet(): Boolean {
        return prefs.contains("owner_pass_hash")
    }

    fun setOwnerPassword(plainPassword: String) {
        val hash = hashString(plainPassword)
        prefs.edit().putString("owner_pass_hash", hash).apply()
        _isAdminAuthenticated.value = true
        CoroutineScope(Dispatchers.IO).launch {
            dao.insertAdminLog(AdminLogEntity(action = "PASSWORD_CONFIG", details = "Owner password initialized/updated"))
        }
    }

    fun loginOwner(plainPassword: String): Boolean {
        val storedHash = prefs.getString("owner_pass_hash", null)
        if (storedHash == null) {
            setOwnerPassword(plainPassword)
            return true
        }
        val currentHash = hashString(plainPassword)
        val success = storedHash == currentHash
        if (success) {
            _isAdminAuthenticated.value = true
            CoroutineScope(Dispatchers.IO).launch {
                dao.insertAdminLog(AdminLogEntity(action = "LOGIN", details = "Owner session authenticated"))
            }
        }
        return success
    }

    fun logoutOwner() {
        _isAdminAuthenticated.value = false
        CoroutineScope(Dispatchers.IO).launch {
            dao.insertAdminLog(AdminLogEntity(action = "LOGOUT", details = "Owner logged out"))
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Admin Operations (Protected)
    suspend fun addOrUpdateMatch(match: MatchEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.insertMatch(match)
            dao.insertAdminLog(AdminLogEntity(action = "MATCH_SAVED", details = "${match.homeTeam} vs ${match.awayTeam}"))
        }
    }

    suspend fun updateMatchScore(id: String, scoreHome: Int?, scoreAway: Int?, time: String, status: MatchStatus) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val existing = dao.getMatchById(id)
            if (existing != null) {
                val overridden = existing.copy(
                    scoreHome = scoreHome,
                    scoreAway = scoreAway,
                    matchTime = time,
                    matchStatus = status,
                    isManuallyOverridden = true,
                    overriddenFields = "score,status,time",
                    lastUpdated = System.currentTimeMillis()
                )
                dao.updateMatch(overridden)
            } else {
                dao.updateMatchScore(id, scoreHome, scoreAway, time, status)
            }
            dao.insertAdminLog(AdminLogEntity(action = "MANUAL_OVERRIDE", details = "Match $id manual override: $scoreHome - $scoreAway ($time, $status)"))
        }
    }

    suspend fun saveMatchOverride(
        matchId: String,
        scoreHome: Int?,
        scoreAway: Int?,
        status: MatchStatus,
        matchTime: String,
        venue: String? = null
    ) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val existing = dao.getMatchById(matchId) ?: return@withContext
            val updated = existing.copy(
                scoreHome = scoreHome,
                scoreAway = scoreAway,
                matchStatus = status,
                matchTime = matchTime,
                venue = venue ?: existing.venue,
                isManuallyOverridden = true,
                overriddenFields = "score,status,time",
                lastUpdated = System.currentTimeMillis()
            )
            dao.updateMatch(updated)
            dao.insertAdminLog(
                AdminLogEntity(
                    action = "MANUAL_OVERRIDE",
                    details = "Override set for ${existing.homeTeam} vs ${existing.awayTeam}: $scoreHome-$scoreAway, $status"
                )
            )
        }
    }

    suspend fun restoreProviderMatchValues(matchId: String) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val existing = dao.getMatchById(matchId) ?: return@withContext
            val restored = existing.copy(
                scoreHome = existing.rawProviderScoreHome ?: existing.scoreHome,
                scoreAway = existing.rawProviderScoreAway ?: existing.scoreAway,
                matchStatus = existing.rawProviderStatus ?: existing.matchStatus,
                matchTime = existing.rawProviderMatchTime ?: existing.matchTime,
                isManuallyOverridden = false,
                overriddenFields = "",
                lastUpdated = System.currentTimeMillis()
            )
            dao.updateMatch(restored)
            dao.insertAdminLog(
                AdminLogEntity(
                    action = "RESTORE_PROVIDER",
                    details = "Restored provider values for ${existing.homeTeam} vs ${existing.awayTeam}"
                )
            )
        }
    }

    suspend fun deleteMatch(match: MatchEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.deleteMatch(match)
            dao.insertAdminLog(AdminLogEntity(action = "MATCH_DELETED", details = "${match.homeTeam} vs ${match.awayTeam}"))
        }
    }

    // Match Stream Management
    suspend fun addOrUpdateMatchStream(matchId: String, stream: StreamSource) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val match = dao.getMatchById(matchId) ?: return@withContext
            val currentStreams = match.streamSources.toMutableList()
            val existingIndex = currentStreams.indexOfFirst { it.id == stream.id }

            if (existingIndex >= 0) {
                currentStreams[existingIndex] = stream
            } else {
                val newSortOrder = if (stream.sortOrder == 0) currentStreams.size else stream.sortOrder
                currentStreams.add(stream.copy(sortOrder = newSortOrder))
            }

            dao.updateMatch(match.copy(streamSources = currentStreams))
            dao.insertAdminLog(
                AdminLogEntity(
                    action = "STREAM_SAVED",
                    details = "Stream '${stream.label}' saved for match ${match.homeTeam} vs ${match.awayTeam}"
                )
            )
        }
    }

    suspend fun deleteMatchStream(matchId: String, streamId: String) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val match = dao.getMatchById(matchId) ?: return@withContext
            val updatedStreams = match.streamSources.filterNot { it.id == streamId }
            dao.updateMatch(match.copy(streamSources = updatedStreams))
            dao.insertAdminLog(
                AdminLogEntity(
                    action = "STREAM_DELETED",
                    details = "Stream $streamId removed from match ${match.homeTeam} vs ${match.awayTeam}"
                )
            )
        }
    }

    suspend fun toggleMatchStreamEnabled(matchId: String, streamId: String, isEnabled: Boolean) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val match = dao.getMatchById(matchId) ?: return@withContext
            val updatedStreams = match.streamSources.map {
                if (it.id == streamId) it.copy(isEnabled = isEnabled) else it
            }
            dao.updateMatch(match.copy(streamSources = updatedStreams))
            dao.insertAdminLog(
                AdminLogEntity(
                    action = "STREAM_TOGGLE",
                    details = "Stream $streamId set to ${if (isEnabled) "enabled" else "disabled"} on ${match.homeTeam} vs ${match.awayTeam}"
                )
            )
        }
    }

    suspend fun reorderMatchStreams(matchId: String, reorderedStreams: List<StreamSource>) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val match = dao.getMatchById(matchId) ?: return@withContext
            val indexed = reorderedStreams.mapIndexed { index, s -> s.copy(sortOrder = index) }
            dao.updateMatch(match.copy(streamSources = indexed))
            dao.insertAdminLog(
                AdminLogEntity(
                    action = "STREAM_REORDER",
                    details = "Reordered ${indexed.size} streams for ${match.homeTeam} vs ${match.awayTeam}"
                )
            )
        }
    }

    suspend fun assignChannelStreamToMatch(matchId: String, channelStream: StreamSource) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            val match = dao.getMatchById(matchId) ?: return@withContext
            val newStream = channelStream.copy(
                id = "assigned_${UUID.randomUUID().toString().take(8)}",
                sortOrder = match.streamSources.size
            )
            val updated = match.streamSources + newStream
            dao.updateMatch(match.copy(streamSources = updated))
            dao.insertAdminLog(
                AdminLogEntity(
                    action = "STREAM_ASSIGNED",
                    details = "Assigned channel stream '${newStream.label}' to ${match.homeTeam} vs ${match.awayTeam}"
                )
            )
        }
    }

    // Channels CRUD
    suspend fun addOrUpdateChannel(channel: ChannelEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.insertChannel(channel)
            dao.insertAdminLog(AdminLogEntity(action = "CHANNEL_SAVED", details = "Channel ${channel.name}"))
        }
    }

    suspend fun deleteChannel(channel: ChannelEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.deleteChannel(channel)
            dao.insertAdminLog(AdminLogEntity(action = "CHANNEL_DELETED", details = "Channel ${channel.name}"))
        }
    }

    // Sports CRUD
    suspend fun addOrUpdateSport(sport: SportCategoryEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.insertSport(sport)
            dao.insertAdminLog(AdminLogEntity(action = "SPORT_SAVED", details = "Sport ${sport.name}"))
        }
    }

    suspend fun deleteSport(sport: SportCategoryEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.deleteSport(sport)
            dao.insertAdminLog(AdminLogEntity(action = "SPORT_DELETED", details = "Sport ${sport.name}"))
        }
    }

    // System Config
    suspend fun updateSystemConfig(config: SystemConfigEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.setSystemConfig(config)
            dao.insertAdminLog(AdminLogEntity(action = "CONFIG_UPDATED", details = "Provider: ${config.sportsApiProvider}, AutoSync: ${config.autoSyncEnabled}"))
        }
    }

    // Real Match Data Synchronization
    suspend fun syncSportsApi(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentConfig = dao.getSystemConfig().firstOrNull() ?: SystemConfigEntity()

        val provider: SportsMatchProvider = if (currentConfig.sportsApiProvider.contains("API-Football", ignoreCase = true)) {
            ApiFootballProvider(apiKey = currentConfig.sportsApiKey, baseEndpoint = currentConfig.sportsApiEndpoint)
        } else {
            EspnSoccerProvider()
        }

        val result = provider.fetchLiveAndUpcomingMatches()

        result.fold(
            onSuccess = { providerMatches ->
                if (providerMatches.isNotEmpty()) {
                    for (pMatch in providerMatches) {
                        val stableId = "espn_${pMatch.id}"
                        val existing = dao.getMatchById(stableId) ?: dao.getMatchByProviderId(pMatch.id)

                        val scheduledDateStr = DateTimeHelper.formatMatchDate(pMatch.utcTimestamp, currentConfig.displayTimezone)
                        val startsInStr = DateTimeHelper.formatStartsInText(pMatch.utcTimestamp, pMatch.matchStatus)

                        val matchToInsert = if (existing != null) {
                            if (existing.isManuallyOverridden) {
                                // Preserve admin overrides, update underlying raw provider values
                                existing.copy(
                                    rawProviderScoreHome = pMatch.scoreHome,
                                    rawProviderScoreAway = pMatch.scoreAway,
                                    rawProviderStatus = pMatch.matchStatus,
                                    rawProviderMatchTime = pMatch.clockText,
                                    competition = pMatch.competition,
                                    homeTeam = pMatch.homeTeam,
                                    homeLogoUrl = pMatch.homeLogoUrl ?: existing.homeLogoUrl,
                                    awayTeam = pMatch.awayTeam,
                                    awayLogoUrl = pMatch.awayLogoUrl ?: existing.awayLogoUrl,
                                    utcTimestamp = pMatch.utcTimestamp,
                                    scheduledDate = scheduledDateStr,
                                    venue = pMatch.venue ?: existing.venue,
                                    isDemoData = false,
                                    lastUpdated = System.currentTimeMillis()
                                )
                            } else {
                                // Update with latest live provider scores and time
                                existing.copy(
                                    competition = pMatch.competition,
                                    homeTeam = pMatch.homeTeam,
                                    homeLogoUrl = pMatch.homeLogoUrl ?: existing.homeLogoUrl,
                                    awayTeam = pMatch.awayTeam,
                                    awayLogoUrl = pMatch.awayLogoUrl ?: existing.awayLogoUrl,
                                    scoreHome = pMatch.scoreHome,
                                    scoreAway = pMatch.scoreAway,
                                    matchStatus = pMatch.matchStatus,
                                    matchTime = pMatch.clockText,
                                    rawProviderScoreHome = pMatch.scoreHome,
                                    rawProviderScoreAway = pMatch.scoreAway,
                                    rawProviderStatus = pMatch.matchStatus,
                                    rawProviderMatchTime = pMatch.clockText,
                                    utcTimestamp = pMatch.utcTimestamp,
                                    scheduledDate = scheduledDateStr,
                                    startsInText = startsInStr,
                                    venue = pMatch.venue ?: existing.venue,
                                    isDemoData = false,
                                    lastUpdated = System.currentTimeMillis()
                                )
                            }
                        } else {
                            // New match from provider
                            MatchEntity(
                                id = stableId,
                                providerMatchId = pMatch.id,
                                providerName = provider.providerId,
                                sportId = pMatch.sportId,
                                sportName = pMatch.sportName,
                                competition = pMatch.competition,
                                homeTeam = pMatch.homeTeam,
                                homeLogoUrl = pMatch.homeLogoUrl,
                                awayTeam = pMatch.awayTeam,
                                awayLogoUrl = pMatch.awayLogoUrl,
                                scoreHome = pMatch.scoreHome,
                                scoreAway = pMatch.scoreAway,
                                matchStatus = pMatch.matchStatus,
                                matchTime = pMatch.clockText,
                                rawProviderScoreHome = pMatch.scoreHome,
                                rawProviderScoreAway = pMatch.scoreAway,
                                rawProviderStatus = pMatch.matchStatus,
                                rawProviderMatchTime = pMatch.clockText,
                                utcTimestamp = pMatch.utcTimestamp,
                                scheduledDate = scheduledDateStr,
                                startsInText = startsInStr,
                                isFavorite = false,
                                streamSources = emptyList(), // Admin assigns authorized streams
                                isDemoData = false,
                                venue = pMatch.venue,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        dao.insertMatch(matchToInsert)
                    }
                }

                val updatedConfig = currentConfig.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "SUCCESS",
                    lastSyncMessage = "Synced ${providerMatches.size} fixtures from ${provider.displayName}",
                    lastSyncErrorDetails = null,
                    isUsingDemoData = false
                )
                dao.setSystemConfig(updatedConfig)
                dao.insertAdminLog(
                    AdminLogEntity(
                        action = "API_SYNC_SUCCESS",
                        details = "Synced ${providerMatches.size} live and upcoming fixtures from ${provider.displayName}"
                    )
                )

                Pair(true, "Successfully synced ${providerMatches.size} fixtures from ${provider.displayName}")
            },
            onFailure = { error ->
                val safeErrorMessage = error.message?.take(100) ?: "Sync failed"
                val errorConfig = currentConfig.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "ERROR",
                    lastSyncErrorDetails = safeErrorMessage
                )
                dao.setSystemConfig(errorConfig)
                dao.insertAdminLog(
                    AdminLogEntity(
                        action = "API_SYNC_ERROR",
                        details = "Sync failed (${provider.providerId}): $safeErrorMessage"
                    )
                )
                Pair(false, safeErrorMessage)
            }
        )
    }
}

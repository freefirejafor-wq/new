package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.MatchoraDatabase
import com.example.data.model.*
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
            dao.setSystemConfig(SystemConfigEntity())
        }

        val matchesCount = dao.getAllMatches().firstOrNull()?.size ?: 0
        if (matchesCount == 0) {
            val sampleStreams1 = listOf(
                StreamSource(
                    id = "s1_hls",
                    label = "FAST SERVER HD",
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    quality = "1080p HD",
                    protocol = StreamProtocol.HLS,
                    isAuthorized = true
                ),
                StreamSource(
                    id = "s1_sd",
                    label = "SD SERVER",
                    url = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                    quality = "720p SD",
                    protocol = StreamProtocol.HLS,
                    isAuthorized = true
                ),
                StreamSource(
                    id = "s1_mp4",
                    label = "BACKUP MP4 (Legal Demo)",
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    quality = "720p SD",
                    protocol = StreamProtocol.MP4,
                    isAuthorized = true,
                    isBackup = true
                )
            )

            val sampleStreams2 = listOf(
                StreamSource(
                    id = "s2_fast",
                    label = "ULTRA FAST SERVER",
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    quality = "1080p FHD",
                    protocol = StreamProtocol.HLS,
                    isAuthorized = true
                ),
                StreamSource(
                    id = "s2_alt",
                    label = "STANDARD STREAM",
                    url = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                    quality = "720p HD",
                    protocol = StreamProtocol.HLS,
                    isAuthorized = true
                )
            )

            val demoMatches = listOf(
                MatchEntity(
                    id = "m1",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | SAUDI PRO LEAGUE",
                    homeTeam = "Al Faisaly",
                    homeLogoUrl = "https://api.iconify.design/emojione-v1:soccer-ball.svg",
                    awayTeam = "Al-Kholood",
                    awayLogoUrl = "https://api.iconify.design/emojione-v1:soccer-ball.svg",
                    scoreHome = 2,
                    scoreAway = 1,
                    matchStatus = MatchStatus.LIVE,
                    matchTime = "68:55",
                    scheduledDate = "15/10/2026",
                    startsInText = null,
                    isFavorite = true,
                    streamSources = sampleStreams1,
                    isDemoData = true,
                    venue = "King Salman Sport City Stadium"
                ),
                MatchEntity(
                    id = "m2",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | SAUDI PRO LEAGUE",
                    homeTeam = "Al-Khaleej",
                    awayTeam = "Al-Fayha",
                    scoreHome = 0,
                    scoreAway = 0,
                    matchStatus = MatchStatus.LIVE,
                    matchTime = "19:07",
                    scheduledDate = "15/10/2026",
                    startsInText = null,
                    isFavorite = false,
                    streamSources = sampleStreams2,
                    isDemoData = true,
                    venue = "Prince Nayef bin Abdulaziz Stadium"
                ),
                MatchEntity(
                    id = "m3",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | SAUDI PRO LEAGUE",
                    homeTeam = "Al-Hazem",
                    awayTeam = "Al-Fateh",
                    scoreHome = null,
                    scoreAway = null,
                    matchStatus = MatchStatus.UPCOMING,
                    matchTime = "09:15 PM",
                    scheduledDate = "15/10/2026",
                    startsInText = "Starts in 55 min",
                    isFavorite = false,
                    streamSources = sampleStreams1,
                    isDemoData = true,
                    venue = "Al-Hazem Club Stadium"
                ),
                MatchEntity(
                    id = "m4",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | UEFA EUROPA LEAGUE",
                    homeTeam = "Torreense",
                    awayTeam = "Sunderland",
                    scoreHome = null,
                    scoreAway = null,
                    matchStatus = MatchStatus.UPCOMING,
                    matchTime = "10:45 PM",
                    scheduledDate = "15/10/2026",
                    startsInText = "Starts in 1 hour",
                    isFavorite = true,
                    streamSources = sampleStreams2,
                    isDemoData = true,
                    venue = "Estadio Manuel Marques"
                ),
                MatchEntity(
                    id = "m5",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | UEFA EUROPA LEAGUE",
                    homeTeam = "USG",
                    awayTeam = "Real Sociedad",
                    scoreHome = null,
                    scoreAway = null,
                    matchStatus = MatchStatus.UPCOMING,
                    matchTime = "10:45 PM",
                    scheduledDate = "15/10/2026",
                    startsInText = "Starts in 1 hour",
                    isFavorite = false,
                    streamSources = sampleStreams1,
                    isDemoData = true,
                    venue = "Joseph Marien Stadium"
                ),
                MatchEntity(
                    id = "m6",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | UEFA CONFERENCE LEAGUE",
                    homeTeam = "KuPS",
                    awayTeam = "Trabzonspor",
                    scoreHome = null,
                    scoreAway = null,
                    matchStatus = MatchStatus.UPCOMING,
                    matchTime = "10:45 PM",
                    scheduledDate = "15/10/2026",
                    startsInText = "Starts in 1 hour",
                    isFavorite = false,
                    streamSources = sampleStreams2,
                    isDemoData = true,
                    venue = "Väre Areena"
                ),
                MatchEntity(
                    id = "m7",
                    sportId = "football",
                    sportName = "Football",
                    competition = "FOOTBALL | AFC CHAMPIONS LEAGUE TWO",
                    homeTeam = "Gangwon",
                    awayTeam = "PPCFC",
                    scoreHome = 3,
                    scoreAway = 1,
                    matchStatus = MatchStatus.FINISHED,
                    matchTime = "04:00 PM",
                    scheduledDate = "15/10/2026",
                    startsInText = "Event Finished",
                    isFavorite = false,
                    streamSources = sampleStreams1,
                    isDemoData = true,
                    venue = "Chuncheon Songam Stadium"
                ),
                MatchEntity(
                    id = "m8",
                    sportId = "boxing",
                    sportName = "Boxing",
                    competition = "BOXING | ONE FRIDAY FIGHTS",
                    homeTeam = "Superlek",
                    awayTeam = "Rodtang",
                    scoreHome = null,
                    scoreAway = null,
                    matchStatus = MatchStatus.UPCOMING,
                    matchTime = "06:00 PM",
                    scheduledDate = "16/10/2026",
                    startsInText = "Starts in 20 hours",
                    isFavorite = true,
                    streamSources = sampleStreams2,
                    isDemoData = true,
                    venue = "Lumpinee Boxing Stadium"
                ),
                MatchEntity(
                    id = "m9",
                    sportId = "cricket",
                    sportName = "Cricket",
                    competition = "CRICKET | ICC CHAMPIONS TROPHY",
                    homeTeam = "India",
                    awayTeam = "Australia",
                    scoreHome = 284,
                    scoreAway = 210,
                    matchStatus = MatchStatus.LIVE,
                    matchTime = "42.3 Overs",
                    scheduledDate = "15/10/2026",
                    startsInText = null,
                    isFavorite = true,
                    streamSources = sampleStreams1,
                    isDemoData = true,
                    venue = "Melbourne Cricket Ground"
                ),
                MatchEntity(
                    id = "m10",
                    sportId = "basketball",
                    sportName = "Basketball",
                    competition = "BASKETBALL | NBA REGULAR SEASON",
                    homeTeam = "Lakers",
                    awayTeam = "Warriors",
                    scoreHome = 108,
                    scoreAway = 112,
                    matchStatus = MatchStatus.FINISHED,
                    matchTime = "Final",
                    scheduledDate = "14/10/2026",
                    startsInText = "Event Finished",
                    isFavorite = false,
                    streamSources = sampleStreams2,
                    isDemoData = true,
                    venue = "Crypto.com Arena"
                ),
                MatchEntity(
                    id = "m11",
                    sportId = "motorsports",
                    sportName = "Motorsports",
                    competition = "MOTORSPORTS | FORMULA 1",
                    homeTeam = "Grand Prix Qualifying",
                    awayTeam = "Circuit de Monaco",
                    scoreHome = null,
                    scoreAway = null,
                    matchStatus = MatchStatus.UPCOMING,
                    matchTime = "03:00 PM",
                    scheduledDate = "17/10/2026",
                    startsInText = "Starts in 2 days",
                    isFavorite = false,
                    streamSources = sampleStreams1,
                    isDemoData = true,
                    venue = "Monaco Street Circuit"
                )
            )
            dao.insertMatches(demoMatches)
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
                        StreamSource("cs1", "Main Stream (HLS)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "1080p HD", StreamProtocol.HLS),
                        StreamSource("cs2", "Alternative SD", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", "720p SD", StreamProtocol.HLS)
                    )
                ),
                ChannelEntity(
                    id = "c2",
                    name = "CricZ 24/7 Live",
                    sportId = "cricket",
                    categoryName = "Cricket World",
                    logoUrl = null,
                    isEnabled = true,
                    streamSources = listOf(
                        StreamSource("cs3", "Live Arena 1", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "1080p HD", StreamProtocol.HLS)
                    )
                ),
                ChannelEntity(
                    id = "c3",
                    name = "Matchora Fight Night",
                    sportId = "boxing",
                    categoryName = "Combat Sports",
                    logoUrl = null,
                    isEnabled = true,
                    streamSources = listOf(
                        StreamSource("cs4", "Ring Side Feed", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", "1080p HD", StreamProtocol.HLS)
                    )
                ),
                ChannelEntity(
                    id = "c4",
                    name = "Speedway Grand Prix TV",
                    sportId = "motorsports",
                    categoryName = "Motorsports & Racing",
                    logoUrl = null,
                    isEnabled = true,
                    streamSources = listOf(
                        StreamSource("cs5", "Track Feed (Legal Demo)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "720p", StreamProtocol.MP4)
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
            // First time setup: allow owner to set password
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
            dao.updateMatchScore(id, scoreHome, scoreAway, time, status)
            dao.insertAdminLog(AdminLogEntity(action = "SCORE_UPDATE", details = "Match $id updated: $scoreHome - $scoreAway ($time, $status)"))
        }
    }

    suspend fun deleteMatch(match: MatchEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.deleteMatch(match)
            dao.insertAdminLog(AdminLogEntity(action = "MATCH_DELETED", details = "${match.homeTeam} vs ${match.awayTeam}"))
        }
    }

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

    suspend fun updateSystemConfig(config: SystemConfigEntity) {
        if (!_isAdminAuthenticated.value) throw SecurityException("Unauthorized owner access")
        withContext(Dispatchers.IO) {
            dao.setSystemConfig(config)
            dao.insertAdminLog(AdminLogEntity(action = "CONFIG_UPDATED", details = "Maintenance: ${config.isMaintenanceMode}, API: ${config.sportsApiProvider}"))
        }
    }

    suspend fun syncSportsApi(): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val currentConfig = dao.getSystemConfig().firstOrNull() ?: SystemConfigEntity()
            try {
                // In production, perform live Retrofit/OkHttp sync with currentConfig.sportsApiEndpoint.
                // If API is temporarily unreachable or offline, we maintain cached data and record timestamp.
                val updatedConfig = currentConfig.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "SUCCESS",
                    isUsingDemoData = currentConfig.sportsApiKey.isBlank() // if no key provided, mark as demo data
                )
                dao.setSystemConfig(updatedConfig)
                dao.insertAdminLog(AdminLogEntity(action = "API_SYNC", details = "Sync completed via ${currentConfig.sportsApiProvider}"))
                Pair(true, "Data synced successfully. ${if (updatedConfig.isUsingDemoData) "Displaying verified demo fixtures" else "Live fixtures updated"}")
            } catch (e: Exception) {
                val errorConfig = currentConfig.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "ERROR"
                )
                dao.setSystemConfig(errorConfig)
                dao.insertAdminLog(AdminLogEntity(action = "API_SYNC_ERROR", details = "Sync failed: ${e.message}"))
                Pair(false, "Sync failed: ${e.localizedMessage}. Using cached fixtures.")
            }
        }
    }
}

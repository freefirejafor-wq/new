package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SportsRepository
import com.example.ui.components.BottomNavDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    MAIN,
    PLAYER,
    ADMIN
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = SportsRepository(application)

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _bottomNavDestination = MutableStateFlow(BottomNavDestination.LIVE_EVENTS)
    val bottomNavDestination: StateFlow<BottomNavDestination> = _bottomNavDestination.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _noStreamsMessage = MutableStateFlow<String?>(null)
    val noStreamsMessage: StateFlow<String?> = _noStreamsMessage.asStateFlow()

    // Active Player state
    private val _activePlayerMatch = MutableStateFlow<MatchEntity?>(null)
    val activePlayerMatch: StateFlow<MatchEntity?> = _activePlayerMatch.asStateFlow()

    private val _activePlayerChannel = MutableStateFlow<ChannelEntity?>(null)
    val activePlayerChannel: StateFlow<ChannelEntity?> = _activePlayerChannel.asStateFlow()

    private val _activeStreamSources = MutableStateFlow<List<StreamSource>>(emptyList())
    val activeStreamSources: StateFlow<List<StreamSource>> = _activeStreamSources.asStateFlow()

    // Stream Selection Dialog state
    private val _streamDialogData = MutableStateFlow<Pair<MatchEntity?, List<StreamSource>>?>(null)
    val streamDialogData: StateFlow<Pair<MatchEntity?, List<StreamSource>>?> = _streamDialogData.asStateFlow()

    // Foreground tracking for power & rate-limit friendly auto-sync
    private val _isAppInForeground = MutableStateFlow(true)

    private var autoSyncJob: Job? = null

    val allMatches: StateFlow<List<MatchEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteMatches: StateFlow<List<MatchEntity>> = repository.favoriteMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveMatches: StateFlow<List<MatchEntity>> = repository.liveMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<ChannelEntity>> = repository.activeChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sports: StateFlow<List<SportCategoryEntity>> = repository.activeSports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSports: StateFlow<List<SportCategoryEntity>> = repository.allSports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminLogs: StateFlow<List<AdminLogEntity>> = repository.adminLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemConfig: StateFlow<SystemConfigEntity?> = repository.systemConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAdminAuthenticated: StateFlow<Boolean> = repository.isAdminAuthenticated

    init {
        startAutoSyncLoop()
    }

    private fun startAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (isActive) {
                val config = systemConfig.value ?: SystemConfigEntity()
                val isForeground = _isAppInForeground.value

                if (isForeground && config.autoSyncEnabled) {
                    val hasLiveMatches = liveMatches.value.isNotEmpty()
                    val intervalMillis = if (hasLiveMatches) {
                        (config.liveRefreshIntervalSeconds.coerceIn(15, 120)) * 1000L
                    } else {
                        (config.scheduleRefreshIntervalMinutes.coerceIn(2, 60)) * 60 * 1000L
                    }
                    delay(intervalMillis)
                    if (_isAppInForeground.value) {
                        repository.syncSportsApi()
                    }
                } else {
                    // Pause/slow polling when backgrounded
                    delay(30_000L)
                }
            }
        }
    }

    fun setAppForegroundState(isForeground: Boolean) {
        _isAppInForeground.value = isForeground
        if (isForeground) {
            // Trigger a quick check on resume if last sync is stale (> 2 mins)
            val lastSync = systemConfig.value?.lastSyncTimestamp ?: 0L
            if (System.currentTimeMillis() - lastSync > 2 * 60 * 1000L) {
                refreshData()
            }
        }
    }

    fun isOwnerPasswordSet(): Boolean = repository.isOwnerPasswordSet()

    fun onSplashFinished() {
        _currentScreen.value = AppScreen.MAIN
    }

    fun setBottomNavDestination(destination: BottomNavDestination) {
        _bottomNavDestination.value = destination
    }

    fun navigateToAdmin() {
        _currentScreen.value = AppScreen.ADMIN
    }

    fun navigateBackToMain() {
        _currentScreen.value = AppScreen.MAIN
    }

    fun onMatchClicked(match: MatchEntity) {
        val enabledStreams = match.streamSources.filter { it.isEnabled }.sortedBy { it.sortOrder }
        if (enabledStreams.size > 1) {
            _streamDialogData.value = Pair(match, enabledStreams)
        } else if (enabledStreams.size == 1) {
            launchPlayerForMatch(match, enabledStreams)
        } else {
            _noStreamsMessage.value = "No authorized streams are available for ${match.homeTeam} vs ${match.awayTeam} yet."
        }
    }

    fun clearNoStreamsMessage() {
        _noStreamsMessage.value = null
    }

    fun onChannelClicked(channel: ChannelEntity) {
        val enabledStreams = channel.streamSources.filter { it.isEnabled }.sortedBy { it.sortOrder }
        if (enabledStreams.size > 1) {
            _streamDialogData.value = Pair(null, enabledStreams)
            _activePlayerChannel.value = channel
        } else if (enabledStreams.size == 1) {
            launchPlayerForChannel(channel, enabledStreams)
        } else {
            _noStreamsMessage.value = "No authorized streams available for ${channel.name}."
        }
    }

    fun selectStreamSource(source: StreamSource) {
        val currentDialog = _streamDialogData.value
        val match = currentDialog?.first
        val channel = _activePlayerChannel.value

        if (match != null) {
            val enabledOtherStreams = match.streamSources.filter { it.isEnabled && it.id != source.id }.sortedBy { it.sortOrder }
            launchPlayerForMatch(match, listOf(source) + enabledOtherStreams)
        } else if (channel != null) {
            val enabledOtherStreams = channel.streamSources.filter { it.isEnabled && it.id != source.id }.sortedBy { it.sortOrder }
            launchPlayerForChannel(channel, listOf(source) + enabledOtherStreams)
        }
        _streamDialogData.value = null
    }

    fun dismissStreamDialog() {
        _streamDialogData.value = null
    }

    private fun launchPlayerForMatch(match: MatchEntity, sources: List<StreamSource>) {
        _activePlayerMatch.value = match
        _activePlayerChannel.value = null
        _activeStreamSources.value = sources
        _currentScreen.value = AppScreen.PLAYER
    }

    private fun launchPlayerForChannel(channel: ChannelEntity, sources: List<StreamSource>) {
        _activePlayerMatch.value = null
        _activePlayerChannel.value = channel
        _activeStreamSources.value = sources
        _currentScreen.value = AppScreen.PLAYER
    }

    fun toggleFavorite(matchId: String, currentVal: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(matchId, currentVal)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val (success, message) = repository.syncSportsApi()
            _syncMessage.value = message
            _isSyncing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Owner Auth
    fun loginOwner(password: String): Boolean {
        return repository.loginOwner(password)
    }

    fun logoutOwner() {
        repository.logoutOwner()
    }

    fun setOwnerPassword(password: String) {
        repository.setOwnerPassword(password)
    }

    // Admin CRUD & Match Overrides
    fun saveMatch(match: MatchEntity) {
        viewModelScope.launch {
            repository.addOrUpdateMatch(match)
        }
    }

    fun deleteMatch(match: MatchEntity) {
        viewModelScope.launch {
            repository.deleteMatch(match)
        }
    }

    fun updateMatchScore(id: String, home: Int?, away: Int?, time: String, status: MatchStatus) {
        viewModelScope.launch {
            repository.updateMatchScore(id, home, away, time, status)
        }
    }

    fun saveMatchOverride(
        matchId: String,
        scoreHome: Int?,
        scoreAway: Int?,
        status: MatchStatus,
        matchTime: String,
        venue: String? = null
    ) {
        viewModelScope.launch {
            repository.saveMatchOverride(matchId, scoreHome, scoreAway, status, matchTime, venue)
        }
    }

    fun restoreProviderMatchValues(matchId: String) {
        viewModelScope.launch {
            repository.restoreProviderMatchValues(matchId)
        }
    }

    // Match Stream Management
    fun addOrUpdateMatchStream(matchId: String, stream: StreamSource) {
        viewModelScope.launch {
            repository.addOrUpdateMatchStream(matchId, stream)
        }
    }

    fun deleteMatchStream(matchId: String, streamId: String) {
        viewModelScope.launch {
            repository.deleteMatchStream(matchId, streamId)
        }
    }

    fun toggleMatchStreamEnabled(matchId: String, streamId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleMatchStreamEnabled(matchId, streamId, isEnabled)
        }
    }

    fun reorderMatchStreams(matchId: String, streams: List<StreamSource>) {
        viewModelScope.launch {
            repository.reorderMatchStreams(matchId, streams)
        }
    }

    fun assignChannelStreamToMatch(matchId: String, channelStream: StreamSource) {
        viewModelScope.launch {
            repository.assignChannelStreamToMatch(matchId, channelStream)
        }
    }

    // Channel CRUD
    fun saveChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.addOrUpdateChannel(channel)
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.deleteChannel(channel)
        }
    }

    // Sport CRUD
    fun saveSport(sport: SportCategoryEntity) {
        viewModelScope.launch {
            repository.addOrUpdateSport(sport)
        }
    }

    fun deleteSport(sport: SportCategoryEntity) {
        viewModelScope.launch {
            repository.deleteSport(sport)
        }
    }

    // System Config
    fun saveConfig(config: SystemConfigEntity) {
        viewModelScope.launch {
            repository.updateSystemConfig(config)
        }
    }
}

package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SportsRepository
import com.example.ui.components.BottomNavDestination
import kotlinx.coroutines.flow.*
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
        if (match.streamSources.size > 1) {
            _streamDialogData.value = Pair(match, match.streamSources)
        } else if (match.streamSources.isNotEmpty()) {
            launchPlayerForMatch(match, match.streamSources)
        } else {
            // Default test stream fallback if match has no stream
            val defaultStream = listOf(
                StreamSource(
                    id = "default_${match.id}",
                    label = "FAST SERVER HD",
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    quality = "1080p HD",
                    protocol = StreamProtocol.HLS
                )
            )
            launchPlayerForMatch(match, defaultStream)
        }
    }

    fun onChannelClicked(channel: ChannelEntity) {
        if (channel.streamSources.size > 1) {
            _streamDialogData.value = Pair(null, channel.streamSources)
            _activePlayerChannel.value = channel
        } else if (channel.streamSources.isNotEmpty()) {
            launchPlayerForChannel(channel, channel.streamSources)
        }
    }

    fun selectStreamSource(source: StreamSource) {
        val currentDialog = _streamDialogData.value
        val match = currentDialog?.first
        val channel = _activePlayerChannel.value

        if (match != null) {
            launchPlayerForMatch(match, listOf(source) + match.streamSources.filter { it.id != source.id })
        } else if (channel != null) {
            launchPlayerForChannel(channel, listOf(source) + channel.streamSources.filter { it.id != source.id })
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
            repository.syncSportsApi()
            _isSyncing.value = false
        }
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

    // Admin CRUD
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

    fun saveConfig(config: SystemConfigEntity) {
        viewModelScope.launch {
            repository.updateSystemConfig(config)
        }
    }
}

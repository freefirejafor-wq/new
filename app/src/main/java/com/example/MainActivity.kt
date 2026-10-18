package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MatchStatus
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.MatchoraBottomBar
import com.example.ui.components.StreamSourceDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MatchoraTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val bottomNavDestination by viewModel.bottomNavDestination.collectAsStateWithLifecycle()

                val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
                val favoriteMatches by viewModel.favoriteMatches.collectAsStateWithLifecycle()
                val liveMatches by viewModel.liveMatches.collectAsStateWithLifecycle()
                val channels by viewModel.channels.collectAsStateWithLifecycle()
                val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
                val sports by viewModel.sports.collectAsStateWithLifecycle()
                val allSports by viewModel.allSports.collectAsStateWithLifecycle()
                val adminLogs by viewModel.adminLogs.collectAsStateWithLifecycle()
                val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
                val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

                val activePlayerMatch by viewModel.activePlayerMatch.collectAsStateWithLifecycle()
                val activePlayerChannel by viewModel.activePlayerChannel.collectAsStateWithLifecycle()
                val activeStreamSources by viewModel.activeStreamSources.collectAsStateWithLifecycle()
                val streamDialogData by viewModel.streamDialogData.collectAsStateWithLifecycle()
                val noStreamsMessage by viewModel.noStreamsMessage.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NavyBackground)
                ) {
                    when (currentScreen) {
                        AppScreen.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = { viewModel.onSplashFinished() }
                            )
                        }

                        AppScreen.PLAYER -> {
                            PlayerScreen(
                                match = activePlayerMatch,
                                channelTitle = activePlayerChannel?.name,
                                initialStreamSources = activeStreamSources,
                                relatedMatches = allMatches.filter { it.id != activePlayerMatch?.id }.take(5),
                                onBack = { viewModel.navigateBackToMain() },
                                onSelectRelatedMatch = { viewModel.onMatchClicked(it) }
                            )
                        }

                        AppScreen.ADMIN -> {
                            AdminPortalScreen(
                                isAdminAuthenticated = isAdminAuthenticated,
                                isOwnerPasswordSet = viewModel.isOwnerPasswordSet(),
                                matches = allMatches,
                                channels = allChannels,
                                sports = allSports,
                                adminLogs = adminLogs,
                                systemConfig = systemConfig,
                                onLogin = { pwd -> viewModel.loginOwner(pwd) },
                                onLogout = { viewModel.logoutOwner() },
                                onSetPassword = { pwd -> viewModel.setOwnerPassword(pwd) },
                                onSaveMatch = { viewModel.saveMatch(it) },
                                onDeleteMatch = { viewModel.deleteMatch(it) },
                                onUpdateMatchScore = { id, home, away, time, status ->
                                    viewModel.updateMatchScore(id, home, away, time, status)
                                },
                                onSaveMatchOverride = { id, home, away, status, time, venue ->
                                    viewModel.saveMatchOverride(id, home, away, status, time, venue)
                                },
                                onRestoreProviderValues = { id ->
                                    viewModel.restoreProviderMatchValues(id)
                                },
                                onAddOrUpdateMatchStream = { matchId, stream ->
                                    viewModel.addOrUpdateMatchStream(matchId, stream)
                                },
                                onDeleteMatchStream = { matchId, streamId ->
                                    viewModel.deleteMatchStream(matchId, streamId)
                                },
                                onToggleMatchStreamEnabled = { matchId, streamId, enabled ->
                                    viewModel.toggleMatchStreamEnabled(matchId, streamId, enabled)
                                },
                                onReorderMatchStreams = { matchId, reordered ->
                                    viewModel.reorderMatchStreams(matchId, reordered)
                                },
                                onAssignChannelStreamToMatch = { matchId, chStream ->
                                    viewModel.assignChannelStreamToMatch(matchId, chStream)
                                },
                                onSaveChannel = { viewModel.saveChannel(it) },
                                onDeleteChannel = { viewModel.deleteChannel(it) },
                                onSaveSport = { viewModel.saveSport(it) },
                                onDeleteSport = { viewModel.deleteSport(it) },
                                onSaveConfig = { viewModel.saveConfig(it) },
                                onTriggerSync = { viewModel.refreshData() },
                                onClose = { viewModel.navigateBackToMain() }
                            )
                        }

                        AppScreen.MAIN -> {
                            BackHandler(enabled = bottomNavDestination != BottomNavDestination.LIVE_EVENTS) {
                                viewModel.setBottomNavDestination(BottomNavDestination.LIVE_EVENTS)
                            }

                            Scaffold(
                                bottomBar = {
                                    MatchoraBottomBar(
                                        currentDestination = bottomNavDestination,
                                        onNavigate = { viewModel.setBottomNavDestination(it) },
                                        liveMatchCount = liveMatches.size
                                    )
                                },
                                containerColor = NavyBackground
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (bottomNavDestination) {
                                        BottomNavDestination.FAVOURITES -> {
                                            FavouritesScreen(
                                                favoriteMatches = favoriteMatches,
                                                onMatchClick = { viewModel.onMatchClicked(it) },
                                                onToggleFavorite = { id, curr -> viewModel.toggleFavorite(id, curr) }
                                            )
                                        }

                                        BottomNavDestination.SPORTS -> {
                                            SportsScreen(
                                                sports = sports,
                                                matches = allMatches,
                                                onMatchClick = { viewModel.onMatchClicked(it) },
                                                onToggleFavorite = { id, curr -> viewModel.toggleFavorite(id, curr) }
                                            )
                                        }

                                        BottomNavDestination.LIVE_EVENTS -> {
                                            // Home screen with All, Live, Upcoming, Finished and Horizontal Sports
                                            HomeScreen(
                                                matches = allMatches,
                                                sports = sports,
                                                systemConfig = systemConfig,
                                                isAdminLoggedIn = isAdminAuthenticated,
                                                onMatchClick = { viewModel.onMatchClicked(it) },
                                                onToggleFavorite = { id, curr -> viewModel.toggleFavorite(id, curr) },
                                                onRefresh = { viewModel.refreshData() },
                                                onOpenAdmin = { viewModel.navigateToAdmin() },
                                                isSyncing = isSyncing
                                            )
                                        }

                                        BottomNavDestination.CATEGORIES -> {
                                            CategoriesScreen(
                                                channels = channels,
                                                onChannelClick = { viewModel.onChannelClicked(it) }
                                            )
                                        }

                                        BottomNavDestination.MENU -> {
                                            MenuScreen(
                                                systemConfig = systemConfig,
                                                isAdminLoggedIn = isAdminAuthenticated,
                                                onOpenAdmin = { viewModel.navigateToAdmin() },
                                                onManualSync = { viewModel.refreshData() },
                                                isSyncing = isSyncing
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Global Multiple Streams Dialog
                    streamDialogData?.let { (match, sources) ->
                        val dialogTitle = match?.let { "${it.homeTeam} vs ${it.awayTeam}" } ?: "Select Server"
                        StreamSourceDialog(
                            title = dialogTitle,
                            streamSources = sources,
                            onSelectSource = { src -> viewModel.selectStreamSource(src) },
                            onDismiss = { viewModel.dismissStreamDialog() }
                        )
                    }

                    // No Authorized Streams Alert Dialog
                    noStreamsMessage?.let { msg ->
                        AlertDialog(
                            onDismissRequest = { viewModel.clearNoStreamsMessage() },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.TvOff,
                                    contentDescription = null,
                                    tint = NeonLiveRed,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = "No Streams Available",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            },
                            text = {
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { viewModel.clearNoStreamsMessage() }) {
                                    Text("OK", color = TurquoisePrimary, fontWeight = FontWeight.Bold)
                                }
                            },
                            containerColor = NavySurface,
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }
        }
    }
}

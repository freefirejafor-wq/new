package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.MatchEntity
import com.example.data.model.StreamSource
import com.example.ui.components.MatchCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    match: MatchEntity?,
    channelTitle: String? = null,
    initialStreamSources: List<StreamSource>,
    relatedMatches: List<MatchEntity> = emptyList(),
    onBack: () -> Unit,
    onSelectRelatedMatch: (MatchEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedSource by remember(initialStreamSources) {
        mutableStateOf(initialStreamSources.firstOrNull())
    }

    var isFullscreen by remember { mutableStateOf(false) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }

    // ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Handle Hardware Back Button
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onBack()
        }
    }

    // Auto hide controls timer
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && !isControlsLocked) {
            delay(4500)
            controlsVisible = false
        }
    }

    // Bind Player Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                isPlaying = exoPlayer.isPlaying
                if (state == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val reason = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection failed. Verify internet connection."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Server returned bad HTTP status. Try another server."
                    else -> error.localizedMessage ?: "Playback error occurred. Switching server recommended."
                }
                playbackError = reason
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Update media source when selectedSource changes
    LaunchedEffect(selectedSource) {
        selectedSource?.let { src ->
            playbackError = null
            isBuffering = true
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(src.url))
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                playbackError = "Error initializing source: ${e.message}"
                isBuffering = false
            }
        }
    }

    val displayTitle = match?.let { "${it.homeTeam} vs ${it.awayTeam}" } ?: channelTitle ?: "Matchora Stream"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        // VIDEO PLAYER CONTAINER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.height(250.dp)
                )
                .background(Color.Black)
                .clickable {
                    if (!isControlsLocked) {
                        controlsVisible = !controlsVisible
                    }
                }
                .testTag("player_surface_container")
        ) {
            // Media3 PlayerView inside AndroidView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // We use custom high polish Jetpack Compose controls!
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Buffering Spinner
            if (isBuffering && playbackError == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TurquoisePrimary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Error Overlay
            if (playbackError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OverlayBlack)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = NeonLiveRed,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Playback Error",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playbackError ?: "Stream connection failed",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    playbackError = null
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                            ) {
                                Text("Retry", color = NavyBackground, fontWeight = FontWeight.Bold)
                            }
                            if (initialStreamSources.size > 1) {
                                OutlinedButton(
                                    onClick = {
                                        val nextIndex = (initialStreamSources.indexOf(selectedSource) + 1) % initialStreamSources.size
                                        selectedSource = initialStreamSources[nextIndex]
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TurquoisePrimary)
                                ) {
                                    Text("Next Server", color = TurquoisePrimary)
                                }
                            }
                        }
                    }
                }
            }

            // CONTROLS OVERLAY
            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isFullscreen) {
                                        isFullscreen = false
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        onBack()
                                    }
                                },
                                modifier = Modifier.testTag("player_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = displayTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Mute toggle
                            IconButton(onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White
                                )
                            }

                            // Lock controls
                            IconButton(onClick = { isControlsLocked = !isControlsLocked }) {
                                Icon(
                                    imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock Controls",
                                    tint = if (isControlsLocked) TurquoisePrimary else Color.White
                                )
                            }
                        }
                    }

                    // Center Playback Buttons (Rewind, Play/Pause, Fast Forward)
                    if (!isControlsLocked) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            IconButton(
                                onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.25f))
                                    .border(1.dp, TurquoisePrimary, CircleShape)
                                    .testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            IconButton(
                                onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Bottom Bar (Live indicator, source quality tag, Fullscreen toggle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonLiveRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE STREAM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonLiveRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedSource?.quality ?: "1080p HD",
                                fontSize = 10.sp,
                                color = TurquoisePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // External player button
                            IconButton(onClick = {
                                selectedSource?.url?.let { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(url), "video/*")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open in External Player"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No external video player found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "External Player",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Fullscreen Toggle
                            IconButton(
                                onClick = {
                                    isFullscreen = !isFullscreen
                                    if (isFullscreen) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                                modifier = Modifier.testTag("player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // NON-FULLSCREEN CONTENT (Server switcher pills & Related Matches)
        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp)
            ) {
                // STREAM SOURCES SELECTOR BUTTONS (matching Screenshot 3: "FAST SERVER", "SD SERVER")
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "AVAILABLE SERVERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(initialStreamSources) { source ->
                                val isChosen = selectedSource?.id == source.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) TurquoisePrimary.copy(alpha = 0.2f) else NavyCard)
                                        .border(
                                            width = if (isChosen) 1.5.dp else 1.dp,
                                            color = if (isChosen) TurquoisePrimary else NavyBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedSource = source }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("server_pill_${source.id}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = if (isChosen) TurquoisePrimary else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = source.label.uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isChosen) TurquoisePrimary else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Match details summary
                match?.let { m ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = m.competition,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${m.homeTeam} vs ${m.awayTeam}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (m.scoreHome != null && m.scoreAway != null) {
                                        Text(
                                            text = "${m.scoreHome} - ${m.scoreAway}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TurquoisePrimary
                                        )
                                    }
                                }
                                m.venue?.let { v ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📍 $v",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Related / Upcoming Matches
                if (relatedMatches.isNotEmpty()) {
                    item {
                        Text(
                            text = "MORE MATCHES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    items(relatedMatches) { relMatch ->
                        MatchCard(
                            match = relMatch,
                            onMatchClick = { onSelectRelatedMatch(it) },
                            onToggleFavorite = { _, _ -> }
                        )
                    }
                }
            }
        }
    }
}

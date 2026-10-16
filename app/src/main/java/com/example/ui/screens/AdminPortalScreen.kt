package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.HealthStatus
import com.example.util.ImageStorageHelper
import com.example.util.StreamHealthChecker
import com.example.util.StreamHealthInfo
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

enum class AdminTab {
    MATCHES,
    CHANNELS,
    SPORTS,
    API_SYNC,
    MAINTENANCE,
    LOGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    isAdminAuthenticated: Boolean,
    isOwnerPasswordSet: Boolean,
    matches: List<MatchEntity>,
    channels: List<ChannelEntity>,
    sports: List<SportCategoryEntity>,
    adminLogs: List<AdminLogEntity>,
    systemConfig: SystemConfigEntity?,
    onLogin: (String) -> Boolean,
    onLogout: () -> Unit,
    onSetPassword: (String) -> Unit,
    onSaveMatch: (MatchEntity) -> Unit,
    onDeleteMatch: (MatchEntity) -> Unit,
    onUpdateMatchScore: (String, Int?, Int?, String, MatchStatus) -> Unit,
    onSaveChannel: (ChannelEntity) -> Unit,
    onDeleteChannel: (ChannelEntity) -> Unit,
    onSaveSport: (SportCategoryEntity) -> Unit,
    onDeleteSport: (SportCategoryEntity) -> Unit,
    onSaveConfig: (SystemConfigEntity) -> Unit,
    onTriggerSync: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AdminTab.MATCHES) }

    // Dialog States
    var showAddMatchDialog by remember { mutableStateOf(false) }
    var matchToEditScore by remember { mutableStateOf<MatchEntity?>(null) }
    var itemToDeleteConfirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var channelToEdit by remember { mutableStateOf<ChannelEntity?>(null) }
    var showAddStreamDialogForChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var showAddSportDialog by remember { mutableStateOf(false) }

    BackHandler {
        onClose()
    }

    if (!isAdminAuthenticated) {
        // OWNER LOGIN / FIRST TIME SETUP SCREEN
        OwnerAuthView(
            isPasswordConfigured = isOwnerPasswordSet,
            onAuthenticate = { pwd ->
                val ok = onLogin(pwd)
                if (!ok) {
                    Toast.makeText(context, "Invalid owner password", Toast.LENGTH_SHORT).show()
                }
            },
            onSetupPassword = { pwd ->
                onSetPassword(pwd)
                Toast.makeText(context, "Owner password successfully set", Toast.LENGTH_SHORT).show()
            },
            onCancel = onClose
        )
        return
    }

    // AUTHENTICATED OWNER DASHBOARD
    Scaffold(
        topBar = {
            Surface(color = NavySurface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Owner Admin Portal",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Full Backend & Fixture Control",
                                    fontSize = 11.sp,
                                    color = TurquoisePrimary
                                )
                            }
                        }

                        // Logout Button
                        OutlinedButton(
                            onClick = onLogout,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLiveRed.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonLiveRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Logout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // TABS ROW
                    ScrollableTabRow(
                        selectedTabIndex = currentTab.ordinal,
                        containerColor = NavySurface,
                        contentColor = TurquoisePrimary,
                        edgePadding = 12.dp,
                        divider = {}
                    ) {
                        Tab(
                            selected = currentTab == AdminTab.MATCHES,
                            onClick = { currentTab = AdminTab.MATCHES },
                            text = { Text("Matches (${matches.size})", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = currentTab == AdminTab.CHANNELS,
                            onClick = { currentTab = AdminTab.CHANNELS },
                            text = { Text("Channels & Streams (${channels.size})", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = currentTab == AdminTab.SPORTS,
                            onClick = { currentTab = AdminTab.SPORTS },
                            text = { Text("Sports (${sports.size})", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = currentTab == AdminTab.API_SYNC,
                            onClick = { currentTab = AdminTab.API_SYNC },
                            text = { Text("API & Sync", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = currentTab == AdminTab.MAINTENANCE,
                            onClick = { currentTab = AdminTab.MAINTENANCE },
                            text = { Text("Maintenance", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = currentTab == AdminTab.LOGS,
                            onClick = { currentTab = AdminTab.LOGS },
                            text = { Text("Activity Log", fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        containerColor = NavyBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                AdminTab.MATCHES -> {
                    AdminMatchesTab(
                        matches = matches,
                        onAddNew = { showAddMatchDialog = true },
                        onEditScore = { matchToEditScore = it },
                        onDelete = { match ->
                            itemToDeleteConfirm = Pair("match '${match.homeTeam} vs ${match.awayTeam}'") {
                                onDeleteMatch(match)
                            }
                        }
                    )
                }
                AdminTab.CHANNELS -> {
                    AdminChannelsTab(
                        channels = channels,
                        onAddNewChannel = { showAddChannelDialog = true },
                        onEditChannel = { channelToEdit = it },
                        onAddStreamSource = { showAddStreamDialogForChannel = it },
                        onToggleEnable = { ch ->
                            onSaveChannel(ch.copy(isEnabled = !ch.isEnabled))
                        },
                        onDeleteChannel = { ch ->
                            itemToDeleteConfirm = Pair("channel '${ch.name}'") {
                                onDeleteChannel(ch)
                            }
                        },
                        onDeleteStreamSource = { ch, src ->
                            onSaveChannel(ch.copy(streamSources = ch.streamSources.filter { it.id != src.id }))
                        }
                    )
                }
                AdminTab.SPORTS -> {
                    AdminSportsTab(
                        sports = sports,
                        onAddNew = { showAddSportDialog = true },
                        onToggleEnable = { sp ->
                            onSaveSport(sp.copy(isEnabled = !sp.isEnabled))
                        },
                        onDelete = { sp ->
                            itemToDeleteConfirm = Pair("sport category '${sp.name}'") {
                                onDeleteSport(sp)
                            }
                        }
                    )
                }
                AdminTab.API_SYNC -> {
                    AdminApiSyncTab(
                        config = systemConfig ?: SystemConfigEntity(),
                        onSaveConfig = onSaveConfig,
                        onTriggerSync = onTriggerSync
                    )
                }
                AdminTab.MAINTENANCE -> {
                    AdminMaintenanceTab(
                        config = systemConfig ?: SystemConfigEntity(),
                        onSaveConfig = onSaveConfig
                    )
                }
                AdminTab.LOGS -> {
                    AdminLogsTab(logs = adminLogs)
                }
            }
        }
    }

    // DIALOGS
    // 1. Delete Confirmation Dialog
    itemToDeleteConfirm?.let { (itemDesc, deleteAction) ->
        AlertDialog(
            onDismissRequest = { itemToDeleteConfirm = null },
            title = { Text("Confirm Deletion", color = Color.White) },
            text = { Text("Are you sure you want to permanently delete $itemDesc? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteAction()
                        itemToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLiveRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteConfirm = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavySurface
        )
    }

    // 2. Add Match Dialog
    if (showAddMatchDialog) {
        AddMatchDialog(
            sports = sports,
            onDismiss = { showAddMatchDialog = false },
            onConfirm = { newMatch ->
                onSaveMatch(newMatch)
                showAddMatchDialog = false
            }
        )
    }

    // 3. Edit Match Score & Status Dialog
    matchToEditScore?.let { match ->
        EditScoreDialog(
            match = match,
            onDismiss = { matchToEditScore = null },
            onSave = { home, away, time, status ->
                onUpdateMatchScore(match.id, home, away, time, status)
                matchToEditScore = null
            }
        )
    }

    // 4. Add Channel Dialog
    if (showAddChannelDialog) {
        AddChannelDialog(
            sports = sports,
            onDismiss = { showAddChannelDialog = false },
            onConfirm = { newChannel ->
                onSaveChannel(newChannel)
                showAddChannelDialog = false
            }
        )
    }

    // 4b. Edit Channel Dialog
    channelToEdit?.let { channel ->
        EditChannelDialog(
            channel = channel,
            sports = sports,
            onDismiss = { channelToEdit = null },
            onConfirm = { updatedChannel ->
                onSaveChannel(updatedChannel)
                channelToEdit = null
            }
        )
    }

    // 5. Add Stream to Channel Dialog
    showAddStreamDialogForChannel?.let { channel ->
        AddStreamSourceDialog(
            targetTitle = channel.name,
            onDismiss = { showAddStreamDialogForChannel = null },
            onConfirm = { newStream ->
                val updatedStreams = channel.streamSources + newStream
                onSaveChannel(channel.copy(streamSources = updatedStreams))
                showAddStreamDialogForChannel = null
            }
        )
    }

    // 6. Add Sport Category Dialog
    if (showAddSportDialog) {
        AddSportCategoryDialog(
            onDismiss = { showAddSportDialog = false },
            onConfirm = { newSport ->
                onSaveSport(newSport)
                showAddSportDialog = false
            }
        )
    }
}

// OWNER AUTHENTICATION VIEW
@Composable
private fun OwnerAuthView(
    isPasswordConfigured: Boolean,
    onAuthenticate: (String) -> Unit,
    onSetupPassword: (String) -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, TurquoisePrimary.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Admin Gate",
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isPasswordConfigured) "Owner Authentication" else "Initialize Owner Password",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isPasswordConfigured)
                        "Enter the secure owner key to access administrative controls."
                    else
                        "Create a master password for your Matchora app. It will be securely hashed on your backend.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Owner Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurquoisePrimary,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (!isPasswordConfigured) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Confirm Owner Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = err, color = NeonLiveRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (password.isBlank()) {
                                errorMessage = "Password cannot be empty"
                                return@Button
                            }
                            if (!isPasswordConfigured) {
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match"
                                    return@Button
                                }
                                onSetupPassword(password)
                            } else {
                                onAuthenticate(password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_login_submit_button")
                    ) {
                        Text(
                            text = if (isPasswordConfigured) "Unlock" else "Save Key",
                            color = NavyBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 1. MATCHES TAB
@Composable
private fun AdminMatchesTab(
    matches: List<MatchEntity>,
    onAddNew: () -> Unit,
    onEditScore: (MatchEntity) -> Unit,
    onDelete: (MatchEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCH SCHEDULES (${matches.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Button(
                    onClick = onAddNew,
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("admin_add_match_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NavyBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Match", color = NavyBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(matches, key = { it.id }) { match ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = match.competition,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseSecondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (match.matchStatus) {
                                        MatchStatus.LIVE -> NeonLiveRed.copy(alpha = 0.2f)
                                        MatchStatus.UPCOMING -> TurquoisePrimary.copy(alpha = 0.2f)
                                        MatchStatus.FINISHED -> Color.Gray.copy(alpha = 0.2f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = match.matchStatus.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (match.matchStatus) {
                                    MatchStatus.LIVE -> NeonLiveRed
                                    MatchStatus.UPCOMING -> TurquoisePrimary
                                    MatchStatus.FINISHED -> Color.LightGray
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${match.homeTeam} vs ${match.awayTeam}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${match.scheduledDate} • ${match.matchTime} • ${match.streamSources.size} Streams",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        if (match.scoreHome != null && match.scoreAway != null) {
                            Text(
                                text = "${match.scoreHome} - ${match.scoreAway}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoisePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onEditScore(match) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TurquoisePrimary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit / Score", fontSize = 11.sp, color = TurquoisePrimary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onDelete(match) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonLiveRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// 2. CHANNELS TAB
@Composable
private fun AdminChannelsTab(
    channels: List<ChannelEntity>,
    onAddNewChannel: () -> Unit,
    onEditChannel: (ChannelEntity) -> Unit,
    onAddStreamSource: (ChannelEntity) -> Unit,
    onToggleEnable: (ChannelEntity) -> Unit,
    onDeleteChannel: (ChannelEntity) -> Unit,
    onDeleteStreamSource: (ChannelEntity, StreamSource) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // In-memory temporary stream health map (avoids database migration)
    val streamHealthMap = remember { mutableStateMapOf<String, StreamHealthInfo>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BROADCAST CHANNELS & STREAMS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${channels.count { it.isEnabled }} of ${channels.size} channels active",
                        fontSize = 11.sp,
                        color = TurquoiseSecondary
                    )
                }
                Button(
                    onClick = onAddNewChannel,
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NavyBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Channel", color = NavyBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (channels.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No channels added yet. Click 'Add Channel' to create one.", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        items(channels, key = { it.id }) { channel ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (channel.isEnabled) NavyCard else NavyCard.copy(alpha = 0.75f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (channel.isEnabled) NavyBorder else NavyBorder.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Row: Channel Logo, Details, Visibility & Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Channel Picture / Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TurquoisePrimary.copy(alpha = 0.15f))
                                    .border(1.dp, TurquoisePrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!channel.logoUrl.isNullOrBlank()) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(channel.logoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "${channel.name} Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp)),
                                        error = {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = null,
                                                tint = TurquoisePrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        loading = {
                                            CircularProgressIndicator(
                                                color = TurquoisePrimary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = channel.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Category: ${channel.categoryName}",
                                    fontSize = 11.sp,
                                    color = TurquoiseSecondary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                // Status badge
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (channel.isEnabled) NeonGreen else TextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (channel.isEnabled) "Visible in App" else "Hidden from Users",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (channel.isEnabled) NeonGreen else TextMuted
                                    )
                                }
                            }
                        }

                        // Toggle enable/disable switch
                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = channel.isEnabled,
                                onCheckedChange = { onToggleEnable(channel) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TurquoisePrimary,
                                    checkedTrackColor = NavyCardElevated,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = NavySurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Configured Streams
                    Text(
                        text = "Configured Streams (${channel.streamSources.size}):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (channel.streamSources.isEmpty()) {
                        Text(
                            text = "No stream sources added. Add one below.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    channel.streamSources.forEach { src ->
                        val isM3u8 = src.protocol == StreamProtocol.HLS || src.url.contains(".m3u8", ignoreCase = true)
                        val health = streamHealthMap[src.id]

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavySurface)
                                .border(1.dp, NavyBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${src.label} (${src.quality})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TurquoisePrimary.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = src.protocol.name,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TurquoisePrimary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = src.url,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }

                                    // Remove stream button
                                    IconButton(
                                        onClick = { onDeleteStreamSource(channel, src) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Stream",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // M3U8 Health Check Section
                                if (isM3u8) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    streamHealthMap[src.id] = StreamHealthInfo(
                                                        status = HealthStatus.CHECKING,
                                                        details = "Checking..."
                                                    )
                                                    val res = StreamHealthChecker.checkM3u8Stream(src.url)
                                                    streamHealthMap[src.id] = res
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                TurquoisePrimary.copy(alpha = 0.6f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Check stream",
                                                tint = TurquoisePrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (health?.status == HealthStatus.CHECKING) "Checking..." else "Check stream",
                                                fontSize = 10.sp,
                                                color = TurquoisePrimary
                                            )
                                        }

                                        // Status display
                                        when (health?.status) {
                                            HealthStatus.CHECKING -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CircularProgressIndicator(
                                                        color = TurquoisePrimary,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Checking...",
                                                        fontSize = 11.sp,
                                                        color = TurquoisePrimary
                                                    )
                                                }
                                            }
                                            HealthStatus.ONLINE -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Online",
                                                        tint = NeonGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Online (${health.lastCheckedTime})",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = NeonGreen
                                                    )
                                                }
                                            }
                                            HealthStatus.OFFLINE -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Cancel,
                                                        contentDescription = "Offline",
                                                        tint = NeonLiveRed,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Offline (${health.details}) ${health.lastCheckedTime}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = NeonLiveRed
                                                    )
                                                }
                                            }
                                            HealthStatus.ERROR -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.ErrorOutline,
                                                        contentDescription = "Error",
                                                        tint = GoldStar,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Error (${health.details}) ${health.lastCheckedTime}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = GoldStar
                                                    )
                                                }
                                            }
                                            else -> {
                                                Text(
                                                    text = "Unchecked",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Channel Action Buttons: Edit, Add Stream, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onEditChannel(channel) },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TurquoisePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Channel", fontSize = 11.sp, color = TurquoisePrimary)
                            }

                            OutlinedButton(
                                onClick = { onAddStreamSource(channel) },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TurquoiseSecondary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddLink,
                                    contentDescription = null,
                                    tint = TurquoiseSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Stream", fontSize = 11.sp, color = TurquoiseSecondary)
                            }
                        }

                        IconButton(
                            onClick = { onDeleteChannel(channel) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Channel",
                                tint = NeonLiveRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. SPORTS TAB
@Composable
private fun AdminSportsTab(
    sports: List<SportCategoryEntity>,
    onAddNew: () -> Unit,
    onToggleEnable: (SportCategoryEntity) -> Unit,
    onDelete: (SportCategoryEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPORTS & CATEGORIES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Button(
                    onClick = onAddNew,
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NavyBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Sport", color = NavyBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(sports, key = { it.id }) { sport ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = sport.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Key: ${sport.id} • Order: ${sport.sortOrder}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = sport.isEnabled,
                            onCheckedChange = { onToggleEnable(sport) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TurquoisePrimary,
                                checkedTrackColor = NavyCardElevated
                            )
                        )
                        if (sport.id != "all") {
                            IconButton(onClick = { onDelete(sport) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonLiveRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. API & SYNC TAB
@Composable
private fun AdminApiSyncTab(
    config: SystemConfigEntity,
    onSaveConfig: (SystemConfigEntity) -> Unit,
    onTriggerSync: () -> Unit
) {
    var provider by remember(config) { mutableStateOf(config.sportsApiProvider) }
    var endpoint by remember(config) { mutableStateOf(config.sportsApiEndpoint) }
    var apiKey by remember(config) { mutableStateOf(config.sportsApiKey) }
    var syncInterval by remember(config) { mutableIntStateOf(config.autoSyncIntervalMinutes) }
    var isUsingDemo by remember(config) { mutableStateOf(config.isUsingDemoData) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUTHORIZED SPORTS DATA API",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("API Endpoint URL") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Authorized API Key / Token") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Sync Interval: ${syncInterval}m",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Row {
                            listOf(15, 30, 60).forEach { mins ->
                                FilterChip(
                                    selected = syncInterval == mins,
                                    onClick = { syncInterval = mins },
                                    label = { Text("${mins}m", fontSize = 11.sp) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Force Demo Data Mode",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = isUsingDemo,
                            onCheckedChange = { isUsingDemo = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveConfig(
                                    config.copy(
                                        sportsApiProvider = provider,
                                        sportsApiEndpoint = endpoint,
                                        sportsApiKey = apiKey,
                                        autoSyncIntervalMinutes = syncInterval,
                                        isUsingDemoData = isUsingDemo
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save API Settings", color = NavyBackground, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onTriggerSync,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyCardElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Now", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 5. MAINTENANCE TAB
@Composable
private fun AdminMaintenanceTab(
    config: SystemConfigEntity,
    onSaveConfig: (SystemConfigEntity) -> Unit
) {
    var isMaintenance by remember(config) { mutableStateOf(config.isMaintenanceMode) }
    var message by remember(config) { mutableStateOf(config.maintenanceMessage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MAINTENANCE MODE CONTROLS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LivePulseDot
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Maintenance Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Displays public banner in the app for all users",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isMaintenance,
                        onCheckedChange = { isMaintenance = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Public Announcement Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurquoisePrimary,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onSaveConfig(
                            config.copy(
                                isMaintenanceMode = isMaintenance,
                                maintenanceMessage = message
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Maintenance State", color = NavyBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 6. LOGS TAB
@Composable
private fun AdminLogsTab(logs: List<AdminLogEntity>) {
    val sdf = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "ADMIN AUDIT TRAIL (${logs.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }

        if (logs.isEmpty()) {
            item {
                Text("No activity recorded yet.", color = TextMuted, fontSize = 12.sp)
            }
        } else {
            items(logs, key = { it.id }) { log ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyCard)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.action,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoisePrimary
                            )
                            Text(
                                text = log.details,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = sdf.format(Date(log.timestamp)),
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// MODAL DIALOGS
@Composable
private fun AddMatchDialog(
    sports: List<SportCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (MatchEntity) -> Unit
) {
    var homeTeam by remember { mutableStateOf("") }
    var awayTeam by remember { mutableStateOf("") }
    var competition by remember { mutableStateOf("FOOTBALL | PREMIER LEAGUE") }
    var selectedSportId by remember { mutableStateOf(sports.firstOrNull { it.id != "all" }?.id ?: "football") }
    var matchTime by remember { mutableStateOf("08:00 PM") }
    var matchDate by remember { mutableStateOf("16/10/2026") }
    var streamUrl by remember { mutableStateOf("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Fixture", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = competition, onValueChange = { competition = it }, label = { Text("Competition") })
                OutlinedTextField(value = homeTeam, onValueChange = { homeTeam = it }, label = { Text("Home Team") })
                OutlinedTextField(value = awayTeam, onValueChange = { awayTeam = it }, label = { Text("Away Team") })
                OutlinedTextField(value = matchTime, onValueChange = { matchTime = it }, label = { Text("Match Time") })
                OutlinedTextField(value = matchDate, onValueChange = { matchDate = it }, label = { Text("Date (DD/MM/YYYY)") })
                OutlinedTextField(value = streamUrl, onValueChange = { streamUrl = it }, label = { Text("Stream URL (HLS/MP4)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (homeTeam.isNotBlank() && awayTeam.isNotBlank()) {
                        val newMatch = MatchEntity(
                            id = "m_${System.currentTimeMillis()}",
                            sportId = selectedSportId,
                            sportName = sports.find { it.id == selectedSportId }?.name ?: "Sports",
                            competition = competition,
                            homeTeam = homeTeam,
                            awayTeam = awayTeam,
                            matchStatus = MatchStatus.UPCOMING,
                            matchTime = matchTime,
                            scheduledDate = matchDate,
                            startsInText = "Upcoming",
                            streamSources = listOf(
                                StreamSource(
                                    id = "s_${System.currentTimeMillis()}",
                                    label = "FAST SERVER HD",
                                    url = streamUrl,
                                    quality = "1080p HD",
                                    protocol = if (streamUrl.endsWith(".mpd")) StreamProtocol.DASH else if (streamUrl.endsWith(".mp4")) StreamProtocol.MP4 else StreamProtocol.HLS
                                )
                            )
                        )
                        onConfirm(newMatch)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("Create Match", color = NavyBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = NavySurface
    )
}

@Composable
private fun EditScoreDialog(
    match: MatchEntity,
    onDismiss: () -> Unit,
    onSave: (Int?, Int?, String, MatchStatus) -> Unit
) {
    var homeScore by remember { mutableIntStateOf(match.scoreHome ?: 0) }
    var awayScore by remember { mutableIntStateOf(match.scoreAway ?: 0) }
    var timeText by remember { mutableStateOf(match.matchTime) }
    var status by remember { mutableStateOf(match.matchStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Live Match Control", color = Color.White) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "${match.homeTeam} vs ${match.awayTeam}", fontWeight = FontWeight.Bold, color = Color.White)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(match.homeTeam, fontSize = 12.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (homeScore > 0) homeScore-- }) { Text("-", fontSize = 20.sp, color = TurquoisePrimary) }
                            Text("$homeScore", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { homeScore++ }) { Text("+", fontSize = 20.sp, color = TurquoisePrimary) }
                        }
                    }

                    Text(":", fontSize = 22.sp, color = TextMuted)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(match.awayTeam, fontSize = 12.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (awayScore > 0) awayScore-- }) { Text("-", fontSize = 20.sp, color = TurquoisePrimary) }
                            Text("$awayScore", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { awayScore++ }) { Text("+", fontSize = 20.sp, color = TurquoisePrimary) }
                        }
                    }
                }

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("Match Clock / Status Text") },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MatchStatus.values().forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.name, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(homeScore, awayScore, timeText, status) },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("Update Score", color = NavyBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = NavySurface
    )
}

@Composable
private fun AddChannelDialog(
    sports: List<SportCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (ChannelEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Football Live") }
    var selectedSportId by remember { mutableStateOf("football") }
    var streamUrl by remember { mutableStateOf("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8") }
    var logoUrl by remember { mutableStateOf<String?>(null) }
    var isSavingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isSavingImage = true
                val saved = ImageStorageHelper.saveImageLocally(context, uri)
                logoUrl = saved
                isSavingImage = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Broadcast Channel", color = Color.White) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Channel Logo / Picture Picker Section
                Text(
                    text = "Channel Logo / Thumbnail",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.15f))
                            .border(1.dp, TurquoisePrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSavingImage) {
                            CircularProgressIndicator(
                                color = TurquoisePrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (!logoUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Channel Logo Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyCardElevated),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (logoUrl.isNullOrBlank()) "Select Image" else "Change Image",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }

                        if (!logoUrl.isNullOrBlank()) {
                            TextButton(
                                onClick = { logoUrl = null },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Remove picture", fontSize = 10.sp, color = NeonLiveRed)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Cricket World)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    label = { Text("Authorized Stream URL (.m3u8 / .mp4)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newChan = ChannelEntity(
                            id = "c_${System.currentTimeMillis()}",
                            name = name,
                            sportId = selectedSportId,
                            categoryName = category,
                            logoUrl = logoUrl?.ifBlank { null },
                            isEnabled = true,
                            streamSources = listOf(
                                StreamSource(
                                    id = "cs_${System.currentTimeMillis()}",
                                    label = "MAIN SERVER HD",
                                    url = streamUrl,
                                    quality = "1080p HD",
                                    protocol = if (streamUrl.endsWith(".mpd")) StreamProtocol.DASH else if (streamUrl.endsWith(".mp4")) StreamProtocol.MP4 else StreamProtocol.HLS
                                )
                            )
                        )
                        onConfirm(newChan)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("Save Channel", color = NavyBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = NavySurface
    )
}

@Composable
private fun EditChannelDialog(
    channel: ChannelEntity,
    sports: List<SportCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (ChannelEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(channel.name) }
    var category by remember { mutableStateOf(channel.categoryName) }
    var selectedSportId by remember { mutableStateOf(channel.sportId) }
    var logoUrl by remember { mutableStateOf(channel.logoUrl) }
    var isSavingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isSavingImage = true
                val saved = ImageStorageHelper.saveImageLocally(context, uri)
                logoUrl = saved
                isSavingImage = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Broadcast Channel", color = Color.White) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Channel Logo / Picture Picker Section
                Text(
                    text = "Channel Logo / Thumbnail",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.15f))
                            .border(1.dp, TurquoisePrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSavingImage) {
                            CircularProgressIndicator(
                                color = TurquoisePrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (!logoUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Channel Logo Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyCardElevated),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (logoUrl.isNullOrBlank()) "Select Image" else "Change Image",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }

                        if (!logoUrl.isNullOrBlank()) {
                            TextButton(
                                onClick = { logoUrl = null },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Remove picture", fontSize = 10.sp, color = NeonLiveRed)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Football Live)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updated = channel.copy(
                            name = name,
                            categoryName = category,
                            sportId = selectedSportId,
                            logoUrl = logoUrl?.ifBlank { null }
                        )
                        onConfirm(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("Update Channel", color = NavyBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = NavySurface
    )
}

@Composable
private fun AddStreamSourceDialog(
    targetTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (StreamSource) -> Unit
) {
    var label by remember { mutableStateOf("FAST SERVER 2") }
    var url by remember { mutableStateOf("https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8") }
    var quality by remember { mutableStateOf("1080p HD") }
    var protocol by remember { mutableStateOf(StreamProtocol.HLS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stream to $targetTitle", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Server Label") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Stream URL (.m3u8 / .mpd / .mp4)") })
                OutlinedTextField(value = quality, onValueChange = { quality = it }, label = { Text("Quality (1080p / 720p / SD)") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StreamProtocol.values().forEach { proto ->
                        FilterChip(
                            selected = protocol == proto,
                            onClick = { protocol = proto },
                            label = { Text(proto.name, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && url.isNotBlank()) {
                        val newSrc = StreamSource(
                            id = "s_${System.currentTimeMillis()}",
                            label = label,
                            url = url,
                            quality = quality,
                            protocol = protocol
                        )
                        onConfirm(newSrc)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("Add Stream", color = NavyBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = NavySurface
    )
}

@Composable
private fun AddSportCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (SportCategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sport Category", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = {
                    name = it
                    if (id.isEmpty()) id = it.lowercase().replace(" ", "_")
                }, label = { Text("Sport Name (e.g. Volleyball)") })
                OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Sport Identifier (e.g. volleyball)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && id.isNotBlank()) {
                        onConfirm(SportCategoryEntity(id = id, name = name, iconKey = id))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("Add Sport", color = NavyBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = NavySurface
    )
}

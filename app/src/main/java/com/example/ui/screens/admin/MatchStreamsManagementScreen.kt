package com.example.ui.screens.admin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelEntity
import com.example.data.model.MatchEntity
import com.example.data.model.StreamProtocol
import com.example.data.model.StreamSource
import com.example.ui.theme.*
import com.example.util.HealthStatus
import com.example.util.StreamHealthChecker
import com.example.util.StreamHealthInfo
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MatchStreamsManagementScreen(
    match: MatchEntity,
    channels: List<ChannelEntity>,
    onBack: () -> Unit,
    onAddOrUpdateStream: (StreamSource) -> Unit,
    onDeleteStream: (String) -> Unit,
    onToggleStreamEnabled: (String, Boolean) -> Unit,
    onReorderStreams: (List<StreamSource>) -> Unit,
    onAssignChannelStream: (StreamSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAddEditDialog by remember { mutableStateOf<StreamSource?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showImportFromChannelDialog by remember { mutableStateOf(false) }
    var streamToDeleteConfirm by remember { mutableStateOf<StreamSource?>(null) }

    // Stream health status map: streamId -> StreamHealthInfo
    val healthStatuses = remember { mutableStateMapOf<String, StreamHealthInfo>() }

    BackHandler {
        onBack()
    }

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to matches",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Manage Streams",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${match.homeTeam} vs ${match.awayTeam}",
                                fontSize = 12.sp,
                                color = TurquoisePrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        containerColor = NavyBackground
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Match info banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = match.competition,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseSecondary
                            )
                            val enabledCount = match.streamSources.count { it.isEnabled }
                            Text(
                                text = "$enabledCount of ${match.streamSources.size} streams active",
                                fontSize = 11.sp,
                                color = if (enabledCount > 0) Color(0xFF10B981) else TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${match.homeTeam} vs ${match.awayTeam}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${match.scheduledDate} • ${match.matchTime} • ${match.venue ?: "Stadium"}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Action Buttons: Add Stream / Import Channel Stream
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isCreatingNew = true
                            showAddEditDialog = StreamSource(
                                id = "stream_${UUID.randomUUID().toString().take(8)}",
                                label = "",
                                url = "",
                                quality = "1080p HD",
                                protocol = StreamProtocol.HLS,
                                isAuthorized = false,
                                isBackup = false,
                                isEnabled = true,
                                sortOrder = match.streamSources.size
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_add_stream_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NavyBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Stream", color = NavyBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showImportFromChannelDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TurquoisePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_import_channel_stream_button")
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("From Channel", color = TurquoisePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Streams List
            if (match.streamSources.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SensorsOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Streams Configured",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Members opening this match will see 'No streams available' until an admin adds and enables a stream.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(match.streamSources, key = { _, s -> s.id }) { index, stream ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_stream_item_${stream.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (stream.isEnabled) NavyCard else NavyCard.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (stream.isEnabled) NavyBorder else Color.DarkGray
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Label, Quality, Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stream.label.ifBlank { "Untitled Stream" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (stream.isAuthorized) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Verified", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if (stream.isBackup) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Fallback", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Quality badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TurquoisePrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stream.quality,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TurquoisePrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Enable/Disable Switch
                                    Switch(
                                        checked = stream.isEnabled,
                                        onCheckedChange = { isEnabled ->
                                            onToggleStreamEnabled(stream.id, isEnabled)
                                        },
                                        modifier = Modifier.testTag("stream_toggle_${stream.id}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Metadata & URL
                            Text(
                                text = "${stream.protocol.name} • ${stream.language} • ${stream.region}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stream.url,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Health check status if available
                            val health = healthStatuses[stream.id]
                            if (health != null && health.status != HealthStatus.IDLE) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (health.status) {
                                                HealthStatus.ONLINE -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                HealthStatus.OFFLINE -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                HealthStatus.ERROR -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                else -> NavyBackground
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${health.status.name}: ${health.details} (${health.lastCheckedTime})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = when (health.status) {
                                            HealthStatus.ONLINE -> Color(0xFF10B981)
                                            HealthStatus.OFFLINE -> Color(0xFFEF4444)
                                            HealthStatus.ERROR -> Color(0xFFF59E0B)
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons row: Check, Up, Down, Edit, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Health check button
                                OutlinedButton(
                                    onClick = {
                                        healthStatuses[stream.id] = StreamHealthInfo(status = HealthStatus.CHECKING, details = "Checking...")
                                        coroutineScope.launch {
                                            val info = StreamHealthChecker.checkStreamHealth(stream.url, stream.protocol)
                                            healthStatuses[stream.id] = info
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Check", fontSize = 10.sp, color = TurquoisePrimary)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Move Up
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = match.streamSources.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                onReorderStreams(mutable)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) Color.White else Color.DarkGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Move Down
                                    IconButton(
                                        onClick = {
                                            if (index < match.streamSources.size - 1) {
                                                val mutable = match.streamSources.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                onReorderStreams(mutable)
                                            }
                                        },
                                        enabled = index < match.streamSources.size - 1,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = if (index < match.streamSources.size - 1) Color.White else Color.DarkGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Edit
                                    IconButton(
                                        onClick = {
                                            isCreatingNew = false
                                            showAddEditDialog = stream
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TurquoisePrimary, modifier = Modifier.size(16.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { streamToDeleteConfirm = stream },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonLiveRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Stream Dialog
    showAddEditDialog?.let { currentStream ->
        AddEditStreamDialog(
            initialStream = currentStream,
            isNew = isCreatingNew,
            onDismiss = { showAddEditDialog = null },
            onSave = { savedStream ->
                onAddOrUpdateStream(savedStream)
                showAddEditDialog = null
                Toast.makeText(context, "Stream '${savedStream.label}' saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Import Stream from Channel Dialog
    if (showImportFromChannelDialog) {
        ImportChannelStreamDialog(
            channels = channels,
            onDismiss = { showImportFromChannelDialog = false },
            onSelectChannelStream = { channelStream ->
                onAssignChannelStream(channelStream)
                showImportFromChannelDialog = false
                Toast.makeText(context, "Channel stream assigned to match", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    streamToDeleteConfirm?.let { stream ->
        AlertDialog(
            onDismissRequest = { streamToDeleteConfirm = null },
            title = { Text("Delete Stream", color = Color.White) },
            text = {
                Text(
                    "Are you sure you want to delete stream '${stream.label}'? It will be removed from this match immediately.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStream(stream.id)
                        streamToDeleteConfirm = null
                        Toast.makeText(context, "Stream deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonLiveRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { streamToDeleteConfirm = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = NavySurface
        )
    }
}

@Composable
private fun AddEditStreamDialog(
    initialStream: StreamSource,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (StreamSource) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var label by remember { mutableStateOf(initialStream.label) }
    var url by remember { mutableStateOf(initialStream.url) }
    var protocol by remember { mutableStateOf(initialStream.protocol) }
    var quality by remember { mutableStateOf(initialStream.quality) }
    var language by remember { mutableStateOf(initialStream.language) }
    var region by remember { mutableStateOf(initialStream.region) }
    var isEnabled by remember { mutableStateOf(initialStream.isEnabled) }
    var isBackup by remember { mutableStateOf(initialStream.isBackup) }
    var isAuthorized by remember { mutableStateOf(initialStream.isAuthorized) }
    var verificationNote by remember { mutableStateOf(initialStream.verificationNote ?: "") }

    var testStatus by remember { mutableStateOf<StreamHealthInfo?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Add Authorized Stream" else "Edit Stream Entry",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it; errorMessage = null },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. Primary English HD Feed") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; errorMessage = null; testStatus = null },
                        label = { Text("Stream URL (http:// or https://)") },
                        placeholder = { Text("https://example.com/live/feed.m3u8") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Format & Quality row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Protocol selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Format", fontSize = 11.sp, color = TextSecondary)
                            Row {
                                listOf(StreamProtocol.HLS, StreamProtocol.DASH, StreamProtocol.MP4).forEach { p ->
                                    FilterChip(
                                        selected = protocol == p,
                                        onClick = { protocol = p },
                                        label = { Text(p.name, fontSize = 10.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Quality Label", fontSize = 11.sp, color = TextSecondary)
                        Row {
                            listOf("1080p FHD", "720p HD", "SD", "4K").forEach { q ->
                                FilterChip(
                                    selected = quality == q,
                                    onClick = { quality = q },
                                    label = { Text(q, fontSize = 10.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = language,
                            onValueChange = { language = it },
                            label = { Text("Language") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurquoisePrimary,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = region,
                            onValueChange = { region = it },
                            label = { Text("Region") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurquoisePrimary,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }

                // Switch options
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Stream", color = Color.White, fontSize = 13.sp)
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Designate as Fallback", color = Color.White, fontSize = 13.sp)
                            Text("Offered if primary stream encounters errors", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(checked = isBackup, onCheckedChange = { isBackup = it })
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mark as Admin-Verified", color = Color.White, fontSize = 13.sp)
                            Text("Shows 'Verified' badge to members", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(checked = isAuthorized, onCheckedChange = { isAuthorized = it })
                    }
                }

                item {
                    OutlinedTextField(
                        value = verificationNote,
                        onValueChange = { verificationNote = it },
                        label = { Text("Verification Note (Optional)") },
                        placeholder = { Text("e.g. Official broadcast partner rights verified") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Best-effort test stream button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val (isValid, msg) = StreamHealthChecker.validateStreamUrl(url, protocol)
                                if (!isValid) {
                                    errorMessage = msg
                                    return@OutlinedButton
                                }
                                isTesting = true
                                testStatus = StreamHealthInfo(status = HealthStatus.CHECKING, details = "Testing stream...")
                                coroutineScope.launch {
                                    val result = StreamHealthChecker.checkStreamHealth(url, protocol)
                                    testStatus = result
                                    isTesting = false
                                    if (result.status == HealthStatus.ONLINE) {
                                        isAuthorized = true
                                    }
                                }
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, TurquoisePrimary),
                            modifier = Modifier.testTag("test_stream_in_dialog_button")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTesting) "Testing..." else "Test Stream", color = TurquoisePrimary, fontSize = 12.sp)
                        }

                        testStatus?.let { ts ->
                            Text(
                                text = "${ts.status.name}: ${ts.details}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (ts.status) {
                                    HealthStatus.ONLINE -> Color(0xFF10B981)
                                    HealthStatus.OFFLINE -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    item {
                        Text(text = err, color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) {
                        errorMessage = "Stream label cannot be empty"
                        return@Button
                    }
                    val (validUrl, urlErr) = StreamHealthChecker.validateStreamUrl(url, protocol)
                    if (!validUrl) {
                        errorMessage = urlErr
                        return@Button
                    }
                    val updatedStream = initialStream.copy(
                        label = label.trim(),
                        url = url.trim(),
                        protocol = protocol,
                        quality = quality,
                        language = language.trim().ifBlank { "English" },
                        region = region.trim().ifBlank { "Global" },
                        isEnabled = isEnabled,
                        isBackup = isBackup,
                        isAuthorized = isAuthorized,
                        verificationNote = verificationNote.trim().ifBlank { null }
                    )
                    onSave(updatedStream)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                modifier = Modifier.testTag("save_stream_confirm_button")
            ) {
                Text("Save Stream", color = NavyBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = NavySurface
    )
}

@Composable
private fun ImportChannelStreamDialog(
    channels: List<ChannelEntity>,
    onDismiss: () -> Unit,
    onSelectChannelStream: (StreamSource) -> Unit
) {
    val allChannelStreams = remember(channels) {
        channels.flatMap { ch -> ch.streamSources.map { Pair(ch.name, it) } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Assign Channel Stream", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            if (allChannelStreams.isEmpty()) {
                Text("No reusable channel streams configured in the Channels section.", color = TextSecondary)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(allChannelStreams) { _, (channelName, stream) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectChannelStream(stream) },
                            colors = CardDefaults.cardColors(containerColor = NavyCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stream.label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(text = "From: $channelName • ${stream.quality}", color = TextSecondary, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.AddCircle, contentDescription = "Assign", tint = TurquoisePrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = NavySurface
    )
}

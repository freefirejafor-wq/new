package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchEntity
import com.example.data.model.MatchStatus
import com.example.ui.theme.*

@Composable
fun ManualOverrideDialog(
    match: MatchEntity,
    onDismiss: () -> Unit,
    onSaveOverride: (scoreHome: Int?, scoreAway: Int?, status: MatchStatus, matchTime: String, venue: String?) -> Unit,
    onRestoreProviderValues: (() -> Unit)? = null
) {
    var homeScoreText by remember { mutableStateOf(match.scoreHome?.toString() ?: "") }
    var awayScoreText by remember { mutableStateOf(match.scoreAway?.toString() ?: "") }
    var status by remember { mutableStateOf(match.matchStatus) }
    var matchTime by remember { mutableStateOf(match.matchTime) }
    var venueText by remember { mutableStateOf(match.venue ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Manual Match Override",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "${match.homeTeam} vs ${match.awayTeam}",
                    color = TurquoisePrimary,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Carefully Scoped Override: Overridden values take precedence over automatic provider sync. You can restore provider values at any time.",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                // Raw Provider baseline info
                item {
                    Text(
                        text = "Provider Baseline: ${match.rawProviderScoreHome ?: "-"} - ${match.rawProviderScoreAway ?: "-"} • ${match.rawProviderStatus?.name ?: match.matchStatus.name} • ${match.rawProviderMatchTime ?: match.matchTime}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Scores
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = homeScoreText,
                            onValueChange = { homeScoreText = it },
                            label = { Text("${match.homeTeam} Score") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("override_home_score_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurquoisePrimary,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = awayScoreText,
                            onValueChange = { awayScoreText = it },
                            label = { Text("${match.awayTeam} Score") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("override_away_score_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurquoisePrimary,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }

                // Status Chips
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Match Status", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(MatchStatus.LIVE, MatchStatus.HALFTIME, MatchStatus.UPCOMING).forEach { st ->
                                FilterChip(
                                    selected = status == st,
                                    onClick = { status = st },
                                    label = { Text(st.name, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(MatchStatus.FINISHED, MatchStatus.POSTPONED, MatchStatus.CANCELLED).forEach { st ->
                                FilterChip(
                                    selected = status == st,
                                    onClick = { status = st },
                                    label = { Text(st.name, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Match Clock
                item {
                    OutlinedTextField(
                        value = matchTime,
                        onValueChange = { matchTime = it },
                        label = { Text("Match Clock / Time Display") },
                        placeholder = { Text("e.g. 68', HT, FT, 09:15 PM") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("override_clock_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Venue
                item {
                    OutlinedTextField(
                        value = venueText,
                        onValueChange = { venueText = it },
                        label = { Text("Venue (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val homeInt = homeScoreText.toIntOrNull()
                    val awayInt = awayScoreText.toIntOrNull()
                    onSaveOverride(homeInt, awayInt, status, matchTime.trim().ifBlank { match.matchTime }, venueText.trim().ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                modifier = Modifier.testTag("save_override_confirm_button")
            ) {
                Text("Apply Override", color = NavyBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (match.isManuallyOverridden && onRestoreProviderValues != null) {
                    TextButton(
                        onClick = {
                            onRestoreProviderValues()
                            onDismiss()
                        }
                    ) {
                        Text("Reset to Provider Data", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        },
        containerColor = NavySurface
    )
}

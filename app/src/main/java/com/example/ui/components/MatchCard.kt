package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MatchEntity
import com.example.data.model.MatchStatus
import com.example.ui.theme.*

@Composable
fun MatchCard(
    match: MatchEntity,
    onMatchClick: (MatchEntity) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteAction: Boolean = true,
    showStreamCount: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live-pulse-${match.id}")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live-dot-alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
            .clickable { onMatchClick(match) }
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (match.sportId.lowercase()) {
                        "cricket" -> Icons.Default.SportsCricket
                        "football", "soccer" -> Icons.Default.SportsSoccer
                        else -> Icons.Default.Sports
                    },
                    contentDescription = match.sportName,
                    tint = TurquoisePrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = match.competition.uppercase(),
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.2.sp
                )
                if (match.isDemoData && showFavoriteAction) {
                    Text(
                        text = "DEMO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }
                if (showFavoriteAction) {
                    IconButton(
                        onClick = { onToggleFavorite(match.id, match.isFavorite) },
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("fav_button_${match.id}")
                    ) {
                        Icon(
                            imageVector = if (match.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (match.isFavorite) "Remove from favourites" else "Add to favourites",
                            tint = if (match.isFavorite) GoldStar else TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamSide(
                    name = match.homeTeam,
                    logoUrl = match.homeLogoUrl,
                    sportId = match.sportId,
                    modifier = Modifier.weight(1f)
                )

                MatchStatusAndScore(
                    match = match,
                    liveDotAlpha = dotAlpha,
                    modifier = Modifier.weight(0.82f)
                )

                TeamSide(
                    name = match.awayTeam,
                    logoUrl = match.awayLogoUrl,
                    sportId = match.sportId,
                    isAway = true,
                    modifier = Modifier.weight(1f)
                )
            }

            val enabledStreams = match.streamSources.count { it.isEnabled }
            if (enabledStreams > 0 && showStreamCount) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "▶  Watch stream · $enabledStreams",
                    modifier = Modifier.align(Alignment.End),
                    fontSize = 9.sp,
                    color = TurquoiseSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TeamSide(
    name: String,
    logoUrl: String?,
    sportId: String,
    modifier: Modifier = Modifier,
    isAway: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isAway) Arrangement.End else Arrangement.Start
    ) {
        if (!isAway) {
            TeamLogoBadge(name = name, logoUrl = logoUrl, sportId = sportId)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = name,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isAway) TextAlign.End else TextAlign.Start
        )
        if (isAway) {
            Spacer(modifier = Modifier.width(6.dp))
            TeamLogoBadge(name = name, logoUrl = logoUrl, sportId = sportId)
        }
    }
}

@Composable
private fun MatchStatusAndScore(
    match: MatchEntity,
    liveDotAlpha: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (match.matchStatus) {
            MatchStatus.LIVE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LivePulseDot.copy(alpha = liveDotAlpha))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonLiveRed)
                }
                Text(match.matchTime, fontSize = 10.sp, color = Color.White, maxLines = 1)
            }

            MatchStatus.HALFTIME -> {
                StatusLabel("HALF-TIME", Color(0xFFF59E0B))
                Text(
                    text = "${match.scoreHome ?: 0} - ${match.scoreAway ?: 0}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TurquoisePrimary,
                    maxLines = 1
                )
                Text("HT", fontSize = 9.sp, color = TextSecondary)
            }

            MatchStatus.UPCOMING -> {
                Text(
                    text = match.matchTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = match.scheduledDate,
                    fontSize = 9.sp,
                    color = TurquoiseSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            MatchStatus.FINISHED -> {
                Text(
                    text = "${match.scoreHome ?: 0} - ${match.scoreAway ?: 0}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                StatusLabel("FINISHED", TextMuted)
            }

            MatchStatus.POSTPONED -> StatusLabel("POSTPONED", Color(0xFFEAB308))
            MatchStatus.CANCELLED -> StatusLabel("CANCELLED", Color(0xFFEF4444))
        }
    }
}

@Composable
private fun StatusLabel(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(tint.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = tint, maxLines = 1)
    }
}

@Composable
fun TeamLogoBadge(name: String, logoUrl: String? = null, sportId: String = "football") {
    val initial = name.firstOrNull()?.uppercase() ?: "T"
    val badgeColors = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
        Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1)
    )
    val color = badgeColors[Math.floorMod(name.hashCode(), badgeColors.size)]
    val flag = if (sportId.equals("cricket", ignoreCase = true)) countryFlag(name) else null

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = flag ?: initial,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (flag != null) 20.sp else 13.sp,
            color = Color.White,
            maxLines = 1
        )
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "$name team logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(CircleShape)
            )
        }
    }
}

private fun countryFlag(teamName: String): String? {
    val name = teamName.trim().lowercase()
    return when {
        name.contains("bangladesh") || name == "ban" -> "🇧🇩"
        name.contains("pakistan") || name == "pak" -> "🇵🇰"
        name.contains("sri lanka") || name == "sl" -> "🇱🇰"
        name.contains("india") || name == "ind" -> "🇮🇳"
        name.contains("australia") || name == "aus" -> "🇦🇺"
        name.contains("south africa") || name == "sa" -> "🇿🇦"
        name.contains("new zealand") || name == "nz" -> "🇳🇿"
        name.contains("england") || name == "eng" -> "🇬🇧"
        name.contains("afghanistan") || name == "afg" -> "🇦🇫"
        name.contains("ireland") || name == "ire" -> "🇮🇪"
        name.contains("zimbabwe") || name == "zim" -> "🇿🇼"
        else -> null
    }
}

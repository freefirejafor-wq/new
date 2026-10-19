package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import com.example.data.model.MatchEntity
import com.example.data.model.MatchStatus
import com.example.data.model.SportCategoryEntity
import com.example.data.model.SystemConfigEntity
import com.example.ui.components.CricwavesWidget
import com.example.ui.components.MatchoraTopBar
import com.example.ui.components.StatusFilter
import com.example.ui.theme.*

private val ScoreboardBlue = Color(0xFF3B82F6)
private val ScoreboardRed = Color(0xFFEF4444)
private val ScoreboardCard = Color(0xFF111C2E)
private val ScoreboardBorder = Color(0xFF26354A)

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    matches: List<MatchEntity>,
    sports: List<SportCategoryEntity>,
    systemConfig: SystemConfigEntity?,
    isAdminLoggedIn: Boolean,
    onMatchClick: (MatchEntity) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenAdmin: () -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedSportId by remember { mutableStateOf("all") }
    var selectedStatus by remember { mutableStateOf(StatusFilter.LIVE) }

    val sportCounts = remember(matches) {
        val counts = mutableMapOf<String, Int>()
        counts["all"] = matches.size
        matches.forEach { match ->
            counts[match.sportId] = (counts[match.sportId] ?: 0) + 1
        }
        // Cricket match totals come from the embedded provider, not the Room match table.
        counts["cricket"] = -1
        counts
    }

    val liveCount = remember(matches) {
        matches.count { it.matchStatus == MatchStatus.LIVE || it.matchStatus == MatchStatus.HALFTIME }
    }
    val upcomingCount = remember(matches) { matches.count { it.matchStatus == MatchStatus.UPCOMING } }
    val finishedCount = remember(matches) { matches.count { it.matchStatus == MatchStatus.FINISHED } }

    val filteredMatches = remember(matches, selectedSportId, selectedStatus, searchQuery) {
        matches.filter { match ->
            val matchesSport = selectedSportId == "all" || match.sportId == selectedSportId
            val matchesStatus = when (selectedStatus) {
                StatusFilter.ALL -> true
                StatusFilter.LIVE ->
                    match.matchStatus == MatchStatus.LIVE || match.matchStatus == MatchStatus.HALFTIME
                StatusFilter.UPCOMING -> match.matchStatus == MatchStatus.UPCOMING
                StatusFilter.FINISHED -> match.matchStatus == MatchStatus.FINISHED
            }
            val matchesSearch = searchQuery.isBlank() ||
                match.homeTeam.contains(searchQuery, ignoreCase = true) ||
                match.awayTeam.contains(searchQuery, ignoreCase = true) ||
                match.competition.contains(searchQuery, ignoreCase = true) ||
                match.sportName.contains(searchQuery, ignoreCase = true)
            matchesSport && matchesStatus && matchesSearch
        }
    }
    val showCricketWidget = selectedSportId == "cricket" && searchQuery.isBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        MatchoraTopBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            isSearching = isSearching,
            onToggleSearch = {
                isSearching = !isSearching
                if (!isSearching) searchQuery = ""
            },
            isSyncing = isSyncing,
            onRefresh = onRefresh,
            onOpenAdmin = onOpenAdmin,
            isMaintenanceActive = systemConfig?.isMaintenanceMode == true,
            isAdminLoggedIn = isAdminLoggedIn
        )

        HomeSportFilters(
            sports = sports,
            selectedSportId = selectedSportId,
            matchCounts = sportCounts,
            onSportSelected = { selectedSportId = it }
        )

        HomeStatusTabs(
            selectedFilter = selectedStatus,
            liveCount = liveCount,
            upcomingCount = upcomingCount,
            finishedCount = finishedCount,
            onFilterSelected = { selectedStatus = it }
        )

        if (filteredMatches.isEmpty() && !showCricketWidget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No matches",
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matches found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try switching sport filter or adjusting your search.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (showCricketWidget) {
                    item(key = "cricwaves_cricket_feed") {
                        CricwavesWidget(filter = selectedStatus)
                    }
                }
                items(
                    items = filteredMatches,
                    key = { it.id }
                ) { match ->
                    ScoreboardMatchCard(
                        match = match,
                        onClick = { onMatchClick(match) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSportFilters(
    sports: List<SportCategoryEntity>,
    selectedSportId: String,
    matchCounts: Map<String, Int>,
    onSportSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(sports, key = { it.id }) { sport ->
            val isSelected = sport.id == selectedSportId
            val count = matchCounts[sport.id] ?: 0
            Column(
                modifier = Modifier
                    .width(62.dp)
                    .clickable { onSportSelected(sport.id) }
                    .testTag("sport_chip_${sport.id}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ScoreboardCard)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ScoreboardBlue else ScoreboardBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (sport.id.equals("all", ignoreCase = true)) {
                        Text(
                            text = "ALL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = sportIcon(sport.id),
                            contentDescription = sport.name,
                            tint = if (isSelected) ScoreboardBlue else Color(0xFF9CA3AF),
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    if (count > 0) {
                        Text(
                            text = count.toString(),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .clip(CircleShape)
                                .background(ScoreboardRed)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    } else if (count < 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ScoreboardRed)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sport.name,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) ScoreboardBlue else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun sportIcon(sportId: String): ImageVector = when (sportId.lowercase()) {
    "football", "soccer" -> Icons.Default.SportsSoccer
    "cricket" -> Icons.Default.SportsCricket
    else -> Icons.Default.Sports
}

@Composable
private fun HomeStatusTabs(
    selectedFilter: StatusFilter,
    liveCount: Int,
    upcomingCount: Int,
    finishedCount: Int,
    onFilterSelected: (StatusFilter) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "home-live-pulse")
    val liveDotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home-live-dot-alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeStatusTab(
            title = "Live ($liveCount)",
            selected = selectedFilter == StatusFilter.LIVE,
            showLiveDot = true,
            liveDotAlpha = liveDotAlpha,
            modifier = Modifier.weight(1f),
            testTag = "status_tab_Live",
            onClick = { onFilterSelected(StatusFilter.LIVE) }
        )
        HomeStatusTab(
            title = "Upcoming ($upcomingCount)",
            selected = selectedFilter == StatusFilter.UPCOMING,
            modifier = Modifier.weight(1f),
            testTag = "status_tab_Upcoming",
            onClick = { onFilterSelected(StatusFilter.UPCOMING) }
        )
        HomeStatusTab(
            title = "Finished ($finishedCount)",
            selected = selectedFilter == StatusFilter.FINISHED,
            modifier = Modifier.weight(1f),
            testTag = "status_tab_Finished",
            onClick = { onFilterSelected(StatusFilter.FINISHED) }
        )
    }
}

@Composable
private fun HomeStatusTab(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    showLiveDot: Boolean = false,
    liveDotAlpha: Float = 1f,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) ScoreboardBlue.copy(alpha = 0.17f) else ScoreboardCard)
            .border(
                width = 1.dp,
                color = if (selected) ScoreboardBlue.copy(alpha = 0.5f) else ScoreboardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (showLiveDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ScoreboardRed.copy(alpha = liveDotAlpha))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) ScoreboardBlue else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScoreboardMatchCard(
    match: MatchEntity,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "card-live-pulse-${match.id}")
    val liveDotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card-live-dot-alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ScoreboardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = ScoreboardCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = sportIcon(match.sportId),
                    contentDescription = match.sportName,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${match.sportName} | ${match.competition}".uppercase(),
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD1D5DB),
                    letterSpacing = 0.35.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeTeamSide(
                    name = match.homeTeam,
                    logoUrl = match.homeLogoUrl,
                    modifier = Modifier.weight(1f)
                )
                HomeMatchStatus(
                    match = match,
                    liveDotAlpha = liveDotAlpha,
                    modifier = Modifier.weight(0.82f)
                )
                HomeTeamSide(
                    name = match.awayTeam,
                    logoUrl = match.awayLogoUrl,
                    isAway = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeTeamSide(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    isAway: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isAway) Arrangement.End else Arrangement.Start
    ) {
        if (!isAway) {
            HomeTeamBadge(name = name, logoUrl = logoUrl)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = name,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE5E7EB),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isAway) TextAlign.End else TextAlign.Start
        )
        if (isAway) {
            Spacer(modifier = Modifier.width(6.dp))
            HomeTeamBadge(name = name, logoUrl = logoUrl)
        }
    }
}

@Composable
private fun HomeTeamBadge(
    name: String,
    logoUrl: String?
) {
    val badgeColors = listOf(
        Color(0xFFE53935),
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFFB8C00),
        Color(0xFF8E24AA),
        Color(0xFF00ACC1)
    )
    val badgeColor = badgeColors[Math.floorMod(name.hashCode(), badgeColors.size)]
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.22f))
            .border(1.dp, badgeColor.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "T",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
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

@Composable
private fun HomeMatchStatus(
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
                            .background(ScoreboardRed.copy(alpha = liveDotAlpha))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreboardRed
                    )
                }
                Text(
                    text = match.matchTime,
                    fontSize = 10.sp,
                    color = Color(0xFFD1D5DB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            MatchStatus.HALFTIME -> {
                Text(
                    text = "${match.scoreHome ?: 0} - ${match.scoreAway ?: 0}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ScoreboardBlue,
                    maxLines = 1
                )
                Text("HALF-TIME", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            }

            MatchStatus.UPCOMING -> {
                Text(
                    text = match.matchTime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = match.scheduledDate,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            MatchStatus.FINISHED -> {
                Text(
                    text = "${match.scoreHome ?: 0} - ${match.scoreAway ?: 0}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text("FINISHED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            }

            MatchStatus.POSTPONED -> StatusPill("POSTPONED", Color(0xFFEAB308))
            MatchStatus.CANCELLED -> StatusPill("CANCELLED", ScoreboardRed)
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1
        )
    }
}

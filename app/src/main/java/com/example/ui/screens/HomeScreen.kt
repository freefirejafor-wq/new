package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

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
    var selectedStatus by remember { mutableStateOf(StatusFilter.ALL) }

    // Calculate match counts dynamically
    val sportCounts = remember(matches) {
        val counts = mutableMapOf<String, Int>()
        counts["all"] = matches.size
        matches.forEach { match ->
            counts[match.sportId] = (counts[match.sportId] ?: 0) + 1
        }
        counts
    }

    val liveCount = remember(matches) { matches.count { it.matchStatus == MatchStatus.LIVE } }
    val upcomingCount = remember(matches) { matches.count { it.matchStatus == MatchStatus.UPCOMING } }
    val finishedCount = remember(matches) { matches.count { it.matchStatus == MatchStatus.FINISHED } }

    // Filter matches based on user selections
    val filteredMatches = remember(matches, selectedSportId, selectedStatus, searchQuery) {
        matches.filter { match ->
            // Sport filter
            val matchesSport = selectedSportId == "all" || match.sportId == selectedSportId
            // Status filter
            val matchesStatus = when (selectedStatus) {
                StatusFilter.ALL -> true
                StatusFilter.LIVE -> match.matchStatus == MatchStatus.LIVE
                StatusFilter.UPCOMING -> match.matchStatus == MatchStatus.UPCOMING
                StatusFilter.FINISHED -> match.matchStatus == MatchStatus.FINISHED
            }
            // Search query filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                match.homeTeam.contains(searchQuery, ignoreCase = true) ||
                        match.awayTeam.contains(searchQuery, ignoreCase = true) ||
                        match.competition.contains(searchQuery, ignoreCase = true) ||
                        match.sportName.contains(searchQuery, ignoreCase = true)
            }
            matchesSport && matchesStatus && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        // TOP APP BAR
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

        // OWNER MAINTENANCE BANNER
        if (systemConfig?.isMaintenanceMode == true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LivePulseDot.copy(alpha = 0.15f))
                    .border(1.dp, LivePulseDot.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Maintenance",
                        tint = LivePulseDot,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = systemConfig.maintenanceMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // DEMO DATA TRANSPARENCY NOTICE (as requested by prompt)
        if (systemConfig?.isUsingDemoData != false) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavyCard)
                    .border(0.5.dp, TurquoisePrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DEMO DATA — Live preview fixtures. Real API sync configurable in Admin.",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "Admin",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoisePrimary,
                        modifier = Modifier.clickable { onOpenAdmin() }
                    )
                }
            }
        }

        // HORIZONTALLY SCROLLABLE SPORTS FILTERS
        SportFilterChips(
            sports = sports,
            selectedSportId = selectedSportId,
            matchCounts = sportCounts,
            onSportSelected = { selectedSportId = it }
        )

        // STATUS TABS (All, Live, Upcoming, Finished)
        StatusFilterTabs(
            selectedFilter = selectedStatus,
            allCount = matches.size,
            liveCount = liveCount,
            upcomingCount = upcomingCount,
            finishedCount = finishedCount,
            onFilterSelected = { selectedStatus = it }
        )

        // MATCHES LIST
        if (filteredMatches.isEmpty()) {
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
                        color = TextSecondary
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
                items(
                    items = filteredMatches,
                    key = { it.id }
                ) { match ->
                    MatchCard(
                        match = match,
                        onMatchClick = onMatchClick,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

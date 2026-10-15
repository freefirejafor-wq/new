package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchEntity
import com.example.data.model.SportCategoryEntity
import com.example.ui.components.MatchCard
import com.example.ui.theme.*

@Composable
fun SportsScreen(
    sports: List<SportCategoryEntity>,
    matches: List<MatchEntity>,
    onMatchClick: (MatchEntity) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSport by remember { mutableStateOf(sports.firstOrNull { it.id != "all" }?.id ?: "football") }

    val sportMatches = remember(matches, selectedSport) {
        matches.filter { it.sportId == selectedSport }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = TurquoisePrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Sports Hub",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Browse schedules & fixtures by sport",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Sport Selector Horizontal Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(sports.filter { it.id != "all" }) { sport ->
                val isSelected = sport.id == selectedSport
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) TurquoisePrimary else NavyCard)
                        .border(1.dp, if (isSelected) TurquoisePrimary else NavyBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedSport = sport.id }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = sport.name,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) NavyBackground else Color.White
                    )
                }
            }
        }

        // Sport Matches List
        if (sportMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matches scheduled for this sport yet.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = sportMatches,
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

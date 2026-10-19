package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SportCategoryEntity
import com.example.ui.theme.*

@Composable
fun SportFilterChips(
    sports: List<SportCategoryEntity>,
    selectedSportId: String,
    matchCounts: Map<String, Int>,
    onSportSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        items(sports) { sport ->
            val isSelected = sport.id == selectedSportId
            SportChipItem(
                sport = sport,
                count = matchCounts[sport.id] ?: 0,
                isSelected = isSelected,
                onClick = { onSportSelected(sport.id) }
            )
        }
    }
}

@Composable
private fun SportChipItem(
    sport: SportCategoryEntity,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconVector: ImageVector = when (sport.id.lowercase()) {
        "all" -> Icons.Default.AllInclusive
        "football", "soccer" -> Icons.Default.SportsSoccer
        "cricket" -> Icons.Default.SportsCricket
        "basketball" -> Icons.Default.SportsBasketball
        "boxing" -> Icons.Default.SportsMma
        "motorsports" -> Icons.Default.SportsMotorsports
        "tennis" -> Icons.Default.SportsTennis
        else -> Icons.Default.Sports
    }

    Box(
        modifier = Modifier
            .width(if (sport.id.equals("all", ignoreCase = true)) 82.dp else 62.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(NavyCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) TurquoisePrimary else NavyBorder,
                shape = RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
            .testTag("sport_chip_" + sport.id),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 3.dp, top = 3.dp)
                .clip(CircleShape)
                .background(LivePulseDot)
                .border(0.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                .padding(horizontal = if (count >= 0) 4.dp else 5.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            if (count >= 0) {
                Text(
                    text = count.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            } else {
                Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White))
            }
        }

        if (sport.id.equals("all", ignoreCase = true)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = sport.name,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "ALL",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(NavyCard.copy(alpha = 0.92f))
                        .padding(horizontal = 2.dp)
                )
            }
        } else {
            Icon(
                imageVector = iconVector,
                contentDescription = sport.name,
                tint = Color.White,
                modifier = Modifier.size(35.dp)
            )
        }
    }
}

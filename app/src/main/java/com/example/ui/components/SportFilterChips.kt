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
import androidx.compose.ui.graphics.Brush
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
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sports) { sport ->
            val isSelected = sport.id == selectedSportId
            val count = matchCounts[sport.id] ?: 0

            SportChipItem(
                sport = sport,
                count = count,
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
    val iconVector: ImageVector = when (sport.id) {
        "all" -> Icons.Default.AllInclusive
        "football" -> Icons.Default.SportsSoccer
        "cricket" -> Icons.Default.SportsCricket
        "basketball" -> Icons.Default.SportsBasketball
        "boxing" -> Icons.Default.SportsMma
        "motorsports" -> Icons.Default.SportsMotorsports
        "tennis" -> Icons.Default.SportsTennis
        else -> Icons.Default.Sports
    }

    val backgroundBrush = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                TurquoisePrimary.copy(alpha = 0.35f),
                NavyCardElevated
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                NavyCard,
                NavySurface
            )
        )
    }

    Box(
        modifier = Modifier
            .width(66.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) TurquoisePrimary else NavyBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("sport_chip_${sport.id}"),
        contentAlignment = Alignment.Center
    ) {
        // Count badge on top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) LivePulseDot else NavyCardElevated)
                .border(0.5.dp, if (isSelected) Color.White.copy(alpha = 0.6f) else NavyBorder, CircleShape)
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Sport Icon and label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = sport.name,
                tint = if (isSelected) TurquoisePrimary else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sport.name,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondary,
                maxLines = 1
            )
        }
    }
}

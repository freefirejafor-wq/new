package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.example.ui.theme.*

enum class BottomNavDestination {
    FAVOURITES,
    SPORTS,
    LIVE_EVENTS,
    CATEGORIES,
    MENU
}

@Composable
fun MatchoraBottomBar(
    currentDestination: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit,
    liveMatchCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySurface,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = NavyBorder, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Favourites
            BottomNavItem(
                icon = Icons.Default.Star,
                label = "Favourites",
                isSelected = currentDestination == BottomNavDestination.FAVOURITES,
                onClick = { onNavigate(BottomNavDestination.FAVOURITES) }
            )

            // 2. Sports
            BottomNavItem(
                icon = Icons.Default.SportsSoccer,
                label = "Sports",
                isSelected = currentDestination == BottomNavDestination.SPORTS,
                onClick = { onNavigate(BottomNavDestination.SPORTS) }
            )

            // 3. Live Events (Special Center Radiant Icon)
            LiveCenterNavItem(
                isSelected = currentDestination == BottomNavDestination.LIVE_EVENTS,
                liveCount = liveMatchCount,
                onClick = { onNavigate(BottomNavDestination.LIVE_EVENTS) }
            )

            // 4. Categories & Channels
            BottomNavItem(
                icon = Icons.Default.Tv,
                label = "Categories",
                isSelected = currentDestination == BottomNavDestination.CATEGORIES,
                onClick = { onNavigate(BottomNavDestination.CATEGORIES) }
            )

            // 5. Menu / Settings
            BottomNavItem(
                icon = Icons.Default.Menu,
                label = "Menu",
                isSelected = currentDestination == BottomNavDestination.MENU,
                onClick = { onNavigate(BottomNavDestination.MENU) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("nav_${label.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) TurquoisePrimary else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TurquoisePrimary else TextSecondary
        )
    }
}

@Composable
private fun LiveCenterNavItem(
    isSelected: Boolean,
    liveCount: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag("nav_live_events")
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isSelected) TurquoisePrimary else NavyCard)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) Color.White else TurquoisePrimary.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = "Live Events",
                tint = if (isSelected) NavyBackground else TurquoisePrimary,
                modifier = Modifier.size(24.dp)
            )

            if (liveCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(LivePulseDot)
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Live Events",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) TurquoisePrimary else TextSecondary
        )
    }
}

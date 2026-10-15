package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchStatus
import com.example.ui.theme.*

enum class StatusFilter {
    ALL,
    LIVE,
    UPCOMING,
    FINISHED
}

@Composable
fun StatusFilterTabs(
    selectedFilter: StatusFilter,
    allCount: Int,
    liveCount: Int,
    upcomingCount: Int,
    finishedCount: Int,
    onFilterSelected: (StatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for Live dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val liveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusTabItem(
            title = "All ($allCount)",
            isSelected = selectedFilter == StatusFilter.ALL,
            showCheck = selectedFilter == StatusFilter.ALL,
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelected(StatusFilter.ALL) }
        )

        StatusTabItem(
            title = "Live ($liveCount)",
            isSelected = selectedFilter == StatusFilter.LIVE,
            showLiveDot = true,
            liveDotAlpha = liveAlpha,
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelected(StatusFilter.LIVE) }
        )

        StatusTabItem(
            title = "Upcoming ($upcomingCount)",
            isSelected = selectedFilter == StatusFilter.UPCOMING,
            modifier = Modifier.weight(1.3f),
            onClick = { onFilterSelected(StatusFilter.UPCOMING) }
        )

        StatusTabItem(
            title = "Finished ($finishedCount)",
            isSelected = selectedFilter == StatusFilter.FINISHED,
            modifier = Modifier.weight(1.2f),
            onClick = { onFilterSelected(StatusFilter.FINISHED) }
        )
    }
}

@Composable
private fun StatusTabItem(
    title: String,
    isSelected: Boolean,
    showCheck: Boolean = false,
    showLiveDot: Boolean = false,
    liveDotAlpha: Float = 1f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) TurquoisePrimary.copy(alpha = 0.18f) else NavyCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) TurquoisePrimary else NavyBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag("status_tab_${title.substringBefore(" ")}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (showCheck) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = TurquoisePrimary,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 2.dp)
                )
            }
            if (showLiveDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(LivePulseDot.copy(alpha = liveDotAlpha))
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TurquoisePrimary else TextSecondary,
                maxLines = 1
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchoraTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearching: Boolean,
    onToggleSearch: () -> Unit,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    onOpenAdmin: () -> Unit,
    isMaintenanceActive: Boolean = false,
    isAdminLoggedIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotation"
    )
    val uriHandler = LocalUriHandler.current

    Surface(
        color = NavyBackground,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Football Live Tv",
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )

                IconButton(
                    onClick = { runCatching { uriHandler.openUri("https://t.me/") } },
                    modifier = Modifier.size(40.dp).testTag("top_telegram_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(29.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF229ED9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp).testTag("top_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isSyncing) TurquoisePrimary else Color.White,
                        modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier
                    )
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.size(40.dp).testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearching) TurquoisePrimary else Color.White
                    )
                }
            }

            AnimatedVisibility(visible = isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_input_field"),
                    placeholder = {
                        Text(
                            "Search matches, teams, or competitions...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TurquoisePrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NavyCard,
                        unfocusedContainerColor = NavyCard,
                        focusedBorderColor = TurquoisePrimary,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onToggleSearch() })
                )
            }
        }
    }
}

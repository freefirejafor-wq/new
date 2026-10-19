package com.example.ui.components

import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TurquoisePrimary

private const val ALL_CRICKET_URL =
    "https://www.cricwaves.com/cricket/widgets/!/f1_kzd/www.cricwaves.com/1/2/All/All/All/ind/w"
private const val LIVE_CRICKET_URL =
    "https://www.cricwaves.com/cricket/widgets/live/!/f1_kzd/www.cricwaves.com/1/2/All/All/All/ind/w"
private const val RECENT_CRICKET_URL =
    "https://www.cricwaves.com/cricket/widgets/completed/!/f1_kzd/www.cricwaves.com/1/2/All/All/All/ind/w"
private const val UPCOMING_CRICKET_URL =
    "https://www.cricwaves.com/cricket/widgets/up-coming/!/f1_kzd/www.cricwaves.com/1/2/All/All/All/ind/w"

@Composable
fun CricwavesWidget(
    filter: StatusFilter,
    modifier: Modifier = Modifier
) {
    val feedUrl = when (filter) {
        StatusFilter.ALL -> ALL_CRICKET_URL
        StatusFilter.LIVE -> LIVE_CRICKET_URL
        StatusFilter.UPCOMING -> UPCOMING_CRICKET_URL
        StatusFilter.FINISHED -> RECENT_CRICKET_URL
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(15.dp))
            .border(1.dp, NavyBorder, RoundedCornerShape(15.dp)),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SportsCricket,
                    contentDescription = "Cricket",
                    tint = TurquoisePrimary
                )
                Text(
                    text = "CRICKET · LIVE SCORES",
                    modifier = Modifier.padding(start = 7.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "LIVE · UPCOMING · RECENT",
                    color = TextMuted,
                    fontSize = 8.sp
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(304.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(NavyBackground),
                factory = { context ->
                    WebView(context).apply {
                        tag = feedUrl
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(false)
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        loadUrl(feedUrl)
                    }
                },
                update = { webView ->
                    if (webView.tag != feedUrl) {
                        webView.tag = feedUrl
                        webView.loadUrl(feedUrl)
                    }
                }
            )
        }
    }
}

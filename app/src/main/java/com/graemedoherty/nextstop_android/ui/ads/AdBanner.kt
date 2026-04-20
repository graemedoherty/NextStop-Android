package com.graemedoherty.nextstop_android.ui.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            // ⚡ THE FIX: Change background to Transparent to allow the map to show through
            .background(Color.Transparent),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdUnitId(adUnitId)
                setAdSize(AdSize.BANNER)
                // Ensure the underlying Android View doesn't have a solid background
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
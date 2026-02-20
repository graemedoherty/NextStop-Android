package com.example.nextstop_android.ui.stepper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextstop_android.ui.components.PrimaryButton
import com.example.nextstop_android.ui.components.SecondaryButton

@Composable
fun Step3Screen(
    selectedTransport: String,
    selectedStation: String,
    distanceMeters: Int?,
    onAlarmSet: () -> Unit,
    onBack: () -> Unit
) {
    val themePurple = Color(0xFF6F66E3)
    val onSurfaceText = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantText = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ───────────────── TOP: ALARM PREVIEW CARD ─────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = themePurple.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Station details
                    Column(modifier = Modifier.weight(1f)) {


                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = selectedStation,
                            fontSize = 18.sp,
                            color = onSurfaceText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Transport + Distance row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedTransport,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onSurfaceVariantText
                            )

                            Text(
                                text = " • ",
                                fontSize = 13.sp,
                                color = onSurfaceVariantText.copy(alpha = 0.5f)
                            )

                            Text(
                                text = formatDistance(distanceMeters),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = themePurple
                            )
                        }
                    }

                    // Right: Large alarm icon
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = "Alarm Ready",
                        tint = themePurple,
                        modifier = Modifier
                            .size(58.dp)
                            .padding(start = 6.dp)
                    )
                }
            }

            // ───────────────── BOTTOM: NAVIGATION BUTTONS ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Set Alarm",
                    onClick = onAlarmSet,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatDistance(meters: Int?): String {
    return when {
        meters == null || meters < 0 -> "Calculating..."
        meters >= 1000 -> "%.1f km".format(meters / 1000.0)
        else -> "$meters m"
    }
}
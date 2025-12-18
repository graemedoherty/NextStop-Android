package com.example.nextstop_android.ui.maps

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class AlarmStatus {
    ACTIVE,
    ARRIVED
}

@Composable
fun AlarmCard(
    destination: String,
    distanceMeters: Int,
    status: AlarmStatus,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val borderColor = when (status) {
        AlarmStatus.ACTIVE -> Color(0xFF6F66E4).copy(alpha = glowAlpha)
        AlarmStatus.ARRIVED -> Color(0xFF181515).copy(alpha = glowAlpha)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = borderColor,
                spotColor = borderColor
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top row: status + cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status)

                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destination
            Text(
                text = destination,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Distance display
            if (status == AlarmStatus.ACTIVE) {
                DistanceDisplay(distanceMeters)
            } else {
                Text(
                    text = "You have arrived at your destination!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DistanceDisplay(distanceMeters: Int) {
    val displayText = when {
        distanceMeters >= 1000 -> {
            val km = distanceMeters / 1000.0
            "%.1f km away".format(km)
        }
        else -> "$distanceMeters m away"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Distance:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusBadge(status: AlarmStatus) {
    val (text, color) = when (status) {
        AlarmStatus.ACTIVE -> "Active" to Color(0xFF2E7D32) // Green
        AlarmStatus.ARRIVED -> "Arrived" to Color(0xFFC62828) // Red
    }

    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
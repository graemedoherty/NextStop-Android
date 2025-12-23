package com.example.nextstop_android.ui.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelInProgress by remember { mutableStateOf(false) }

    // 🔑 Auto-dismiss dialog if state changes (alarm reset elsewhere)
    LaunchedEffect(status) {
        if (status != AlarmStatus.ACTIVE) {
            showCancelDialog = false
            cancelInProgress = false
        }
    }

    val borderColor = when (status) {
        AlarmStatus.ACTIVE -> MaterialTheme.colorScheme.outline
        AlarmStatus.ARRIVED -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destination
            Text(
                text = destination,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status content
            if (status == AlarmStatus.ACTIVE) {
                DistanceDisplay(distanceMeters)
            } else {
                Text(
                    text = "You have arrived at your destination!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stop Alarm Button (always visible)
            Button(
                onClick = { showCancelDialog = true },
                enabled = !cancelInProgress,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    // 🔑 Using a high-visibility transport red
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (status == AlarmStatus.ARRIVED) "Dismiss" else "Cancel Alarm",
                    color = Color.White
                )
            }
        }
    }

    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!cancelInProgress) showCancelDialog = false
            },
            title = { Text("Stop Alarm?") },
            text = {
                Text(
                    if (status == AlarmStatus.ARRIVED)
                        "Dismiss the arrival notification?"
                    else
                        "Are you sure you want to stop your alarm for $destination?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !cancelInProgress,
                    onClick = {
                        cancelInProgress = true
                        showCancelDialog = false
                        onCancel() // 🔑 This triggers full reset via MapViewModel
                    }
                ) {
                    Text(
                        text = if (status == AlarmStatus.ARRIVED) "Dismiss" else "Yes, Stop",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !cancelInProgress,
                    onClick = { showCancelDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DistanceDisplay(distanceMeters: Int) {
    val displayText = when {
        distanceMeters < 0 -> "Calculating distance..."
        distanceMeters >= 1000 -> "%.1f km away".format(distanceMeters / 1000.0)
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
            color = if (distanceMeters < 0)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusBadge(status: AlarmStatus) {
    val (text, color) = when (status) {
        AlarmStatus.ACTIVE -> "Active" to Color(0xFF2E7D32)
        AlarmStatus.ARRIVED -> "Arrived" to Color(0xFFC62828)
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
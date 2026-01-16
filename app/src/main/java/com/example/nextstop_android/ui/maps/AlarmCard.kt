package com.example.nextstop_android.ui.maps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

            // ────── TOP ROW: Destination + Status Badge ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Destination
                Text(
                    text = destination,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Right: Status Badge
                StatusBadge(status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ────── BOTTOM ROW: Distance + Cancel Button ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Distance or Arrival Message
                Box(modifier = Modifier.weight(1f)) {
                    if (status == AlarmStatus.ACTIVE) {
                        DistanceDisplay(distanceMeters)
                    } else {
                        Text(
                            text = "You've arrived!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right: Cancel Button
// Right: Cancel Button
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    enabled = !cancelInProgress,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFD32F2F))
                ) {
                    Text(
                        text = if (status == AlarmStatus.ARRIVED) "Dismiss" else "Cancel",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Cancel confirmation dialog (same as before)
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
                        onCancel()
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
        distanceMeters < 0 -> "Calculating..."
        distanceMeters >= 1000 -> "%.1f km away".format(distanceMeters / 1000.0)
        else -> "$distanceMeters m away"
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.titleMedium,
        color = if (distanceMeters < 0)
            MaterialTheme.colorScheme.onSurfaceVariant
        else
            MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
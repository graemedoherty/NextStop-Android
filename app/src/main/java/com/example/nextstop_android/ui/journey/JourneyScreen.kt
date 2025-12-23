package com.example.nextstop_android.ui.journey

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.example.nextstop_android.ui.maps.AlarmCard
import com.example.nextstop_android.ui.maps.AlarmStatus

@Composable
fun JourneyScreen() {
    val mapViewModel: MapViewModel = viewModel()
    val stepperViewModel: StepperViewModel = viewModel()
    val mapUiState by mapViewModel.uiState.collectAsState()

    // Root is a Column. Inside here, the "Receiver" is ColumnScope.
    Column(modifier = Modifier.fillMaxSize()) {

        // 1. This AnimatedVisibility is a direct child of Column.
        // It handles the "Slide Up" exit.
        AnimatedVisibility(
            visible = !mapUiState.alarmArmed,
            enter = expandVertically(),
            exit = slideOutVertically(
                targetOffsetY = { -it }, // Slides out towards the top
                animationSpec = tween(1000)
            ) + shrinkVertically(animationSpec = tween(1000))
        ) {
            // Content inside the stepper area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                StepperScreen(
                    mapViewModel = mapViewModel,
                    viewModel = stepperViewModel,
                    onAlarmCreated = { station ->
                        mapViewModel.startAlarm(station)
                    }
                )
            }
        }

        // 2. This Box takes the remaining space.
        // When the Stepper disappears, weight(1f) expands this to 100%.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // LAYER 1: The Map
            MapsScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = mapViewModel
            )

            // LAYER 2: The Alarm Card
            // We use the standard AnimatedVisibility here and align it via Modifier.
            androidx.compose.animation.AnimatedVisibility(
                visible = mapUiState.alarmArmed,
                enter = slideInVertically(
                    initialOffsetY = { it }, // Slides in from the bottom
                    animationSpec = tween(1000)
                ) + fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter) // 🔑 Scope fixed
            ) {
                val currentStatus = if (mapUiState.distanceToDestination in 0..100) {
                    AlarmStatus.ARRIVED
                } else {
                    AlarmStatus.ACTIVE
                }

                // Inside JourneyScreen.kt
                AlarmCard(
                    destination = mapUiState.selectedStation?.name ?: "Destination",
                    distanceMeters = mapUiState.distanceToDestination,
                    status = currentStatus,
                    onCancel = {
                        // 🔑 Pass the stepperViewModel here so it can be reset
                        mapViewModel.cancelAlarm(stepperViewModel)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
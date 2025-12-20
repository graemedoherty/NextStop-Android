package com.example.nextstop_android.ui.journey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.viewmodel.StepperViewModel

@Composable
fun JourneyScreen() {
    val mapViewModel: MapViewModel = viewModel()
    val stepperViewModel: StepperViewModel = viewModel()
    val mapUiState by mapViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // 1. Stepper Section
        // We only apply weight if it is visible.
        AnimatedVisibility(
            visible = !mapUiState.alarmArmed,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f) // Explicitly take half height when visible
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

        // 2. Map Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // This will now always fill the REMAINING space
        ) {
            MapsScreen(
                viewModel = mapViewModel,
                destinationStation = null,
                onBack = {
                    stepperViewModel.reset()
                }
            )
        }
    }
}
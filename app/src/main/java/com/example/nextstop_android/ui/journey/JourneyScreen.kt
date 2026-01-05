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
import com.example.nextstop_android.ui.ads.AdBanner
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.maps.MapsScreen
import com.example.nextstop_android.ui.maps.AlarmCard
import com.example.nextstop_android.ui.maps.AlarmStatus
import com.example.nextstop_android.ui.stepper.StepperScreen
import com.example.nextstop_android.viewmodel.StepperViewModel

@Composable
fun JourneyScreen() {

    // 🔑 SINGLE source of truth for ViewModels
    val mapViewModel: MapViewModel = viewModel()
    val stepperViewModel: StepperViewModel = viewModel()

    val mapUiState by mapViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // 🎯 Ad banner
        AdBanner(modifier = Modifier.fillMaxWidth())

        // ───────────────── STEP PER ─────────────────
        AnimatedVisibility(
            visible = !mapUiState.alarmArmed,
            enter = expandVertically(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(1000)
            ) + shrinkVertically(animationSpec = tween(1000))
        ) {
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

        // ───────────────── MAP + ALARM ─────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            // 🗺 MAP
            MapsScreen(
                modifier = Modifier.fillMaxSize(),
                mapViewModel = mapViewModel,
                stepperViewModel = stepperViewModel
            )

            // 🚨 Alarm card
            androidx.compose.animation.AnimatedVisibility(
                visible = mapUiState.alarmArmed,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(1000)
                ) + fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val status =
                    if (mapUiState.distanceToDestination in 0..100)
                        AlarmStatus.ARRIVED
                    else
                        AlarmStatus.ACTIVE

                AlarmCard(
                    destination = mapUiState.selectedStation?.name ?: "Destination",
                    distanceMeters = mapUiState.distanceToDestination,
                    status = status,
                    onCancel = {
                        mapViewModel.cancelAlarm(stepperViewModel)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

package com.example.nextstop_android.ui.stepper

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.viewmodel.StepperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepperScreen(
    onAlarmCreated: (Station) -> Unit,
    mapViewModel: MapViewModel,
    viewModel: StepperViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentStep by viewModel.currentStep.collectAsState()
    val selectedTransport by viewModel.selectedTransport.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        StepIndicators(currentStep)

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "stepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1Screen(
                        selectedTransport = selectedTransport,
                        onTransportSelected = viewModel::selectTransport,
                        onNext = viewModel::nextStep
                    )

// Inside StepperScreen.kt -> Step 2
                    2 -> Step2Screen(
                        selectedTransport = selectedTransport ?: "",
                        savedStation = selectedStation,
                        onStationSelected = { name, lat, lng ->
                            // 1. Update the Stepper state (for the UI/Next button)
                            viewModel.selectStation(name, lat, lng)

                            // 2. 🔑 NEW/FIX: Update the Map state immediately to show the marker
                            val stationObj = Station(
                                name = name,
                                type = selectedTransport ?: "",
                                latitude = lat,
                                longitude = lng,
                                distance = 0
                            )
                            mapViewModel.setDestination(stationObj)
                        },
                        onClearStation = {
                            viewModel.clearStation()
                            mapViewModel.cancelAlarm(viewModel)
                        },
                        onNext = viewModel::nextStep,
                        onBack = viewModel::goBack,
                        mapViewModel = mapViewModel
                    )

                    3 -> Step3Screen(
                        selectedTransport = selectedTransport ?: "",
                        selectedStation = selectedStation?.name ?: "",
// Inside StepperScreen Step 3 onAlarmSet
                        onAlarmSet = {
                            selectedStation?.let { station ->
                                onAlarmCreated(station)

                                // 🔑 THIS IS CRITICAL: Update the service with the new destination
                                val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                                    action = LocationTrackingService.ACTION_SET_DESTINATION
                                    putExtra(LocationTrackingService.EXTRA_DESTINATION_LAT, station.latitude)
                                    putExtra(LocationTrackingService.EXTRA_DESTINATION_LNG, station.longitude)
                                    putExtra(LocationTrackingService.EXTRA_DESTINATION_NAME, station.name)
                                }
                                context.startService(serviceIntent)
                            }
                        },
                        onBack = viewModel::goBack
                    )
                }
            }
        }
    }
}
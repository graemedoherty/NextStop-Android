package com.example.nextstop_android.ui.stepper

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.ui.maps.MapViewModel
import com.example.nextstop_android.ui.stations.StationViewModel
import com.example.nextstop_android.viewmodel.StationViewModelFactory
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

    val stationViewModel: StationViewModel = viewModel(
        factory = StationViewModelFactory(context)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        StepIndicators(currentStep)

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    // 🔑 Determine direction: Is the user going forward or backward?
                    val direction = if (targetState > initialState) 1 else -1

                    val animationDuration = 700 // milliseconds
                    val curve = EaseInOutQuart

                    (slideInHorizontally(
                        animationSpec = tween(
                            animationDuration,
                            easing = curve
                        )
                    ) { it * direction } +
                            fadeIn(animationSpec = tween(animationDuration)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(
                                    animationDuration,
                                    easing = curve
                                )
                            ) { -it * direction } +
                                    fadeOut(animationSpec = tween(animationDuration))
                        )
                },
                label = "stepTransition"
            ) { step ->
                when (step) {
                    1 -> Step1Screen(
                        selectedTransport = selectedTransport,
                        onTransportSelected = viewModel::selectTransport,
                        onNext = viewModel::nextStep
                    )

                    2 -> Step2Screen(
                        selectedTransport = selectedTransport ?: "",
                        savedStation = selectedStation,
                        onStationSelected = { name, lat, lng ->
                            viewModel.selectStation(name, lat, lng)
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
                        mapViewModel = mapViewModel,
                        stationViewModel = stationViewModel
                    )

                    3 -> Step3Screen(
                        selectedTransport = selectedTransport ?: "",
                        selectedStation = selectedStation?.name ?: "",
                        onAlarmSet = {
                            selectedStation?.let { station ->
                                onAlarmCreated(station)
                                val serviceIntent =
                                    Intent(context, LocationTrackingService::class.java).apply {
                                        action = LocationTrackingService.ACTION_SET_DESTINATION
                                        putExtra(
                                            LocationTrackingService.EXTRA_DESTINATION_LAT,
                                            station.latitude
                                        )
                                        putExtra(
                                            LocationTrackingService.EXTRA_DESTINATION_LNG,
                                            station.longitude
                                        )
                                        putExtra(
                                            LocationTrackingService.EXTRA_DESTINATION_NAME,
                                            station.name
                                        )
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
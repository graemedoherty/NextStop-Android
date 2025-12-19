package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.R
import com.example.nextstop_android.service.LocationTrackingService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapsScreen(
    onBack: () -> Unit,
    destinationStation: Station?,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    /* ---------------- MAP STYLE ---------------- */
    val isDarkTheme = isSystemInDarkTheme()
    val mapProperties = remember(isDarkTheme) {
        MapProperties(
            isMyLocationEnabled = true,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    if (isDarkTheme) R.raw.map_dark_style else R.raw.map_light_style
                )
            } catch (_: Exception) {
                null
            }
        )
    }

    /* ---------------- PERMISSIONS ---------------- */
    val permissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    /* ---------------- START ALARM ---------------- */
    LaunchedEffect(destinationStation, permissions.allPermissionsGranted) {
        if (destinationStation != null && permissions.allPermissionsGranted) {
            viewModel.startAlarm(destinationStation)

            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(
                    LocationTrackingService.EXTRA_DESTINATION_LAT,
                    destinationStation.latitude
                )
                putExtra(
                    LocationTrackingService.EXTRA_DESTINATION_LNG,
                    destinationStation.longitude
                )
                putExtra(
                    LocationTrackingService.EXTRA_DESTINATION_NAME,
                    destinationStation.name
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    /* ---------------- BROADCAST RECEIVER ---------------- */
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != LocationTrackingService.ACTION_DISTANCE_UPDATE) return

                val distance =
                    intent.getIntExtra(LocationTrackingService.EXTRA_DISTANCE, -1)
                val lat =
                    intent.getDoubleExtra(LocationTrackingService.EXTRA_USER_LAT, 0.0)
                val lng =
                    intent.getDoubleExtra(LocationTrackingService.EXTRA_USER_LNG, 0.0)

                Log.d("MapsScreen", "Broadcast received: $distance m")

                if (lat != 0.0 && lng != 0.0) {
                    viewModel.updateTracking(lat, lng, distance)
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LocationTrackingService.ACTION_DISTANCE_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    /* ---------------- MAP STATE ---------------- */
    val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng =
        uiState.destinationLocation?.let { LatLng(it.first, it.second) }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(userLatLng, destinationLatLng) {
        if (userLatLng != null && destinationLatLng != null) {
            val bounds = LatLngBounds.builder()
                .include(userLatLng)
                .include(destinationLatLng)
                .build()

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 150),
                800
            )
        }
    }

    /* ---------------- UI ---------------- */
    Box(Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties
        ) {
            userLatLng?.let {
                Marker(
                    state = rememberMarkerState(position = it),
                    title = "You",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE
                    )
                )
            }

            destinationLatLng?.let {
                Marker(
                    state = rememberMarkerState(position = it),
                    title = "Destination"
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.alarmActive,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AlarmCard(
                destination = uiState.selectedStation?.name ?: "",
                distanceMeters = uiState.distanceToDestination,
                status =
                    if (uiState.alarmArrived) AlarmStatus.ARRIVED
                    else AlarmStatus.ACTIVE,
                onCancel = {
                    context.startService(
                        Intent(context, LocationTrackingService::class.java).apply {
                            action = LocationTrackingService.ACTION_STOP
                        }
                    )
                    viewModel.cancelAlarm()
                    onBack()
                }
            )
        }
    }
}

data class Station(
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Int
)

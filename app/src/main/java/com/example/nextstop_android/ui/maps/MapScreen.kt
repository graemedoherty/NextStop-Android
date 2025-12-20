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
    destinationStation: Station?, // Still used for initial entry if needed
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val isDarkTheme = isSystemInDarkTheme()
    val mapProperties = remember(isDarkTheme) {
        MapProperties(
            isMyLocationEnabled = true,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    if (isDarkTheme) R.raw.map_dark_style else R.raw.map_light_style
                )
            } catch (_: Exception) { null }
        )
    }

    val cameraPositionState = rememberCameraPositionState()
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    // Start background tracking service on launch
    LaunchedEffect(permissions.allPermissionsGranted) {
        if (permissions.allPermissionsGranted) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    // Listen for distance updates from Service
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != LocationTrackingService.ACTION_DISTANCE_UPDATE) return

                val distance = intent.getIntExtra(LocationTrackingService.EXTRA_DISTANCE, -1)
                val lat = intent.getDoubleExtra(LocationTrackingService.EXTRA_USER_LAT, 0.0)
                val lng = intent.getDoubleExtra(LocationTrackingService.EXTRA_USER_LNG, 0.0)

                if (lat != 0.0 && lng != 0.0) {
                    viewModel.updateTracking(lat, lng, distance)

                    if (!hasCenteredOnUser) {
                        hasCenteredOnUser = true
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 14f))
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(LocationTrackingService.ACTION_DISTANCE_UPDATE), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Auto-zoom to fit User and Destination
    val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng = uiState.destinationLocation?.let { LatLng(it.first, it.second) }

    LaunchedEffect(userLatLng, destinationLatLng) {
        if (userLatLng != null && destinationLatLng != null) {
            val bounds = LatLngBounds.builder().include(userLatLng).include(destinationLatLng).build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 200), 1000)
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            // Marker for current user position
            userLatLng?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Your Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
                )
            }

            // Marker for destination station
            destinationLatLng?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = uiState.selectedStation?.name ?: "Destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                )
            }
        }

        /* 🔑 IMPROVED VISIBILITY LOGIC */
        AnimatedVisibility(
            visible = uiState.alarmArmed,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AlarmCard(
                destination = uiState.selectedStation?.name ?: "Destination",
                distanceMeters = uiState.distanceToDestination,
                status = if (uiState.alarmArrived) AlarmStatus.ARRIVED else AlarmStatus.ACTIVE,
                onCancel = {
                    // 1. Stop the background service
                    context.startService(Intent(context, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_STOP
                    })

                    // 2. Clear the Map markers and state
                    viewModel.cancelAlarm()

                    // 3. 🔑 Trigger the back callback to reset Stepper to Step 1
                    onBack()
                }
            )
        }
    }
}

/**
 * Represents a transit station destination.
 */
data class Station(
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Int = 0
)
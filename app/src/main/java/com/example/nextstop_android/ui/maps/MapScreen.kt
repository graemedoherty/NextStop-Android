package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.example.nextstop_android.service.LocationTrackingService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapsScreen(
    onBack: () -> Unit,
    destinationStation: Station? = null,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Immediately set map ready to true
    val isMapReady by remember { mutableStateOf(true) }

    // Request location and notification permissions
    val permissionsList = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = permissionsList
    )

    var showPermissionRationale by remember { mutableStateOf(false) }

    // Get initial location
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            try {
                val locationClient = LocationServices.getFusedLocationProviderClient(context)
                locationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        // Just update location, distance will come from service
                        viewModel.updateTracking(it.latitude, it.longitude, 0)
                        android.util.Log.d("MapsScreen", "Initial location: ${it.latitude}, ${it.longitude}")
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("MapsScreen", "Failed to get initial location: ${e.message}")
            }
        }
    }

    // Set the destination and start tracking
    LaunchedEffect(destinationStation, locationPermissions.allPermissionsGranted) {
        destinationStation?.let {
            viewModel.startAlarm(it)

            if (locationPermissions.allPermissionsGranted) {
                // Start location tracking service
                val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_START
                    putExtra(LocationTrackingService.EXTRA_DESTINATION_LAT, it.latitude)
                    putExtra(LocationTrackingService.EXTRA_DESTINATION_LNG, it.longitude)
                    putExtra(LocationTrackingService.EXTRA_DESTINATION_NAME, it.name)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                // Request permissions
                if (locationPermissions.shouldShowRationale) {
                    showPermissionRationale = true
                } else {
                    locationPermissions.launchMultiplePermissionRequest()
                }
            }
        }
    }

    // Register broadcast receiver for location updates
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                android.util.Log.d("MapsScreen", "onReceive called, intent action: ${intent?.action}")

                val distance = intent?.getIntExtra(LocationTrackingService.EXTRA_DISTANCE, -1) ?: -1
                val userLat = intent?.getDoubleExtra(LocationTrackingService.EXTRA_USER_LAT, 0.0) ?: 0.0
                val userLng = intent?.getDoubleExtra(LocationTrackingService.EXTRA_USER_LNG, 0.0) ?: 0.0

                android.util.Log.d("MapsScreen", "Broadcast received: distance=$distance, lat=$userLat, lng=$userLng")

                if (distance >= 0 && userLat != 0.0 && userLng != 0.0) {
                    viewModel.updateTracking(userLat, userLng, distance)
                    android.util.Log.d("MapsScreen", "ViewModel.updateTracking called with distance=$distance")
                }
            }
        }

        val filter = IntentFilter(LocationTrackingService.ACTION_DISTANCE_UPDATE)

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )


        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver was already unregistered
            }
        }
    }

    // NOTE: We do NOT stop the service when leaving the screen
    // The service continues running in the background until:
    // 1. User cancels the alarm manually
    // 2. User arrives at destination
    // This allows the app to track location even when closed

    // Only create LatLng if we have actual user location
    val userLocation = uiState.userLocation?.let {
        LatLng(it.first, it.second)
    }

    val destinationLatLng = uiState.destinationLocation?.let {
        LatLng(it.first, it.second)
    }

    val cameraPositionState = rememberCameraPositionState {
        // Start centered on destination immediately for faster initial render
        position = if (destinationLatLng != null) {
            CameraPosition.fromLatLngZoom(destinationLatLng, 13f)
        } else {
            CameraPosition.fromLatLngZoom(LatLng(53.4509, -6.1501), 12f)
        }
    }

    // Only zoom to fit both markers once when we have both locations
    var hasZoomedToFit by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation, destinationLatLng) {
        if (!hasZoomedToFit && userLocation != null && destinationLatLng != null) {
            val bounds = LatLngBounds.builder()
                .include(userLocation)
                .include(destinationLatLng)
                .build()

            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory
                    .newLatLngBounds(bounds, 150),
                durationMs = 800
            )
            hasZoomedToFit = true
        }
    }

    // Show loading screen or map
    if (!isMapReady) {
        LoadingScreen(
            message = "Preparing your journey to ${destinationStation?.name ?: "destination"}..."
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Only show user marker if we have actual location
                userLocation?.let {
                    Marker(
                        state = rememberMarkerState(position = it),
                        title = "Your Location"
                    )
                }

                if (destinationLatLng != null) {
                    Marker(
                        state = rememberMarkerState(position = destinationLatLng),
                        title = "Destination"
                    )
                }
            }

            // Alarm Card at the bottom - only show when ACTIVE with valid distance
            AnimatedVisibility(
                visible = uiState.alarmActive && uiState.distanceToDestination >= 0,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight }
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight }
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AlarmCard(
                    destination = uiState.selectedStation?.name ?: "Unknown",
                    distanceMeters = uiState.distanceToDestination,
                    status = AlarmStatus.ACTIVE,
                    onCancel = {
                        // Stop the service
                        val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
                            action = LocationTrackingService.ACTION_STOP
                        }
                        context.startService(stopIntent)

                        viewModel.cancelAlarm()
                        onBack()
                    }
                )
            }
        }
    }

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Permission Required") },
            text = {
                Text("This app needs location access to track your journey and alert you when you're approaching your destination. Please grant location permission to continue.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        locationPermissions.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        onBack()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class Station(
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Int
)

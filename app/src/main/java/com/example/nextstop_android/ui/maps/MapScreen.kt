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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
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

    /* ---------------------------------------------------
     * MAP STYLE (LIGHT / DARK)
     * --------------------------------------------------- */
    val isDarkTheme = isSystemInDarkTheme()

    val mapProperties = remember(isDarkTheme) {
        MapProperties(
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    if (isDarkTheme) {
                        R.raw.map_dark_style
                    } else {
                        R.raw.map_light_style
                    }
                )
            } catch (e: Exception) {
                null
            }
        )
    }

    /* ---------------------------------------------------
     * PERMISSIONS
     * --------------------------------------------------- */
    val permissionsList = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val locationPermissions = rememberMultiplePermissionsState(permissionsList)
    var showPermissionRationale by remember { mutableStateOf(false) }

    /* ---------------------------------------------------
     * INITIAL LOCATION
     * --------------------------------------------------- */
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        viewModel.updateTracking(it.latitude, it.longitude, 0)
                    }
                }
            } catch (_: SecurityException) { }
        }
    }

    /* ---------------------------------------------------
     * START TRACKING
     * --------------------------------------------------- */
    LaunchedEffect(destinationStation, locationPermissions.allPermissionsGranted) {
        destinationStation?.let {
            viewModel.startAlarm(it)

            if (locationPermissions.allPermissionsGranted) {
                val intent = Intent(context, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_START
                    putExtra(LocationTrackingService.EXTRA_DESTINATION_LAT, it.latitude)
                    putExtra(LocationTrackingService.EXTRA_DESTINATION_LNG, it.longitude)
                    putExtra(LocationTrackingService.EXTRA_DESTINATION_NAME, it.name)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                if (locationPermissions.shouldShowRationale) {
                    showPermissionRationale = true
                } else {
                    locationPermissions.launchMultiplePermissionRequest()
                }
            }
        }
    }

    /* ---------------------------------------------------
     * LOCATION UPDATES
     * --------------------------------------------------- */
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val distance =
                    intent?.getIntExtra(LocationTrackingService.EXTRA_DISTANCE, -1) ?: -1
                val lat =
                    intent?.getDoubleExtra(LocationTrackingService.EXTRA_USER_LAT, 0.0) ?: 0.0
                val lng =
                    intent?.getDoubleExtra(LocationTrackingService.EXTRA_USER_LNG, 0.0) ?: 0.0

                if (distance >= 0 && lat != 0.0 && lng != 0.0) {
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

        onDispose { context.unregisterReceiver(receiver) }
    }

    /* ---------------------------------------------------
     * MAP STATE
     * --------------------------------------------------- */
    val userLocation = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng = uiState.destinationLocation?.let { LatLng(it.first, it.second) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            destinationLatLng ?: LatLng(53.4509, -6.1501),
            11f
        )
    }

    var hasZoomedToFit by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation, destinationLatLng) {
        if (!hasZoomedToFit && userLocation != null && destinationLatLng != null) {
            val bounds = LatLngBounds.builder()
                .include(userLocation)
                .include(destinationLatLng)
                .build()

            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory
                    .newLatLngBounds(bounds, 150),
                800
            )
            hasZoomedToFit = true
        }
    }

    /* ---------------------------------------------------
     * UI
     * --------------------------------------------------- */
    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties
        ) {
            userLocation?.let {
                Marker(
                    state = rememberMarkerState(position = it),
                    title = "Your Location"
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
            visible = uiState.alarmActive && uiState.distanceToDestination >= 0,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AlarmCard(
                destination = uiState.selectedStation?.name ?: "Unknown",
                distanceMeters = uiState.distanceToDestination,
                status = AlarmStatus.ACTIVE,
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

    /* ---------------------------------------------------
     * PERMISSION DIALOG
     * --------------------------------------------------- */
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs location access to track your journey.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    locationPermissions.launchMultiplePermissionRequest()
                }) { Text("Grant Permission") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    onBack()
                }) { Text("Cancel") }
            }
        )
    }
}

/* ---------------------------------------------------
 * MODEL
 * --------------------------------------------------- */
data class Station(
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Int
)

package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.R
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.service.LocationTrackingService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapsScreen(
    destinationStation: Station?,
    onBack: () -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    /* ---------------- THEME + MAP STYLE ---------------- */

    val isDarkTheme = isSystemInDarkTheme()

    val permissions = rememberMultiplePermissionsState(
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    val mapProperties = remember(
        isDarkTheme,
        permissions.allPermissionsGranted
    ) {
        MapProperties(
            isMyLocationEnabled = permissions.allPermissionsGranted, // Shows blue dot
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

    LaunchedEffect(Unit) {
        permissions.launchMultiplePermissionRequest()
    }

    /* ---------------- UI LOCATION TRACKING (ALWAYS ON) ---------------- */

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    DisposableEffect(permissions.allPermissionsGranted) {
        if (!permissions.allPermissionsGranted) {
            return@DisposableEffect onDispose { }
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@DisposableEffect onDispose { }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3_000L
        )
            .setMinUpdateIntervalMillis(1_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    viewModel.updateUserLocation(it.latitude, it.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        onDispose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /* ---------------- START ALARM ---------------- */

    LaunchedEffect(destinationStation) {
        destinationStation?.let {
            viewModel.startAlarm(it)

            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_SET_DESTINATION
                putExtra(LocationTrackingService.EXTRA_DESTINATION_LAT, it.latitude)
                putExtra(LocationTrackingService.EXTRA_DESTINATION_LNG, it.longitude)
                putExtra(LocationTrackingService.EXTRA_DESTINATION_NAME, it.name)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    /* ---------------- SERVICE DISTANCE UPDATES ---------------- */

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != LocationTrackingService.ACTION_DISTANCE_UPDATE) return

                val lat = intent.getDoubleExtra(
                    LocationTrackingService.EXTRA_USER_LAT,
                    0.0
                )
                val lng = intent.getDoubleExtra(
                    LocationTrackingService.EXTRA_USER_LNG,
                    0.0
                )
                val distance = intent.getIntExtra(
                    LocationTrackingService.EXTRA_DISTANCE,
                    -1
                )

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

    /* ---------------- CAMERA ---------------- */

    val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng = uiState.destinationLocation?.let {
        LatLng(it.first, it.second)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(53.3498, -6.2603),
            6f
        )
    }

    LaunchedEffect(userLatLng, destinationLatLng) {
        when {
            userLatLng != null && destinationLatLng != null -> {
                val bounds = LatLngBounds.builder()
                    .include(userLatLng)
                    .include(destinationLatLng)
                    .build()

                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 300),
                    800
                )
            }

            userLatLng != null -> {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                    800
                )
            }
        }
    }

    /* ---------------- UI ---------------- */

    Box(Modifier.fillMaxSize()) {

        if (!permissions.allPermissionsGranted) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Location permission required")
                Button(onClick = { permissions.launchMultiplePermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        } else {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties
            ) {
                // Blue dot shows automatically via isMyLocationEnabled = true
                // Only show destination marker
                destinationLatLng?.let {
                    Marker(
                        state = rememberMarkerState(position = it),
                        title = uiState.selectedStation?.name ?: "Destination",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.alarmArmed,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AlarmCard(
                    destination = uiState.selectedStation?.name ?: "",
                    distanceMeters = uiState.distanceToDestination,
                    status = if (uiState.alarmArrived)
                        AlarmStatus.ARRIVED
                    else
                        AlarmStatus.ACTIVE,
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
}
package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings // 🔑 Required for Overlay check
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // 🔑 Added for AlertDialog and Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapsScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 🔑 State for the "Wake Screen" explanation dialog
    var showOverlayRationale by remember { mutableStateOf(false) }

    /* ---------------- 1. OVERLAY PERMISSION RATIONALE ---------------- */

    LaunchedEffect(Unit) {
        // Check if we already have permission; if not, show the explanation
        if (!Settings.canDrawOverlays(context)) {
            showOverlayRationale = true
        }
    }

    if (showOverlayRationale) {
        AlertDialog(
            onDismissRequest = { showOverlayRationale = false },
            title = { Text("Wake Screen Permission") },
            text = {
                Text("To ensure your alarm wakes you up even if your screen is off, please allow 'Display over other apps' on the next screen.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayRationale = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayRationale = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }

    /* ---------------- 2. PERMISSIONS (Updated for Android 13+) ---------------- */

    // 🔑 We combine Location and Notification permissions here
    val permissionList = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissionList)

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    /* ---------------- 3. MAP STYLE ---------------- */

    val isDarkTheme = isSystemInDarkTheme()

    val mapProperties = remember(isDarkTheme) {
        MapProperties(
            isMyLocationEnabled = true,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    if (isDarkTheme) R.raw.map_dark_style else R.raw.map_light_style
                )
            } catch (e: Exception) {
                null
            }
        )
    }

    /* ---------------- 4. LOCATION TRACKING (ALWAYS ON) ---------------- */

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    DisposableEffect(permissionsState.allPermissionsGranted) {
        if (!permissionsState.allPermissionsGranted) {
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
            2_000L
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

    /* ---------------- 5. CAMERA LOGIC ---------------- */

    val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng = uiState.destinationLocation?.let { LatLng(it.first, it.second) }

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
                    CameraUpdateFactory.newLatLngBounds(bounds, 200),
                    700
                )
            }

            userLatLng != null -> {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                    700
                )
            }
        }
    }

    /* ---------------- 6. MAP UI ---------------- */

    Box(modifier = modifier.fillMaxSize()) {

        if (!permissionsState.allPermissionsGranted) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Shows if any required permission (Location or Notification) is missing
                Text(
                    text = "Location and Notification permissions are required.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                destinationLatLng?.let {
                    Marker(
                        state = rememberMarkerState(position = it),
                        title = uiState.selectedStation?.name ?: "Destination"
                    )
                }
            }
        }
    }
}
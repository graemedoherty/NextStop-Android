package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
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

    // 🔑 Tracks which setup card to show
    // 0: Welcome, 1: Location, 2: Notifications, 3: Overlay, 4: Finish
    var onboardingStep by remember { mutableStateOf(0) }

    /* ---------------- 1. PERMISSION STATES ---------------- */

    val locationState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    val notificationState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Check if everything is already granted to bypass onboarding for returning users
    val isFullyConfigured = locationState.allPermissionsGranted &&
            (notificationState?.status?.isGranted ?: true) &&
            Settings.canDrawOverlays(context)

    /* ---------------- 2. SEQUENTIAL ONBOARDING UI ---------------- */

    if (!isFullyConfigured && onboardingStep < 4) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black), // Matches your OLED black theme
            contentAlignment = Alignment.Center
        ) {
            when (onboardingStep) {
                0 -> PermissionStepCard(
                    title = "Welcome to NextStop",
                    description = "Let's get you ready for your journey. We need a few permissions to make sure you never miss your stop again!",
                    buttonText = "Start Setup",
                    currentStep = 0,
                    onAction = { onboardingStep = 1 }
                )
                1 -> PermissionStepCard(
                    title = "📍 Location",
                    description = "We use your GPS to track your journey in real-time and trigger the alarm precisely as you approach your station.",
                    buttonText = "Enable Location",
                    currentStep = 1,
                    onAction = {
                        locationState.launchMultiplePermissionRequest()
                        onboardingStep = 2
                    }
                )
                2 -> PermissionStepCard(
                    title = "🔔 Notifications",
                    description = "This allows us to keep the tracking service active in the background while you relax or use other apps.",
                    buttonText = "Enable Notifications",
                    currentStep = 2,
                    onAction = {
                        notificationState?.launchPermissionRequest()
                        onboardingStep = 3
                    }
                )
                3 -> PermissionStepCard(
                    title = "✨ Wake Screen",
                    description = "To make sure you wake up on time, the app needs permission to wake your screen even if it's locked.",
                    buttonText = "Grant Permission",
                    currentStep = 3,
                    onAction = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                        onboardingStep = 4
                    }
                )
            }
        }
    } else {
        /* ---------------- 3. THE MAP CONTENT ---------------- */

        // Map Style & Properties
        val isDarkTheme = isSystemInDarkTheme()
        val mapProperties = remember(isDarkTheme) {
            MapProperties(
                isMyLocationEnabled = true,
                mapStyleOptions = try {
                    MapStyleOptions.loadRawResourceStyle(
                        context,
                        if (isDarkTheme) R.raw.map_dark_style else R.raw.map_light_style
                    )
                } catch (e: Exception) { null }
            )
        }

        // Location Update Logic
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
        DisposableEffect(locationState.allPermissionsGranted) {
            if (!locationState.allPermissionsGranted) return@DisposableEffect onDispose { }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { viewModel.updateUserLocation(it.latitude, it.longitude) }
                }
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
            onDispose { fusedLocationClient.removeLocationUpdates(callback) }
        }

        // Camera Logic
        val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
        val destinationLatLng = uiState.destinationLocation?.let { LatLng(it.first, it.second) }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(53.3498, -6.2603), 6f)
        }

        LaunchedEffect(userLatLng, destinationLatLng) {
            when {
                userLatLng != null && destinationLatLng != null -> {
                    val bounds = LatLngBounds.builder().include(userLatLng).include(destinationLatLng).build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 200), 700)
                }
                userLatLng != null -> {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f), 700)
                }
            }
        }

        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
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

@Composable
fun PermissionStepCard(
    title: String,
    description: String,
    buttonText: String,
    currentStep: Int,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), // Dark Grey/Charcoal
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Indicator
            if (currentStep > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        val color = if (index + 1 <= currentStep) Color(0xFF6F66E4) else Color.DarkGray
                        Box(modifier = Modifier.size(width = 32.dp, height = 4.dp).background(color, RoundedCornerShape(2.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F66E4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(buttonText, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}
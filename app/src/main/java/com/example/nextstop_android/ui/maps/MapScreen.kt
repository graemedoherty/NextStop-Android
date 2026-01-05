package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.R
import com.example.nextstop_android.ui.stepper.PermissionStepCard
import com.example.nextstop_android.ui.stations.StationViewModel
import com.example.nextstop_android.viewmodel.StationViewModelFactory
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

/* ---------- HELPERS ---------- */

fun provideStationIcon(context: android.content.Context): BitmapDescriptor {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin)
    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
    val width = 100
    val height = (width * aspectRatio).toInt()
    return BitmapDescriptorFactory.fromBitmap(
        Bitmap.createScaledBitmap(bitmap, width, height, true)
    )
}

// ✅ CREATE PURPLE DESTINATION MARKER
fun providePurpleDestinationIcon(context: android.content.Context): BitmapDescriptor {
    val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin)
    val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width.toFloat()
    val width = 120 // Slightly larger for destination
    val height = (width * aspectRatio).toInt()

    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

    // Apply purple tint
    val purpleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(purpleBitmap)
    val paint = Paint().apply {
        colorFilter = PorterDuffColorFilter(0xFF6F66E3.toInt(), PorterDuff.Mode.SRC_ATOP)
    }
    canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

    return BitmapDescriptorFactory.fromBitmap(purpleBitmap)
}

private fun createInfoWindowBitmap(
    title: String,
    isAlarmArmed: Boolean,
    isDarkTheme: Boolean
): Bitmap {
    val width = 700
    val height = if (isAlarmArmed) 280 else 340
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val backgroundColor =
        if (isDarkTheme) 0xFF1A1A1A.toInt() else 0xFFFFFFFF.toInt()

    val titleColor =
        if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK

    val subtitleColor =
        if (isAlarmArmed) {
            0xFF4CAF50.toInt()
        } else {
            if (isDarkTheme) 0xFF8F87EB.toInt() else 0xFF5C54C7.toInt()
        }

    val cornerRadius = 40f

    // Background
    val bgPaint = Paint().apply {
        color = backgroundColor
        isAntiAlias = true
    }
    val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

    // ✅ PURPLE BORDER
    val borderPaint = Paint().apply {
        color = 0xFF6F66E3.toInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint)

    // Title
    val titlePaint = Paint().apply {
        color = titleColor
        textSize = 48f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText(title, 40f, 100f, titlePaint)

    // Subtitle
    val subtitlePaint = Paint().apply {
        color = subtitleColor
        textSize = 40f
        isAntiAlias = true
    }
    canvas.drawText(
        if (isAlarmArmed) "Alarm Active" else "Tap to Select",
        40f,
        180f,
        subtitlePaint
    )

    return bitmap
}


@Composable
fun CustomMarker(
    position: LatLng,
    title: String,
    isAlarmArmed: Boolean,
    isDarkTheme: Boolean,
    icon: BitmapDescriptor?,
    onSelect: () -> Unit
) {
    MarkerInfoWindow(
        state = rememberMarkerState(position = position),
        icon = icon,
        onInfoWindowClick = { if (!isAlarmArmed) onSelect() }
    ) {
        Image(
            bitmap = remember(title, isAlarmArmed, isDarkTheme) {
                createInfoWindowBitmap(title, isAlarmArmed, isDarkTheme)
            }.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.width(300.dp)
        )
    }
}


/* ---------- MAP SCREEN ---------- */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapsScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel,
    stepperViewModel: StepperViewModel
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()

    val stationViewModel: StationViewModel =
        viewModel(factory = StationViewModelFactory(context))

    val selectedTransport by stepperViewModel.selectedTransport.collectAsState()
    val transportConfirmed by stepperViewModel.transportConfirmed.collectAsState()

    /* ---------- LOCATION UPDATES ---------- */

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    DisposableEffect(Unit) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000L
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    mapViewModel.updateUserLocation(it.latitude, it.longitude)
                }
            }
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        }

        onDispose { fusedLocationClient.removeLocationUpdates(callback) }
    }

    /* ---------- STATIONS ---------- */

    LaunchedEffect(selectedTransport, transportConfirmed) {
        if (transportConfirmed && selectedTransport != null) {
            stationViewModel.loadStations(selectedTransport!!)
        }
    }

    val visibleStations by stationViewModel.visibleStations.collectAsState()
    LaunchedEffect(visibleStations) {
        mapViewModel.setStations(visibleStations)
    }

    /* ---------- PERMISSIONS ---------- */

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val notificationPermission =
        if (Build.VERSION.SDK_INT >= 33)
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        else null

    val fullyConfigured =
        locationPermissions.allPermissionsGranted &&
                (notificationPermission?.status?.isGranted ?: true) &&
                Settings.canDrawOverlays(context)

    if (!fullyConfigured) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            PermissionStepCard(
                title = "Permissions Required",
                description = "Enable location and notifications.",
                buttonText = "Continue"
            ) {
                locationPermissions.launchMultiplePermissionRequest()
            }
        }
        return
    }

    /* ---------- CAMERA & THEME LOGIC ---------- */

    val cameraPositionState = rememberCameraPositionState()

    val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng =
        uiState.destinationLocation?.let { LatLng(it.first, it.second) }

    val shouldFollowUser =
        userLatLng != null &&
                destinationLatLng == null &&
                !uiState.alarmArmed

    // Follow user ONLY when appropriate
    LaunchedEffect(userLatLng, shouldFollowUser) {
        if (userLatLng != null && shouldFollowUser) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userLatLng, 14f),
                800
            )
        }
    }

    // Frame route when destination selected
    LaunchedEffect(userLatLng, destinationLatLng) {
        if (userLatLng != null && destinationLatLng != null) {
            val bounds = LatLngBounds.builder()
                .include(userLatLng)
                .include(destinationLatLng)
                .build()

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 200),
                700
            )
        }
    }

    // Refresh stations on pan
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && transportConfirmed) {
            delay(150)
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let {
                stationViewModel.updateVisibleBounds(it)
            }
        }
    }

    /* ---------- MAP THEME (LIVE SWITCHING) ---------- */

    val isDarkTheme = isSystemInDarkTheme()

    val mapProperties = remember(isDarkTheme) {
        MapProperties(
            isMyLocationEnabled = true,
            mapStyleOptions = runCatching {
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    if (isDarkTheme) {
                        R.raw.map_dark_style
                    } else {
                        R.raw.map_light_style
                    }
                )
            }.getOrNull()
        )
    }

    /* ---------- MAP ---------- */

    var sharedPinIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var purpleDestinationIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false
        ),
        onMapLoaded = {
            sharedPinIcon = provideStationIcon(context)
            purpleDestinationIcon = providePurpleDestinationIcon(context)
        }
    ) {
        uiState.stations.forEach { station ->
            CustomMarker(
                position = LatLng(station.latitude, station.longitude),
                title = station.name,
                isAlarmArmed = uiState.alarmArmed,
                isDarkTheme = isDarkTheme,
                icon = sharedPinIcon
            ) {
                mapViewModel.setDestination(station)
                stepperViewModel.selectStation(
                    station.name,
                    station.latitude,
                    station.longitude
                )
            }
        }

        // ✅ PURPLE DESTINATION MARKER
        destinationLatLng?.let {
            Marker(
                state = MarkerState(it),
                title = uiState.selectedStation?.name ?: "Destination",
                icon = purpleDestinationIcon
            )
        }
    }
}
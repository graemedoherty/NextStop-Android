package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.R
import com.example.nextstop_android.ui.stations.StationViewModel
import com.example.nextstop_android.ui.stepper.PermissionStepCard
import com.example.nextstop_android.viewmodel.StationViewModelFactory
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.delay

/* ---------- PERMISSION STEPS ---------- */

private enum class PermissionStep {
    LOCATION,
    NOTIFICATIONS,
    OVERLAY,
    DONE
}

/* ---------- MARKER HELPERS ---------- */

private fun stationIcon(context: android.content.Context): BitmapDescriptor {
    return try {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin)
        val width = 96
        val height = (bitmap.height * (width / bitmap.width.toFloat())).toInt()
        BitmapDescriptorFactory.fromBitmap(
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        )
    } catch (e: Exception) {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }
}

private fun purpleDestinationIcon(context: android.content.Context): BitmapDescriptor {
    return try {
        val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin)
        val width = 110
        val height =
            (originalBitmap.height * (width / originalBitmap.width.toFloat())).toInt()

        val scaledBitmap =
            Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        val purpleBitmap =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(purpleBitmap)

        val paint = Paint().apply {
            colorFilter =
                PorterDuffColorFilter(0xFF6F66E3.toInt(), PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

        BitmapDescriptorFactory.fromBitmap(purpleBitmap)
    } catch (e: Exception) {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
    }
}

private fun infoWindowBitmap(title: String, dark: Boolean): Bitmap {
    val width = 640
    val height = 240
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = if (dark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
    val textColor = if (dark) Color.White else Color.Black
    val accent = 0xFF6F66E3.toInt()
    val cornerRadius = 32f

    val bgPaint = Paint().apply {
        color = bgColor
        isAntiAlias = true
    }
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    val titlePaint = Paint().apply {
        color = textColor.toArgb()
        textSize = 46f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText(title, 40f, 100f, titlePaint)

    val buttonPaint = Paint().apply {
        color = accent
        isAntiAlias = true
    }
    canvas.drawRoundRect(
        RectF(40f, 140f, width - 40f, 205f),
        20f,
        20f,
        buttonPaint
    )

    val buttonTextPaint = Paint().apply {
        color = Color.White.toArgb()
        textAlign = Paint.Align.CENTER
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText(
        "TAP TO SELECT",
        width / 2f,
        185f,
        buttonTextPaint
    )

    return bitmap
}

/* ---------- CUSTOM MARKER ---------- */

@Composable
private fun StationMarker(
    position: LatLng,
    title: String,
    icon: BitmapDescriptor?,
    darkTheme: Boolean,
    onSelect: () -> Unit
) {
    if (icon == null) return

    MarkerInfoWindow(
        state = rememberMarkerState(position = position),
        icon = icon,
        onInfoWindowClick = { onSelect() }
    ) {
        Image(
            bitmap = remember(title, darkTheme) {
                infoWindowBitmap(title, darkTheme)
            }.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.width(320.dp)
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
    val darkTheme = isSystemInDarkTheme()

    val stationViewModel: StationViewModel =
        viewModel(factory = StationViewModelFactory(context))

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

    val overlayGranted = remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted.value = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionStep = when {
        !locationPermissions.allPermissionsGranted -> PermissionStep.LOCATION
        Build.VERSION.SDK_INT >= 33 &&
                notificationPermission?.status?.isGranted == false ->
            PermissionStep.NOTIFICATIONS

        !overlayGranted.value -> PermissionStep.OVERLAY
        else -> PermissionStep.DONE
    }

    if (permissionStep != PermissionStep.DONE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (permissionStep) {
                PermissionStep.LOCATION ->
                    PermissionStepCard(
                        "Location Access",
                        "We need your location to alert you before your stop.",
                        "Allow Location"
                    ) { locationPermissions.launchMultiplePermissionRequest() }

                PermissionStep.NOTIFICATIONS ->
                    PermissionStepCard(
                        "Notifications",
                        "Allow notifications so we can alert you in time.",
                        "Allow Notifications"
                    ) { notificationPermission?.launchPermissionRequest() }

                PermissionStep.OVERLAY ->
                    PermissionStepCard(
                        "Display Over Other Apps",
                        "Required to show alarms over navigation apps.",
                        "Enable Overlay"
                    ) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }

                else -> Unit
            }
        }
        return
    }

    /* ---------- LOCATION ---------- */

    val fusedLocationClient =
        remember { LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(Unit) {
        val request =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000).build()

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

    val selectedTransport by stepperViewModel.selectedTransport.collectAsState()
    val transportConfirmed by stepperViewModel.transportConfirmed.collectAsState()

    LaunchedEffect(selectedTransport, transportConfirmed) {
        if (transportConfirmed && selectedTransport != null) {
            stationViewModel.loadStations(selectedTransport!!)
        }
    }

    val visibleStations by stationViewModel.visibleStations.collectAsState()
    LaunchedEffect(visibleStations) {
        mapViewModel.setStations(visibleStations)
    }

    /* ---------- CAMERA ---------- */

    // 🔑 INITIAL CAMERA POSITION: Centered over Ireland (Dublin)
    val irelandInitial = LatLng(53.3498, -6.2603)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(irelandInitial, 7f)
    }

    var hasCenteredOnUser by rememberSaveable { mutableStateOf(false) }

    val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng =
        uiState.destinationLocation?.let { LatLng(it.first, it.second) }

    LaunchedEffect(userLatLng) {
        if (userLatLng != null && !hasCenteredOnUser) {
            hasCenteredOnUser = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userLatLng, 14f),
                800
            )
        }
    }

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

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && transportConfirmed) {
            delay(150)
            cameraPositionState.projection?.visibleRegion?.latLngBounds?.let {
                stationViewModel.updateVisibleBounds(it)
            }
        }
    }

    /* ---------- MAP ---------- */

    var pinIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var purpleIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(Unit) {
        pinIcon = stationIcon(context)
        purpleIcon = purpleDestinationIcon(context)
    }

    LaunchedEffect(transportConfirmed) {
        if (!transportConfirmed) return@LaunchedEffect

        while (cameraPositionState.projection == null) {
            delay(16)
        }

        cameraPositionState.projection
            ?.visibleRegion
            ?.latLngBounds
            ?.let { bounds ->
                stationViewModel.updateVisibleBounds(bounds)
            }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    if (darkTheme) R.raw.map_dark_style else R.raw.map_light_style
                )
            } catch (e: Exception) {
                null
            }
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false
        )
    ) {

        if (destinationLatLng == null) {
            uiState.stations.forEach { station ->
                StationMarker(
                    position = LatLng(station.latitude, station.longitude),
                    title = station.name,
                    icon = pinIcon,
                    darkTheme = darkTheme
                ) {
                    mapViewModel.setDestination(station)
                    stepperViewModel.selectStation(
                        station.name,
                        station.latitude,
                        station.longitude
                    )
                }
            }
        }

        destinationLatLng?.let {
            Marker(
                state = MarkerState(it),
                title = uiState.selectedStation?.name ?: "Destination",
                icon = purpleIcon
            )
        }
    }
}
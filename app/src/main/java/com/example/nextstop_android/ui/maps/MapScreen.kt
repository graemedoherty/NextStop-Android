package com.example.nextstop_android.ui.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextstop_android.R
import com.example.nextstop_android.ui.stations.StationViewModel
import com.example.nextstop_android.viewmodel.StationViewModelFactory
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/* ---------- 🎨 MARKER HELPERS ---------- */

private fun stationIcon(context: Context): BitmapDescriptor {
    return try {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin)
        val width = 96
        val height = (bitmap.height * (width / bitmap.width.toFloat())).toInt()
        BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, width, height, true))
    } catch (e: Exception) {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }
}

private fun purpleDestinationIcon(context: Context): BitmapDescriptor {
    return try {
        val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin)
        val width = 110
        val height = (originalBitmap.height * (width / originalBitmap.width.toFloat())).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        val purpleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(purpleBitmap)
        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(0xFF6F66E3.toInt(), PorterDuff.Mode.SRC_ATOP)
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
    val textColor = if (dark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    val accent = 0xFF6F66E3.toInt()
    val cornerRadius = 32f
    val bgPaint = Paint().apply { color = bgColor; isAntiAlias = true }
    canvas.drawRoundRect(
        RectF(0f, 0f, width.toFloat(), height.toFloat()),
        cornerRadius,
        cornerRadius,
        bgPaint
    )

    val titlePaint = Paint().apply {
        color = textColor
        textSize = 46f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText(title, 40f, 100f, titlePaint)

    val buttonPaint = Paint().apply { color = accent; isAntiAlias = true }
    canvas.drawRoundRect(RectF(40f, 140f, width - 40f, 205f), 20f, 20f, buttonPaint)

    val buttonTextPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText("TAP TO SELECT", width / 2f, 185f, buttonTextPaint)
    return bitmap
}

private fun coreDotIcon(): BitmapDescriptor {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val borderPaint = Paint().apply { color = android.graphics.Color.WHITE; isAntiAlias = true }
    canvas.drawCircle(center, center, size / 2f, borderPaint)
    val corePaint = Paint().apply { color = 0xFF6F66E3.toInt(); isAntiAlias = true }
    canvas.drawCircle(center, center, (size / 2f) - 6f, corePaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/* ---------- 📍 CUSTOM COMPONENTS ---------- */

@Composable
fun PulsatingLocationMarker(position: LatLng) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val dotIcon = remember { coreDotIcon() }
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScaleAnimation"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AlphaAnimation"
    )
    Circle(
        center = position,
        radius = (scale * 250).toDouble(),
        fillColor = Color(0xFF6F66E3).copy(alpha = alpha),
        strokeWidth = 0f
    )
    Marker(state = MarkerState(position = position), anchor = Offset(0.5f, 0.5f), icon = dotIcon)
}

@Composable
private fun StationMarker(
    position: LatLng,
    title: String,
    icon: BitmapDescriptor?,
    darkTheme: Boolean,
    onSelect: () -> Unit
) {
    if (icon == null) return
    val markerState = rememberMarkerState(position = position)
    MarkerInfoWindow(
        state = markerState,
        icon = icon,
        onInfoWindowClick = { onSelect() }
    ) {
        Image(
            bitmap = remember(title, darkTheme) {
                infoWindowBitmap(
                    title,
                    darkTheme
                )
            }.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.width(320.dp)
        )
    }
}

/* ---------- 🗺️ MAIN SCREEN ---------- */

@Composable
fun MapsScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel,
    stepperViewModel: StepperViewModel
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()
    val darkTheme = isSystemInDarkTheme()

    val stationViewModel = viewModel<StationViewModel>(factory = StationViewModelFactory(context))

    val isOverlayShowing = stepperViewModel.showPermissionOverlay
    val selectedTransport by stepperViewModel.selectedTransport.collectAsState()
    val transportConfirmed by stepperViewModel.transportConfirmed.collectAsState()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(isOverlayShowing) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission && !isOverlayShowing) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000).build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        mapViewModel.updateUserLocation(
                            it.latitude,
                            it.longitude
                        )
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                android.os.Looper.getMainLooper()
            )
            onDispose { fusedLocationClient.removeLocationUpdates(callback) }
        } else onDispose { }
    }

    // 🔑 rememberCameraPositionState uses rememberSaveable internally,
    // but its initial 'position' block only runs the VERY first time the state is created.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(53.3498, -6.2603), 7f)
    }

    LaunchedEffect(selectedTransport, transportConfirmed) {
        if (transportConfirmed && selectedTransport != null) {
            stationViewModel.loadStations(selectedTransport!!)
        }
    }

    val visibleStations by stationViewModel.visibleStations.collectAsState()
    LaunchedEffect(visibleStations) { mapViewModel.setStations(visibleStations) }

    val userLatLng = uiState.userLocation?.let { LatLng(it.first, it.second) }
    val destinationLatLng = uiState.destinationLocation?.let { LatLng(it.first, it.second) }

    // 🔑 PERSISTENT AUTO-CENTER: Move 'hasCenteredOnUser' logic to ViewModel
    // This prevents the map from "snapping" back to the user when returning from About screen.
    LaunchedEffect(userLatLng) {
        if (userLatLng != null && !mapViewModel.hasInitialCenterPerformed && destinationLatLng == null) {
            mapViewModel.hasInitialCenterPerformed = true
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f), 1000)
        }
    }

    // AUTO-FIT: Only triggers when a destination is actually selected
    LaunchedEffect(destinationLatLng) {
        if (userLatLng != null && destinationLatLng != null) {
            val bounds = LatLngBounds.Builder()
                .include(userLatLng)
                .include(destinationLatLng)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 250), 1000)
        }
    }

    LaunchedEffect(transportConfirmed) {
        if (transportConfirmed) {
            snapshotFlow { Pair(cameraPositionState.projection, cameraPositionState.position.zoom) }
                .distinctUntilChanged()
                .collectLatest { (projection, zoom) ->
                    if (zoom >= 11f && projection != null) {
                        stationViewModel.updateVisibleBounds(projection.visibleRegion.latLngBounds)
                    }
                }
        }
    }

    var pinIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var purpleIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    LaunchedEffect(Unit) {
        pinIcon = stationIcon(context)
        purpleIcon = purpleDestinationIcon(context)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    if (darkTheme) R.raw.map_dark_style else R.raw.map_light_style
                )
            } catch (e: Exception) {
                null
            }
        ),
        uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
    ) {
        userLatLng?.let { PulsatingLocationMarker(it) }

        if (destinationLatLng == null && cameraPositionState.position.zoom >= 11f) {
            uiState.stations.forEach { station ->
                key(station.name + station.latitude) {
                    StationMarker(
                        position = LatLng(station.latitude, station.longitude),
                        title = station.name,
                        icon = pinIcon,
                        darkTheme = darkTheme,
                        onSelect = {
                            mapViewModel.setDestination(station)
                            stepperViewModel.selectStation(
                                station.name,
                                station.latitude,
                                station.longitude
                            )
                        }
                    )
                }
            }
        }

        destinationLatLng?.let {
            Marker(state = MarkerState(it), icon = purpleIcon)
            Circle(
                center = it,
                radius = 300.0,
                fillColor = Color(0x446F66E3),
                strokeColor = Color(0xFF6F66E3),
                strokeWidth = 2f
            )
        }
    }
}
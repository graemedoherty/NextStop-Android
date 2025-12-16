package com.example.nextstop_android.ui.maps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    onBack: () -> Unit,
    destinationLocation: Pair<Double, Double>? = null,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val userLocation = LatLng(53.4509, -6.1501)
    val destinationLatLng = destinationLocation?.let { LatLng(it.first, it.second) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 12f)
    }

    // Effect to zoom to bounds when destination changes
    LaunchedEffect(destinationLatLng) {
        if (destinationLatLng != null) {
            val bounds = LatLngBounds.builder()
                .include(userLocation)
                .include(destinationLatLng)
                .build()

            // Animate camera to show both markers with padding
            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    100 // padding in pixels
                ),
                durationMs = 1000
            )
        }
    }

    // Map takes full screen
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        onMapLoaded = {
            viewModel.loadNearbyStations()
        }
    ) {
        // User location marker
        Marker(
            state = rememberMarkerState(position = userLocation),
            title = "Your Location",
            snippet = "Current position"
        )

        // Destination marker (if available)
        if (destinationLatLng != null) {
            Marker(
                state = rememberMarkerState(position = destinationLatLng),
                title = "Destination Station",
                snippet = "Selected station"
            )
        }

        // Add markers for stations
        uiState.stations.forEach { station ->
            Marker(
                state = rememberMarkerState(
                    position = LatLng(station.latitude, station.longitude)
                ),
                title = station.name,
                snippet = station.type
            )
        }
    }
}

@Composable
fun StationListBottomSheet(
    stations: List<Station>,
    onStationSelected: (Station) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nearby Stations (${stations.size})",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            stations.take(5).forEach { station ->
                StationCard(
                    station = station,
                    onClick = { onStationSelected(station) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationCard(
    station: Station,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(station.name, style = MaterialTheme.typography.bodyLarge)
                Text(station.type, style = MaterialTheme.typography.labelSmall)
            }
            Text("${station.distance}m", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class Station(
    val name: String,
    val type: String, // Train, Bus, Luas
    val latitude: Double,
    val longitude: Double,
    val distance: Int // in meters
)
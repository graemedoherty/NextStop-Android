package com.example.nextstop_android.ui.maps

import androidx.lifecycle.ViewModel
import com.example.nextstop_android.ui.maps.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MapUiState(
    val stations: List<Station> = emptyList(),
    val selectedStation: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLocation: Pair<Double, Double>? = null,
    val destinationLocation: Pair<Double, Double>? = null
)

class MapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    // Mock data - replace with real API calls
    private val mockStations = listOf(
        Station(
            name = "User Location",
            type = "Current",
            latitude = 53.4509,
            longitude = -6.1501,
            distance = 0
        )
    )

    fun loadNearbyStations() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        try {
            // Simulate loading delay
            Thread.sleep(500)

            // In a real app, you would:
            // 1. Get user's current location
            // 2. Query an API for nearby stations
            // 3. Calculate distances

            _uiState.value = _uiState.value.copy(
                stations = mockStations,
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message,
                isLoading = false
            )
        }
    }

    fun selectStation(stationName: String) {
        _uiState.value = _uiState.value.copy(
            selectedStation = stationName
        )
    }

    fun setUserLocation(latitude: Double, longitude: Double) {
        _uiState.value = _uiState.value.copy(
            userLocation = Pair(latitude, longitude)
        )
    }

    fun setDestinationLocation(latitude: Double, longitude: Double) {
        _uiState.value = _uiState.value.copy(
            destinationLocation = Pair(latitude, longitude)
        )
    }
}
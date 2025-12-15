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
    val userLocation: Pair<Double, Double>? = null
)

class MapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    // Mock data - replace with real API calls
    private val mockStations = listOf(
        Station(
            name = "Dublin Connolly",
            type = "Train",
            latitude = 53.3626,
            longitude = -6.2421,
            distance = 450
        ),
        Station(
            name = "Dublin Heuston",
            type = "Train",
            latitude = 53.6452,
            longitude = -6.2884,
            distance = 2100
        ),
        Station(
            name = "O'Connell Street",
            type = "Bus",
            latitude = 53.3506,
            longitude = -6.2603,
            distance = 200
        ),
        Station(
            name = "Smithfield",
            type = "Luas",
            latitude = 53.3470,
            longitude = -6.2788,
            distance = 890
        ),
        Station(
            name = "College Green",
            type = "Bus",
            latitude = 53.3436,
            longitude = -6.2597,
            distance = 350
        ),
        Station(
            name = "Tallaght",
            type = "Luas",
            latitude = 53.2878,
            longitude = -6.3770,
            distance = 8500
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
}
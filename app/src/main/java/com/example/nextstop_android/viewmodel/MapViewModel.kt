package com.example.nextstop_android.ui.maps

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    fun loadNearbyStations() {
        // Mock only – no blocking
        _uiState.update {
            it.copy(
                stations = emptyList(),
                isLoading = false
            )
        }
    }

    fun startAlarm(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                alarmActive = true,
                alarmArrived = false,
                distanceToDestination = station.distance
            )
        }
    }

    fun cancelAlarm() {
        _uiState.update {
            it.copy(
                alarmActive = false,
                alarmArrived = false,
                selectedStation = null,
                destinationLocation = null,
                distanceToDestination = 0
            )
        }
    }

    /**
     * 🔑 Single state update to avoid recomposition storms
     */
    fun updateTracking(
        latitude: Double,
        longitude: Double,
        distanceMeters: Int
    ) {
        val arrived = distanceMeters <= 100

        _uiState.update {
            it.copy(
                userLocation = latitude to longitude,
                distanceToDestination = distanceMeters,
                alarmArrived = arrived,
                alarmActive = !arrived
            )
        }
    }
}

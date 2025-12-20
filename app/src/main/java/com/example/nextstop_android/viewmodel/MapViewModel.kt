package com.example.nextstop_android.ui.maps

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    fun loadNearbyStations() {
        _uiState.update {
            it.copy(
                stations = emptyList(),
                isLoading = false
            )
        }
    }

    // Step 2: Just set destination marker (no alarm yet)
    fun setDestination(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude
            )
        }
    }

    // Step 3: Arm the alarm
    fun startAlarm(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                alarmArmed = true,
                alarmActive = true,
                alarmArrived = false,
                distanceToDestination = -1 // 🔑 Reset to -1 to show "Calculating..."
            )
        }
    }

    fun cancelAlarm() {
        _uiState.update {
            it.copy(
                alarmArmed = false,
                alarmActive = false,
                alarmArrived = false,
                selectedStation = null,
                destinationLocation = null,
                distanceToDestination = -1
            )
        }
    }

    fun updateTracking(
        latitude: Double,
        longitude: Double,
        distanceMeters: Int
    ) {
        // Only mark as arrived if we have a valid positive distance within threshold
        val arrived = distanceMeters in 1..100

        _uiState.update {
            it.copy(
                userLocation = latitude to longitude,
                distanceToDestination = distanceMeters,
                alarmArrived = arrived,
                // 🔑 FIX: Keep alarm active even if distance is -1 (calculating)
                // and don't kill it if distance is 0 during warmup.
                alarmActive = it.alarmArmed && !arrived
            )
        }
    }
}
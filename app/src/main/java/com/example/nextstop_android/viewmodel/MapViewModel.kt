package com.example.nextstop_android.ui.maps

import androidx.lifecycle.ViewModel
import com.example.nextstop_android.model.Station
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

    fun setDestination(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude
            )
        }
    }

    fun startAlarm(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                alarmArmed = true,          // 🔑 Alarm UI should appear
                alarmActive = true,         // 🔑 Tracking is active
                alarmArrived = false,
                distanceToDestination = -1
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
        val arrived = distanceMeters in 0..100 && distanceMeters != -1

        _uiState.update {
            it.copy(
                userLocation = latitude to longitude,
                distanceToDestination = distanceMeters,
                alarmArrived = arrived,

                alarmArmed = true,
                alarmActive = true
            )
        }
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                userLocation = latitude to longitude
            )
        }
    }
}

package com.example.nextstop_android.ui.maps

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.viewmodel.StepperViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    /**
     * 🔔 Receiver for live distance + user location updates
     */
    private val distanceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            val distance = intent.getIntExtra("distance", -1)
            val lat = intent.getDoubleExtra("user_lat", 0.0)
            val lng = intent.getDoubleExtra("user_lng", 0.0)

            updateTracking(lat, lng, distance)
        }
    }

    /**
     * 🛑 Receiver fired when alarm is stopped from anywhere
     */
    private val alarmStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            resetAllState()
        }
    }

    init {
        // Distance updates
        ContextCompat.registerReceiver(
            appContext,
            distanceReceiver,
            IntentFilter(LocationTrackingService.ACTION_DISTANCE_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Alarm fully stopped
        ContextCompat.registerReceiver(
            appContext,
            alarmStoppedReceiver,
            IntentFilter(LocationTrackingService.ACTION_ALARM_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /* -----------------------------------------------------------------------
     * Station / Map state
     * --------------------------------------------------------------------- */

    /**
     * 📍 Stations currently visible / relevant to the map
     */
    fun setStations(stations: List<Station>) {
        _uiState.update {
            it.copy(
                stations = stations,
                isLoading = false,
                error = null
            )
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update {
            it.copy(isLoading = isLoading)
        }
    }

    fun setError(message: String?) {
        _uiState.update {
            it.copy(
                error = message,
                isLoading = false
            )
        }
    }

    /**
     * 📍 User selects destination
     */
    fun setDestination(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                distanceToDestination = -1,
                stations = emptyList()  // ✅ CLEAR ALL STATION MARKERS
            )
        }
    }

    /* -----------------------------------------------------------------------
     * Alarm lifecycle
     * --------------------------------------------------------------------- */

    /**
     * 🚨 Alarm armed (Journey started)
     */
    fun startAlarm(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                alarmArmed = true,
                alarmActive = true,
                alarmArrived = false,
                distanceToDestination = -1,
                stations = emptyList()  // ✅ CLEAR ALL STATION MARKERS
            )
        }
    }

    /**
     * 📍 Passive location update (map tracking only)
     */
    fun updateUserLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(userLocation = latitude to longitude)
        }
    }

    /**
     * 📡 Live tracking updates from service
     */
    fun updateTracking(latitude: Double, longitude: Double, distanceMeters: Int) {
        val arrived = distanceMeters in 0..LocationTrackingService.ARRIVAL_THRESHOLD_METERS

        _uiState.update {
            it.copy(
                userLocation = latitude to longitude,
                distanceToDestination = distanceMeters,
                alarmArrived = arrived
            )
        }
    }

    /**
     * ❌ User cancels alarm manually
     */
    fun cancelAlarm(stepperViewModel: StepperViewModel? = null) {
        val stopIntent = Intent(appContext, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        appContext.startService(stopIntent)

        resetAllState()
        stepperViewModel?.reset()
    }

    /**
     * 🔄 Hard reset — used when alarm stops ANYWHERE
     */
    private fun resetAllState() {
        _uiState.update { MapUiState() }
    }

    /**
     * 🧹 Lifecycle cleanup
     */
    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(distanceReceiver)
            appContext.unregisterReceiver(alarmStoppedReceiver)
        } catch (_: Exception) {
            // Already unregistered — safe to ignore
        }
    }
}

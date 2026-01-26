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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    private val distanceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val distance = intent.getIntExtra("distance", -1)
            val lat = intent.getDoubleExtra("user_lat", 0.0)
            val lng = intent.getDoubleExtra("user_lng", 0.0)
            updateTracking(lat, lng, distance)
        }
    }

    private val alarmStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            resetAllState()
        }
    }

    init {
        // 🔑 SILENT WARM-UP: Get location immediately while splash is scrolling
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    updateUserLocation(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) { /* Handle missing permissions if necessary */
        }

        ContextCompat.registerReceiver(
            appContext,
            distanceReceiver,
            IntentFilter(LocationTrackingService.ACTION_DISTANCE_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ContextCompat.registerReceiver(
            appContext,
            alarmStoppedReceiver,
            IntentFilter(LocationTrackingService.ACTION_ALARM_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun setStations(stations: List<Station>) {
        _uiState.update { it.copy(stations = stations, isLoading = false, error = null) }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun setError(message: String?) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    fun setDestination(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                distanceToDestination = -1,
                stations = emptyList()
            )
        }
    }

    fun startAlarm(station: Station) {
        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                alarmArmed = true,
                alarmActive = true,
                alarmArrived = false,
                distanceToDestination = -1,
                stations = emptyList()
            )
        }
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(userLocation = latitude to longitude)
        }
    }

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

    fun cancelAlarm(stepperViewModel: StepperViewModel? = null) {
        val stopIntent = Intent(appContext, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        appContext.startService(stopIntent)
        resetAllState()
        stepperViewModel?.reset()
    }

    private fun resetAllState() {
        _uiState.update { MapUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(distanceReceiver)
            appContext.unregisterReceiver(alarmStoppedReceiver)
        } catch (_: Exception) {
        }
    }
}
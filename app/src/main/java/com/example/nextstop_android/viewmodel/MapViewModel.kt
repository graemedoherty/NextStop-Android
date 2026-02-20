package com.example.nextstop_android.ui.maps

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextstop_android.model.Station
import com.example.nextstop_android.service.LocationTrackingService
import com.example.nextstop_android.viewmodel.StepperViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState

    var hasInitialCenterPerformed = false
    private var isResetting = false

    private val distanceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isResetting) return

            if (intent == null) return
            val distance = intent.getIntExtra("distance", -1)
            val lat = intent.getDoubleExtra("user_lat", 0.0)
            val lng = intent.getDoubleExtra("user_lng", 0.0)

            if (_uiState.value.alarmActive) {
                updateTracking(lat, lng, distance)
            }
        }
    }

    private val alarmStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            resetAllState()
        }
    }

    init {
        val killServiceIntent = Intent(appContext, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        try {
            appContext.startService(killServiceIntent)
        } catch (e: Exception) {
            // Service might not be running
        }

        resetAllState()

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    if (_uiState.value.userLocation == null && !isResetting) {
                        Log.d(
                            "MapViewModel",
                            "Initial location from FusedLocation: ${it.latitude}, ${it.longitude}"
                        )
                        updateUserLocation(it.latitude, it.longitude)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission handled in UI
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
        _uiState.update { it.copy(stations = stations, isStationsLoading = false, error = null) }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun setError(message: String?) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    fun setDestination(station: Station) {
        Log.d("MapViewModel", "setDestination called for: ${station.name}")

        val currentUserLocation = _uiState.value.userLocation
        Log.d("MapViewModel", "Current user location: $currentUserLocation")

        val initialDistance = if (currentUserLocation != null) {
            val dist = calculateDistance(
                currentUserLocation.first,
                currentUserLocation.second,
                station.latitude,
                station.longitude
            )
            Log.d("MapViewModel", "Calculated distance: $dist meters")
            dist
        } else {
            Log.w("MapViewModel", "User location is NULL - cannot calculate distance yet")
            -1
        }

        _uiState.update {
            it.copy(
                selectedStation = station,
                destinationLocation = station.latitude to station.longitude,
                distanceToDestination = initialDistance,
                stations = emptyList()
            )
        }

        // 🔥 NEW FIX: If user location is null, try to get it immediately
        if (currentUserLocation == null) {
            Log.d("MapViewModel", "Attempting to fetch location immediately...")
            viewModelScope.launch {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            Log.d("MapViewModel", "Got location, recalculating distance")
                            updateUserLocation(it.latitude, it.longitude)
                        } ?: run {
                            Log.w("MapViewModel", "Location is still null after request")
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("MapViewModel", "Security exception getting location", e)
                }
            }
        }
    }

    fun clearDestination() {
        hasInitialCenterPerformed = false
        _uiState.update { currentState ->
            currentState.copy(
                selectedStation = null,
                destinationLocation = null,
                distanceToDestination = -1,
                alarmArmed = false,
                alarmActive = false,
                alarmArrived = false,
                stations = emptyList()
            )
        }
    }

    fun startAlarm(station: Station) {
        isResetting = false
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
        if (isResetting) return

        Log.d("MapViewModel", "updateUserLocation: $latitude, $longitude")

        val currentState = _uiState.value

        // 🔥 CRITICAL FIX: Recalculate distance if we have a destination but no active alarm
        val newDistance =
            if (currentState.destinationLocation != null && !currentState.alarmActive) {
                val dist = calculateDistance(
                    latitude,
                    longitude,
                    currentState.destinationLocation.first,
                    currentState.destinationLocation.second
                )
                Log.d("MapViewModel", "Recalculated distance after location update: $dist meters")
                dist
            } else {
                currentState.distanceToDestination
            }

        _uiState.update {
            it.copy(
                userLocation = latitude to longitude,
                distanceToDestination = newDistance
            )
        }
    }

    fun updateTracking(latitude: Double, longitude: Double, distanceMeters: Int) {
        if (isResetting || !_uiState.value.alarmActive) return

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

    fun resetAllState() {
        isResetting = true
        hasInitialCenterPerformed = false

        _uiState.value = MapUiState()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isResetting = false
        }, 1000)
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        return (2 * r * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(distanceReceiver)
            appContext.unregisterReceiver(alarmStoppedReceiver)
        } catch (_: Exception) {
            // Ignore
        }
    }
}
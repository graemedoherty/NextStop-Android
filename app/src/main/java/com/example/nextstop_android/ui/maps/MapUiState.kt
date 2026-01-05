package com.example.nextstop_android.ui.maps

import com.example.nextstop_android.model.Station
import com.google.android.gms.maps.model.LatLngBounds

data class MapUiState(

    /* ---------------- MAP + STATIONS ---------------- */

    // Stations currently loaded for the visible map area
    val stations: List<Station> = emptyList(),

    // Station user has selected (from map OR search)
    val selectedStation: Station? = null,

    // Current visible map bounds (used to fetch/update stations)
    val visibleBounds: LatLngBounds? = null,

    // True while fetching stations for new map position
    val isStationsLoading: Boolean = false,

    /* ---------------- USER LOCATION ---------------- */

    // Live user GPS location
    val userLocation: Pair<Double, Double>? = null,

    // Selected destination coordinates
    val destinationLocation: Pair<Double, Double>? = null,

    /* ---------------- ALARM STATE ---------------- */

    // User explicitly started a journey (Step 3)
    val alarmArmed: Boolean = false,

    // Background tracking service running
    val alarmActive: Boolean = false,

    // User has arrived within threshold distance
    val alarmArrived: Boolean = false,

    // Distance (meters) from destination
    val distanceToDestination: Int = -1,

    /* ---------------- UI STATE ---------------- */

    val isLoading: Boolean = false,
    val error: String? = null
)

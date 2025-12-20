package com.example.nextstop_android.ui.maps

data class MapUiState(
    val stations: List<Station> = emptyList(),
    val selectedStation: Station? = null,

    // 🔑 NEW: user explicitly armed alarm (Step 3)
    val alarmArmed: Boolean = false,

    val alarmActive: Boolean = false,
    val alarmArrived: Boolean = false,
    val distanceToDestination: Int = 0,

    val isLoading: Boolean = false,
    val error: String? = null,

    val userLocation: Pair<Double, Double>? = null,
    val destinationLocation: Pair<Double, Double>? = null
)

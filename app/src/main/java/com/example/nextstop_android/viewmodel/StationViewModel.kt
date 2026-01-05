package com.example.nextstop_android.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextstop_android.data.StationData
import com.example.nextstop_android.data.StationDataLoader
import com.example.nextstop_android.model.Station
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StationViewModel(
    private val dataLoader: StationDataLoader
) : ViewModel() {

    private val _allStations = MutableStateFlow<List<Station>>(emptyList())

    private val _visibleStations = MutableStateFlow<List<Station>>(emptyList())
    val visibleStations: StateFlow<List<Station>> = _visibleStations

    private val _currentTransport = MutableStateFlow<String?>(null)
    val currentTransport: StateFlow<String?> = _currentTransport

    fun loadStations(transport: String) {
        if (_currentTransport.value == transport) return
        _currentTransport.value = transport

        viewModelScope.launch {
            val rawData: List<StationData> = when (transport) {
                "Train" -> dataLoader.loadTrainStations()
                "Luas" -> dataLoader.loadLuasStations()
                "Bus" -> dataLoader.loadBusStations()
                else -> emptyList()
            }

            // Map directly using new lat/long properties
            val stations = rawData.map { it.toStation(transport) }
            _allStations.value = stations

            // Bus data is usually too large to show all at once, so we start empty
            _visibleStations.value = if (transport == "Bus") emptyList() else stations
        }
    }

    fun updateVisibleBounds(bounds: LatLngBounds?) {
        if (bounds == null || _currentTransport.value != "Bus") return

        viewModelScope.launch {
            val filtered = _allStations.value.filter { station ->
                bounds.contains(LatLng(station.latitude, station.longitude))
            }.take(60)

            _visibleStations.value = filtered
        }
    }

    fun searchStations(query: String): List<Station> {
        if (query.length < 3) return emptyList()
        return _allStations.value
            .filter { it.name.contains(query, ignoreCase = true) }
            .take(5)
    }
}

/**
 * 🔁 Mapping from JSON Data -> Domain Model
 * Now using direct property access: .name, .lat, .long
 */
private fun StationData.toStation(type: String): Station {
    return Station(
        name = this.name,
        type = type,
        latitude = this.lat,
        longitude = this.long
    )
}
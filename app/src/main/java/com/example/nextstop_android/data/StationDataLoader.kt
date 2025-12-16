package com.example.nextstop_android.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class StationData(
    @SerializedName("lat")
    val lat: String? = null,
    @SerializedName("long")
    val long: String? = null,
    @SerializedName("destination")
    val destination: String? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("stopId")
    val stopId: String? = null,
    @SerializedName("stopName")
    val stopName: String? = null
) {
    // Helper function to get the name regardless of format
    fun getName(): String {
        return destination ?: stopName ?: "Unknown"
    }

    // Helper function to get latitude as Double
    fun getLatitude(): Double {
        return latitude ?: (lat?.toDoubleOrNull() ?: 0.0)
    }

    // Helper function to get longitude as Double
    fun getLongitude(): Double {
        return longitude ?: (long?.toDoubleOrNull() ?: 0.0)
    }
}

class StationDataLoader(private val context: Context) {
    private val gson = Gson()

    fun loadLuasStations(): List<StationData> {
        return loadStationsFromAsset("data/luasData.json")
    }

    fun loadTrainStations(): List<StationData> {
        return loadStationsFromAsset("data/trainData.json")
    }

    fun loadBusStations(): List<StationData> {
        return loadStationsFromAsset("data/dublinbus.json")
    }

    private fun loadStationsFromAsset(fileName: String): List<StationData> {
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            gson.fromJson(jsonString, Array<StationData>::class.java).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
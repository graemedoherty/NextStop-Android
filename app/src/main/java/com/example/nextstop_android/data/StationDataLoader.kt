package com.example.nextstop_android.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class StationData(
    @SerializedName("name")
    val name: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("long")
    val long: Double
)

class StationDataLoader(private val context: Context) {
    private val gson = Gson()

    fun loadLuasStations(): List<StationData> {
        return loadStationsFromAsset("data/luasData.json")
    }

    fun loadTrainStations(): List<StationData> {
        return loadStationsFromAsset("data/trainData.json")
    }

    fun loadBusStations(): List<StationData> {
        return loadStationsFromAsset("data/busData.json")
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
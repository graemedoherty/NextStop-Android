package com.graemedoherty.nextstop_android.model

data class Station(
    val name: String,
    val type: String,
    val latitude: Double,  // 🔑 Must be named 'latitude'
    val longitude: Double, // 🔑 Must be named 'longitude'
    val distance: Int = 0
)
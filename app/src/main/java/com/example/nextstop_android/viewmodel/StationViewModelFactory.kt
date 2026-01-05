package com.example.nextstop_android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nextstop_android.data.StationDataLoader
import com.example.nextstop_android.ui.stations.StationViewModel

class StationViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StationViewModel::class.java)) {
            val loader = StationDataLoader(context.applicationContext)
            return StationViewModel(loader) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

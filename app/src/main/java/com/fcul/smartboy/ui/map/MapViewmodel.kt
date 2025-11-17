package com.fcul.smartboy.ui.map

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class MapViewmodel : ViewModel() {
    private val _selectedPoint = MutableLiveData<LatLng?>(null)
    val selectedPoint: LiveData<LatLng?> = _selectedPoint

    fun setPoint(location: LatLng) {
        Log.d("MapViewmodel", "onNewRad setValue: $location")
        _selectedPoint.value = location
    }

    fun clearPoint() {
        _selectedPoint.value = null
    }
}
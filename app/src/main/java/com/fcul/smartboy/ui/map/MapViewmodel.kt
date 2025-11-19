package com.fcul.smartboy.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.repository.MapRouteRepository
import com.fcul.smartboy.repository.radiation.RadiationRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewmodel @Inject constructor(
    private val mapRouteRepository: MapRouteRepository,
    private val radiationRepository: RadiationRepository
) : ViewModel() {
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val _selectedPoint = MutableStateFlow<LatLng?>(null)
    val selectedPoint: StateFlow<LatLng?> = _selectedPoint

    private val _checkpoints = MutableStateFlow<List<LatLng>>(emptyList())
    val checkpoints: StateFlow<List<LatLng>> = _checkpoints

    private val _radSpots = MutableStateFlow<List<RadiationData>>(emptyList())
    val radSpots: StateFlow<List<RadiationData>> = _radSpots

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _radiationAlert = MutableStateFlow<RadiationData?>(null)
    val radiationAlert: StateFlow<RadiationData?> = _radiationAlert

    private val enteredZones = mutableSetOf<String>()


    fun updateCurrentLocation(location: LatLng) {
        _currentLocation.value = location
        loadRadSpots()
    }

    private fun loadRadSpots(radiusMeters: Double = 5000.0) {
        val loc = _currentLocation.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val spots = radiationRepository.filter(loc, radiusMeters)
                _radSpots.value = spots
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun createRadPoint(location: LatLng, radiationLevel: Double, radius: Double) {
        viewModelScope.launch {
            try {
                val radData = RadiationData(
                    location = LatLng(
                        location.latitude,
                        location.longitude
                    ),
                    radiationLevelInSv = radiationLevel,
                    radius = radius,
                    timestamp = System.currentTimeMillis()
                )
                radiationRepository.create(radData)
                loadRadSpots()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun setPoint(location: LatLng) {
        _selectedPoint.value = location
    }

    fun onEnteringRadiationZone(radData: RadiationData) {
        val zoneId = radData.id ?: return

        if (enteredZones.add(zoneId)) {
            Log.w("MapViewmodel", "⚠️⚠️⚠️ USER ENTERED RADIATION ZONE ⚠️⚠️⚠️")
            Log.w("MapViewmodel", "Location: ${radData.location}")
            Log.w("MapViewmodel", "Radiation Level: ${radData.radiationLevelInSv} Sv")
            Log.w("MapViewmodel", "Radius: ${radData.radius}m")

            _radiationAlert.value = radData
        }
    }

    fun dismissRadiationAlert() {
        _radiationAlert.value = null
    }

    fun onLeavingRadiationZone(radData: RadiationData) {
        val zoneId = radData.id ?: return
        enteredZones.remove(zoneId)
        Log.i("MapViewmodel", "✅ User left radiation zone: $zoneId")
    }
}
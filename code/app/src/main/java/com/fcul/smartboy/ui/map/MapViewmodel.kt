package com.fcul.smartboy.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.BuildConfig
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.repository.MapRouteRepository
import com.fcul.smartboy.repository.radiation.RadiationRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
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

    private val _isRouteActive = MutableStateFlow(false)
    val isRouteActive: StateFlow<Boolean> = _isRouteActive

    private val _routeCheckpoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routeCheckpoints: StateFlow<List<LatLng>> = _routeCheckpoints

    private val _pendingCheckpoints = MutableStateFlow<List<LatLng>>(emptyList())
    val pendingCheckpoints: StateFlow<List<LatLng>> = _pendingCheckpoints

    private val _routePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val routePolyline: StateFlow<List<LatLng>> = _routePolyline

    private val _reachedCheckpoints = MutableStateFlow<Set<Int>>(emptySet())
    val reachedCheckpoints: StateFlow<Set<Int>> = _reachedCheckpoints

    private val _checkpointAlert = MutableStateFlow<String?>(null)
    val checkpointAlert: StateFlow<String?> = _checkpointAlert

    private val enteredZones = mutableSetOf<String>()

    private var currentRouteId: String? = null
    private var routeStartTime: Long = 0L

    companion object {
        private const val CHECKPOINT_PROXIMITY_RADIUS = 50.0 // meters
    }

    fun updateCurrentLocation(location: LatLng) {
        _currentLocation.value = location
        loadRadSpots()
        checkCheckpointProximity(location)
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

    fun addPendingCheckpoint() {
        val selected = _selectedPoint.value
        if (selected == null) {
            _error.value = "No point selected to add as checkpoint."
            return
        }
        _pendingCheckpoints.value += selected
        fetchRouteForPendingCheckpoints()
    }

    fun removePendingCheckpoint(index: Int) {
        _pendingCheckpoints.value =
            _pendingCheckpoints.value.toMutableList().apply { removeAt(index) }
    }

    fun clearPendingCheckpoints() {
        _pendingCheckpoints.value = emptyList()
    }

    fun addCheckpointToActiveRoute() {
        if (!_isRouteActive.value || currentRouteId == null) {
            _error.value = "Start a route first before adding checkpoints"
            return
        }
        val selected = _selectedPoint.value
        if (selected == null) {
            _error.value = "No point selected to add as checkpoint."
            return
        }
        viewModelScope.launch {
            try {
                val updatedCheckpoints = _routeCheckpoints.value + selected
                mapRouteRepository.addCheckpointToActiveRoute(
                    routeId = currentRouteId!!,
                    checkpoints = _routeCheckpoints.value,
                    location = selected
                )
                _routeCheckpoints.value = updatedCheckpoints
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun startRoute() {
        viewModelScope.launch {
            try {
                Log.i(
                    "MapViewmodel",
                    "Starting route with checkpoints: ${_pendingCheckpoints.value}"
                )
                if (_pendingCheckpoints.value.size < 2) {
                    _error.value = "At least 2 checkpoints are required to start a route."
                    Log.e("MapViewmodel", "❌ Not enough checkpoints to start route.")
                    return@launch
                }
                val routeId = System.currentTimeMillis().toString()
                currentRouteId = routeId
                routeStartTime = System.currentTimeMillis()
                Log.i("MapViewmodel", "Generated route ID: $routeId at $routeStartTime")
                mapRouteRepository.startRouteWithCheckpoints(
                    routeId = routeId,
                    routeStartTime = routeStartTime,
                    checkpoints = _pendingCheckpoints.value
                )
                Log.i("MapViewmodel", "🚀 Route started with ID: $routeId")
                _isRouteActive.value = true
                _routeCheckpoints.value = _pendingCheckpoints.value
                _reachedCheckpoints.value = emptySet()
                Log.i("MapViewmodel", "Checkpoints: ${_routeCheckpoints.value}")
                clearPendingCheckpoints()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun endRoute() {
        viewModelScope.launch {
            try {
                if (currentRouteId == null) {
                    _error.value = "No active route to end."
                    return@launch
                }
                mapRouteRepository.endRoute(
                    routeId = currentRouteId!!,
                    checkpoints = _routeCheckpoints.value,
                    routeStartTime = routeStartTime
                )
                _isRouteActive.value = false
                _routeCheckpoints.value = emptyList()
                _reachedCheckpoints.value = emptySet()
                currentRouteId = null
                routeStartTime = 0L
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
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

    fun dismissCheckpointAlert() {
        _checkpointAlert.value = null
    }

    private fun checkCheckpointProximity(currentLocation: LatLng) {
        if (!_isRouteActive.value) return

        val checkpoints = _routeCheckpoints.value
        val reached = _reachedCheckpoints.value.toMutableSet()

        checkpoints.forEachIndexed { index, checkpoint ->
            if (!reached.contains(index)) {
                val distance = calculateDistance(currentLocation, checkpoint)
                if (distance <= CHECKPOINT_PROXIMITY_RADIUS) {
                    reached.add(index)
                    _reachedCheckpoints.value = reached
                    _checkpointAlert.value = "Checkpoint ${index + 1} reached! ✓"
                    Log.i(
                        "MapViewmodel",
                        "✅ Checkpoint $index reached at distance: $distance meters"
                    )
                }
            }
        }
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude,
            point1.longitude,
            point2.latitude,
            point2.longitude,
            results
        )
        return results[0].toDouble()
    }

    fun onLeavingRadiationZone(radData: RadiationData) {
        val zoneId = radData.id ?: return
        enteredZones.remove(zoneId)
        Log.i("MapViewmodel", "✅ User left radiation zone: $zoneId")
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng
            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }

    fun fetchRoutePolyline(origin: LatLng, destination: LatLng) {
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.MAPS_API_KEY
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origin.latitude},${origin.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&key=$apiKey"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val body = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().body?.string()
                        ?: throw Exception("No response body")
                }
                val json = Json.parseToJsonElement(body).jsonObject
                val routes = json["routes"]?.jsonArray
                if (routes != null && routes.isNotEmpty()) {
                    val overviewPolyline = routes[0].jsonObject["overview_polyline"]?.jsonObject
                    val points = overviewPolyline?.get("points")?.jsonPrimitive?.content
                    if (points != null) {
                        val decoded = decodePolyline(points)
                        _routePolyline.value = decoded
                        _error.value = null
                        return@launch
                    }
                }
                _error.value = "No route found"
                _routePolyline.value = emptyList()
            } catch (e: Exception) {
                _error.value = "Failed to fetch route: ${e.message}"
                _routePolyline.value = emptyList()
            }
        }
    }

    fun fetchRouteForPendingCheckpoints() {
        val points = _pendingCheckpoints.value
        if (points.size >= 2) {
            fetchRoutePolyline(points.first(), points.last())
        } else {
            _error.value = "Need at least 2 checkpoints to fetch route."
        }
    }
}


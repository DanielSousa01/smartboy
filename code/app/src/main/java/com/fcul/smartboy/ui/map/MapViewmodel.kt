package com.fcul.smartboy.ui.map

import android.location.Location.distanceBetween
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.BuildConfig
import com.fcul.smartboy.data.api.RoutesRepository
import com.fcul.smartboy.data.api.RoutesRepository.RouteResult
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.domain.route.RouteInfo
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.radiation.RadiationRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.PolyUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class MapViewmodel @Inject constructor(
    private val radiationRepository: RadiationRepository,
    private val profileRepository: ProfileRepository,
    private val routesRepository: RoutesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    // User location tracking
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    // Radiation spots
    private val _radSpots = MutableStateFlow<List<RadiationData>>(emptyList())
    val radSpots: StateFlow<List<RadiationData>> = _radSpots

    private val _radiationAlert = MutableStateFlow<RadiationData?>(null)
    val radiationAlert: StateFlow<RadiationData?> = _radiationAlert

    // Route drawing
    private val _selectedPoint = MutableStateFlow<LatLng?>(null)

    // Selected markers
    private val _selectedRadiationMarker = MutableStateFlow<RadiationData?>(null)
    val selectedRadiationMarker: StateFlow<RadiationData?> = _selectedRadiationMarker

    private val _selectedCheckpointMarker = MutableStateFlow<LatLng?>(null)
    val selectedCheckpointMarker: StateFlow<LatLng?> = _selectedCheckpointMarker

    private val _pendingCheckpoints = MutableStateFlow<List<LatLng>>(emptyList())
    val pendingCheckpoints: StateFlow<List<LatLng>> = _pendingCheckpoints

    private val _routePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val routePolyline: StateFlow<List<LatLng>> = _routePolyline

    private val _routeInfo = MutableStateFlow<RouteInfo?>(null)
    val routeInfo: StateFlow<RouteInfo?> = _routeInfo

    // Progressive route drawing
    private val _traveledPath = MutableStateFlow<List<LatLng>>(emptyList())
    val traveledPath: StateFlow<List<LatLng>> = _traveledPath

    private val _remainingRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val remainingRoute: StateFlow<List<LatLng>> = _remainingRoute

    private val _isRouteActive = MutableStateFlow(false)
    val isRouteActive: StateFlow<Boolean> = _isRouteActive

    private val enteredZones = mutableSetOf<String>()
    private val activeRadiationZones = mutableMapOf<String, RadiationData>()
    private var lastRadiationUpdate = System.currentTimeMillis()
    private var lastRouteRecalculation = System.currentTimeMillis()

    companion object {
        private const val RADIATION_UPDATE_INTERVAL = 5000L
        private const val RADIATION_MULTIPLIER = 0.1
        private const val STEPS_DEDUCTION_PER_SV = 10L
        private const val MAX_ROUTE_DEVIATION_METERS =
            100.0 // Recalculate if user is >100m off route
        private const val ROUTE_RECALC_COOLDOWN = 10000L // Wait 10s between recalculations
    }

    // User location tracking
    fun updateCurrentLocation(location: LatLng) {
        _currentLocation.value = location
        loadRadSpots()
        checkRadiationZones(location)
        applyRadiationDamage()

        // Update route drawing if route is active
        if (_isRouteActive.value && _routePolyline.value.isNotEmpty()) {
            updateRouteDrawing(location)
        }
    }

    private fun loadRadSpots(radiusMeters: Double = 5000.0) {
        val loc = _currentLocation.value ?: return
        viewModelScope.launch {
            try {
                val spots = radiationRepository.filter(loc, radiusMeters)
                _radSpots.value = spots
            } catch (e: Exception) {
                Log.e("MapViewmodel", "Failed to load radiation spots: ${e.message}")
            }
        }
    }

    private fun updateRouteDrawing(currentLocation: LatLng) {
        val fullRoute = _routePolyline.value
        if (fullRoute.isEmpty()) return

        // Find the closest point on the route to the current location
        var closestIndex = 0
        var minDistance = Float.MAX_VALUE

        fullRoute.forEachIndexed { index, point ->
            val distance = calculateDistance(currentLocation, point).toFloat()
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }

        // Check if user has deviated too far from the route
        if (minDistance > MAX_ROUTE_DEVIATION_METERS) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRecalc = currentTime - lastRouteRecalculation

            if (timeSinceLastRecalc >= ROUTE_RECALC_COOLDOWN) {
                Log.w("MapViewmodel", "User deviated ${minDistance}m from route, recalculating...")
                lastRouteRecalculation = currentTime
                recalculateRouteFromCurrentLocation()
                return
            } else {
                Log.d(
                    "MapViewmodel",
                    "User off route but cooldown active (${timeSinceLastRecalc}ms < ${ROUTE_RECALC_COOLDOWN}ms)"
                )
            }
        }

        // If user is close to the route (within threshold), update the drawing
        if (minDistance <= MAX_ROUTE_DEVIATION_METERS) {
            // Traveled path: all points from start up to current position + current location
            val traveled = fullRoute.take(closestIndex + 1).toMutableList()
            traveled.add(currentLocation)
            _traveledPath.value = traveled

            // Remaining route: from current position to end
            val remaining = mutableListOf(currentLocation)
            remaining.addAll(fullRoute.drop(closestIndex + 1))
            _remainingRoute.value = remaining

            Log.d(
                "MapViewmodel",
                "Route drawing: ${traveled.size} traveled, ${remaining.size} remaining (${minDistance}m from route)"
            )
        }
    }

    private fun recalculateRouteFromCurrentLocation() {
        val currentLoc = _currentLocation.value ?: return
        val checkpoints = _pendingCheckpoints.value
        if (checkpoints.isEmpty()) return

        // Find which checkpoints are still ahead
        val closestCheckpointIndex = checkpoints.indexOfFirst { checkpoint ->
            calculateDistance(currentLoc, checkpoint) < 50.0
        }

        val remainingCheckpoints = if (closestCheckpointIndex >= 0) {
            // User is near a checkpoint, use remaining checkpoints from there
            checkpoints.drop(closestCheckpointIndex)
        } else {
            // User is not near any checkpoint, recalculate to all checkpoints
            checkpoints
        }

        if (remainingCheckpoints.isEmpty()) {
            Log.i("MapViewmodel", "All checkpoints reached, ending route")
            endRoute()
            return
        }

        // Recalculate route from current location
        Log.i(
            "MapViewmodel",
            "Recalculating route from current location to ${remainingCheckpoints.size} checkpoints"
        )
        fetchRoutePolyline(
            origin = currentLoc,
            destination = remainingCheckpoints.last(),
            waypoints = if (remainingCheckpoints.size > 2) {
                remainingCheckpoints.subList(1, remainingCheckpoints.size - 1)
            } else {
                emptyList()
            },
            useCurrentLocationAsOrigin = true
        )
    }

    private fun checkRadiationZones(location: LatLng) {
        val spots = _radSpots.value
        val currentZoneIds = mutableSetOf<String>()

        spots.forEach { radSpot ->
            val zoneId = radSpot.id ?: return@forEach
            val distance = calculateDistance(location, radSpot.location)

            if (distance <= radSpot.radius) {
                currentZoneIds.add(zoneId)
                if (!activeRadiationZones.containsKey(zoneId)) {
                    // Just entered this zone
                    activeRadiationZones[zoneId] = radSpot
                    Log.w(
                        "MapViewmodel",
                        "Entered radiation zone: $zoneId (${radSpot.radiationLevelInSv} Sv)"
                    )
                }
            }
        }

        // Remove zones user has left
        val zonesToRemove = activeRadiationZones.keys.filter { it !in currentZoneIds }
        zonesToRemove.forEach { zoneId ->
            activeRadiationZones.remove(zoneId)
            Log.i("MapViewmodel", "Left radiation zone: $zoneId")
        }
    }

    private fun applyRadiationDamage() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastRadiationUpdate

        if (timeSinceLastUpdate >= RADIATION_UPDATE_INTERVAL && activeRadiationZones.isNotEmpty()) {
            val userId = auth.currentUser?.uid ?: return

            // Calculate total radiation from all active zones
            val totalRadiation = activeRadiationZones.values.sumOf {
                it.radiationLevelInSv * RADIATION_MULTIPLIER
            }

            // Calculate steps to deduct (based on radiation level)
            val stepsToDeduct = activeRadiationZones.values.sumOf {
                (it.radiationLevelInSv * STEPS_DEDUCTION_PER_SV).roundToLong()
            }

            viewModelScope.launch {
                try {
                    // Add radiation to user profile
                    profileRepository.addRadiation(userId, totalRadiation)

                    // Deduct steps as penalty
                    if (stepsToDeduct > 0) {
                        profileRepository.deductSteps(userId, stepsToDeduct)
                        Log.w(
                            "MapViewmodel",
                            "Radiation damage: +${totalRadiation} Sv, -${stepsToDeduct} steps"
                        )
                    }

                    lastRadiationUpdate = currentTime
                } catch (e: Exception) {
                    Log.e("MapViewmodel", "Failed to apply radiation damage", e)
                }
            }
        }
    }

    // Route management
    fun addPendingCheckpoint() {
        val selected = _selectedPoint.value
        if (selected == null) {
            Log.w("MapViewmodel", "No point selected to add as checkpoint")
            return
        }
        _pendingCheckpoints.value += selected
        Log.i("MapViewmodel", "Checkpoint added, total: ${_pendingCheckpoints.value.size}")
    }

    fun clearPendingCheckpoints() {
        _pendingCheckpoints.value = emptyList()
        _routePolyline.value = emptyList()
        _routeInfo.value = null
    }

    fun clearSelectedCheckpoint() {
        val selectedMarker = _selectedCheckpointMarker.value
        if (selectedMarker != null) {
            _pendingCheckpoints.value = _pendingCheckpoints.value.filter { it != selectedMarker }
            _selectedCheckpointMarker.value = null
            Log.d("MapViewmodel", "Removed checkpoint: $selectedMarker")
        } else {
            Log.w("MapViewmodel", "No checkpoint selected to remove")
        }
    }

    fun startRoute() {
        if (_pendingCheckpoints.value.size < 2) {
            Log.e("MapViewmodel", "Need at least 2 checkpoints to start route")
            return
        }

        Log.i("MapViewmodel", "Starting route with ${_pendingCheckpoints.value.size} checkpoints")
        fetchRouteForPendingCheckpoints()

        _isRouteActive.value = true
        Log.i("MapViewmodel", "Route activated")
    }

    fun endRoute() {
        _isRouteActive.value = false
        _traveledPath.value = emptyList()
        _remainingRoute.value = emptyList()
        clearPendingCheckpoints()
        Log.i("MapViewmodel", "Route ended")
    }

    fun setPoint(location: LatLng) {
        _selectedPoint.value = location
    }

    fun onRadiationMarkerClick(radiationData: RadiationData) {
        _selectedRadiationMarker.value = radiationData
        Log.d(
            "MapViewmodel",
            "Radiation marker selected: ${radiationData.radiationLevelInSv} Sv at ${radiationData.location}"
        )
    }

    fun onCheckpointMarkerClick(checkpoint: LatLng) {
        _selectedCheckpointMarker.value = checkpoint
        Log.d("MapViewmodel", "Checkpoint marker selected: $checkpoint")
    }

    fun clearMarkerSelection() {
        _selectedRadiationMarker.value = null
        _selectedCheckpointMarker.value = null
        Log.d("MapViewmodel", "Marker selection cleared")
    }

    fun createRadPoint(location: LatLng, radiationLevel: Double, radius: Double) {
        viewModelScope.launch {
            try {
                val radData = RadiationData(
                    location = location,
                    radiationLevelInSv = radiationLevel,
                    radius = radius,
                    timestamp = System.currentTimeMillis()
                )
                radiationRepository.create(radData)
                loadRadSpots()
            } catch (e: Exception) {
                Log.e("MapViewmodel", "Failed to create radiation point: ${e.message}")
            }
        }
    }

    fun onEnteringRadiationZone(radData: RadiationData) {
        val zoneId = radData.id ?: return

        if (enteredZones.add(zoneId)) {
            Log.w("MapViewmodel", "USER ENTERED RADIATION ZONE")
            Log.w("MapViewmodel", "Location: ${radData.location}")
            Log.w("MapViewmodel", "Radiation Level: ${radData.radiationLevelInSv} Sv")
            Log.w("MapViewmodel", "Radius: ${radData.radius}m")

            _radiationAlert.value = radData
        }
    }

    fun dismissRadiationAlert() {
        _radiationAlert.value = null
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        distanceBetween(
            point1.latitude,
            point1.longitude,
            point2.latitude,
            point2.longitude,
            results
        )
        return results[0].toDouble()
    }

    // Route fetching with Google Routes API (Compute Routes)
    fun fetchRoutePolyline(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList(),
        useCurrentLocationAsOrigin: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.MAPS_API_KEY

                // Determine the actual origin to use
                val actualOrigin = if (useCurrentLocationAsOrigin) {
                    _currentLocation.value ?: origin
                } else {
                    origin
                }

                Log.d("MapViewmodel", "========================================")
                Log.d("MapViewmodel", "fetchRoutePolyline called")
                Log.d(
                    "MapViewmodel",
                    "Origin: ${actualOrigin.latitude}, ${actualOrigin.longitude} (current location: $useCurrentLocationAsOrigin)"
                )
                Log.d(
                    "MapViewmodel",
                    "Destination: ${destination.latitude}, ${destination.longitude}"
                )
                Log.d("MapViewmodel", "Waypoints: ${waypoints.size}")
                Log.d(
                    "MapViewmodel",
                    "API Key: ${if (apiKey.isBlank()) "EMPTY/BLANK" else "Present (${apiKey.take(10)}...)"}"
                )
                Log.d("MapViewmodel", "========================================")

                if (apiKey.isBlank()) {
                    // Fallback: use checkpoints as polyline (straight lines)
                    val fallbackRoute = mutableListOf(actualOrigin)
                    fallbackRoute.addAll(waypoints)
                    fallbackRoute.add(destination)
                    _routePolyline.value = fallbackRoute
                    initializeRouteDrawing()
                    return@launch
                }

                Log.i("MapViewmodel", "Calling Google Routes API (Compute Routes)...")

                // Use clean RoutesRepository with Retrofit
                val result = routesRepository.computeRoute(
                    apiKey = apiKey,
                    origin = actualOrigin,
                    destination = destination,
                    waypoints = waypoints
                )

                when (result) {
                    is RouteResult.Success -> {
                        // Use Google's built-in polyline decoder from maps-utils
                        val decodedPolyline = PolyUtil.decode(result.encodedPolyline)
                        _routePolyline.value = decodedPolyline

                        Log.i("MapViewmodel", "Route computed: ${decodedPolyline.size} points")

                        // Format distance
                        val distanceKm = result.distanceMeters / 1000.0
                        val distanceText = if (distanceKm >= 1.0) {
                            "%.1f km".format(distanceKm)
                        } else {
                            "${result.distanceMeters} m"
                        }

                        // Format duration
                        val durationMins = result.durationSeconds / 60
                        val durationText = if (durationMins >= 60) {
                            val hours = durationMins / 60
                            val mins = durationMins % 60
                            "${hours}h ${mins}m"
                        } else {
                            "$durationMins min"
                        }

                        _routeInfo.value = RouteInfo(
                            distance = distanceText,
                            duration = durationText,
                            distanceMeters = result.distanceMeters,
                            durationSeconds = result.durationSeconds
                        )

                        initializeRouteDrawing()
                    }

                    is RouteResult.Error -> {
                        Log.e("MapViewmodel", "Routes API error: ${result.message}")

                        // Fallback to checkpoints
                        val allPoints = mutableListOf(actualOrigin)
                        allPoints.addAll(waypoints)
                        allPoints.add(destination)
                        _routePolyline.value = allPoints
                        initializeRouteDrawing()
                    }
                }

            } catch (e: Exception) {
                Log.e("MapViewmodel", "Route fetch error: ${e.message}", e)
                Log.e("MapViewmodel", "Using fallback: straight lines")

                // Fallback: use checkpoints directly
                val checkpoints = _pendingCheckpoints.value
                if (checkpoints.size >= 2) {
                    _routePolyline.value = checkpoints
                    initializeRouteDrawing()
                }
            }
        }
    }

    private fun initializeRouteDrawing() {
        val route = _routePolyline.value
        if (route.isNotEmpty()) {
            // Start with empty traveled path, full route as remaining
            _traveledPath.value = listOf(route.first())
            _remainingRoute.value = route
            Log.d("MapViewmodel", "Route drawing initialized: ${route.size} points")
        }
    }

    fun fetchRouteForPendingCheckpoints() {
        val points = _pendingCheckpoints.value
        if (points.size >= 2) {
            // When starting a route, use current location as origin and ALL checkpoints as waypoints/destination
            // This ensures the first checkpoint is not skipped
            val destination = points.last()
            val waypoints = if (points.size > 1) {
                // Include all checkpoints except the last one (which is the destination)
                points.dropLast(1)
            } else {
                emptyList()
            }
            // Pass first checkpoint as origin (will be overridden by useCurrentLocationAsOrigin=true)
            // but this ensures fallback works correctly
            fetchRoutePolyline(
                origin = points.first(),
                destination = destination,
                waypoints = waypoints,
                useCurrentLocationAsOrigin = true
            )
        } else {
            Log.w("MapViewmodel", "Need at least 2 checkpoints to fetch route")
        }
    }
}


package com.fcul.smartboy.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location.distanceBetween
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcul.smartboy.BuildConfig
import com.fcul.smartboy.data.api.RoutesRepository
import com.fcul.smartboy.data.api.RoutesRepository.RouteResult
import com.fcul.smartboy.domain.route.ActiveRoute
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.domain.route.RouteInfo
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.ProfileRepository
import com.fcul.smartboy.repository.radiation.RadiationRepository
import com.fcul.smartboy.repository.route.ActiveRouteRepository
import com.fcul.smartboy.utils.MeasurementUtils
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.PolyUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class MapViewmodel @Inject constructor(
    private val radiationRepository: RadiationRepository,
    private val profileRepository: ProfileRepository,
    private val routesRepository: RoutesRepository,
    private val activeRouteRepository: ActiveRouteRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val locationClient =
        LocationServices.getFusedLocationProviderClient(context)

    // User location tracking
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private var locationCallback: LocationCallback? = null

    // User profile (for preferences like measurement unit)
    private val _userProfile = MutableStateFlow<Profile?>(null)
    val userProfile: StateFlow<Profile?> = _userProfile

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

    private val _otherActiveRoutes = MutableStateFlow<List<ActiveRoute>>(emptyList())
    val otherActiveRoutes: StateFlow<List<ActiveRoute>> = _otherActiveRoutes

    // Progressive route drawing
    private val _traveledPath = MutableStateFlow<List<LatLng>>(emptyList())
    val traveledPath: StateFlow<List<LatLng>> = _traveledPath

    private val _remainingRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val remainingRoute: StateFlow<List<LatLng>> = _remainingRoute

    private val _isRouteActive = MutableStateFlow(false)
    val isRouteActive: StateFlow<Boolean> = _isRouteActive

    // Navigation events
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent

    private val enteredZones = mutableSetOf<String>()
    private val activeRadiationZones = mutableMapOf<String, RadiationData>()
    private var lastRadiationUpdate = System.currentTimeMillis()
    private var lastRouteRecalculation = System.currentTimeMillis()

    init {
        loadInitialLocation()
        startLocationUpdates()
        observeActiveRoutes()
        startRadXDecayTimer()
        observeUserProfile()
    }

    fun onUserClick(userId: String) {
        Log.d("MapViewmodel", "User marker clicked: $userId")
        _navigationEvent.value = NavigationEvent.NavigateToUserDetails(userId)
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    private fun observeUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            profileRepository.observeProfile(userId).collect { profile ->
                _userProfile.value = profile
                Log.d("MapViewmodel", "User profile updated: preferences=${profile?.preferences}")
            }
        }
    }

    fun loadInitialLocation() {
        viewModelScope.launch {
            try {
                locationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(
                            "MapViewmodel",
                            "Got last location: ${location.latitude}, ${location.longitude}"
                        )
                        updateCurrentLocation(
                            LatLng(location.latitude, location.longitude)
                        )
                    } else {
                        Log.w(
                            "MapViewmodel",
                            "Last location is null, location updates should provide location"
                        )
                    }
                }.addOnFailureListener { e ->
                    Log.e("MapViewmodel", "Failed to get last location: ${e.message}")
                }
            } catch (e: SecurityException) {
                Log.e("MapViewmodel", "Location permission not granted", e)
            }
        }
    }

    fun startLocationUpdates() {
        if (locationCallback != null) {
            Log.d("MapViewmodel", "Location updates already started")
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(1500L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(
                        "MapViewmodel",
                        "Location update: ${location.latitude}, ${location.longitude}"
                    )
                    updateCurrentLocation(
                        LatLng(location.latitude, location.longitude)
                    )
                }
            }
        }

        try {
            Log.d("MapViewmodel", "Starting location updates...")
            locationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("MapViewmodel", "Location permission not granted", e)
            locationCallback = null
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            locationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun startRadXDecayTimer() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(RADX_DECAY_INTERVAL)

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    try {
                        profileRepository.decayRadXEffect(userId)
                    } catch (e: Exception) {
                        Log.e("MapViewmodel", "Failed to decay Rad-X effect: ${e.message}")
                    }
                }
            }
        }
    }

    fun updateCurrentLocation(location: LatLng) {
        _currentLocation.value = location
        loadRadSpots()
        checkRadiationZones(location)
        applyRadiationDamage()

        // Update route drawing if route is active
        if (_isRouteActive.value && _routePolyline.value.isNotEmpty()) {
            updateRouteDrawing(location)
            // Update current location in Firestore for other users to see
            updateActiveRouteLocation(location)
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
            calculateDistance(currentLoc, checkpoint) < CHECKPOINT_TRIGGER_DISTANCE
        }

        val remainingCheckpoints = if (closestCheckpointIndex >= 0) {
            // User reached a checkpoint! Award caps
            val userId = auth.currentUser?.uid
            if (userId != null) {
                viewModelScope.launch {
                    try {
                        profileRepository.addCaps(userId, CAPS_PER_CHECKPOINT)
                        Log.i("MapViewmodel", "✅ Checkpoint reached! Awarded $CAPS_PER_CHECKPOINT caps")
                    } catch (e: Exception) {
                        Log.e("MapViewmodel", "Failed to award checkpoint caps", e)
                    }
                }
            }

            // User is near a checkpoint, use remaining checkpoints from there
            checkpoints.drop(closestCheckpointIndex + 1)
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
            val zoneId = radSpot.id
            val distance = calculateDistance(location, radSpot.location)

            if (distance <= radSpot.radius) {
                currentZoneIds.add(zoneId)
                if (!activeRadiationZones.containsKey(zoneId)) {
                    // Just entered this zone
                    activeRadiationZones[zoneId] = radSpot
                    Log.w(
                        "MapViewmodel",
                        "Entered radiation zone: $zoneId (${radSpot.radiationLevelInMSv} mSv)"
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

            viewModelScope.launch {
                try {
                    // Get current profile to check radiation resistance
                    val profile = profileRepository.read(userId)
                    val resistance = profile?.radiationResistance ?: 0.0

                    // Calculate total radiation from all active zones
                    val rawRadiation = activeRadiationZones.values.sumOf {
                        it.radiationLevelInMSv * RADIATION_MULTIPLIER
                    }

                    // Apply radiation resistance (Rad-X effect)
                    val actualRadiation = rawRadiation * (1.0 - resistance)

                    // Calculate steps to deduct (based on actual radiation after resistance)
                    val stepsToDeduct =
                        (actualRadiation * (STEPS_DEDUCTION_PER_SV / RADIATION_MULTIPLIER)).roundToLong()

                    // Add radiation to profile
                    if (actualRadiation > 0) {
                        profileRepository.addRadiation(userId, actualRadiation)
                    }

                    // Deduct steps as penalty
                    if (stepsToDeduct > 0) {
                        profileRepository.deductSteps(userId, stepsToDeduct)
                    }

                    if (resistance > 0) {
                        Log.w(
                            "MapViewmodel",
                            "Radiation damage: +${actualRadiation} Sv (${rawRadiation} reduced by ${resistance * 100}%), -${stepsToDeduct} steps"
                        )
                    } else {
                        Log.w(
                            "MapViewmodel",
                            "Radiation damage: +${actualRadiation} Sv, -${stepsToDeduct} steps"
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

        saveActiveRouteToFirestore()

        Log.i("MapViewmodel", "Route activated and saved to Firestore")
    }

    fun endRoute() {
        val userId = auth.currentUser?.uid

        // Calculate rewards if user is authenticated and route was completed
        if (userId != null && _traveledPath.value.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    // Calculate distance traveled in kilometers
                    val distanceKm = calculateTotalDistance(_traveledPath.value) / 1000.0

                    // Calculate total checkpoints reached (total - remaining)
                    val totalCheckpoints = (_pendingCheckpoints.value.size).coerceAtLeast(0)

                    // Calculate rewards
                    val distanceReward = (distanceKm * CAPS_PER_KILOMETER).roundToLong().toInt()
                    val checkpointReward = totalCheckpoints * CAPS_PER_CHECKPOINT
                    val totalReward = ROUTE_COMPLETION_BASE_CAPS + distanceReward + checkpointReward

                    // Award caps
                    if (totalReward > 0) {
                        profileRepository.addCaps(userId, totalReward)
                        Log.i(
                            "MapViewmodel",
                            "🎉 Route completed! Awarded $totalReward caps " +
                                    "(Base: $ROUTE_COMPLETION_BASE_CAPS, " +
                                    "Distance: $distanceReward for ${distanceKm.roundToLong()}km, " +
                                    "Checkpoints: $checkpointReward for $totalCheckpoints checkpoints)"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MapViewmodel", "Failed to award route completion caps", e)
                }
            }
        }

        _isRouteActive.value = false
        _traveledPath.value = emptyList()
        _remainingRoute.value = emptyList()
        clearPendingCheckpoints()
        endActiveRouteInFirestore()

        Log.i("MapViewmodel", "Route ended and removed from Firestore")
    }

    private fun calculateTotalDistance(path: List<LatLng>): Double {
        if (path.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until path.size - 1) {
            totalDistance += calculateDistance(path[i], path[i + 1])
        }
        return totalDistance
    }

    fun setPoint(location: LatLng) {
        _selectedPoint.value = location
    }

    fun onRadiationMarkerClick(radiationData: RadiationData) {
        _selectedRadiationMarker.value = radiationData
        Log.d(
            "MapViewmodel",
            "Radiation marker selected: ${radiationData.radiationLevelInMSv} Sv at ${radiationData.location}"
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
                    id = UUID.randomUUID().toString(),
                    location = location,
                    radiationLevelInMSv = radiationLevel,
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
        val zoneId = radData.id

        if (enteredZones.add(zoneId)) {
            _radiationAlert.value = radData

            // Trigger vibration with intensity based on radiation level
            triggerRadiationVibration(radData.radiationLevelInMSv)
        }
    }

    private fun triggerRadiationVibration(radiationLevelInMSv: Double) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                // Calculate vibration intensity based on radiation level
                // 0-0.1 mSv = Low (128), 0.1-1.0 mSv = Medium (192), >1.0 mSv = High (255)
                val amplitude = when {
                    radiationLevelInMSv < 0.1 -> 128
                    radiationLevelInMSv < 1.0 -> 192
                    else -> 255
                }

                val vibrateDuration = when {
                    radiationLevelInMSv < 0.1 -> 300L
                    radiationLevelInMSv < 1.0 -> 500L
                    else -> 700L
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, vibrateDuration, 200, vibrateDuration)
                    val amplitudes = intArrayOf(0, amplitude, 0, amplitude)
                    val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
                    it.vibrate(effect)

                    Log.d("MapViewmodel", "Vibration triggered: ${radiationLevelInMSv}mSv -> amplitude=$amplitude, duration=${vibrateDuration}ms")
                } else {
                    // Fallback for older devices (can't control amplitude)
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, vibrateDuration, 200, vibrateDuration), -1)

                    Log.d("MapViewmodel", "Vibration triggered (legacy): ${radiationLevelInMSv}mSv -> duration=${vibrateDuration}ms")
                }
            } ?: Log.w("MapViewmodel", "Vibrator not available")
        } catch (e: Exception) {
            Log.e("MapViewmodel", "Failed to trigger vibration", e)
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

                // Get user's measurement preference
                val measurementUnit = _userProfile.value?.preferences?.measurementUnit
                    ?: com.fcul.smartboy.domain.user.MeasurementUnit.METRIC

                val result = routesRepository.computeRoute(
                    apiKey = apiKey,
                    origin = actualOrigin,
                    destination = destination,
                    measurementUnit = measurementUnit,
                    waypoints = waypoints
                )

                when (result) {
                    is RouteResult.Success -> {
                        // Use Google's built-in polyline decoder from maps-utils
                        val decodedPolyline = PolyUtil.decode(result.encodedPolyline)
                        _routePolyline.value = decodedPolyline

                        Log.i("MapViewmodel", "Route computed: ${decodedPolyline.size} points")

                        // Format distance based on user's measurement preference
                        val measurementUnit = _userProfile.value?.preferences?.measurementUnit
                            ?: com.fcul.smartboy.domain.user.MeasurementUnit.METRIC

                        val distanceText = MeasurementUtils.formatDistance(
                            result.distanceMeters.toDouble(),
                            measurementUnit
                        )

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

    override fun onCleared() {
        super.onCleared()
        Log.d("MapViewmodel", "ViewModel cleared, stopping location updates")
        stopLocationUpdates()

        // Clean up active route if user closes app
        if (_isRouteActive.value) {
            endActiveRouteInFirestore()
        }
    }

    // Active Routes Management
    private fun observeActiveRoutes() {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // Combine current location and observe routes within 10km radius
            currentLocation.collect { location ->
                activeRouteRepository.observeActiveRoutes(
                    excludeUserId = currentUserId,
                    userLocation = location,
                    radiusKm = 10.0  // Only show routes within 10km
                ).collect { routes ->
                    _otherActiveRoutes.value = routes
                    Log.d(
                        "MapViewmodel",
                        "Updated other active routes: ${routes.size} routes within 10km"
                    )
                }
            }
        }
    }

    private fun saveActiveRouteToFirestore() {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Unknown User"
        val currentLoc = _currentLocation.value
        val checkpoints = _pendingCheckpoints.value

        if (checkpoints.size < 2) {
            Log.w("MapViewmodel", "Cannot save route with less than 2 checkpoints")
            return
        }

        val activeRoute = ActiveRoute(
            id = userId,
            userId = userId,
            userName = userName,
            startTime = System.currentTimeMillis(),
            checkpoints = checkpoints,
            currentLocation = currentLoc,
            isActive = true,
            totalDistance = calculateTotalRouteDistance(checkpoints)
        )

        viewModelScope.launch {
            activeRouteRepository.saveActiveRoute(activeRoute).onSuccess {
                Log.i("MapViewmodel", "Active route saved to Firestore")
            }.onFailure { e ->
                Log.e("MapViewmodel", "Failed to save active route", e)
            }
        }
    }

    private fun updateActiveRouteLocation(location: LatLng) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            activeRouteRepository.updateCurrentLocation(userId, location).onFailure { e ->
                Log.e("MapViewmodel", "Failed to update route location", e)
            }
        }
    }

    private fun endActiveRouteInFirestore() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            activeRouteRepository.endActiveRoute(userId).onSuccess {
                Log.i("MapViewmodel", "Active route removed from Firestore")
            }.onFailure { e ->
                Log.e("MapViewmodel", "Failed to end active route", e)
            }
        }
    }

    private fun calculateTotalRouteDistance(checkpoints: List<LatLng>): Double {
        if (checkpoints.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until checkpoints.size - 1) {
            totalDistance += calculateDistance(checkpoints[i], checkpoints[i + 1])
        }
        return totalDistance / 1000.0 // Convert to kilometers
    }

    companion object {
        private const val RADIATION_UPDATE_INTERVAL = 5000L
        private const val RADIATION_MULTIPLIER = 0.1
        private const val STEPS_DEDUCTION_PER_SV = 10L
        private const val MAX_ROUTE_DEVIATION_METERS =
            100.0 // Recalculate if user is >100m off route
        private const val ROUTE_RECALC_COOLDOWN = 10000L // Wait 10s between recalculations
        private const val RADX_DECAY_INTERVAL = 15000L // Decay Rad-X effect every 15 seconds

        private const val LOCATION_UPDATE_INTERVAL = 5000L
        private const val CHECKPOINT_TRIGGER_DISTANCE = 50.0
        private const val RAD_ZONE_CHECK_DISTANCE = 100.0

        // Reward constants
        private const val ROUTE_COMPLETION_BASE_CAPS = 100 // Base reward for completing a route
        private const val CAPS_PER_KILOMETER = 10 // Additional caps per km traveled
        private const val CAPS_PER_CHECKPOINT = 25 // Caps for each checkpoint reached
    }
}

sealed class NavigationEvent {
    data class NavigateToUserDetails(val userId: String) : NavigationEvent()
}

package com.fcul.smartboy.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fcul.smartboy.R
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.domain.route.RouteInfo
import com.fcul.smartboy.ui.map.components.ActiveRadiationAlertDialog
import com.fcul.smartboy.ui.map.components.AddRadPointDialog
import com.fcul.smartboy.ui.map.components.FloatingMenu
import com.fcul.smartboy.ui.map.components.GoogleMapComponent
import com.fcul.smartboy.ui.map.components.MapDebugOverlay
import com.fcul.smartboy.ui.map.components.RadiationAlertDialog
import com.fcul.smartboy.ui.map.components.RouteInfoPanel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapScreen(
    currentLocation: LatLng?,
    radiationSpots: List<RadiationData>,
    radiationAlert: RadiationData?,
    pendingCheckpoints: List<LatLng>,
    routePolyline: List<LatLng>,
    routeInfo: RouteInfo?,
    traveledPath: List<LatLng>,
    remainingRoute: List<LatLng>,
    selectedRadiationMarker: RadiationData?,
    selectedCheckpointMarker: LatLng?,
    isRouteActive: Boolean,
    onEnteringRadPoint: (RadiationData) -> Unit,
    onDismissAlert: () -> Unit,
    onSetPoint: (LatLng) -> Unit,
    onCreateRadPoint: (LatLng, Double, Double) -> Unit,
    onLocationUpdate: (LatLng) -> Unit,
    onRadiationMarkerClick: (RadiationData) -> Unit,
    onCheckpointMarkerClick: (LatLng) -> Unit,
    onClearMarkerSelection: () -> Unit,
    onStartRoute: () -> Unit,
    onAddPendingCheckpoint: () -> Unit,
    onClearPendingCheckpoints: () -> Unit,
    onClearSelectedCheckpoint: () -> Unit,
    onEndRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    var showAddRadDialog by remember { mutableStateOf(false) }
    var selectedRadLocation by remember { mutableStateOf<LatLng?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }

    var activeAlert by remember { mutableStateOf<RadiationData?>(null) }

    // If no permission, show permission request UI
    if (!hasLocationPermission) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = stringResource(R.string.map_permissions_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.map_permissions_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
        return
    }

    // Camera state for Compose GoogleMap
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation ?: LatLng(0.0, 0.0), 15f)
    }

    DisposableEffect(Unit) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L // 3 seconds
        ).apply {
            setMinUpdateIntervalMillis(1500L)
            setWaitForAccurateLocation(false)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val newLoc = LatLng(loc.latitude, loc.longitude)
                    onLocationUpdate(newLoc) // called even if the user hasn't moved
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission not granted - location updates won't work
        }

        onDispose {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (e: SecurityException) {
                // Permission issue - ignore
            }
        }
    }

    // Animate camera to user location whenever it updates
    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
            onLocationUpdate(it)
        }
    }

    // Check if user enters any radiation zone
    LaunchedEffect(currentLocation, radiationSpots) {
        val userLoc = currentLocation ?: return@LaunchedEffect
        radiationSpots.forEach { radSpot ->
            val results = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude,
                userLoc.longitude,
                radSpot.location.latitude,
                radSpot.location.longitude,
                results
            )
            if (results[0] <= radSpot.radius) {
                onEnteringRadPoint(radSpot)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMapComponent(
            cameraPositionState = cameraPositionState,
            radiationSpots = radiationSpots,
            selectedRadLocation = selectedRadLocation,
            pendingCheckpoints = pendingCheckpoints,
            routePolyline = routePolyline,
            traveledPath = traveledPath,
            remainingRoute = remainingRoute,
            selectedRadiationMarker = selectedRadiationMarker,
            selectedCheckpointMarker = selectedCheckpointMarker,
            isRouteActive = isRouteActive,
            onMapClick = { latLng ->
                selectedRadLocation = latLng
                onSetPoint(latLng)
                onClearMarkerSelection() // Clear marker selection when clicking the map
            },
            onRadiationMarkerClick = onRadiationMarkerClick,
            onCheckpointMarkerClick = onCheckpointMarkerClick,
            modifier = Modifier.fillMaxSize(),
        )

        // Route Info Panel (like Google Maps navigation bottom panel)
        if (isRouteActive && routeInfo != null) {
            RouteInfoPanel(
                routeInfo = routeInfo,
                traveledPath = traveledPath,
                remainingRoute = remainingRoute,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            )
        }

        // Debug overlay
        Surface(
            modifier = modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            MapDebugOverlay(
                currentLocation = currentLocation,
                radiationSpots = radiationSpots,
                isRouteActive = isRouteActive,
                pendingCheckpoints = pendingCheckpoints,
                routePolyline = routePolyline,
                traveledPath = traveledPath,
                remainingRoute = remainingRoute,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        Box(
            modifier = modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingMenu(
                isRouteActive = isRouteActive,
                pendingCheckpoints = pendingCheckpoints,
                cameraPositionState = cameraPositionState,
                checkpointSelected = selectedCheckpointMarker,
                onClearPendingCheckpoints = onClearPendingCheckpoints,
                onClearSelectedCheckpoint = onClearSelectedCheckpoint,
                onAddPendingCheckpoint = onAddPendingCheckpoint,
                onStartRoute = onStartRoute,
                onEndRoute = onEndRoute,
                onAddRadPoint = { targetPos ->
                    isMenuOpen = false
                    selectedRadLocation = targetPos
                    showAddRadDialog = true
                },
                isMenuOpen = isMenuOpen,
                onMenuOpenChange = { isMenuOpen = it }
            )
        }
    }

    activeAlert?.let { alert ->
        ActiveRadiationAlertDialog(
            alert = alert,
            onDismiss = { activeAlert = null }
        )
    }

    if (showAddRadDialog && selectedRadLocation != null) {
        AddRadPointDialog(
            location = selectedRadLocation!!,
            onDismiss = {
                showAddRadDialog = false
                selectedRadLocation = null
            },
            onConfirm = { radiationLevel, radius ->
                onCreateRadPoint(selectedRadLocation!!, radiationLevel, radius)
                showAddRadDialog = false
                selectedRadLocation = null
            }
        )
    }

    if (radiationAlert != null) {
        RadiationAlertDialog(
            radiationAlert = radiationAlert,
            onDismiss = onDismissAlert
        )
    }
}

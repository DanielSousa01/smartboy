package com.fcul.smartboy.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.fcul.smartboy.domain.route.ActiveRoute
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.domain.route.RouteInfo
import com.fcul.smartboy.ui.map.components.AddRadPointDialog
import com.fcul.smartboy.ui.map.components.FloatingMenu
import com.fcul.smartboy.ui.map.components.GoogleMapComponent
import com.fcul.smartboy.ui.map.components.MapDebugOverlay
import com.fcul.smartboy.ui.map.components.RadiationAlertDialog
import com.fcul.smartboy.ui.map.components.RouteInfoPanel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
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
    otherActiveRoutes: List<ActiveRoute>,
    onEnteringRadPoint: (RadiationData) -> Unit,
    onDismissAlert: () -> Unit,
    onSetPoint: (LatLng) -> Unit,
    onCreateRadPoint: (LatLng, Double, Double) -> Unit,
    onRadiationMarkerClick: (RadiationData) -> Unit,
    onCheckpointMarkerClick: (LatLng) -> Unit,
    onClearMarkerSelection: () -> Unit,
    onStartRoute: () -> Unit,
    onAddPendingCheckpoint: () -> Unit,
    onClearPendingCheckpoints: () -> Unit,
    onClearSelectedCheckpoint: () -> Unit,
    onEndRoute: () -> Unit,
    onUserMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var followUser by remember { mutableStateOf(true) }

    // Permission state - check location AND activity recognition
    val hasLocationPermission = remember(context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    val hasActivityRecognition = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older Android versions
        }
    }

    // Map requires BOTH location and activity recognition
    val hasAllPermissions = hasLocationPermission && hasActivityRecognition

    var showAddRadDialog by remember { mutableStateOf(false) }
    var selectedRadLocation by remember { mutableStateOf<LatLng?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var enteredZones by remember { mutableStateOf(setOf<String>()) }


    // If no permission, show permission request UI
    if (!hasAllPermissions) {
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
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        }
        return
    }
    if (currentLocation == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Camera state for Compose GoogleMap
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
    }

    var hasInitializedCamera by remember { mutableStateOf(false) }

    // Detect when user manually moves the camera and disable follow mode
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            followUser = false
        }
    }

    // Animate camera to user location only on initial load
    LaunchedEffect(currentLocation) {
        if (!hasInitializedCamera) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(currentLocation, 15f),
                durationMs = 1000
            )
            hasInitializedCamera = true
        }
    }

    // Update camera position ONLY when followUser is true AND location changes
    LaunchedEffect(currentLocation, followUser) {
        if (followUser && hasInitializedCamera) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(currentLocation),
                durationMs = 500
            )
        }
    }

    // Check if user enters any radiation zone
    LaunchedEffect(currentLocation, radiationSpots) {
        val newEntered = mutableSetOf<String>()

        radiationSpots.forEach { radSpot ->
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                radSpot.location.latitude,
                radSpot.location.longitude,
                results
            )

            if (results[0] <= radSpot.radius) {
                newEntered.add(radSpot.id)
                if (radSpot.id !in enteredZones) {
                    onEnteringRadPoint(radSpot)
                }
            }
        }

        enteredZones = newEntered
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
            otherActiveRoutes = otherActiveRoutes,
            onMapClick = { latLng ->
                selectedRadLocation = latLng
                onSetPoint(latLng)
                onClearMarkerSelection()
            },
            onRadiationMarkerClick = onRadiationMarkerClick,
            onCheckpointMarkerClick = onCheckpointMarkerClick,
            onUserMarkerClick = onUserMarkerClick,
            modifier = Modifier.fillMaxSize(),
        )

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
            modifier = Modifier
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
                modifier = Modifier.padding(8.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingMenu(
                isRouteActive = isRouteActive,
                pendingCheckpoints = pendingCheckpoints,
                checkpointSelected = selectedCheckpointMarker,
                onClearPendingCheckpoints = onClearPendingCheckpoints,
                onClearSelectedCheckpoint = onClearSelectedCheckpoint,
                onAddPendingCheckpoint = onAddPendingCheckpoint,
                onStartRoute = {
                    followUser = true
                    onStartRoute()
                },
                onEndRoute = onEndRoute,
                onAddRadPoint = {
                    isMenuOpen = false
                    showAddRadDialog = true
                },
                isMenuOpen = isMenuOpen,
                onMenuOpenChange = { isMenuOpen = it }
            )
        }

        selectedRadLocation?.let { location ->
            if (showAddRadDialog) {
                AddRadPointDialog(
                    location = location,
                    onDismiss = {
                        showAddRadDialog = false
                        selectedRadLocation = null
                    },
                    onConfirm = { radiationLevel, radius ->
                        onCreateRadPoint(location, radiationLevel, radius)
                        showAddRadDialog = false
                        selectedRadLocation = null
                    }
                )
            }
        }

        if (radiationAlert != null) {
            RadiationAlertDialog(
                radiationAlert = radiationAlert,
                onDismiss = onDismissAlert
            )
        }
    }
}

package com.fcul.smartboy.ui.map

import android.location.Location
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.ui.map.components.AddRadPointDialog
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapScreen(
    currentLocation: LatLng?,
    radiationSpots: List<RadiationData>,
    radiationAlert: RadiationData?,
    isRouteActive: Boolean,
    routeCheckpoints: List<LatLng>,
    pendingCheckpoints: List<LatLng>,
    routePolyline: List<LatLng>,
    onEnteringRadPoint: (RadiationData) -> Unit,
    onDismissAlert: () -> Unit,
    onSetPoint: (LatLng) -> Unit,
    onCreateRadPoint: (LatLng, Double, Double) -> Unit,
    onLocationUpdate: (LatLng) -> Unit,
    onStartRoute: () -> Unit,
    onAddPendingCheckpoint: () -> Unit,
    onAddCheckpoint: () -> Unit,
    onEndRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var showAddRadDialog by remember { mutableStateOf(false) }
    var selectedRadLocation by remember { mutableStateOf<LatLng?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }

    var activeAlert by remember { mutableStateOf<RadiationData?>(null) }

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

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
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
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true
            ),
            properties = MapProperties(isMyLocationEnabled = true),
            onMapClick = { latLng ->
                selectedRadLocation = latLng
                onSetPoint(latLng)
            }
        ) {
            // Stable circles and markers
            val markers = remember(radiationSpots) {
                radiationSpots.map { rad -> rad to MarkerState(rad.location) }
            }

            markers.forEach { (rad, state) ->
                Circle(
                    center = rad.location,
                    radius = rad.radius.toDouble(),
                    strokeColor = Color(0xCCFF6B00),
                    fillColor = Color(0x33FF6B00),
                    strokeWidth = 3f
                )
                Marker(
                    state = state,
                    title = "⚠️ Radiation Zone",
                    snippet = "Level: ${rad.radiationLevelInSv} Sv\nRadius: ${rad.radius} m",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }

            // Marker for selected RAD
            selectedRadLocation?.let {
                Marker(state = MarkerState(it), title = "RAD Point")
            }

            // Show pending checkpoints if not active, else show active route checkpoints
            val checkpointsToShow = if (isRouteActive) routeCheckpoints else pendingCheckpoints
            checkpointsToShow.forEachIndexed { index, checkpoint ->
                Marker(
                    state = MarkerState(checkpoint),
                    title = if (isRouteActive) "Route Checkpoint ${index + 1}" else "Pending Checkpoint ${index + 1}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
            }
            if (checkpointsToShow.size >= 2) {
                Polyline(
                    points = checkpointsToShow,
                    color = Color.Blue,
                    width = 8f
                )
            }
            // Draw Directions API polyline if present
            if (routePolyline.isNotEmpty()) {
                Polyline(
                    points = routePolyline,
                    color = Color.Red,
                    width = 10f
                )
            }
        }

        // Debug overlay
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "📍 Radiation Spots: ${radiationSpots.size}",
                    color = Color.White
                )
                Text(
                    text = "🚩 Route: ${if (isRouteActive) "Active" else "Inactive"}",
                    color = if (isRouteActive) Color.Green else Color.Gray
                )
                if (isRouteActive) {
                    Text(
                        text = "📌 Checkpoints: ${routeCheckpoints.size}",
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "🕒 Pending Checkpoints: ${pendingCheckpoints.size}",
                        color = Color.White
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButtonMenu(
                expanded = isMenuOpen,
                button = {
                    FloatingActionButton(onClick = { isMenuOpen = !isMenuOpen }) {
                        Icon(Icons.Default.Add, contentDescription = "Menu")
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add RAD") },
                    text = { Text("Create RAD") },
                    onClick = {
                        val targetPos = cameraPositionState.position.target
                        selectedRadLocation = targetPos
                        showAddRadDialog = true
                        isMenuOpen = false
                    }
                )
                if (!isRouteActive) {
                    FloatingActionButtonMenuItem(
                        icon = { Text("📍") },
                        text = { Text("Add Checkpoint") },
                        onClick = {
                            onAddPendingCheckpoint()
                            isMenuOpen = false
                        }
                    )
                    if (pendingCheckpoints.size >= 2) {
                        FloatingActionButtonMenuItem(
                            icon = { Text("🚀") },
                            text = { Text("Start Route") },
                            onClick = {
                                onStartRoute()
                                isMenuOpen = false
                            },
                        )
                    }
                } else {
                    FloatingActionButtonMenuItem(
                        icon = { Text("📍") },
                        text = { Text("Add Checkpoint") },
                        onClick = {
                            onAddCheckpoint()
                            isMenuOpen = false
                        }
                    )
                    FloatingActionButtonMenuItem(
                        icon = { Text("🏁") },
                        text = { Text("End Route") },
                        onClick = {
                            onEndRoute()
                            isMenuOpen = false
                        }
                    )
                }
            }
        }
    }

    activeAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { activeAlert = null },
            title = { Text("⚠️ Radiation Alert") },
            text = {
                Text(
                    "You entered a radiation zone!\n" +
                            "Level: ${alert.radiationLevelInSv} Sv\n" +
                            "Radius: ${alert.radius} m"
                )
            },
            confirmButton = {
                TextButton(onClick = { activeAlert = null }) {
                    Text("OK")
                }
            }
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
        AlertDialog(
            onDismissRequest = onDismissAlert,
            icon = { Text("⚠️", style = MaterialTheme.typography.displayMedium) },
            title = { Text("Radiation Zone Warning") },
            text = {
                Column {
                    Text("You have entered a radiation zone!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Radiation Level: ${radiationAlert.radiationLevelInSv} Sv")
                    Text("Affected Radius: ${radiationAlert.radius}m")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ Take appropriate safety measures!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissAlert) {
                    Text("OK")
                }
            }
        )
    }
}

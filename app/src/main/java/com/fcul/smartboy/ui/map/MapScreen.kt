package com.fcul.smartboy.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.ui.map.components.AddRadPointDialog
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapScreen(
    currentLocation: LatLng?,
    radiationSpots: List<RadiationData>,
    radiationAlert: RadiationData?,
    onEnteringRadPoint: (RadiationData) -> Unit,
    onDismissAlert: () -> Unit,
    onSetPoint: (LatLng) -> Unit,
    onCreateRadPoint: (LatLng, Double, Double) -> Unit,
    onLocationUpdate: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var currentCameraPosition by remember { mutableStateOf(currentLocation) }
    var showAddRadDialog by remember { mutableStateOf(false) }
    var selectedRadLocation by remember { mutableStateOf<LatLng?>(null) }

    val mapView = remember {
        MapView(context).apply {
            post {
                val surface = getChildAt(0) as? SurfaceView
                surface?.setZOrderOnTop(false)
                surface?.setZOrderMediaOverlay(false)
            }
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val newLoc = LatLng(location.latitude, location.longitude)
                    onLocationUpdate(newLoc)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    // Start location updates when map is ready
    @SuppressLint("MissingPermission")
    LaunchedEffect(googleMap) {
        if (googleMap == null) return@LaunchedEffect

        // Check if we have location permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000L // 3 seconds interval
            ).apply {
                setMinUpdateIntervalMillis(1500L)
                setWaitForAccurateLocation(false)
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
        } else {
            Log.e("MapScreen", "❌ Location permission NOT granted")
        }
    }

    // Stop location updates when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    LaunchedEffect(currentLocation) {
        val gMap = googleMap
        if (currentLocation != null && gMap != null) {
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        }
    }

    // Check if user enters a radiation zone
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
            val distanceInMeters = results[0]

            if (distanceInMeters <= radSpot.radius) {
                Log.d(
                    "MapScreen",
                    "⚠️ User entered radiation zone! Distance: ${distanceInMeters}m, Radius: ${radSpot.radius}m"
                )
                onEnteringRadPoint(radSpot)
            }
        }
    }

    // Render radiation spots on the map
    LaunchedEffect(radiationSpots, googleMap) {
        val gMap = googleMap ?: run {
            return@LaunchedEffect
        }

        if (radiationSpots.isEmpty()) {
            return@LaunchedEffect
        }

        gMap.clear()

        // Add markers and circles for each radiation spot
        radiationSpots.forEachIndexed { index, radData ->
            try {
                val circleCenter = radData.location
                val circleRadius = radData.radius

                gMap.addCircle(
                    CircleOptions()
                        .center(circleCenter)
                        .radius(circleRadius)
                        .strokeColor(
                            android.graphics.Color.argb(
                                204,
                                255,
                                107,
                                0
                            )
                        ) // 0.8 alpha orange
                        .fillColor(
                            android.graphics.Color.argb(
                                51,
                                255,
                                107,
                                0
                            )
                        )    // 0.2 alpha orange
                        .strokeWidth(3f)
                        .visible(true)
                        .clickable(true)
                )

                gMap.addMarker(
                    MarkerOptions()
                        .position(radData.location)
                        .title("⚠️ Radiation Zone")
                        .snippet("Level: ${radData.radiationLevelInSv} Sv\nRadius: ${radData.radius}m")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )
            } catch (e: Exception) {
                Log.e("MapScreen", "❌ Error rendering radiation spot at ${radData.location}", e)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.getMapAsync { gMap ->
                    googleMap = gMap

                    try {
                        @SuppressLint("MissingPermission")
                        gMap.isMyLocationEnabled = true

                        currentLocation?.let {
                            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                        }
                    } catch (e: SecurityException) {
                        Log.e("MapScreen", "❌ Location permission not granted", e)
                    }

                    gMap.setOnCameraMoveListener {
                        currentCameraPosition = gMap.cameraPosition.target
                    }

                    gMap.setOnMapClickListener { latLng ->
                        gMap.addMarker(MarkerOptions().position(latLng).title("Selected Point"))
                        onSetPoint(latLng)
                    }
                }
            }
        )

        // Debug overlay showing radiation spots count
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "📍 Radiation Spots: ${radiationSpots.size}",
                    color = Color.White
                )
                currentLocation?.let {
                    Text(
                        text = "📌 Location: ${
                            String.format(
                                "%.4f",
                                it.latitude
                            )
                        }, ${String.format("%.4f", it.longitude)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
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
                    FloatingActionButton(
                        onClick = { isMenuOpen = !isMenuOpen }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Menu")
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add RAD") },
                    text = { Text("Create RAD") },
                    onClick = {
                        val targetPos = currentCameraPosition
                        if (targetPos != null) {
                            selectedRadLocation = targetPos
                            showAddRadDialog = true
                            googleMap?.addMarker(
                                MarkerOptions().position(targetPos).title("RAD Point")
                            )
                        }
                        isMenuOpen = false
                    }
                )
            }
        }
    }

    // Show Toast when radiation alert is triggered
    /*
    LaunchedEffect(radiationAlert) {
        radiationAlert?.let { radData ->
            Toast.makeText(
                context,
                "⚠️ WARNING: Entered radiation zone! Level: ${radData.radiationLevelInSv} Sv",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    */

    // Show dialog when user wants to add a RAD point
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

    // Show radiation warning dialog
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
package com.fcul.smartboy.ui.map

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapScreen(
    onSetPoint: (LatLng) -> Unit,
    onClearPoint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLocation by remember { mutableStateOf(LatLng(38.7223, -9.1393)) }
    val context = LocalContext.current

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var currentCameraPosition by remember { mutableStateOf(currentLocation) }

    val mapView = remember {
        MapView(context).apply {
            post {
                val surface = getChildAt(0) as? SurfaceView
                surface?.setZOrderOnTop(false)
                surface?.setZOrderMediaOverlay(false)
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.getMapAsync { gMap ->
                    googleMap = gMap
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f))

                    // Listen for camera movements
                    gMap.setOnCameraMoveListener {
                        currentCameraPosition = gMap.cameraPosition.target
                    }

                    gMap.setOnMapClickListener { latLng ->
                        gMap.clear()
                        gMap.addMarker(MarkerOptions().position(latLng).title("Selected Point"))
                        onSetPoint(latLng)
                    }
                }
            }
        )

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
                        googleMap?.let { gMap ->
                            gMap.clear()
                            gMap.addMarker(
                                MarkerOptions().position(currentCameraPosition).title("RAD Point")
                            )
                            onSetPoint(currentCameraPosition)
                        }
                        isMenuOpen = false
                    }
                )
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Delete RAD") },
                    text = { Text("Delete RAD") },
                    onClick = {
                        googleMap?.clear()
                        onClearPoint()
                        isMenuOpen = false
                    }
                )
            }
        }
    }
}
package com.fcul.smartboy.ui.map.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fcul.smartboy.domain.route.RadiationData
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import kotlin.math.roundToLong

@Composable
fun GoogleMapComponent(
    cameraPositionState: CameraPositionState,
    radiationSpots: List<RadiationData>,
    selectedRadLocation: LatLng?,
    pendingCheckpoints: List<LatLng>,
    routePolyline: List<LatLng>,
    traveledPath: List<LatLng>,
    remainingRoute: List<LatLng>,
    isRouteActive: Boolean,
    selectedRadiationMarker: RadiationData?,
    selectedCheckpointMarker: LatLng?,
    onMapClick: (LatLng) -> Unit,
    onRadiationMarkerClick: (RadiationData) -> Unit,
    onCheckpointMarkerClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
) {
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false,
            tiltGesturesEnabled = true,
            rotationGesturesEnabled = true,
        ),
        properties = MapProperties(
            isMyLocationEnabled = true,
            isBuildingEnabled = true
        ),
        onMapClick = onMapClick
    ) {
        // Stable circles and markers
        val markers = radiationSpots.map { rad -> rad to MarkerState(rad.location) }
        markers.forEach { (rad, state) ->
            val isSelected = selectedRadiationMarker?.id == rad.id

            // Inner danger circle - more opaque
            Circle(
                center = rad.location,
                radius = rad.radius * 0.5,
                strokeColor = if (isSelected) Color(0xFFFFFF00) else Color(0xFFFF0000),
                fillColor = Color(0x66FF0000),
                strokeWidth = if (isSelected) 4f else 2f
            )
            // Outer warning circle - less opaque
            Circle(
                center = rad.location,
                radius = rad.radius,
                strokeColor = if (isSelected) Color(0xFFFFFF00) else Color(0xFFFF6B00),
                fillColor = Color(0x33FF6B00),
                strokeWidth = if (isSelected) 5f else 3f
            )
            Marker(
                state = state,
                title = "☢️ Radiation Zone",
                snippet = "Level: ${rad.radiationLevelInSv} Sv\nRadius: ${rad.radius} m\n⚠️ Danger: -${(rad.radiationLevelInSv * 10).roundToLong()} steps every 5s",
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (isSelected) BitmapDescriptorFactory.HUE_YELLOW
                    else BitmapDescriptorFactory.HUE_RED
                ),
                onClick = {
                    onRadiationMarkerClick(rad)
                    true
                }
            )
        }
        // Marker for selected RAD
        selectedRadLocation?.let {
            Marker(state = MarkerState(it), title = "RAD Point")
        }
        // Show pending checkpoints if not active, else show active route checkpoints
        val checkpointsToShow = pendingCheckpoints
        checkpointsToShow.forEachIndexed { index, checkpoint ->
            val isSelected = selectedCheckpointMarker == checkpoint

            val markerColor = when {
                isSelected -> BitmapDescriptorFactory.HUE_YELLOW
                isRouteActive -> BitmapDescriptorFactory.HUE_BLUE
                else -> BitmapDescriptorFactory.HUE_AZURE
            }

            val title = if (isRouteActive) {
                "Checkpoint ${index + 1}"
            } else {
                "Pending Checkpoint ${index + 1}"
            }

            Marker(
                state = MarkerState(checkpoint),
                title = title,
                icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                onClick = {
                    onCheckpointMarkerClick(checkpoint)
                    true
                }
            )
        }
        // Draw simple checkpoint connection line (dashed style) - only when no route polyline
        if (checkpointsToShow.size >= 2 && routePolyline.isEmpty()) {
            Polyline(
                points = checkpointsToShow,
                color = Color(0xFF4A90E2),
                width = 5f,
                pattern = listOf(
                    Dot(),
                    Gap(10f)
                )
            )
        }
        // Draw the route polyline (from Google Directions API or fallback)
        if (routePolyline.isNotEmpty() && isRouteActive && traveledPath.size > 1 && remainingRoute.size > 1) {
            // During active navigation with progress tracking
            // TRAVELED PATH (Already covered)
            Polyline(
                points = traveledPath,
                color = Color(0xFF388E3C),
                width = 16f,
                geodesic = true,
                zIndex = 3f
            )
            Polyline(
                points = traveledPath,
                color = Color(0xFF66BB6A),
                width = 12f,
                geodesic = true,
                zIndex = 4f
            )
            // REMAINING ROUTE (Yet to travel)
            Polyline(
                points = remainingRoute,
                color = Color(0xFF1565C0),
                width = 16f,
                geodesic = true,
                zIndex = 1f
            )
            Polyline(
                points = remainingRoute,
                color = Color(0xFF2196F3),
                width = 12f,
                geodesic = true,
                zIndex = 2f
            )
        }
    }
}
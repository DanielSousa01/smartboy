package com.fcul.smartboy.ui.map.components

import android.location.Location
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.route.RadiationData
import com.google.android.gms.maps.model.LatLng

@Composable
fun MapDebugOverlay(
    currentLocation: LatLng?,
    radiationSpots: List<RadiationData>,
    pendingCheckpoints: List<LatLng>,
    routePolyline: List<LatLng>,
    traveledPath: List<LatLng>,
    remainingRoute: List<LatLng>,
    isRouteActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(
            text = "📍 Radiation Spots: ${radiationSpots.size}",
            color = Color.White
        )

        // Check if user is in any radiation zone
        val inRadZone = currentLocation?.let { userLoc ->
            radiationSpots.any { radSpot ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLoc.latitude,
                    userLoc.longitude,
                    radSpot.location.latitude,
                    radSpot.location.longitude,
                    results
                )
                results[0] <= radSpot.radius
            }
        } ?: false

        if (inRadZone) {
            Text(
                text = "☢️ IN RADIATION ZONE!",
                color = Color.Red,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = "🚩 Route: ${if (isRouteActive) "Active" else "Inactive"}",
            color = if (isRouteActive) Color.Green else Color.Gray
        )
        if (isRouteActive) {
            Text(
                text = "📌 Checkpoints: ${pendingCheckpoints.size}",
                color = Color.White
            )
            if (routePolyline.isNotEmpty()) {
                Text(
                    text = "🗺️ Route Loaded",
                    color = Color.Cyan
                )
            }
            if (traveledPath.isNotEmpty()) {
                val progress =
                    (traveledPath.size.toFloat() / (traveledPath.size + remainingRoute.size)) * 100
                Text(
                    text = "✅ Progress: ${progress.toInt()}%",
                    color = Color.Green
                )
            }
        } else {
            Text(
                text = "🕒 Pending Checkpoints: ${pendingCheckpoints.size}",
                color = Color.White
            )
        }
    }
}

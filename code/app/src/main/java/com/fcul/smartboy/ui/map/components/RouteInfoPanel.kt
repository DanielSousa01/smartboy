package com.fcul.smartboy.ui.map.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fcul.smartboy.domain.route.RouteInfo
import com.google.android.gms.maps.model.LatLng

@Composable
fun RouteInfoPanel(
    routeInfo: RouteInfo,
    traveledPath: List<LatLng>,
    remainingRoute: List<LatLng>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = routeInfo.duration,
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF1A73E8)
            )
            Text(
                text = routeInfo.distance,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (traveledPath.isNotEmpty() && remainingRoute.isNotEmpty()) {
                val totalPoints = traveledPath.size + remainingRoute.size
                val progress = (traveledPath.size.toFloat() / totalPoints) * 100
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Progress: ${progress.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF34A853)
                )
            }
        }
    }
}

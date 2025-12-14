package com.fcul.smartboy.domain.route

import com.google.android.gms.maps.model.LatLng

data class ActiveRoute(
    val id: String? = null,
    val userId: String,
    val userName: String? = null,
    val startTime: Long,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val checkpoints: List<LatLng> = emptyList(),
    val currentLocation: LatLng? = null,
    val isActive: Boolean = true,
    val totalDistance: Double = 0.0 // in kilometers
)


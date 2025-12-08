package com.fcul.smartboy.domain.route

import com.google.android.gms.maps.model.LatLng

data class MapRoute(
    val startCoordinates: LatLng,
    val endCoordinates: LatLng,
    val distanceInKm: Double,
    val estimatedTimeInMinutes: Int
)
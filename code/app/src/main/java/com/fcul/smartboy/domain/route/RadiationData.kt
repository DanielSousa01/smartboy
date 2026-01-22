package com.fcul.smartboy.domain.route

import com.google.android.gms.maps.model.LatLng

data class RadiationData(
    val id: String,
    val location: LatLng,
    val radiationLevelInMSv: Double,
    val radius: Double,
    val timestamp: Long
)
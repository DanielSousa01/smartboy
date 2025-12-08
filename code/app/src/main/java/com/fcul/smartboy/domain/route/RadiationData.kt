package com.fcul.smartboy.domain.route

import com.google.android.gms.maps.model.LatLng

data class RadiationData(
    val id: String? = null,
    val location: LatLng,
    val radiationLevelInSv: Double,
    val radius: Double,
    val timestamp: Long
)
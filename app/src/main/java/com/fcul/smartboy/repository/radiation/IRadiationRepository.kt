package com.fcul.smartboy.repository.radiation

import com.fcul.smartboy.domain.route.RadiationData
import com.google.android.gms.maps.model.LatLng

interface IRadiationRepository {
    suspend fun filter(
        userCoordinates: LatLng,
        radiusMeters: Double,
        minLevel: Double? = null,
        maxLevel: Double? = null
    ): List<RadiationData>
}
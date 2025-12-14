package com.fcul.smartboy.domain.route

import com.google.android.gms.maps.model.LatLng

data class Checkpoint(
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val name: String? = null,
    val location: LatLng,
    val userId: String,
    val id: String? = null,
)




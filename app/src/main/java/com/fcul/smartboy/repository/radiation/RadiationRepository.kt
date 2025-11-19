package com.fcul.smartboy.repository.radiation

import android.location.Location
import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.Path
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RadiationRepository(
    private val firestore: FirebaseFirestore
) : CRUD<RadiationData, String>, IRadiationRepository {

    private val col get() = firestore.collection(Path.RADIATION_DATA.path)

    override suspend fun create(document: RadiationData): String {
        val id = document.id ?: col.document().id
        val toSave = if (document.id == null) document.copy(id = id) else document
        col.document(id).set(toMap(toSave)).await()
        return id
    }

    override suspend fun read(id: String): RadiationData? {
        val snap = col.document(id).get().await()
        return if (snap.exists()) fromMap(snap.data) else null
    }

    override suspend fun update(id: String, data: Any): Boolean {
        when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                col.document(id).update(data as Map<String, Any?>).await()
            }

            is RadiationData -> col.document(id).set(toMap(data)).await()
            else -> throw IllegalArgumentException("Unsupported update type: ${data::class}")
        }
        return true
    }

    override suspend fun delete(id: String): Boolean {
        col.document(id).delete().await()
        return true
    }

    override suspend fun filter(
        userCoordinates: LatLng,
        radiusMeters: Double,
        minLevel: Double?,
        maxLevel: Double?
    ): List<RadiationData> {
        val baseQuery = when {
            minLevel != null && maxLevel != null ->
                col.whereGreaterThanOrEqualTo("radiationLevelInSv", minLevel)
                    .whereLessThanOrEqualTo("radiationLevelInSv", maxLevel)

            minLevel != null ->
                col.whereGreaterThanOrEqualTo("radiationLevelInSv", minLevel)

            maxLevel != null ->
                col.whereLessThanOrEqualTo("radiationLevelInSv", maxLevel)

            else -> col
        }

        val snaps = baseQuery.get().await()
        return snaps.documents.mapNotNull { doc ->
            val data = fromMap(doc.data) ?: return@mapNotNull null
            val distArr = FloatArray(1)
            Location.distanceBetween(
                userCoordinates.latitude, userCoordinates.longitude,
                data.location.latitude, data.location.longitude,
                distArr
            )
            if (distArr[0] <= radiusMeters) data else null
        }
    }

    private fun toMap(d: RadiationData): Map<String, Any?> = mapOf(
        "id" to d.id,
        "location" to mapOf(
            "lat" to d.location.latitude,
            "lng" to d.location.longitude
        ),
        "radiationLevelInSv" to d.radiationLevelInSv,
        "radius" to d.radius,
        "timestamp" to d.timestamp
    )

    @Suppress("UNCHECKED_CAST")
    private fun fromMap(data: Map<String, Any?>?): RadiationData? {
        if (data == null) return null
        val loc = data["location"] as? Map<String, Any?> ?: return null
        val lat = loc["lat"] as? Double ?: return null
        val lng = loc["lng"] as? Double ?: return null

        val radius = when (val radiusRaw = data["radius"]) {
            is Double -> radiusRaw
            is Long -> radiusRaw.toDouble()
            is Int -> radiusRaw.toDouble()
            else -> {
                0.0
            }
        }

        return RadiationData(
            id = data["id"] as? String,
            location = LatLng(lat, lng),
            radiationLevelInSv = (data["radiationLevelInSv"] as? Double) ?: 0.0,
            radius = radius,
            timestamp = (data["timestamp"] as? Long) ?: 0L
        )
    }
}
package com.fcul.smartboy.repository

import android.util.Log
import com.fcul.smartboy.domain.route.MapRoute
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.Path
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class MapRouteRepository(
    private val user: FirebaseUser,
    private val db: FirebaseDatabase,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : CRUD<MapRoute, Long> {

    private val routesCol get() = firestore.collection(Path.ROUTES.path)

    // ============= Completed Routes (CRUD Implementation) =============

    override suspend fun create(document: MapRoute): Long {
        val timestamp = System.currentTimeMillis()
        val routeData = mapOf(
            "userId" to user.uid,
            "startLat" to document.startCoordinates.latitude,
            "startLng" to document.startCoordinates.longitude,
            "endLat" to document.endCoordinates.latitude,
            "endLng" to document.endCoordinates.longitude,
            "distanceInKm" to document.distanceInKm,
            "estimatedTimeInMinutes" to document.estimatedTimeInMinutes,
            "timestamp" to timestamp,
            "checkpoints" to document.routeCheckpoints.map { latLngToMap(it) }
        )

        routesCol.add(routeData).await()
        Log.d(TAG, "Route saved to Firestore")
        return timestamp
    }

    override suspend fun read(id: Long): MapRoute? {
        val snapshot = routesCol
            .whereEqualTo("userId", user.uid)
            .whereEqualTo("timestamp", id)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let { doc ->
            val data = doc.data ?: return null
            MapRoute(
                startCoordinates = LatLng(
                    data["startLat"] as Double,
                    data["startLng"] as Double
                ),
                endCoordinates = LatLng(
                    data["endLat"] as Double,
                    data["endLng"] as Double
                ),
                distanceInKm = data["distanceInKm"] as Double,
                estimatedTimeInMinutes = (data["estimatedTimeInMinutes"] as Long).toInt(),
                routeCheckpoints = (data["checkpoints"] as List<Map<String, Any>>).mapNotNull {
                    latLngFromMap(it)
                }
            )
        }
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        val snapshot = routesCol
            .whereEqualTo("userId", user.uid)
            .whereEqualTo("timestamp", id)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return false

        when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                routesCol.document(doc.id).update(data as Map<String, Any>).await()
            }

            else -> return false
        }

        return true
    }

    override suspend fun delete(id: Long): Boolean {
        val snapshot = routesCol
            .whereEqualTo("userId", user.uid)
            .whereEqualTo("timestamp", id)
            .get()
            .await()

        snapshot.documents.forEach { doc ->
            routesCol.document(doc.id).delete().await()
        }

        return true
    }

    // ============= Serialization Helpers =============

    private fun latLngToMap(latLng: LatLng): Map<String, Double> = mapOf(
        "latitude" to latLng.latitude,
        "longitude" to latLng.longitude
    )

    private fun latLngFromMap(data: Map<String, Any>?): LatLng? {
        if (data == null) return null
        return try {
            LatLng(
                data["latitude"] as Double,
                data["longitude"] as Double
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "MapRouteRepository"
    }
}
package com.fcul.smartboy.repository

import android.util.Log
import com.fcul.smartboy.domain.route.ActiveRoute
import com.fcul.smartboy.domain.route.Checkpoint
import com.fcul.smartboy.domain.route.MapRoute
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.Path
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapRouteRepository(
    private val user: FirebaseUser,
    private val db: FirebaseDatabase,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : CRUD<MapRoute, Long> {

    private val TAG = "MapRouteRepository"

    private val checkpointsCol get() = firestore.collection(Path.CHECKPOINTS.path)
    private val routesCol get() = firestore.collection(Path.ROUTES.path)
    private val activeRoutesRef get() = db.getReference(Path.ACTIVE_ROUTES.path)

    // ============= Checkpoint Management (Firestore) =============

    suspend fun createCheckpoint(
        location: LatLng,
        name: String? = null,
        notes: String? = null
    ): String {
        val checkpoint = Checkpoint(
            userId = user.uid,
            location = location,
            name = name,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )

        val docRef = checkpointsCol.document()
        val checkpointWithId = checkpoint.copy(id = docRef.id)
        docRef.set(checkpointToMap(checkpointWithId)).await()

        Log.d(TAG, "Checkpoint created: ${docRef.id}")
        return docRef.id
    }

    suspend fun getCheckpoints(): List<LatLng> {
        val snapshot = checkpointsCol
            .whereEqualTo("userId", user.uid)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            checkpointFromMap(doc.data)?.location
        }
    }

    suspend fun getCheckpointsWithDetails(): List<Checkpoint> {
        val snapshot = checkpointsCol
            .whereEqualTo("userId", user.uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            checkpointFromMap(doc.data)
        }
    }

    suspend fun deleteCheckpoint(checkpointId: String): Boolean {
        checkpointsCol.document(checkpointId).delete().await()
        Log.d(TAG, "Checkpoint deleted: $checkpointId")
        return true
    }

    // ============= Active Route Management (Realtime Database) =============

    suspend fun startRoute(routeId: String, routeStartTime: Long) {
        val activeRoute = ActiveRoute(
            id = routeId,
            userId = user.uid,
            userName = user.displayName,
            startTime = routeStartTime,
            checkpoints = emptyList(),
            isActive = true
        )
        activeRoutesRef.child(routeId).setValue(activeRouteToMap(activeRoute)).await()
        Log.d(TAG, "Active route started: $routeId")
    }

    suspend fun startRouteWithCheckpoints(
        routeId: String,
        routeStartTime: Long,
        checkpoints: List<LatLng>
    ) {
        val activeRoute = ActiveRoute(
            id = routeId,
            userId = user.uid,
            userName = user.displayName,
            startTime = routeStartTime,
            checkpoints = checkpoints,
            isActive = true
        )
        Log.i(TAG, "Starting active route with checkpoints: $checkpoints")
        activeRoutesRef.child(routeId).setValue(activeRouteToMap(activeRoute)).await()
        Log.d(TAG, "Active route started with checkpoints: $routeId")
    }

    suspend fun addCheckpointToActiveRoute(
        routeId: String,
        checkpoints: List<LatLng>,
        location: LatLng
    ) {
        val updatedCheckpoints = checkpoints + location
        updateActiveRoute(routeId, updatedCheckpoints, location)
    }

    suspend fun updateActiveRoute(
        routeId: String,
        checkpoints: List<LatLng>,
        currentLocation: LatLng
    ) {
        val totalDistance = calculateTotalDistance(checkpoints)
        val updates = mapOf(
            "lastUpdateTime" to System.currentTimeMillis(),
            "checkpoints" to checkpoints.map { latLngToMap(it) },
            "currentLocation" to latLngToMap(currentLocation),
            "totalDistance" to totalDistance
        )
        activeRoutesRef.child(routeId).updateChildren(updates).await()
        Log.d(TAG, "Active route updated with ${checkpoints.size} checkpoints")
    }

    suspend fun endRoute(routeId: String, checkpoints: List<LatLng>, routeStartTime: Long) {
        if (routeId.isEmpty() || checkpoints.isEmpty()) {
            Log.w(TAG, "No active route to end")
            return
        }

        activeRoutesRef.child(routeId).child("isActive").setValue(false).await()

        if (checkpoints.size >= 2) {
            val mapRoute = MapRoute(
                startCoordinates = checkpoints.first(),
                endCoordinates = checkpoints.last(),
                distanceInKm = calculateTotalDistance(checkpoints),
                routeCheckpoints = checkpoints,
                estimatedTimeInMinutes = ((System.currentTimeMillis() - routeStartTime) / 60000).toInt()
            )

            create(mapRoute)
        }

        activeRoutesRef.child(routeId).removeValue().await()

        Log.d(TAG, "Route ended and saved: $routeId")
    }

    fun observeNearbyActiveRoutes(
        centerLocation: LatLng,
        radiusKm: Double = 10.0
    ): Flow<List<ActiveRoute>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val routes = snapshot.children.mapNotNull { childSnapshot ->
                    try {
                        activeRouteFromMap(childSnapshot.value as? Map<*, *>)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing active route", e)
                        null
                    }
                }.filter { route ->
                    route.isActive &&
                            route.userId != user.uid &&
                            route.currentLocation?.let {
                                calculateDistance(centerLocation, it) <= radiusKm
                            } ?: false
                }

                trySend(routes)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to observe active routes", error.toException())
                close(error.toException())
            }
        }

        activeRoutesRef.orderByChild("isActive").equalTo(true).addValueEventListener(listener)

        awaitClose {
            activeRoutesRef.removeEventListener(listener)
        }
    }

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

    suspend fun getAllRoutes(): List<MapRoute> {
        val snapshot = routesCol
            .whereEqualTo("userId", user.uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
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

    // ============= Helper Functions =============

    private fun calculateTotalDistance(points: List<LatLng>): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += calculateDistance(points[i], points[i + 1])
        }
        return totalDistance
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLon = Math.toRadians(point2.longitude - point1.longitude)

        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    // ============= Serialization Helpers =============

    private fun checkpointToMap(checkpoint: Checkpoint): Map<String, Any?> = mapOf(
        "id" to checkpoint.id,
        "userId" to checkpoint.userId,
        "latitude" to checkpoint.location.latitude,
        "longitude" to checkpoint.location.longitude,
        "name" to checkpoint.name,
        "timestamp" to checkpoint.timestamp,
        "notes" to checkpoint.notes
    )

    private fun checkpointFromMap(data: Map<String, Any?>?): Checkpoint? {
        if (data == null) return null
        return try {
            Checkpoint(
                id = data["id"] as? String,
                userId = data["userId"] as? String ?: return null,
                location = LatLng(
                    data["latitude"] as Double,
                    data["longitude"] as Double
                ),
                name = data["name"] as? String,
                timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
                notes = data["notes"] as? String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing checkpoint", e)
            null
        }
    }

    private fun activeRouteToMap(route: ActiveRoute): Map<String, Any?> = mapOf(
        "id" to route.id,
        "userId" to route.userId,
        "userName" to route.userName,
        "startTime" to route.startTime,
        "lastUpdateTime" to route.lastUpdateTime,
        "checkpoints" to route.checkpoints.map { latLngToMap(it) },
        "currentLocation" to route.currentLocation?.let { latLngToMap(it) },
        "isActive" to route.isActive,
        "totalDistance" to route.totalDistance
    )

    private fun activeRouteFromMap(data: Map<*, *>?): ActiveRoute? {
        if (data == null) return null
        return try {
            @Suppress("UNCHECKED_CAST")
            val checkpointsList = (data["checkpoints"] as? List<Map<String, Any>>)?.mapNotNull {
                latLngFromMap(it)
            } ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val currentLoc = (data["currentLocation"] as? Map<String, Any>)?.let {
                latLngFromMap(it)
            }

            ActiveRoute(
                id = data["id"] as? String,
                userId = data["userId"] as? String ?: return null,
                userName = data["userName"] as? String,
                startTime = (data["startTime"] as? Long) ?: 0L,
                lastUpdateTime = (data["lastUpdateTime"] as? Long) ?: 0L,
                checkpoints = checkpointsList,
                currentLocation = currentLoc,
                isActive = (data["isActive"] as? Boolean) ?: false,
                totalDistance = (data["totalDistance"] as? Double) ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing active route", e)
            null
        }
    }

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
}
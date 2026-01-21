package com.fcul.smartboy.repository.route

import android.location.Location
import android.util.Log
import com.fcul.smartboy.domain.route.ActiveRoute
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveRouteRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val activeRoutesRef = database.getReference("active_routes")

    companion object {
        private const val TAG = "ActiveRouteRepository"

        private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
            val results = FloatArray(1)
            Location.distanceBetween(
                point1.latitude,
                point1.longitude,
                point2.latitude,
                point2.longitude,
                results
            )
            return results[0].toDouble()
        }
    }

    suspend fun saveActiveRoute(route: ActiveRoute): Result<String> {
        return try {
            val routeData = hashMapOf(
                "userId" to route.userId,
                "userName" to (route.userName ?: "Unknown"),
                "startTime" to route.startTime,
                "lastUpdateTime" to System.currentTimeMillis(),
                "checkpoints" to route.checkpoints.map { checkpoint ->
                    hashMapOf(
                        "latitude" to checkpoint.latitude,
                        "longitude" to checkpoint.longitude
                    )
                },
                "currentLocation" to route.currentLocation?.let {
                    hashMapOf(
                        "latitude" to it.latitude,
                        "longitude" to it.longitude
                    )
                },
                "isActive" to route.isActive,
                "totalDistance" to route.totalDistance
            )

            val userId = route.userId
            activeRoutesRef.child(userId).setValue(routeData).await()
            Log.d(TAG, "Active route saved for user: ${route.userId}")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save active route", e)
            Result.failure(e)
        }
    }

    suspend fun updateCurrentLocation(userId: String, location: LatLng): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "currentLocation" to hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude
                ),
                "lastUpdateTime" to System.currentTimeMillis()
            )
            activeRoutesRef.child(userId).updateChildren(updates).await()
            Log.d(TAG, "Updated current location for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update current location", e)
            Result.failure(e)
        }
    }

    suspend fun endActiveRoute(userId: String): Result<Unit> {
        return try {
            activeRoutesRef.child(userId).removeValue().await()
            Log.d(TAG, "Ended active route for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end active route", e)
            Result.failure(e)
        }
    }

    fun observeActiveRoutes(
        excludeUserId: String,
        userLocation: LatLng? = null,
        radiusKm: Double = 10.0
    ): Flow<List<ActiveRoute>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val routes = snapshot.children.mapNotNull { childSnapshot ->
                    try {
                        val userId = childSnapshot.child("userId").getValue(String::class.java)
                            ?: return@mapNotNull null

                        // Exclude the current user's route
                        if (userId == excludeUserId) return@mapNotNull null

                        // Parse checkpoints
                        val checkpointsSnapshot = childSnapshot.child("checkpoints")
                        val checkpoints = checkpointsSnapshot.children.mapNotNull { checkpoint ->
                            val lat = checkpoint.child("latitude").getValue(Double::class.java)
                            val lng = checkpoint.child("longitude").getValue(Double::class.java)
                            if (lat != null && lng != null) {
                                LatLng(lat, lng)
                            } else null
                        }

                        // Parse current location
                        val currentLocationSnapshot = childSnapshot.child("currentLocation")
                        val currentLocation = if (currentLocationSnapshot.exists()) {
                            val lat = currentLocationSnapshot.child("latitude")
                                .getValue(Double::class.java)
                            val lng = currentLocationSnapshot.child("longitude")
                                .getValue(Double::class.java)
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        } else null

                        ActiveRoute(
                            id = childSnapshot.key,
                            userId = userId,
                            userName = childSnapshot.child("userName").getValue(String::class.java),
                            startTime = childSnapshot.child("startTime").getValue(Long::class.java)
                                ?: 0L,
                            lastUpdateTime = childSnapshot.child("lastUpdateTime")
                                .getValue(Long::class.java)
                                ?: 0L,
                            checkpoints = checkpoints,
                            currentLocation = currentLocation,
                            isActive = childSnapshot.child("isActive").getValue(Boolean::class.java)
                                ?: true,
                            totalDistance = childSnapshot.child("totalDistance")
                                .getValue(Double::class.java)
                                ?: 0.0
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing active route", e)
                        null
                    }
                }

                // Filter by radius if user location is provided
                val filteredRoutes = if (userLocation != null) {
                    routes.filter { route ->
                        // Check if any checkpoint or current location is within radius
                        val withinRadius = route.checkpoints.any { checkpoint ->
                            calculateDistance(userLocation, checkpoint) <= radiusKm * 1000
                        } || route.currentLocation?.let { loc ->
                            calculateDistance(userLocation, loc) <= radiusKm * 1000
                        } ?: false

                        withinRadius
                    }
                } else {
                    routes
                }

                trySend(filteredRoutes)
                Log.d(TAG, "Observed ${filteredRoutes.size} active routes within ${radiusKm}km")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing active routes: ${error.message}")
            }
        }

        activeRoutesRef.addValueEventListener(listener)

        awaitClose {
            activeRoutesRef.removeEventListener(listener)
        }
    }

    suspend fun getActiveRoutes(excludeUserId: String): Result<List<ActiveRoute>> {
        return try {
            val snapshot = activeRoutesRef.get().await()

            val routes = snapshot.children.mapNotNull { childSnapshot ->
                try {
                    val userId = childSnapshot.child("userId").getValue(String::class.java)
                        ?: return@mapNotNull null

                    // Exclude the current user's route
                    if (userId == excludeUserId) return@mapNotNull null

                    // Parse checkpoints
                    val checkpointsSnapshot = childSnapshot.child("checkpoints")
                    val checkpoints = checkpointsSnapshot.children.mapNotNull { checkpoint ->
                        val lat = checkpoint.child("latitude").getValue(Double::class.java)
                        val lng = checkpoint.child("longitude").getValue(Double::class.java)
                        if (lat != null && lng != null) {
                            LatLng(lat, lng)
                        } else null
                    }

                    // Parse current location
                    val currentLocationSnapshot = childSnapshot.child("currentLocation")
                    val currentLocation = if (currentLocationSnapshot.exists()) {
                        val lat =
                            currentLocationSnapshot.child("latitude").getValue(Double::class.java)
                        val lng =
                            currentLocationSnapshot.child("longitude").getValue(Double::class.java)
                        if (lat != null && lng != null) LatLng(lat, lng) else null
                    } else null

                    ActiveRoute(
                        id = childSnapshot.key,
                        userId = userId,
                        userName = childSnapshot.child("userName").getValue(String::class.java),
                        startTime = childSnapshot.child("startTime").getValue(Long::class.java)
                            ?: 0L,
                        lastUpdateTime = childSnapshot.child("lastUpdateTime")
                            .getValue(Long::class.java) ?: 0L,
                        checkpoints = checkpoints,
                        currentLocation = currentLocation,
                        isActive = childSnapshot.child("isActive").getValue(Boolean::class.java)
                            ?: true,
                        totalDistance = childSnapshot.child("totalDistance")
                            .getValue(Double::class.java) ?: 0.0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing active route", e)
                    null
                }
            }

            Log.d(TAG, "Fetched ${routes.size} active routes")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch active routes", e)
            Result.failure(e)
        }
    }
}

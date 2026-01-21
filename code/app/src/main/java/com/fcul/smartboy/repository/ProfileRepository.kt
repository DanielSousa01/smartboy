package com.fcul.smartboy.repository

import android.util.Log
import com.fcul.smartboy.domain.user.MeasurementUnit
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.domain.user.UserPreferences
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val database: FirebaseDatabase
) : CRUD<Profile, String> {

    private val profilesRef get() = database.getReference(Path.USERS.path)

    companion object {
        private const val TAG = "ProfileRepository"
    }

    override suspend fun create(document: Profile): String {
        profilesRef.child(document.userId).setValue(toMap(document)).await()
        Log.d(TAG, "Profile created for user: ${document.userId}")
        return document.userId
    }

    override suspend fun read(id: String): Profile? {
        val snapshot = profilesRef.child(id).get().await()
        return if (snapshot.exists()) {
            fromSnapshot(snapshot)
        } else null
    }

    override suspend fun update(id: String, data: Any): Boolean {
        when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                profilesRef.child(id).updateChildren(data as Map<String, Any>).await()
            }

            is Profile -> profilesRef.child(id).setValue(toMap(data)).await()
            else -> throw IllegalArgumentException("Unsupported update type: ${data::class}")
        }
        Log.d(TAG, "Profile updated for user: $id")
        return true
    }

    override suspend fun delete(id: String): Boolean {
        profilesRef.child(id).removeValue().await()
        Log.d(TAG, "Profile deleted for user: $id")
        return true
    }

    suspend fun updateSteps(userId: String, steps: Long): Boolean {
        profilesRef.child(userId).child("steps").setValue(steps).await()
        Log.d(TAG, "Updated steps for user $userId: $steps")
        return true
    }

    suspend fun incrementSteps(userId: String, increment: Long): Boolean {
        val profile = read(userId)
        if (profile != null) {
            val newSteps = profile.steps + increment
            updateSteps(userId, newSteps)
            return true
        }
        return false
    }

    suspend fun updateCaps(userId: String, caps: Int): Boolean {
        profilesRef.child(userId).child("caps").setValue(caps).await()
        Log.d(TAG, "Updated caps for user $userId: $caps")
        return true
    }

    suspend fun addCaps(userId: String, amount: Int): Boolean {
        val profile = read(userId)
        if (profile != null) {
            val newCaps = profile.caps + amount
            updateCaps(userId, newCaps)
            return true
        }
        return false
    }

    suspend fun deductCaps(userId: String, amount: Int): Boolean {
        val profile = read(userId)
        if (profile != null && profile.caps >= amount) {
            val newCaps = profile.caps - amount
            updateCaps(userId, newCaps)
            return true
        }
        return false
    }

    suspend fun transferCaps(fromUserId: String, toUserId: String, amount: Int): Boolean {
        val fromProfile = read(fromUserId)
        if (fromProfile != null && fromProfile.caps >= amount) {
            if (deductCaps(fromUserId, amount)) {
                addCaps(toUserId, amount)
                return true
            }
        }
        return false
    }

    suspend fun updateRadiation(userId: String, radiation: Double): Boolean {
        profilesRef.child(userId).child("radiation").setValue(radiation).await()
        Log.d(TAG, "Updated radiation for user $userId: $radiation")
        return true
    }

    suspend fun addRadiation(userId: String, amount: Double): Boolean {
        val profile = read(userId)
        if (profile != null) {
            val newRadiation = profile.radiation + amount
            updateRadiation(userId, newRadiation)
            return true
        }
        return false
    }

    suspend fun deductSteps(userId: String, amount: Long): Boolean {
        val profile = read(userId)
        if (profile != null && profile.steps >= amount) {
            val newSteps = profile.steps - amount
            updateSteps(userId, newSteps)
            return true
        }
        return false
    }

    suspend fun updatePreferences(userId: String, preferences: UserPreferences): Boolean {
        val preferencesMap = mapOf(
            "measurementUnit" to preferences.measurementUnit.name
        )
        profilesRef.child(userId).child("preferences").setValue(preferencesMap).await()
        Log.d(TAG, "Updated preferences for user $userId: $preferences")
        return true
    }

    suspend fun updateMeasurementUnit(userId: String, unit: MeasurementUnit): Boolean {
        profilesRef.child(userId).child("preferences").child("measurementUnit").setValue(unit.name).await()
        Log.d(TAG, "Updated measurement unit for user $userId: $unit")
        return true
    }

    suspend fun useRadAway(userId: String, radiationReduction: Double): Boolean {
        val profile = read(userId)
        if (profile != null && profile.radiation > 0) {
            val newRadiation = maxOf(0.0, profile.radiation - radiationReduction)
            updateRadiation(userId, newRadiation)
            Log.d(TAG, "RadAway used: reduced radiation by $radiationReduction Sv (${profile.radiation} -> $newRadiation)")
            return true
        }
        return false
    }

    suspend fun useRadX(userId: String, resistanceBoost: Double, durationMillis: Long): Boolean {
        val profile = read(userId)
        if (profile != null) {
            val currentTime = System.currentTimeMillis()
            val expiryTime = currentTime + durationMillis

            // Cap resistance at 0.95 (95% max resistance)
            val newResistance = minOf(0.95, profile.radiationResistance + resistanceBoost)

            // Update both resistance and expiry time
            val updates = mapOf(
                "radiationResistance" to newResistance,
                "radXExpiryTime" to expiryTime
            )
            profilesRef.child(userId).updateChildren(updates).await()

            Log.d(TAG, "Rad-X used: increased resistance by ${resistanceBoost * 100}% (${profile.radiationResistance} -> $newResistance), expires at $expiryTime")
            return true
        }
        return false
    }

    suspend fun updateRadiationResistance(userId: String, resistance: Double): Boolean {
        val cappedResistance = minOf(0.95, maxOf(0.0, resistance))
        profilesRef.child(userId).child("radiationResistance").setValue(cappedResistance).await()
        Log.d(TAG, "Updated radiation resistance for user $userId: $cappedResistance")
        return true
    }

    suspend fun decreaseRadiationResistance(userId: String, amount: Double): Boolean {
        val profile = read(userId)
        if (profile != null) {
            val newResistance = maxOf(0.0, profile.radiationResistance - amount)
            updateRadiationResistance(userId, newResistance)
            return true
        }
        return false
    }

    suspend fun decayRadXEffect(userId: String): Boolean {
        val profile = read(userId) ?: return false
        val currentTime = System.currentTimeMillis()

        // If no Rad-X effect active or already expired, reset to 0
        if (profile.radXExpiryTime == 0L || currentTime >= profile.radXExpiryTime) {
            if (profile.radiationResistance > 0 || profile.radXExpiryTime > 0) {
                val updates = mapOf(
                    "radiationResistance" to 0.0,
                    "radXExpiryTime" to 0L
                )
                profilesRef.child(userId).updateChildren(updates).await()
                Log.d(TAG, "Rad-X effect expired, resistance reset to 0")
                return true
            }
            return false
        }

        // Rad-X effect is active, log current status
        val timeRemaining = profile.radXExpiryTime - currentTime
        Log.d(TAG, "Rad-X active: resistance = ${profile.radiationResistance}, time remaining = ${timeRemaining / 1000}s")

        return true
    }

    fun observeProfile(userId: String): Flow<Profile?> = callbackFlow {
        val profileRef = profilesRef.child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = if (snapshot.exists()) {
                    fromSnapshot(snapshot)
                } else {
                    null
                }
                trySend(profile)
                Log.d(TAG, "Profile observed for user $userId: $profile")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing profile: ${error.message}")
                close(error.toException())
            }
        }

        profileRef.addValueEventListener(listener)

        awaitClose {
            profileRef.removeEventListener(listener)
        }
    }

    private fun toMap(d: Profile): Map<String, Any?> = mapOf(
        "userId" to d.userId,
        "caps" to d.caps,
        "steps" to d.steps,
        "distance" to d.distance,
        "radiation" to d.radiation,
        "radiationResistance" to d.radiationResistance,
        "radXExpiryTime" to d.radXExpiryTime,
        "preferences" to mapOf(
            "measurementUnit" to d.preferences.measurementUnit.name
        )
    )

    private fun fromSnapshot(snapshot: DataSnapshot): Profile? {
        return try {
            val userId = snapshot.child("userId").getValue(String::class.java) ?: return null
            val caps = snapshot.child("caps").getValue(Long::class.java)?.toInt() ?: 0
            val steps = snapshot.child("steps").getValue(Long::class.java) ?: 0L
            val distance = snapshot.child("distance").getValue(Double::class.java) ?: 0.0
            val radiation = snapshot.child("radiation").getValue(Double::class.java) ?: 0.0
            val radiationResistance = snapshot.child("radiationResistance").getValue(Double::class.java) ?: 0.0
            val radXExpiryTime = snapshot.child("radXExpiryTime").getValue(Long::class.java) ?: 0L

            // Parse preferences
            val preferencesSnapshot = snapshot.child("preferences")
            val measurementUnitStr = preferencesSnapshot.child("measurementUnit").getValue(String::class.java)
            val measurementUnit = try {
                measurementUnitStr?.let { MeasurementUnit.valueOf(it) } ?: MeasurementUnit.METRIC
            } catch (_: IllegalArgumentException) {
                MeasurementUnit.METRIC
            }
            val preferences = UserPreferences(measurementUnit = measurementUnit)

            Profile(
                userId = userId,
                caps = caps,
                steps = steps,
                distance = distance,
                radiation = radiation,
                radiationResistance = radiationResistance,
                radXExpiryTime = radXExpiryTime,
                preferences = preferences
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing profile from snapshot", e)
            null
        }
    }
}
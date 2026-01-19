package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val firestore: FirebaseFirestore
) : CRUD<Profile, String> {

    private val col get() = firestore.collection(Path.USERS.path)

    override suspend fun create(document: Profile): String {
        col.document(document.userId).set(document).await()
        return document.userId
    }

    override suspend fun read(id: String): Profile? {
        val snap = col.document(id).get().await()
        return if (snap.exists()) fromMap(snap.data) else null
    }

    override suspend fun update(id: String, data: Any): Boolean {
        when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                col.document(id).update(data as Map<String, Any?>).await()
            }

            is Profile -> col.document(id).set(data).await()
            else -> throw IllegalArgumentException("Unsupported update type: ${data::class}")
        }
        return true
    }

    override suspend fun delete(id: String): Boolean {
        col.document(id).delete().await()
        return true
    }

    suspend fun updateSteps(userId: String, steps: Long): Boolean {
        col.document(userId).update("steps", steps).await()
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
        col.document(userId).update("caps", caps).await()
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
        col.document(userId).update("radiation", radiation).await()
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

    fun observeProfile(userId: String): Flow<Profile?> = callbackFlow {
        val docRef = col.document(userId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val profile = if (snapshot?.exists() == true) {
                fromMap(snapshot.data)
            } else {
                null
            }
            trySend(profile)
        }

        awaitClose { listener.remove() }
    }

    private fun toMap(d: Profile): Map<String, Any?> = mapOf(
        "userId" to d.userId,
        "caps" to d.caps,
        "steps" to d.steps,
        "distance" to d.distance,
        "radiation" to d.radiation
    )

    @Suppress("UNCHECKED_CAST")
    private fun fromMap(data: Map<String, Any?>?): Profile? {
        if (data == null) return null
        val userId = data["userId"] as? String ?: return null
        val caps = (data["caps"] as? Long)?.toInt() ?: 0
        val steps = (data["steps"] as? Long) ?: 0L
        val distance = (data["distance"] as? Double) ?: 0.0
        val radiation = (data["radiation"] as? Double) ?: 0.0

        return Profile(
            userId = userId,
            caps = caps,
            steps = steps,
            distance = distance,
            radiation = radiation
        )
    }
}
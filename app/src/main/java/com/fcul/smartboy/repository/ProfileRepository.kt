package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.route.RadiationData
import com.fcul.smartboy.domain.user.Profile
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.Path
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
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

    private fun toMap(d: Profile): Map<String, Any?> = mapOf(
        "userId" to d.userId,
        "steps" to d.steps,
        "distance" to d.distance,
        "radiation" to d.radiation
    )

    @Suppress("UNCHECKED_CAST")
    private fun fromMap(data: Map<String, Any?>?): Profile? {
        if (data == null) return null
        val userId = data["userId"] as? String ?: return null
        val steps = (data["steps"] as? Long) ?: 0L
        val distance = (data["distance"] as? Double) ?: 0.0
        val radiation = (data["radiation"] as? Double) ?: 0.0

        return Profile(
            userId = userId,
            steps = steps,
            distance = distance,
            radiation = radiation
        )
    }
}
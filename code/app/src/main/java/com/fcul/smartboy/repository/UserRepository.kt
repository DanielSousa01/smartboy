package com.fcul.smartboy.repository

import android.util.Log
import com.fcul.smartboy.domain.user.User
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore
) : CRUD<User, String> {

    private val usersCollection get() = firestore.collection(Path.USERS.path)

    override suspend fun create(document: User): String {
        usersCollection.document(document.userId).set(toMap(document)).await()
        Log.d(TAG, "User created in Firestore: ${document.userId}")
        return document.userId
    }

    override suspend fun read(id: String): User? {
        return try {
            val snapshot = usersCollection.document(id).get().await()
            if (snapshot.exists()) {
                fromMap(id, snapshot.data)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading user: $id", e)
            null
        }
    }

    override suspend fun update(id: String, data: Any): Boolean {
        return try {
            when (data) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    usersCollection.document(id).update(data as Map<String, Any>).await()
                }
                is User -> usersCollection.document(id).set(toMap(data)).await()
                else -> throw IllegalArgumentException("Unsupported update type: ${data::class}")
            }
            Log.d(TAG, "User updated: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: $id", e)
            false
        }
    }

    override suspend fun delete(id: String): Boolean {
        return try {
            usersCollection.document(id).delete().await()
            Log.d(TAG, "User deleted: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user: $id", e)
            false
        }
    }

    private fun toMap(user: User): Map<String, Any?> = mapOf(
        "userId" to user.userId,
        "username" to user.username,
        "email" to user.email,
        "photoUrl" to user.photoUrl
    )

    private fun fromMap(userId: String, data: Map<String, Any>?): User? {
        if (data == null) return null
        return try {
            User(
                userId = userId,
                username = data["username"] as? String ?: "Unknown",
                email = data["email"] as? String ?: "",
                photoUrl = data["photoUrl"] as? String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user from map", e)
            null
        }
    }

    companion object {
        private const val TAG = "UserRepository"
    }
}

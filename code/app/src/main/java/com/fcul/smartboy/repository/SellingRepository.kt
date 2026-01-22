package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.inventory.SellingItem
import com.fcul.smartboy.domain.inventory.SellingItemEntity
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.CRUD.Companion.awaitTask
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class SellingRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CRUD<SellingItem, Long> {

    private val user: FirebaseUser?
        get() = auth.currentUser

    private val col
        get() = firestore.collection(Path.USERS.path)

    fun observeSellingItems(): Flow<List<SellingItem>> = callbackFlow {
        val user = user ?: run {
            close()
            return@callbackFlow
        }

        val sellingRef = col.document(user.uid)
            .collection(Path.SELLING.path)

        val listener = sellingRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(SellingItemEntity::class.java)?.toSellingItem()
            } ?: emptyList()

            trySend(items)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun create(document: SellingItem): Long {
        val user = user ?: return -1

        val docRef = col.document(user.uid)
            .collection(Path.SELLING.path)
            .document(document.id.toString())

        val entity = document.toEntity()

        docRef.set(entity).awaitTask()
        return document.id
    }

    override suspend fun read(id: Long): SellingItem? {
        val user = user ?: return null

        val docRef = col.document(user.uid)
            .collection(Path.SELLING.path)

        return docRef.document(id.toString()).get().awaitTask()
            .toObject(SellingItemEntity::class.java)?.toSellingItem()
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        val user = user ?: return false

        val item = data as SellingItem

        val docRef = col.document(user.uid)
            .collection(Path.SELLING.path)
            .document(id.toString())

        val entity = item.toEntity()

        docRef.set(entity).awaitTask()
        return true

    }

    override suspend fun delete(id: Long): Boolean {
        val user = user ?: return false

        val docRef = col.document(user.uid)
            .collection(Path.SELLING.path)
            .document(id.toString())

        docRef.delete().awaitTask()
        return true
    }

    /**
     * Read a selling item from a specific user's inventory
     */
    suspend fun readFromUser(userId: String, itemId: Long): SellingItem? {
        return try {
            val docRef = col.document(userId)
                .collection(Path.SELLING.path)
                .document(itemId.toString())

            docRef.get().awaitTask()
                .toObject(SellingItemEntity::class.java)?.toSellingItem()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update item quantity for a specific user
     */
    suspend fun updateForUser(userId: String, itemId: Long, item: SellingItem): Boolean {
        return try {
            val docRef = col.document(userId)
                .collection(Path.SELLING.path)
                .document(itemId.toString())

            val entity = item.toEntity()
            docRef.set(entity).awaitTask()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete item from a specific user's selling inventory
     */
    suspend fun deleteFromUser(userId: String, itemId: Long): Boolean {
        return try {
            val docRef = col.document(userId)
                .collection(Path.SELLING.path)
                .document(itemId.toString())

            docRef.delete().awaitTask()
            true
        } catch (e: Exception) {
            false
        }
    }
}
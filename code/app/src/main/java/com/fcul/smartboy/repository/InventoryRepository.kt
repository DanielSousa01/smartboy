package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.domain.inventory.ItemEntity
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.CRUD.Companion.awaitTask
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class InventoryRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CRUD<Item, Long> {
    private val user: FirebaseUser?
        get() = auth.currentUser

    private val col get() = firestore.collection(Path.USERS.path)

    suspend fun getInventory(): List<Item> {
        val user = user ?: return emptyList()

        val inventorySnapshot = col.document(user.uid)
            .collection(Path.INVENTORY.path).get().awaitTask()

        return inventorySnapshot.documents.mapNotNull { doc ->
            doc.toObject(ItemEntity::class.java)?.toItem()
        }
    }

    override suspend fun create(document: Item): Long {
        val user = user ?: return -1
        val id = document.id

        val docRef = col.document(user.uid)
            .collection(Path.INVENTORY.path)
            .document(id.toString())


        val entity = document.toEntity()

        docRef.set(entity).awaitTask()

        return id
    }

    override suspend fun read(id: Long): Item? {
        val user = user ?: return null

        val docRef = col.document(user.uid)
            .collection(Path.INVENTORY.path)

        return docRef.document(id.toString()).get().awaitTask()
            .toObject(ItemEntity::class.java)?.toItem()
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        val user = user ?: return false
        val item = data as Item

        val docRef = col.document(user.uid)
            .collection(Path.INVENTORY.path)
            .document(id.toString())

        val entity = item.toEntity()

        docRef.set(entity).awaitTask()
        return true
    }

    override suspend fun delete(id: Long): Boolean {
        val user = user ?: return false

        val docRef = col.document(user.uid)
            .collection(Path.INVENTORY.path)
            .document(id.toString())

        docRef.delete().awaitTask()
        return true
    }
}
package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.inventory.Item
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.CRUD.Companion.awaitTask
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class InventoryRepository(
    private val user: FirebaseUser,
    private val db: FirebaseDatabase,
    private val storage: FirebaseStorage
) : CRUD<Item, Long> {
    override suspend fun create(document: Item): Long {
        val id = document.id
        val ref = db.getReference(Path.USERS.path).child(user.uid).child(Path.INVENTORY.path)
            .child(id.toString())
        // val uploadTask = storage.reference.child("${Path.INVENTORY.path}/${user.uid}/${document.id}").putFile(document.imageUri)
        // uploadTask.awaitTask()

        ref.setValue(document).awaitTask()
        return id
    }

    override suspend fun read(id: Long): Item? {
        val ref = db.getReference(Path.USERS.path).child(user.uid).child(Path.INVENTORY.path)
            .child(id.toString())
        val snapshot: DataSnapshot = ref.get().awaitTask()
        return snapshot.getValue(Item::class.java)
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        val ref = db.getReference(Path.USERS.path).child(user.uid).child(Path.INVENTORY.path)
            .child(id.toString())

        when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val map = data.entries.associate { it.key.toString() to it.value }
                ref.updateChildren(map).awaitTask()
            }

            is Item -> {
                ref.setValue(data).awaitTask()
            }

            else -> throw IllegalArgumentException("Unsupported data type for update: ${data::class}")
        }
        return true
    }

    override suspend fun delete(id: Long): Boolean {
        val ref = db.getReference(Path.USERS.path).child(user.uid).child(Path.INVENTORY.path)
            .child(id.toString())

        ref.removeValue().awaitTask()
        return true
    }
}
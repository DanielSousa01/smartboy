package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.transaction.Transaction
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.CRUD.Companion.awaitTask
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class TransactionRepository(
    private val user: FirebaseUser,
    private val db: FirebaseDatabase,
    private val storage: FirebaseStorage
) : CRUD<Transaction, Long> {
    override suspend fun create(document: Transaction): Long {
        val id = document.id
        val ref = db.getReference(Path.TRANSACTIONS.path).child(user.uid).child(id.toString())

        ref.setValue(document).awaitTask()
        return id
    }

    override suspend fun read(id: Long): Transaction? {
        val ref = db.getReference(Path.TRANSACTIONS.path).child(user.uid).child(id.toString())
        val snapshot = ref.get().awaitTask()

        return snapshot.getValue(Transaction::class.java)
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        val ref = db.getReference(Path.TRANSACTIONS.path).child(user.uid).child(id.toString())

        when (data) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val map = data.entries.associate { it.key.toString() to it.value }
                ref.updateChildren(map).awaitTask()
            }

            is Transaction -> {
                ref.setValue(data).awaitTask()
            }

            else -> throw IllegalArgumentException("Unsupported data type for update: ${data::class}")
        }
        return true
    }

    override suspend fun delete(id: Long): Boolean {
        val ref = db.getReference(Path.TRANSACTIONS.path).child(user.uid).child(id.toString())

        ref.removeValue().awaitTask()
        return true
    }
}
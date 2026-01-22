package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.transaction.Transaction
import com.fcul.smartboy.domain.transaction.TransactionEntity
import com.fcul.smartboy.repository.base.CRUD
import com.fcul.smartboy.repository.base.CRUD.Companion.awaitTask
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CRUD<Transaction, Long> {

    private val user: FirebaseUser?
        get() = auth.currentUser

    private val col
        get() = firestore.collection(Path.USERS.path)

    suspend fun getTransactions(): List<Transaction> {
        val user = user ?: return emptyList()

        val transactionsSnapshot = col.document(user.uid)
            .collection(Path.TRANSACTIONS.path).get().awaitTask()

        return transactionsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(TransactionEntity::class.java)?.toTransaction()
        }
    }

    override suspend fun create(document: Transaction): Long {
        val user = user ?: return -1
        val destinationUserId = document.userDestination.userId


        val transactionId = document.id

        val sourceDocRef = col.document(user.uid)
            .collection(Path.TRANSACTIONS.path)

        val sourceTransactionEntity = document.copy(amount = -document.amount).toEntity()
        sourceDocRef.document(transactionId.toString())
            .set(sourceTransactionEntity).awaitTask()

        document.toEntity()
        val destinationDocRef = col.document(destinationUserId)
            .collection(Path.TRANSACTIONS.path)
        destinationDocRef.document(transactionId.toString())
            .set(document.toEntity()).awaitTask()




        return transactionId
    }

    override suspend fun read(id: Long): Transaction {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: Long): Boolean {
        TODO("Not yet implemented")
    }


}
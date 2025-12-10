package com.fcul.smartboy.repository.base

import com.fcul.smartboy.domain.inventory.Item
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface CRUD<T, Y> {
    suspend fun create(document: T): Y
    suspend fun read(id: Y): T?
    suspend fun update(id: Y, data: Any): Boolean
    suspend fun delete(id: Y): Boolean

    companion object {
        suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result ->
                cont.resume(result)
            }
            addOnFailureListener { ex ->
                cont.resumeWithException(ex)
            }
            addOnCanceledListener {
                cont.cancel()
            }
        }
    }
}
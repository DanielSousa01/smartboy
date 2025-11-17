package com.fcul.smartboy.repository.base

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface CRUD<T> {
    suspend fun create(document: T): Long
    suspend fun read(id: Long): T?
    suspend fun update(id: Long, data: Any): Boolean
    suspend fun delete(id: Long): Boolean

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
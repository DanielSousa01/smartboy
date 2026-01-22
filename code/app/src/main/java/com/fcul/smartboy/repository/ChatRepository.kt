package com.fcul.smartboy.repository

import android.net.Uri
import com.fcul.smartboy.domain.chat.Conversation
import com.fcul.smartboy.domain.chat.Message
import com.fcul.smartboy.repository.base.Path
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val messagesRef = database.getReference(Path.MESSAGES.path)

    private fun getConversationId(userId1: String, userId2: String): String {
        val ids = listOf(userId1, userId2).sorted()
        return "${ids[0]}_${ids[1]}"
    }

    suspend fun sendMessage(
        recipientId: String,
        recipientName: String,
        text: String,
        imageUrl: String? = null
    ): String {
        val currentUser = auth.currentUser ?: throw IllegalStateException("User must be logged in")
        val messageId = UUID.randomUUID().toString()
        val conversationId = getConversationId(currentUser.uid, recipientId)

        val message = Message(
            id = messageId,
            senderId = currentUser.uid,
            senderName = currentUser.displayName ?: "Anonymous",
            recipientId = recipientId,
            recipientName = recipientName,
            text = text,
            timestamp = System.currentTimeMillis(),
            imageUrl = imageUrl
        )

        messagesRef.child(conversationId).child(messageId).setValue(toMap(message)).await()
        return messageId
    }

    fun observeConversation(otherUserId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }

        val conversationId = getConversationId(currentUserId, otherUserId)
        val conversationRef = messagesRef.child(conversationId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    fromMap(child.value as? Map<*, *>)
                }.sortedBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        conversationRef.addValueEventListener(listener)
        awaitClose { conversationRef.removeEventListener(listener) }
    }

    fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = mutableListOf<Conversation>()

                snapshot.children.forEach { conversationSnapshot ->
                    val conversationId = conversationSnapshot.key ?: return@forEach

                    if (conversationId.contains(currentUserId)) {
                        val messages = conversationSnapshot.children.mapNotNull { msgSnapshot ->
                            fromMap(msgSnapshot.value as? Map<*, *>)
                        }

                        if (messages.isNotEmpty()) {
                            val lastMessage = messages.maxByOrNull { it.timestamp }
                            val otherUserId = conversationId.split("_")
                                .first { it != currentUserId }

                            lastMessage?.let { msg ->
                                conversations.add(
                                    Conversation(
                                        conversationId = conversationId,
                                        participantIds = listOf(currentUserId, otherUserId),
                                        participantNames = mapOf(
                                            msg.senderId to msg.senderName,
                                            msg.recipientId to msg.recipientName
                                        ),
                                        lastMessage = msg.text.ifEmpty { "Image" },
                                        lastMessageTime = msg.timestamp,
                                        unreadCount = 0
                                    )
                                )
                            }
                        }
                    }
                }

                trySend(conversations.sortedByDescending { it.lastMessageTime })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    suspend fun uploadImage(imageUri: Uri): String {
        val user = auth.currentUser ?: throw IllegalStateException("User must be logged in")
        val imageId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("chat_images/${user.uid}/$imageId.jpg")

        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    private fun toMap(message: Message): Map<String, Any?> = mapOf(
        "id" to message.id,
        "senderId" to message.senderId,
        "senderName" to message.senderName,
        "recipientId" to message.recipientId,
        "recipientName" to message.recipientName,
        "text" to message.text,
        "timestamp" to message.timestamp,
        "imageUrl" to message.imageUrl
    )

    @Suppress("UNCHECKED_CAST")
    private fun fromMap(data: Map<*, *>?): Message? {
        if (data == null) return null
        return try {
            Message(
                id = data["id"] as? String ?: "",
                senderId = data["senderId"] as? String ?: "",
                senderName = data["senderName"] as? String ?: "Anonymous",
                recipientId = data["recipientId"] as? String ?: "",
                recipientName = data["recipientName"] as? String ?: "Anonymous",
                text = data["text"] as? String ?: "",
                timestamp = (data["timestamp"] as? Long) ?: 0L,
                imageUrl = data["imageUrl"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }
}


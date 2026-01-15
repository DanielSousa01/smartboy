package com.fcul.smartboy.domain.chat

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
)


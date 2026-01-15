package com.fcul.smartboy.domain.chat

data class ChatUser(
    val userId: String = "",
    val userName: String = "",
    val photoUrl: String? = null,
    val lastSeen: Long = System.currentTimeMillis()
)


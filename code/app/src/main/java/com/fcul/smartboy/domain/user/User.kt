package com.fcul.smartboy.domain.user

data class User(
    val userId: String,
    val username: String,
    val email: String = "",
    val photoUrl: String? = null
)


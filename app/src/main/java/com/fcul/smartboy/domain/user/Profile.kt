package com.fcul.smartboy.domain.user

data class Profile(
    val userId: String,
    val steps: Long = 0,
    val distance: Double = 0.0,
    val radiation: Double = 0.0
)
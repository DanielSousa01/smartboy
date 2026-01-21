package com.fcul.smartboy.domain.user

data class Profile(
    val userId: String,
    val caps: Int = 0,
    val steps: Long = 0,
    val distance: Double = 0.0,
    val radiation: Double = 0.0,
    val radiationResistance: Double = 0.0, // Percentage (0.0 to 1.0) from Rad-X
    val radXExpiryTime: Long = 0, // Timestamp when Rad-X effect expires
    val preferences: UserPreferences = UserPreferences()
)
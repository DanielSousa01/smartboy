package com.fcul.smartboy.domain.user

data class UserPreferences(
    val measurementUnit: MeasurementUnit = MeasurementUnit.METRIC
)

enum class MeasurementUnit {
    METRIC,     // Kilometers, meters
    IMPERIAL    // Miles, feet
}

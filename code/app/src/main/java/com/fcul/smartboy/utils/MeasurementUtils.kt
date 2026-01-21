package com.fcul.smartboy.utils

import com.fcul.smartboy.domain.user.MeasurementUnit
import java.util.Locale

/**
 * Utility functions for formatting measurements based on user preferences
 */
object MeasurementUtils {

    /**
     * Format distance in meters to a human-readable string based on measurement unit
     * @param distanceMeters Distance in meters
     * @param unit Measurement unit preference (METRIC or IMPERIAL)
     * @return Formatted string (e.g., "5.2 km" or "3.2 mi")
     */
    fun formatDistance(distanceMeters: Double, unit: MeasurementUnit): String {
        return when (unit) {
            MeasurementUnit.METRIC -> {
                val distanceKm = distanceMeters / 1000.0
                if (distanceKm >= 1.0) {
                    String.format(Locale.US, "%.2f km", distanceKm)
                } else {
                    String.format(Locale.US, "%.0f m", distanceMeters)
                }
            }

            MeasurementUnit.IMPERIAL -> {
                val distanceFeet = distanceMeters * 3.28084
                if (distanceFeet >= 5280) {
                    val miles = distanceFeet / 5280
                    String.format(Locale.US, "%.2f mi", miles)
                } else {
                    String.format(Locale.US, "%.0f ft", distanceFeet)
                }
            }
        }
    }

    /**
     * Convert meters to the preferred unit
     * @param distanceMeters Distance in meters
     * @param unit Measurement unit preference
     * @return Pair of (value, unit string) e.g., (5.2, "km") or (3.2, "mi")
     */
    fun convertDistance(distanceMeters: Double, unit: MeasurementUnit): Pair<Double, String> {
        return when (unit) {
            MeasurementUnit.METRIC -> {
                val distanceKm = distanceMeters / 1000.0
                if (distanceKm >= 1.0) {
                    Pair(distanceKm, "km")
                } else {
                    Pair(distanceMeters, "m")
                }
            }

            MeasurementUnit.IMPERIAL -> {
                val distanceFeet = distanceMeters * 3.28084
                if (distanceFeet >= 5280) {
                    val miles = distanceFeet / 5280
                    Pair(miles, "mi")
                } else {
                    Pair(distanceFeet, "ft")
                }
            }
        }
    }
}

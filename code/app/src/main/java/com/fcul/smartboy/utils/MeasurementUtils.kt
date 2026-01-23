package com.fcul.smartboy.utils

import android.location.Location
import com.fcul.smartboy.domain.user.MeasurementUnit
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Date
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
     * Format a Date object to a readable string
     * @param date Date object
     * @return Formatted date string (e.g., "Jan 01, 2024 14:30")
     */
    fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(date)
    }

    /**
     * Format duration in seconds to a human-readable string
     * @param seconds Duration in seconds
     * @return Formatted string (e.g., "1h 30m" or "45 min")
     */
    fun formatDuration(seconds: Int): String {
        val durationMins = seconds / 60
        return if (durationMins >= 60) {
            val hours = durationMins / 60
            val mins = durationMins % 60
            "${hours}h ${mins}m"
        } else {
            "$durationMins min"
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

    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude,
            point1.longitude,
            point2.latitude,
            point2.longitude,
            results
        )
        return results[0].toDouble()
    }
}

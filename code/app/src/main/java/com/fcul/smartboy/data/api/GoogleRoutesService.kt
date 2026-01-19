package com.fcul.smartboy.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GoogleRoutesService {
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline",
        @Body request: ComputeRoutesRequest
    ): Response<ComputeRoutesResponse>
}

data class ComputeRoutesRequest(
    val origin: Waypoint,
    val destination: Waypoint,
    val intermediates: List<Waypoint>? = null,
    val travelMode: String = "WALK",
    val computeAlternativeRoutes: Boolean = false,
    val languageCode: String = "en-US",
    val units: String = "METRIC"
)

data class ComputeRoutesResponse(
    val routes: List<Route>?
)

data class Route(
    val distanceMeters: Int?,
    val duration: String?,
    val polyline: Polyline?
)

data class Polyline(
    val encodedPolyline: String?
)

data class Waypoint(
    val location: WaypointLocation
)

data class WaypointLocation(
    val latLng: LatLngLiteral
)

data class LatLngLiteral(
    val latitude: Double,
    val longitude: Double
)

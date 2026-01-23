package com.fcul.smartboy.data.api

import android.util.Log
import com.fcul.smartboy.domain.user.MeasurementUnit
import com.google.android.gms.maps.model.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutesRepository @Inject constructor() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://routes.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val routesService = retrofit.create(GoogleRoutesService::class.java)

    suspend fun computeRoute(
        apiKey: String,
        origin: LatLng,
        destination: LatLng,
        measurementUnit: MeasurementUnit,
        waypoints: List<LatLng> = emptyList()
    ): RouteResult {
        return try {
            val request = ComputeRoutesRequest(
                origin = Waypoint(
                    location = WaypointLocation(
                        latLng = LatLngLiteral(origin.latitude, origin.longitude)
                    )
                ),
                destination = Waypoint(
                    location = WaypointLocation(
                        latLng = LatLngLiteral(destination.latitude, destination.longitude)
                    )
                ),
                intermediates = if (waypoints.isNotEmpty()) {
                    waypoints.map { waypoint ->
                        Waypoint(
                            location = WaypointLocation(
                                latLng = LatLngLiteral(waypoint.latitude, waypoint.longitude)
                            )
                        )
                    }
                } else null,
                travelMode = "WALK",
                computeAlternativeRoutes = false,
                languageCode = "en-US",
                units = measurementUnit.name
            )

            val response = routesService.computeRoutes(apiKey, request = request)

            if (!response.isSuccessful) {
                return RouteResult.Error("HTTP ${response.code()}: ${response.message()}")
            }

            val body = response.body()
            val routes = body?.routes

            if (routes.isNullOrEmpty()) {
                return RouteResult.Error("No routes found")
            }

            val route = routes[0]
            val encodedPolyline = route.polyline?.encodedPolyline
                ?: return RouteResult.Error("No polyline in response")

            val distanceMeters = route.distanceMeters ?: 0
            val durationString = route.duration ?: "0s"
            val durationSeconds = durationString.replace("s", "").toIntOrNull() ?: 0

            RouteResult.Success(
                encodedPolyline = encodedPolyline,
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds
            )

        } catch (e: Exception) {
            Log.e("RoutesRepository", "Error computing route", e)
            RouteResult.Error(e.message ?: "Unknown error")
        }
    }

    sealed class RouteResult {
        data class Success(
            val encodedPolyline: String,
            val distanceMeters: Int,
            val durationSeconds: Int
        ) : RouteResult()

        data class Error(val message: String) : RouteResult()
    }
}

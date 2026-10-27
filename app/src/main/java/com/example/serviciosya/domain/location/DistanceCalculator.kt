package com.example.serviciosya.domain.location

import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.domain.model.Provider
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceCalculator {
    private const val EARTH_RADIUS_KM = 6371.0

    /** Great-circle distance in kilometers (haversine formula). */
    fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Distance from [origin] to the provider, or null when either location is unknown. */
    fun distanceKm(origin: GeoPoint?, provider: Provider): Double? {
        val destination = provider.location ?: return null
        return origin?.let { distanceKm(it, destination) }
    }

    /**
     * Pairs every provider with its distance. With an origin the list is sorted from nearest to
     * farthest and providers without coordinates go last; without one the original order is kept.
     */
    fun withDistances(providers: List<Provider>, origin: GeoPoint?): List<Pair<Provider, Double?>> {
        val paired = providers.map { it to distanceKm(origin, it) }
        return if (origin == null) paired else paired.sortedWith(compareBy(nullsLast()) { it.second })
    }
}

/** "a 800 m" below one kilometer, "a 2,3 km" above (Argentine decimal comma). */
fun formatDistance(km: Double): String {
    val meters = (km * 100).roundToInt() * 10
    return if (meters < 1000) {
        "a $meters m"
    } else {
        "a " + String.format(Locale.forLanguageTag("es-AR"), "%.1f km", km)
    }
}

/** Coarse range sent to Analytics instead of coordinates. */
fun distanceBucket(km: Double?): String = when {
    km == null -> "unknown"
    km < 1 -> "<1km"
    km < 3 -> "1-3km"
    km < 10 -> "3-10km"
    else -> ">10km"
}

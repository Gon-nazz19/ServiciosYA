package com.example.serviciosya.data.location

import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.model.GeoPoint

/** Used when Google Play Services is missing: providers keep their usual order, without distances. */
class NoOpLocationProvider : LocationProvider {
    override fun hasPermission(): Boolean = false

    override suspend fun getCurrentLocation(): GeoPoint? = null
}

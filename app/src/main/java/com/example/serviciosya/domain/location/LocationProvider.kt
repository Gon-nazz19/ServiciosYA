package com.example.serviciosya.domain.location

import com.example.serviciosya.domain.model.GeoPoint

/** Approximate location of the user. The app works the same without it, only without distances. */
interface LocationProvider {
    fun hasPermission(): Boolean

    /** Null when the permission was not granted or the device has no location to give. */
    suspend fun getCurrentLocation(): GeoPoint?
}

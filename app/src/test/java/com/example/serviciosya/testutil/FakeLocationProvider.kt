package com.example.serviciosya.testutil

import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.model.GeoPoint

class FakeLocationProvider(
    var location: GeoPoint? = null,
    var permissionGranted: Boolean = location != null,
) : LocationProvider {
    var calls = 0
        private set

    override fun hasPermission(): Boolean = permissionGranted

    override suspend fun getCurrentLocation(): GeoPoint? {
        calls++
        return if (permissionGranted) location else null
    }
}

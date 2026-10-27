package com.example.serviciosya.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.model.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Coarse location from Google Play Services. The last fix is reused for a short time so the
 * list and the provider profile do not ask the GPS again on every screen.
 */
class FusedLocationProvider(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationProvider {
    private var cached: GeoPoint? = null
    private var cachedAtMillis = 0L

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override suspend fun getCurrentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        val now = SystemClock.elapsedRealtime()
        cached?.let { if (now - cachedAtMillis < CACHE_MILLIS) return it }
        val cancellation = CancellationTokenSource()
        val location = try {
            // The last known fix is instant and precise enough for distances; ask for a new one only without it.
            client.lastLocation.await() ?: withTimeoutOrNull(TIMEOUT_MILLIS) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // SecurityException if the permission was revoked after the check, or a Play Services error.
            null
        } finally {
            cancellation.cancel()
        }
        return location?.let { GeoPoint(it.latitude, it.longitude) }?.also {
            cached = it
            cachedAtMillis = now
        }
    }

    private companion object {
        const val CACHE_MILLIS = 2 * 60 * 1000L
        const val TIMEOUT_MILLIS = 10_000L
    }
}

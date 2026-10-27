package com.example.serviciosya.domain.location

import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.testutil.testProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceCalculatorTest {
    private val sanFrancisco = GeoPoint(-31.4280, -62.0830)

    @Test
    fun `same point is zero kilometers`() {
        assertEquals(0.0, DistanceCalculator.distanceKm(sanFrancisco, sanFrancisco), 0.0)
    }

    @Test
    fun `known distance between two cities`() {
        val cordoba = GeoPoint(-31.4201, -64.1888)

        // About 200 km in a straight line between San Francisco and Córdoba capital.
        assertEquals(200.0, DistanceCalculator.distanceKm(sanFrancisco, cordoba), 2.0)
    }

    @Test
    fun `one hundredth of a degree of latitude is about 1,1 km`() {
        val north = GeoPoint(sanFrancisco.latitude + 0.01, sanFrancisco.longitude)

        assertEquals(1.112, DistanceCalculator.distanceKm(sanFrancisco, north), 0.005)
    }

    @Test
    fun `distance to a provider is null without origin or coordinates`() {
        val located = testProvider(latitude = -31.43, longitude = -62.08)

        assertNull(DistanceCalculator.distanceKm(null, located))
        assertNull(DistanceCalculator.distanceKm(sanFrancisco, testProvider()))
    }

    @Test
    fun `providers are sorted by distance and the ones without coordinates go last`() {
        val far = testProvider(id = "far", latitude = -31.46, longitude = -62.08)
        val unknown = testProvider(id = "unknown")
        val near = testProvider(id = "near", latitude = -31.429, longitude = -62.083)

        val sorted = DistanceCalculator.withDistances(listOf(far, unknown, near), sanFrancisco)

        assertEquals(listOf("near", "far", "unknown"), sorted.map { it.first.id })
        assertNull(sorted.last().second)
    }

    @Test
    fun `without origin the original order is kept`() {
        val providers = listOf(testProvider(id = "b", latitude = -31.46, longitude = -62.08), testProvider(id = "a"))

        val result = DistanceCalculator.withDistances(providers, null)

        assertEquals(listOf("b", "a"), result.map { it.first.id })
        assertEquals(listOf(null, null), result.map { it.second })
    }

    @Test
    fun `short distances are shown in meters`() {
        assertEquals("a 800 m", formatDistance(0.8))
        assertEquals("a 50 m", formatDistance(0.047))
    }

    @Test
    fun `long distances are shown in kilometers with decimal comma`() {
        assertEquals("a 2,3 km", formatDistance(2.34))
        assertEquals("a 1,0 km", formatDistance(0.998))
        assertEquals("a 12,0 km", formatDistance(12.0))
    }

    @Test
    fun `distance buckets never reveal the exact distance`() {
        assertEquals("unknown", distanceBucket(null))
        assertEquals("<1km", distanceBucket(0.4))
        assertEquals("1-3km", distanceBucket(1.0))
        assertEquals("3-10km", distanceBucket(9.9))
        assertEquals(">10km", distanceBucket(25.0))
    }
}

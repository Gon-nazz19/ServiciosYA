package com.example.serviciosya.data.model

import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.testutil.testProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderDtoTest {
    @Test
    fun `provider dto maps every domain field`() {
        val provider = ProviderDto(
            id = "provider-1",
            userId = "user-1",
            name = "Carlos Electricidad",
            description = "Instalaciones domiciliarias",
            categoryId = "electricistas",
            city = "San Francisco",
            latitude = -31.4268,
            longitude = -62.0849,
            profileImageUrl = "https://example.com/profile.webp",
            phone = "3564-000000",
            rating = 4.7,
            reviewCount = 12,
            verified = true,
            active = true,
        ).toDomain()

        assertEquals("provider-1", provider.id)
        assertEquals("electricistas", provider.categoryId)
        assertEquals(4.7, provider.rating ?: 0.0, 0.0)
        assertEquals(12, provider.reviewCount)
        assertTrue(provider.verified)
        assertTrue(provider.active)
        assertEquals(GeoPoint(-31.4268, -62.0849), provider.location)
    }

    @Test
    fun `provider without coordinates has no location`() {
        val provider = testProvider(latitude = -31.4, longitude = null)

        assertNull(provider.location)
    }
}

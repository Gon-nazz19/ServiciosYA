package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.ads.NativeAdContent
import com.example.serviciosya.presentation.provider.ProviderListEntry.AdEntry
import com.example.serviciosya.presentation.provider.ProviderListEntry.ProviderEntry
import com.example.serviciosya.testutil.testProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderListEntriesTest {
    private fun providersOf(count: Int) =
        (1..count).map { ProviderListItem(testProvider(id = "p$it"), distanceKm = null) }

    private fun adsOf(count: Int) = (1..count).map { NativeAdContent(headline = "ad$it") }

    private fun List<ProviderListEntry>.kinds() = map { if (it is AdEntry) "ad" else "provider" }

    @Test
    fun `without loaded ads the list is just the providers`() {
        val entries = providerListEntries(providersOf(13), emptyList())

        assertEquals(13, entries.size)
        assertTrue(entries.all { it is ProviderEntry })
    }

    @Test
    fun `six providers or fewer never carry an ad even if one was loaded`() {
        val entries = providerListEntries(providersOf(6), adsOf(1))

        assertEquals(6, entries.size)
        assertTrue(entries.all { it is ProviderEntry })
    }

    @Test
    fun `seven providers get the ad after the sixth`() {
        val entries = providerListEntries(providersOf(7), adsOf(1))

        assertEquals(List(6) { "provider" } + "ad" + "provider", entries.kinds())
    }

    @Test
    fun `an ad is never placed at the end of the list`() {
        val entries = providerListEntries(providersOf(12), adsOf(3))

        assertTrue(entries.last() is ProviderEntry)
        assertEquals(1, entries.count { it is AdEntry })
    }

    @Test
    fun `a single loaded ad takes the first position and leaves no gap`() {
        val entries = providerListEntries(providersOf(13), adsOf(1))

        assertEquals(14, entries.size)
        assertEquals(6, entries.indexOfFirst { it is AdEntry })
        assertEquals(1, entries.count { it is AdEntry })
    }

    @Test
    fun `at most three ads are used and they keep their order`() {
        val entries = providerListEntries(providersOf(30), adsOf(5))

        val ads = entries.filterIsInstance<AdEntry>()
        assertEquals(listOf("ad1", "ad2", "ad3"), ads.map { it.ad.headline })
        assertEquals(listOf(0, 1, 2), ads.map { it.index })
    }
}

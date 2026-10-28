package com.example.serviciosya.domain.billing

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.testutil.testProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedRankingTest {
    private val now = 1_000_000_000_000L
    private val active = now + 1_000
    private val expired = now - 1_000

    /** Providers "0", "1", "2"...; [featured] maps an index to the end of its plan. */
    private fun providers(count: Int, featured: Map<Int, Long> = emptyMap()): List<Provider> =
        (0 until count).map { index ->
            testProvider(id = index.toString()).copy(featuredUntil = featured[index])
        }

    private fun rank(
        providers: List<Provider>,
        distances: Map<String, Double?> = emptyMap(),
    ): List<String> = FeaturedRanking.rank(providers, now) { distances[it.id] }.map { it.id }

    private fun ids(vararg values: Int) = values.map { it.toString() }

    @Test
    fun `an empty list stays empty`() {
        assertEquals(emptyList<String>(), rank(emptyList()))
    }

    @Test
    fun `without featured providers the order is kept`() {
        assertEquals(ids(0, 1, 2, 3, 4, 5, 6, 7), rank(providers(8)))
    }

    @Test
    fun `five results or fewer are never reordered`() {
        assertEquals(ids(0, 1, 2, 3, 4), rank(providers(5, featured = mapOf(4 to active))))
        assertEquals(ids(0, 1, 2), rank(providers(3, featured = mapOf(2 to active))))
    }

    @Test
    fun `one featured provider from further down takes position four`() {
        val result = rank(providers(8, featured = mapOf(6 to active)))

        assertEquals(ids(0, 1, 2, 6, 3, 4, 5, 7), result)
    }

    @Test
    fun `two featured providers from further down take positions four and five nearest first`() {
        val list = providers(8, featured = mapOf(5 to active, 7 to active))
        val result = rank(list, distances = mapOf("5" to 4.0, "7" to 1.0))

        assertEquals(ids(0, 1, 2, 7, 5, 3, 4, 6), result)
    }

    @Test
    fun `with three or more featured providers only the two nearest get a slot`() {
        val list = providers(10, featured = mapOf(5 to active, 7 to active, 9 to active))
        val result = rank(list, distances = mapOf("5" to 3.0, "7" to 1.0, "9" to 2.0))

        // 5 is featured but out of the slots: it stays behind in its relative order.
        assertEquals(ids(0, 1, 2, 7, 9, 3, 4, 5, 6, 8), result)
    }

    @Test
    fun `an expired plan does not count`() {
        val result = rank(providers(8, featured = mapOf(6 to expired, 7 to now)))

        assertEquals(ids(0, 1, 2, 3, 4, 5, 6, 7), result)
    }

    @Test
    fun `without a location the incoming order decides who gets the slots`() {
        val result = rank(providers(9, featured = mapOf(8 to active, 6 to active, 7 to active)))

        assertEquals(ids(0, 1, 2, 6, 7, 3, 4, 5, 8), result)
    }

    @Test
    fun `providers with a known distance beat those without one`() {
        val list = providers(9, featured = mapOf(6 to active, 7 to active, 8 to active))
        val result = rank(list, distances = mapOf("8" to 2.0))

        // 8 is the only one with a distance, so it goes first; the next slot is 6 by incoming order.
        assertEquals(ids(0, 1, 2, 8, 6, 3, 4, 5, 7), result)
    }

    @Test
    fun `ties keep the incoming order`() {
        val list = providers(9, featured = mapOf(6 to active, 7 to active, 8 to active))
        val result = rank(list, distances = mapOf("6" to 2.0, "7" to 2.0, "8" to 2.0))

        assertEquals(ids(0, 1, 2, 6, 7, 3, 4, 5, 8), result)
    }

    @Test
    fun `a featured provider already within the first five stays and the other takes the free slot`() {
        val atFour = rank(providers(8, featured = mapOf(3 to active, 6 to active)), mapOf("3" to 1.0, "6" to 2.0))
        assertEquals(ids(0, 1, 2, 3, 6, 4, 5, 7), atFour)

        val atFive = rank(providers(8, featured = mapOf(4 to active, 7 to active)), mapOf("4" to 1.0, "7" to 2.0))
        assertEquals(ids(0, 1, 2, 7, 4, 3, 5, 6), atFive)
    }

    @Test
    fun `a featured provider at the top stays there and another one comes up to position four`() {
        val result = rank(providers(8, featured = mapOf(0 to active, 6 to active)), mapOf("0" to 1.0, "6" to 2.0))

        assertEquals(ids(0, 1, 2, 6, 3, 4, 5, 7), result)
    }

    @Test
    fun `two featured providers already within the first five change nothing`() {
        val result = rank(providers(8, featured = mapOf(1 to active, 4 to active)))

        assertEquals(ids(0, 1, 2, 3, 4, 5, 6, 7), result)
    }

    @Test
    fun `non featured providers keep their relative order and nobody is lost`() {
        val list = providers(12, featured = mapOf(8 to active, 11 to active))
        val result = rank(list, distances = mapOf("8" to 1.0, "11" to 2.0))

        assertEquals(list.size, result.size)
        assertEquals(list.map { it.id }.toSet(), result.toSet())
        val others = result.filter { it != "8" && it != "11" }
        assertEquals(ids(0, 1, 2, 3, 4, 5, 6, 7, 9, 10), others)
    }

    @Test
    fun `a provider is featured only while its plan has not expired`() {
        val provider = testProvider()

        assertFalse(provider.isFeaturedAt(now))
        assertTrue(provider.copy(featuredUntil = now + 1).isFeaturedAt(now))
        assertFalse(provider.copy(featuredUntil = now).isFeaturedAt(now))
        assertFalse(provider.copy(featuredUntil = now - 1).isFeaturedAt(now))
    }
}

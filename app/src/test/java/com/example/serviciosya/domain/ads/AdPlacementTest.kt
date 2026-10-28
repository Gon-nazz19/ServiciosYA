package com.example.serviciosya.domain.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPlacementTest {
    @Test
    fun `empty list has no ads`() {
        assertTrue(AdPlacement.positions(0).isEmpty())
    }

    @Test
    fun `six providers or fewer have no ads`() {
        (1..6).forEach { count ->
            assertTrue("count=$count", AdPlacement.positions(count).isEmpty())
        }
    }

    @Test
    fun `seven providers get one ad after the sixth`() {
        assertEquals(listOf(6), AdPlacement.positions(7))
    }

    @Test
    fun `twelve providers get one ad because the next would be at the end`() {
        assertEquals(listOf(6), AdPlacement.positions(12))
    }

    @Test
    fun `thirteen providers get two ads`() {
        assertEquals(listOf(6, 12), AdPlacement.positions(13))
    }

    @Test
    fun `nineteen providers get three ads`() {
        assertEquals(listOf(6, 12, 18), AdPlacement.positions(19))
    }

    @Test
    fun `more than eighteen providers never get more than three ads`() {
        listOf(19, 25, 100).forEach { count ->
            assertEquals("count=$count", listOf(6, 12, 18), AdPlacement.positions(count))
        }
    }

    @Test
    fun `an ad is never placed at the end of the list`() {
        (0..60).forEach { count ->
            assertTrue("count=$count", AdPlacement.positions(count).all { it < count })
        }
    }
}

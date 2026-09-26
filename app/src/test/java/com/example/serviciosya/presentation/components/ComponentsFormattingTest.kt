package com.example.serviciosya.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComponentsFormattingTest {
    @Test
    fun `initials use the first letter of the first two words`() {
        assertEquals("CE", initialsOf("Carlos Electricidad"))
        assertEquals("PG", initialsOf("  plomería   García  López "))
        assertEquals("E", initialsOf("ElectroFix"))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun `rating is shown with one decimal only when present`() {
        assertEquals("★ 4.7", formatRating(4.7))
        assertEquals("★ 5.0", formatRating(5.0))
        assertNull(formatRating(null))
        assertNull(formatRating(0.0))
    }
}

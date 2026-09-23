package com.example.serviciosya.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDtoTest {
    @Test
    fun `category dto maps to domain without Firebase types`() {
        val category = CategoryDto(
            id = "electricistas",
            name = "Electricista",
            icon = "electrical_services",
            active = true,
        ).toDomain()

        assertEquals("electricistas", category.id)
        assertEquals("Electricista", category.name)
        assertEquals("electrical_services", category.icon)
        assertTrue(category.active)
    }
}

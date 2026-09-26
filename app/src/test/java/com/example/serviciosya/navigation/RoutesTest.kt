package com.example.serviciosya.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test
    fun `only the three main sections show the bottom bar`() {
        assertTrue(isTopLevelRoute(Routes.HOME))
        assertTrue(isTopLevelRoute(Routes.REQUESTS))
        assertTrue(isTopLevelRoute(Routes.PROFILE))

        assertFalse(isTopLevelRoute(Routes.LOGIN))
        assertFalse(isTopLevelRoute(Routes.REGISTER))
        assertFalse(isTopLevelRoute(Routes.SESSION))
        assertFalse(isTopLevelRoute(Routes.CATEGORY))
        assertFalse(isTopLevelRoute(Routes.PROVIDER))
        assertFalse(isTopLevelRoute(null))
    }

    @Test
    fun `bottom bar sections keep the backlog order and labels`() {
        assertEquals(
            listOf("Inicio", "Solicitudes", "Perfil"),
            TopLevelDestination.entries.map { it.label },
        )
    }

    @Test
    fun `route builders fill the arguments`() {
        assertEquals("category/plomeros", Routes.category("plomeros"))
        assertEquals("provider/abc", Routes.provider("abc"))
    }
}

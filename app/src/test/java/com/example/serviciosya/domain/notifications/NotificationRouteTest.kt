package com.example.serviciosya.domain.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRouteTest {
    @Test
    fun `category and provider routes are parsed`() {
        assertEquals(NotificationRoute.Category("electricistas"), NotificationRoute.parse("category/electricistas"))
        assertEquals(NotificationRoute.Provider("carlos-electricidad"), NotificationRoute.parse("/provider/carlos-electricidad/"))
        assertEquals(NotificationRoute.Requests, NotificationRoute.parse(" requests "))
    }

    @Test
    fun `builders produce routes the parser understands`() {
        assertEquals(NotificationRoute.Category("plomeros"), NotificationRoute.parse(NotificationRoute.category("plomeros")))
        assertEquals(NotificationRoute.Provider("p1"), NotificationRoute.parse(NotificationRoute.provider("p1")))
    }

    @Test
    fun `empty unknown or malformed routes are ignored`() {
        assertNull(NotificationRoute.parse(null))
        assertNull(NotificationRoute.parse(""))
        assertNull(NotificationRoute.parse("settings"))
        assertNull(NotificationRoute.parse("category/"))
        assertNull(NotificationRoute.parse("category/a/b"))
        assertNull(NotificationRoute.parse("provider/../admin"))
        assertNull(NotificationRoute.parse("https://example.com/category/x"))
    }

    @Test
    fun `category topics are valid FCM topic names`() {
        assertEquals("categoria_electricistas", Topics.category("electricistas"))
        assertEquals("categoria_aire-acondicionado", Topics.category("Aire-Acondicionado"))
        assertEquals("categoria_tecnico_pc_", Topics.category("técnico pc!"))
    }
}

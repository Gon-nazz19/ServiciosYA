package com.example.serviciosya.domain.contact

import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.testutil.testProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactLinkBuilderTest {
    @Test
    fun `local number with hyphen gets the country and mobile prefixes`() {
        assertEquals("5493564400101", ContactLinkBuilder.normalizeArgentinePhone("3564-400101"))
    }

    @Test
    fun `leading zero and 15 prefix are removed`() {
        assertEquals("5493564400101", ContactLinkBuilder.normalizeArgentinePhone("03564-400101"))
        assertEquals("5493564400101", ContactLinkBuilder.normalizeArgentinePhone("03564 15-400101"))
        assertEquals("5491123456789", ContactLinkBuilder.normalizeArgentinePhone("011 15 2345-6789"))
    }

    @Test
    fun `international formats are accepted`() {
        assertEquals("5493564400101", ContactLinkBuilder.normalizeArgentinePhone("+54 9 3564 400101"))
        assertEquals("5493564400101", ContactLinkBuilder.normalizeArgentinePhone("+54 3564 400101"))
        assertEquals("5493564400101", ContactLinkBuilder.normalizeArgentinePhone("5493564400101"))
    }

    @Test
    fun `invalid numbers return null`() {
        assertNull(ContactLinkBuilder.normalizeArgentinePhone(""))
        assertNull(ContactLinkBuilder.normalizeArgentinePhone("400101"))
        assertNull(ContactLinkBuilder.normalizeArgentinePhone("sin teléfono"))
        assertNull(ContactLinkBuilder.normalizeArgentinePhone("3564-400101-999"))
    }

    @Test
    fun `whatsapp message greets the provider and appends the user message`() {
        assertEquals(
            "Hola Carlos, te encontré en ServiciosYA. Necesito cambiar un enchufe",
            ContactLinkBuilder.whatsAppMessage("Carlos", "  Necesito cambiar un enchufe "),
        )
    }

    @Test
    fun `empty user message keeps only the greeting`() {
        assertEquals(
            "Hola Carlos, te encontré en ServiciosYA.",
            ContactLinkBuilder.whatsAppMessage("Carlos", "   "),
        )
    }

    @Test
    fun `whatsapp url encodes spaces accents and enie`() {
        val url = ContactLinkBuilder.whatsAppUrl("5493564400101", "Hola, ¿mañana podés?")

        assertEquals("https://wa.me/5493564400101?text=Hola%2C%20%C2%BFma%C3%B1ana%20pod%C3%A9s%3F", url)
    }

    @Test
    fun `directions uri pins the location with the encoded name`() {
        val uri = ContactLinkBuilder.directionsUri(GeoPoint(-31.4268, -62.0849), "Plomería García")

        assertEquals("geo:-31.4268,-62.0849?q=-31.4268,-62.0849(Plomer%C3%ADa%20Garc%C3%ADa)", uri)
    }

    @Test
    fun `share text includes name category city and phone`() {
        val text = ContactLinkBuilder.shareText(testProvider(), "Electricista")

        assertEquals(
            "Carlos Electricidad - Electricista, San Francisco\nTeléfono: 3564-000000\nLo encontré en ServiciosYA.",
            text,
        )
    }
}

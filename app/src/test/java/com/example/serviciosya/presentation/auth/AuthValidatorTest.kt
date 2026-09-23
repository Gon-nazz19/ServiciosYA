package com.example.serviciosya.presentation.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun `valid credentials have no validation errors`() {
        assertNull(AuthValidator.validateName("Ana"))
        assertNull(AuthValidator.validateEmail("ana@example.com"))
        assertNull(AuthValidator.validatePassword("secret1"))
    }

    @Test
    fun `blank fields return helpful errors`() {
        assertEquals("Ingresá tu nombre.", AuthValidator.validateName(" "))
        assertEquals("Ingresá tu email.", AuthValidator.validateEmail(""))
        assertEquals("Ingresá tu contraseña.", AuthValidator.validatePassword(""))
    }

    @Test
    fun `malformed email and short password are rejected`() {
        assertEquals("Ingresá un email válido.", AuthValidator.validateEmail("invalid"))
        assertEquals(
            "La contraseña debe tener al menos 6 caracteres.",
            AuthValidator.validatePassword("12345"),
        )
    }
}

package com.example.serviciosya.presentation.auth

object AuthValidator {
    fun validateName(name: String): String? = when {
        name.isBlank() -> "Ingresá tu nombre."
        name.trim().length < 2 -> "El nombre debe tener al menos 2 caracteres."
        else -> null
    }

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Ingresá tu email."
        !EMAIL_PATTERN.matches(email.trim()) -> "Ingresá un email válido."
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Ingresá tu contraseña."
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
        else -> null
    }

    private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
}

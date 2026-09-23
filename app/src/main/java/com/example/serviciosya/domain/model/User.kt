package com.example.serviciosya.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String = ROLE_CLIENT,
) {
    companion object {
        const val ROLE_CLIENT = "CLIENT"
    }
}

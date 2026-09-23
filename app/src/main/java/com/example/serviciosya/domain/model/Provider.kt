package com.example.serviciosya.domain.model

data class Provider(
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val city: String,
    val profileImageUrl: String,
    val phone: String,
    val rating: Double?,
    val reviewCount: Int,
    val verified: Boolean,
    val active: Boolean,
)

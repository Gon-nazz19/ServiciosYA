package com.example.serviciosya.data.model

import com.example.serviciosya.domain.model.Provider
import com.google.firebase.firestore.DocumentSnapshot

data class ProviderDto(
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

fun DocumentSnapshot.toProviderDto(): ProviderDto = ProviderDto(
    id = id,
    userId = getString("userId").orEmpty(),
    name = getString("name").orEmpty(),
    description = getString("description").orEmpty(),
    categoryId = getString("categoryId").orEmpty(),
    city = getString("city").orEmpty(),
    profileImageUrl = getString("profileImageUrl").orEmpty(),
    phone = getString("phone").orEmpty(),
    rating = (get("rating") as? Number)?.toDouble(),
    reviewCount = (get("reviewCount") as? Number)?.toInt() ?: 0,
    verified = getBoolean("verified") ?: false,
    active = getBoolean("active") ?: false,
)

fun ProviderDto.toDomain(): Provider = Provider(
    id = id,
    userId = userId,
    name = name,
    description = description,
    categoryId = categoryId,
    city = city,
    profileImageUrl = profileImageUrl,
    phone = phone,
    rating = rating,
    reviewCount = reviewCount,
    verified = verified,
    active = active,
)

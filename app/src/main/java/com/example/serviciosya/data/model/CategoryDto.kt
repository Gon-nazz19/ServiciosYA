package com.example.serviciosya.data.model

import com.example.serviciosya.domain.model.Category
import com.google.firebase.firestore.DocumentSnapshot

data class CategoryDto(
    val id: String,
    val name: String,
    val icon: String,
    val active: Boolean,
)

fun DocumentSnapshot.toCategoryDto(): CategoryDto = CategoryDto(
    id = id,
    name = getString("name").orEmpty(),
    icon = getString("icon").orEmpty(),
    active = getBoolean("active") ?: false,
)

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    active = active,
)

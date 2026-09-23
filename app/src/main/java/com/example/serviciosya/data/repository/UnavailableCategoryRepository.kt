package com.example.serviciosya.data.repository

import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.repository.CategoryRepository

class UnavailableCategoryRepository : CategoryRepository {
    override suspend fun getActiveCategories(): Result<List<Category>> = Result.failure(
        IllegalStateException(
            "Firebase no está configurado. Agregá app/google-services.json para cargar categorías.",
        ),
    )
}

package com.example.serviciosya.domain.repository

import com.example.serviciosya.domain.model.Category

interface CategoryRepository {
    suspend fun getActiveCategories(): Result<List<Category>>
}

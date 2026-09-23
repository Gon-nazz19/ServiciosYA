package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.repository.CategoryRepository

class GetActiveCategoriesUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(): Result<List<Category>> = repository.getActiveCategories()
}

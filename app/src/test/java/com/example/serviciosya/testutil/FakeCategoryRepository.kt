package com.example.serviciosya.testutil

import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.repository.CategoryRepository

class FakeCategoryRepository(
    var categoriesResult: Result<List<Category>> = Result.success(emptyList()),
) : CategoryRepository {
    override suspend fun getActiveCategories(): Result<List<Category>> = categoriesResult
}

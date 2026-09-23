package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderRepository

class GetActiveProvidersByCategoryUseCase(
    private val repository: ProviderRepository,
) {
    suspend operator fun invoke(categoryId: String): Result<List<Provider>> {
        if (categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("La categoría es obligatoria."))
        }
        return repository.getActiveProvidersByCategory(categoryId)
    }
}

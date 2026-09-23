package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderRepository

class GetProviderUseCase(
    private val repository: ProviderRepository,
) {
    suspend operator fun invoke(providerId: String): Result<Provider?> {
        if (providerId.isBlank()) {
            return Result.failure(IllegalArgumentException("El prestador es obligatorio."))
        }
        return repository.getProvider(providerId)
    }
}

package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderRepository

class GetActiveProvidersUseCase(
    private val repository: ProviderRepository,
) {
    suspend operator fun invoke(): Result<List<Provider>> = repository.getActiveProviders()
}

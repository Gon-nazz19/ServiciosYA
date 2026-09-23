package com.example.serviciosya.data.repository

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderRepository

class UnavailableProviderRepository : ProviderRepository {
    override suspend fun getActiveProvidersByCategory(
        categoryId: String,
    ): Result<List<Provider>> = unavailableResult()

    override suspend fun getProvider(providerId: String): Result<Provider?> = unavailableResult()

    private fun <T> unavailableResult(): Result<T> = Result.failure(
        IllegalStateException(
            "Firebase no está configurado. Agregá app/google-services.json para cargar prestadores.",
        ),
    )
}

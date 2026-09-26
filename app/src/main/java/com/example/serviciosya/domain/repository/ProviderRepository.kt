package com.example.serviciosya.domain.repository

import com.example.serviciosya.domain.model.Provider

interface ProviderRepository {
    suspend fun getActiveProvidersByCategory(categoryId: String): Result<List<Provider>>

    suspend fun getProvider(providerId: String): Result<Provider?>

    suspend fun getActiveProviders(): Result<List<Provider>>
}

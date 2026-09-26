package com.example.serviciosya.testutil

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderRepository

class FakeProviderRepository(
    var providersResult: Result<List<Provider>> = Result.success(emptyList()),
    var providerResult: Result<Provider?> = Result.success(null),
    var allProvidersResult: Result<List<Provider>> = Result.success(emptyList()),
) : ProviderRepository {
    var providersByCategoryCalls = 0
        private set
    var lastCategoryId: String? = null
        private set

    override suspend fun getActiveProvidersByCategory(categoryId: String): Result<List<Provider>> {
        providersByCategoryCalls++
        lastCategoryId = categoryId
        return providersResult
    }

    var allProvidersCalls = 0
        private set

    override suspend fun getProvider(providerId: String): Result<Provider?> = providerResult

    override suspend fun getActiveProviders(): Result<List<Provider>> {
        allProvidersCalls++
        return allProvidersResult
    }
}

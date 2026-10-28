package com.example.serviciosya.data.billing

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderAccountRepository

/** Used when Firebase is not configured: no account is linked to a provider. */
class UnavailableProviderAccountRepository : ProviderAccountRepository {
    override suspend fun findByUserId(userId: String): Result<Provider?> = Result.success(null)

    override suspend fun setFeaturedUntil(providerId: String, untilMillis: Long): Result<Unit> = Result.failure(
        IllegalStateException(
            "Firebase no está configurado. Agregá app/google-services.json para usar los planes.",
        ),
    )
}

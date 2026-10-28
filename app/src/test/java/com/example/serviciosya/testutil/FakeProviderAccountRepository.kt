package com.example.serviciosya.testutil

import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderAccountRepository

/** [provider] is the profile linked to the account whose id equals its `userId`. */
class FakeProviderAccountRepository(
    var provider: Provider? = null,
) : ProviderAccountRepository {
    var findFails = false
    var setFeaturedUntilFails = false
    val featuredUntilCalls = mutableListOf<Pair<String, Long>>()

    override suspend fun findByUserId(userId: String): Result<Provider?> =
        if (findFails) {
            Result.failure(IllegalStateException("offline"))
        } else {
            Result.success(provider?.takeIf { it.userId == userId })
        }

    override suspend fun setFeaturedUntil(providerId: String, untilMillis: Long): Result<Unit> {
        if (setFeaturedUntilFails) return Result.failure(IllegalStateException("offline"))
        featuredUntilCalls += providerId to untilMillis
        return Result.success(Unit)
    }
}

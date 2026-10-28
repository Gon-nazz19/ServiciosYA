package com.example.serviciosya.domain.repository

import com.example.serviciosya.domain.model.Provider

/** Links a signed-in account to its provider profile, which is what makes it a provider account. */
interface ProviderAccountRepository {
    /** The provider whose `userId` is [userId], or null when the account is not a provider. */
    suspend fun findByUserId(userId: String): Result<Provider?>

    suspend fun setFeaturedUntil(providerId: String, untilMillis: Long): Result<Unit>
}

package com.example.serviciosya.data.billing

import com.example.serviciosya.domain.billing.Entitlements
import kotlinx.coroutines.flow.Flow

/** Where the simulated gateway keeps what each user bought. */
interface SubscriptionStore {
    fun observe(userId: String): Flow<Entitlements>

    suspend fun load(userId: String): Result<Entitlements>

    suspend fun save(userId: String, entitlements: Entitlements): Result<Unit>
}

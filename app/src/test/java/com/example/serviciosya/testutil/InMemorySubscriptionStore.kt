package com.example.serviciosya.testutil

import com.example.serviciosya.data.billing.SubscriptionStore
import com.example.serviciosya.domain.billing.Entitlements
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemorySubscriptionStore : SubscriptionStore {
    private val data = MutableStateFlow<Map<String, Entitlements>>(emptyMap())
    var loadFails = false
    var saveFails = false
    var saveCalls = 0
        private set

    fun put(userId: String, entitlements: Entitlements) {
        data.value = data.value + (userId to entitlements)
    }

    fun current(userId: String): Entitlements = data.value[userId] ?: Entitlements.NONE

    override fun observe(userId: String): Flow<Entitlements> = data.map { it[userId] ?: Entitlements.NONE }

    override suspend fun load(userId: String): Result<Entitlements> =
        if (loadFails) Result.failure(IllegalStateException("load failed")) else Result.success(current(userId))

    override suspend fun save(userId: String, entitlements: Entitlements): Result<Unit> {
        saveCalls++
        if (saveFails) return Result.failure(IllegalStateException("save failed"))
        put(userId, entitlements)
        return Result.success(Unit)
    }
}

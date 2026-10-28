package com.example.serviciosya.testutil

import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.PurchaseResult
import com.example.serviciosya.domain.billing.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A successful purchase grants the plan, like the real repositories do. */
class FakeBillingRepository(
    initial: Entitlements = Entitlements.NONE,
    private val now: () -> Long = { NOW },
) : BillingRepository {
    val entitlements = MutableStateFlow(initial)
    var purchaseResult: PurchaseResult = PurchaseResult.Success
    var restoreResult: Result<Entitlements>? = null

    val purchaseCalls = mutableListOf<PlanId>()
    var restoreCalls = 0
        private set

    override fun observeSubscription(): Flow<Entitlements> = entitlements

    override suspend fun purchase(plan: PlanId): PurchaseResult {
        purchaseCalls += plan
        if (purchaseResult == PurchaseResult.Success) {
            entitlements.value = entitlements.value.withPurchase(Subscription.start(PricingCatalog.plan(plan), now()))
        }
        return purchaseResult
    }

    override suspend fun restore(): Result<Entitlements> {
        restoreCalls++
        return restoreResult ?: Result.success(entitlements.value)
    }

    companion object {
        /** Fixed "now" shared by the tests that use this fake. */
        const val NOW = 1_000_000_000_000L
    }
}

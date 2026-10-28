package com.example.serviciosya.data.billing

import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.billing.CheckoutOutcome
import com.example.serviciosya.domain.billing.Audience
import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.PendingCheckout
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.PurchaseResult
import com.example.serviciosya.domain.billing.SimulatedCheckout
import com.example.serviciosya.domain.billing.Subscription
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.AuthRepository
import com.example.serviciosya.domain.repository.ProviderAccountRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Simulated payment gateway: no real money and no card data. [purchase] waits until the user picks
 * the result on the simulated payment screen (see [SimulatedCheckout]). Only an approved payment
 * grants the plan.
 */
class SimulatedBillingRepository(
    private val auth: AuthRepository,
    private val store: SubscriptionStore,
    private val providerAccounts: ProviderAccountRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : BillingRepository, SimulatedCheckout {
    private val _pending = MutableStateFlow<PendingCheckout?>(null)
    override val pending: StateFlow<PendingCheckout?> = _pending.asStateFlow()

    private var waiting: CompletableDeferred<CheckoutOutcome>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSubscription(): Flow<Entitlements> =
        auth.observeCurrentUser().flatMapLatest { user ->
            if (user == null) flowOf(Entitlements.NONE) else store.observe(user.id)
        }

    override suspend fun purchase(plan: PlanId): PurchaseResult {
        val user = auth.observeCurrentUser().first() ?: return PurchaseResult.Error(REASON_NOT_SIGNED_IN)
        if (waiting != null) return PurchaseResult.Error(REASON_BUSY)
        val catalogPlan = PricingCatalog.plan(plan)

        // A provider plan needs a provider profile; check it before asking for any payment.
        val provider: Provider? = if (catalogPlan.audience == Audience.PROVIDER) {
            providerAccounts.findByUserId(user.id)
                .getOrElse { return PurchaseResult.Error(REASON_NETWORK) }
                ?: return PurchaseResult.Error(REASON_NOT_A_PROVIDER)
        } else {
            null
        }

        return when (awaitOutcome(PendingCheckout(catalogPlan))) {
            CheckoutOutcome.APPROVED -> grant(user.id, plan, provider)
            CheckoutOutcome.DECLINED -> PurchaseResult.Error(REASON_DECLINED)
            CheckoutOutcome.PENDING -> PurchaseResult.Pending
            CheckoutOutcome.CANCELLED -> PurchaseResult.Cancelled
        }
    }

    override fun resolve(outcome: CheckoutOutcome) {
        waiting?.complete(outcome)
    }

    override suspend fun restore(): Result<Entitlements> {
        val user = auth.observeCurrentUser().first()
            ?: return Result.failure(IllegalStateException(REASON_NOT_SIGNED_IN))
        return store.load(user.id).onSuccess { entitlements ->
            // Best effort: bring the provider profile back in line with the plan that is still valid.
            val featured = entitlements.featured
            val until = featured?.expiresAtMillis
            if (featured != null && until != null && featured.isActive(now())) {
                providerAccounts.findByUserId(user.id).getOrNull()?.let {
                    providerAccounts.setFeaturedUntil(it.id, until)
                }
            }
        }
    }

    private suspend fun awaitOutcome(checkout: PendingCheckout): CheckoutOutcome {
        val deferred = CompletableDeferred<CheckoutOutcome>()
        waiting = deferred
        _pending.value = checkout
        try {
            return deferred.await()
        } finally {
            waiting = null
            _pending.value = null
        }
    }

    private suspend fun grant(userId: String, planId: PlanId, provider: Provider?): PurchaseResult {
        val subscription = Subscription.start(PricingCatalog.plan(planId), now())
        val current = store.load(userId).getOrElse { return PurchaseResult.Error(REASON_STORE) }
        store.save(userId, current.withPurchase(subscription)).onFailure {
            return PurchaseResult.Error(REASON_STORE)
        }
        val until = subscription.expiresAtMillis
        if (provider != null && until != null) {
            providerAccounts.setFeaturedUntil(provider.id, until).onFailure {
                // The plan is saved; restoring purchases retries this step.
                return PurchaseResult.Error(REASON_SYNC)
            }
        }
        return PurchaseResult.Success
    }

    companion object {
        const val REASON_NOT_SIGNED_IN = "not_signed_in"
        const val REASON_NOT_A_PROVIDER = "not_a_provider"
        const val REASON_DECLINED = "declined"
        const val REASON_BUSY = "busy"
        const val REASON_NETWORK = "network"
        const val REASON_STORE = "store"
        const val REASON_SYNC = "sync"
    }
}

package com.example.serviciosya.domain.billing

import kotlinx.coroutines.flow.Flow

/** Outcome of a purchase. Only [Success] grants the plan. */
sealed interface PurchaseResult {
    data object Success : PurchaseResult

    data object Cancelled : PurchaseResult

    /** The payment is not confirmed yet: nothing is granted until it is. */
    data object Pending : PurchaseResult

    /** [reason] is a short code such as "declined" or "not_a_provider", never free text. */
    data class Error(val reason: String) : PurchaseResult
}

/**
 * Plans of the current user. Today it is backed by a simulated gateway; Google Play Billing can
 * replace it without changes to the domain or the screens.
 */
interface BillingRepository {
    /** What the signed-in user has bought; [Entitlements.NONE] without a session. */
    fun observeSubscription(): Flow<Entitlements>

    suspend fun purchase(plan: PlanId): PurchaseResult

    /** Reloads the purchases of the account, including the lifetime plan. */
    suspend fun restore(): Result<Entitlements>
}

package com.example.serviciosya.domain.billing

import kotlinx.coroutines.flow.Flow

/** Outcome of a purchase. Only [Success] grants the plan. */
sealed interface PurchaseResult {
    data object Success : PurchaseResult

    data object Cancelled : PurchaseResult

    /** The payment is not confirmed yet: nothing is granted until it is. */
    data object Pending : PurchaseResult

    /** [reason] is a short code such as [DECLINED] or [NOT_A_PROVIDER], never free text. */
    data class Error(val reason: String) : PurchaseResult {
        companion object {
            const val NOT_SIGNED_IN = "not_signed_in"
            const val NOT_A_PROVIDER = "not_a_provider"
            const val DECLINED = "declined"
            const val BUSY = "busy"
            const val NETWORK = "network"
            const val STORE = "store"
            const val SYNC = "sync"
            const val UNAVAILABLE = "unavailable"
        }
    }
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

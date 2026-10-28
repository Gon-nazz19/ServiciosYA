package com.example.serviciosya.domain.billing

import kotlinx.coroutines.flow.StateFlow

/** What the simulated payment screen lets the user choose as the result of the payment. */
enum class CheckoutOutcome { APPROVED, DECLINED, PENDING, CANCELLED }

data class PendingCheckout(val plan: Plan)

/**
 * Only the simulated gateway has this: a payment waiting for the user to pick its result on the
 * simulated payment screen. Google Play Billing shows its own sheet and does not need it.
 */
interface SimulatedCheckout {
    val pending: StateFlow<PendingCheckout?>

    fun resolve(outcome: CheckoutOutcome)
}

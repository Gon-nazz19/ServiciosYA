package com.example.serviciosya.presentation.billing

import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PurchaseResult

/** Value sent to Analytics as the result of a subscription. */
fun PurchaseResult.analyticsValue(): String = when (this) {
    PurchaseResult.Success -> "success"
    PurchaseResult.Cancelled -> "cancelled"
    PurchaseResult.Pending -> "pending"
    is PurchaseResult.Error -> "error"
}

fun PurchaseResult.userMessage(): String = when (this) {
    PurchaseResult.Success -> "¡Listo! Tu plan ya está activo."
    PurchaseResult.Cancelled -> "Cancelaste el pago. No se cobró nada."
    PurchaseResult.Pending -> "Tu pago quedó pendiente. Todavía no activamos el plan."
    is PurchaseResult.Error -> when (reason) {
        PurchaseResult.Error.DECLINED -> "Rechazaron el pago. Probá de nuevo."
        PurchaseResult.Error.NOT_A_PROVIDER -> "Tu cuenta no está asociada a un prestador."
        PurchaseResult.Error.NETWORK -> "No pudimos conectarnos. Revisá tu conexión."
        PurchaseResult.Error.STORE, PurchaseResult.Error.SYNC ->
            "No pudimos guardar tu plan. Probá de nuevo o tocá Restaurar compras."
        else -> "No pudimos procesar el pago. Probá de nuevo."
    }
}

/** Buys [plan] and reports the start and the result to Analytics, without any user data. */
suspend fun BillingRepository.purchaseTracked(plan: PlanId, analytics: AnalyticsTracker): PurchaseResult {
    analytics.trackSubscribeStart(plan.value)
    val result = purchase(plan)
    analytics.trackSubscribeResult(plan.value, result.analyticsValue())
    return result
}

package com.example.serviciosya.presentation.billing

import com.example.serviciosya.domain.billing.Plan
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.Subscription
import com.example.serviciosya.domain.billing.formatPesos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The plan of the user as the screens need it, already resolved against the current time. */
data class PlanStatus(
    val plan: PlanId,
    val isInTrial: Boolean,
    /** Null for the lifetime plan. */
    val expiresAtMillis: Long?,
)

/** The status of an active subscription, or null when there is none or it already expired. */
fun Subscription?.statusAt(nowMillis: Long): PlanStatus? =
    this?.takeIf { it.isActive(nowMillis) }
        ?.let { PlanStatus(it.plan, it.isInTrial(nowMillis), it.expiresAtMillis) }

fun planPeriodLabel(plan: Plan): String = when (plan.id) {
    PlanId.FEATURED_WEEKLY -> "por semana"
    PlanId.FEATURED_MONTHLY -> "por mes"
    PlanId.FEATURED_YEARLY, PlanId.ADFREE_YEARLY -> "por año"
    PlanId.ADFREE_LIFETIME -> "pago único"
}

fun planHighlights(plan: Plan): List<String> {
    val featured = "Insignia \"Destacado\" y lugar prioritario en el listado"
    return when (plan.id) {
        PlanId.FEATURED_WEEKLY -> listOf(featured)
        PlanId.FEATURED_MONTHLY -> listOf("${plan.trialDays} días de prueba gratis", featured)
        PlanId.FEATURED_YEARLY -> listOf(
            "Equivale a ${formatPesos(PricingCatalog.monthlyEquivalent(plan) ?: plan.priceArs)} por mes",
            "Ahorrás ${formatPesos(PricingCatalog.featuredYearlySavings)} frente al plan mensual",
            featured,
        )
        PlanId.ADFREE_YEARLY -> listOf("Sin anuncios en los listados durante 1 año", "Se renueva cada año")
        PlanId.ADFREE_LIFETIME -> listOf(
            "Sin anuncios para siempre",
            "Pago único: nunca vence",
            "Equivale a ${PricingCatalog.lifetimeEqualsYears} años del plan anual",
        )
    }
}

fun planActionLabel(plan: Plan): String =
    if (plan.trialDays > 0) "Probar ${plan.trialDays} días gratis" else "Elegir plan"

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-AR")).format(Date(millis))

fun statusText(status: PlanStatus): String = when {
    status.expiresAtMillis == null -> "Plan de por vida: nunca vence"
    status.isInTrial -> "Prueba gratis activa · el plan empieza a cobrarse el ${formatDate(status.expiresAtMillis)}"
    else -> "Plan activo hasta el ${formatDate(status.expiresAtMillis)}"
}

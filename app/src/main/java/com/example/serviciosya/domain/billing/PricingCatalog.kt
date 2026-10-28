package com.example.serviciosya.domain.billing

import java.util.Locale

enum class Audience { PROVIDER, CLIENT }

/** [value] is what gets stored and sent to Analytics, so it must never change. */
enum class PlanId(val value: String, val audience: Audience) {
    FEATURED_WEEKLY("featured_weekly", Audience.PROVIDER),
    FEATURED_MONTHLY("featured_monthly", Audience.PROVIDER),
    FEATURED_YEARLY("featured_yearly", Audience.PROVIDER),
    ADFREE_YEARLY("adfree_yearly", Audience.CLIENT),
    ADFREE_LIFETIME("adfree_lifetime", Audience.CLIENT),
    ;

    companion object {
        fun fromValue(value: String?): PlanId? = entries.firstOrNull { it.value == value }
    }
}

data class Plan(
    val id: PlanId,
    val title: String,
    /** Reference price in Argentine pesos. */
    val priceArs: Int,
    /** Null for the lifetime plan: a single payment that never expires. */
    val periodDays: Int?,
    val trialDays: Int = 0,
    val isBestOffer: Boolean = false,
) {
    val audience: Audience get() = id.audience
}

object PricingCatalog {
    val featuredWeekly = Plan(PlanId.FEATURED_WEEKLY, "Destacado semanal", priceArs = 3_999, periodDays = 7)
    val featuredMonthly = Plan(
        PlanId.FEATURED_MONTHLY,
        "Destacado mensual",
        priceArs = 9_999,
        periodDays = 30,
        trialDays = 7,
        isBestOffer = true,
    )
    val featuredYearly = Plan(PlanId.FEATURED_YEARLY, "Destacado anual", priceArs = 99_999, periodDays = 365)
    val adFreeYearly = Plan(PlanId.ADFREE_YEARLY, "Sin anuncios anual", priceArs = 4_999, periodDays = 365)
    val adFreeLifetime = Plan(PlanId.ADFREE_LIFETIME, "Sin anuncios de por vida", priceArs = 14_999, periodDays = null)

    val providerPlans = listOf(featuredWeekly, featuredMonthly, featuredYearly)
    val clientPlans = listOf(adFreeYearly, adFreeLifetime)
    val all = providerPlans + clientPlans

    fun plan(id: PlanId): Plan = all.first { it.id == id }

    /** Price per month of the yearly and monthly plans, or null for the others. */
    fun monthlyEquivalent(plan: Plan): Int? = when (plan.id) {
        PlanId.FEATURED_YEARLY, PlanId.ADFREE_YEARLY -> plan.priceArs / MONTHS_PER_YEAR
        PlanId.FEATURED_MONTHLY -> plan.priceArs
        else -> null
    }

    /** What the yearly provider plan saves compared with paying the monthly plan twelve times. */
    val featuredYearlySavings: Int = MONTHS_PER_YEAR * featuredMonthly.priceArs - featuredYearly.priceArs

    /** The lifetime plan costs as much as this many years of the yearly client plan. */
    val lifetimeEqualsYears: Int = adFreeLifetime.priceArs / adFreeYearly.priceArs

    private const val MONTHS_PER_YEAR = 12
}

/** "$ 14.999", with the Argentine thousands dot. */
fun formatPesos(amount: Int): String = "$ " + String.format(Locale.forLanguageTag("es-AR"), "%,d", amount)

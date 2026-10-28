package com.example.serviciosya.domain.billing

/** A plan the user bought. [expiresAtMillis] is null for the lifetime plan, which never expires. */
data class Subscription(
    val plan: PlanId,
    val startedAtMillis: Long,
    val trialEndsAtMillis: Long? = null,
    val expiresAtMillis: Long?,
) {
    fun isActive(nowMillis: Long): Boolean = expiresAtMillis == null || nowMillis < expiresAtMillis

    fun isInTrial(nowMillis: Long): Boolean =
        trialEndsAtMillis != null && nowMillis < trialEndsAtMillis && isActive(nowMillis)

    companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000

        /** Starts [plan] at [nowMillis]: the free trial comes first and the paid period starts after it. */
        fun start(plan: Plan, nowMillis: Long): Subscription {
            val trialEnd = if (plan.trialDays > 0) nowMillis + plan.trialDays * DAY_MILLIS else null
            val expiresAt = plan.periodDays?.let { (trialEnd ?: nowMillis) + it * DAY_MILLIS }
            return Subscription(plan.id, nowMillis, trialEnd, expiresAt)
        }
    }
}

/** What the user has bought: one slot for the client plans and one for the provider plans. */
data class Entitlements(
    val adFree: Subscription? = null,
    val featured: Subscription? = null,
) {
    /** Ads are hidden with an active client plan or an active provider plan. */
    fun isAdFree(nowMillis: Long): Boolean =
        adFree?.isActive(nowMillis) == true || featured?.isActive(nowMillis) == true

    fun isFeatured(nowMillis: Long): Boolean = featured?.isActive(nowMillis) == true

    val hasLifetimeAdFree: Boolean get() = adFree?.plan == PlanId.ADFREE_LIFETIME

    /** The lifetime plan prevails: buying the yearly one afterwards never downgrades it. */
    fun withPurchase(subscription: Subscription): Entitlements = when {
        subscription.plan.audience == Audience.PROVIDER -> copy(featured = subscription)
        hasLifetimeAdFree && subscription.plan != PlanId.ADFREE_LIFETIME -> this
        else -> copy(adFree = subscription)
    }

    companion object {
        val NONE = Entitlements()
    }
}

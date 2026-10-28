package com.example.serviciosya.domain.billing

import com.example.serviciosya.domain.billing.Subscription.Companion.DAY_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsTest {
    private val now = 1_000_000_000_000L

    private fun subscription(plan: Plan, startedAt: Long = now) = Subscription.start(plan, startedAt)

    @Test
    fun `without plans ads are shown and nobody is featured`() {
        assertFalse(Entitlements.NONE.isAdFree(now))
        assertFalse(Entitlements.NONE.isFeatured(now))
        assertFalse(Entitlements.NONE.hasLifetimeAdFree)
    }

    @Test
    fun `an active yearly client plan hides ads`() {
        val entitlements = Entitlements.NONE.withPurchase(subscription(PricingCatalog.adFreeYearly))

        assertTrue(entitlements.isAdFree(now + 100 * DAY_MILLIS))
        assertFalse(entitlements.isFeatured(now))
    }

    @Test
    fun `an expired yearly client plan shows ads again`() {
        val entitlements = Entitlements.NONE.withPurchase(subscription(PricingCatalog.adFreeYearly))

        assertFalse(entitlements.isAdFree(now + 366 * DAY_MILLIS))
    }

    @Test
    fun `the lifetime plan hides ads forever`() {
        val entitlements = Entitlements.NONE.withPurchase(subscription(PricingCatalog.adFreeLifetime))

        assertTrue(entitlements.hasLifetimeAdFree)
        assertTrue(entitlements.isAdFree(now + 50 * 365 * DAY_MILLIS))
    }

    @Test
    fun `buying the lifetime plan while on the yearly one replaces it`() {
        val entitlements = Entitlements.NONE
            .withPurchase(subscription(PricingCatalog.adFreeYearly))
            .withPurchase(subscription(PricingCatalog.adFreeLifetime))

        assertEquals(PlanId.ADFREE_LIFETIME, entitlements.adFree?.plan)
    }

    @Test
    fun `the yearly plan never downgrades the lifetime one`() {
        val lifetime = Entitlements.NONE.withPurchase(subscription(PricingCatalog.adFreeLifetime))

        val after = lifetime.withPurchase(subscription(PricingCatalog.adFreeYearly))

        assertEquals(PlanId.ADFREE_LIFETIME, after.adFree?.plan)
        assertTrue(after.isAdFree(now + 10 * 365 * DAY_MILLIS))
    }

    @Test
    fun `an active provider plan features the provider and also hides ads`() {
        val entitlements = Entitlements.NONE.withPurchase(subscription(PricingCatalog.featuredWeekly))

        assertTrue(entitlements.isFeatured(now))
        assertTrue(entitlements.isAdFree(now))
        assertEquals(null, entitlements.adFree)
    }

    @Test
    fun `an expired provider plan neither features nor hides ads`() {
        val entitlements = Entitlements.NONE.withPurchase(subscription(PricingCatalog.featuredWeekly))

        assertFalse(entitlements.isFeatured(now + 8 * DAY_MILLIS))
        assertFalse(entitlements.isAdFree(now + 8 * DAY_MILLIS))
    }

    @Test
    fun `client and provider plans live in their own slots`() {
        val entitlements = Entitlements.NONE
            .withPurchase(subscription(PricingCatalog.adFreeYearly))
            .withPurchase(subscription(PricingCatalog.featuredMonthly))

        assertEquals(PlanId.ADFREE_YEARLY, entitlements.adFree?.plan)
        assertEquals(PlanId.FEATURED_MONTHLY, entitlements.featured?.plan)
    }
}

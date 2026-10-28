package com.example.serviciosya.domain.billing

import com.example.serviciosya.domain.billing.Subscription.Companion.DAY_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionStateTest {
    private val now = 1_000_000_000_000L

    @Test
    fun `a yearly plan is active until a year from the purchase`() {
        val subscription = Subscription.start(PricingCatalog.adFreeYearly, now)

        assertEquals(now + 365 * DAY_MILLIS, subscription.expiresAtMillis)
        assertNull(subscription.trialEndsAtMillis)
        assertTrue(subscription.isActive(now))
        assertTrue(subscription.isActive(now + 364 * DAY_MILLIS))
    }

    @Test
    fun `an expired plan is not active`() {
        val subscription = Subscription.start(PricingCatalog.featuredWeekly, now)

        assertTrue(subscription.isActive(now + 6 * DAY_MILLIS))
        assertFalse(subscription.isActive(now + 7 * DAY_MILLIS))
        assertFalse(subscription.isActive(now + 30 * DAY_MILLIS))
    }

    @Test
    fun `the monthly plan starts with a free trial and the paid month comes after it`() {
        val subscription = Subscription.start(PricingCatalog.featuredMonthly, now)

        assertEquals(now + 7 * DAY_MILLIS, subscription.trialEndsAtMillis)
        assertEquals(now + 37 * DAY_MILLIS, subscription.expiresAtMillis)
        assertTrue(subscription.isInTrial(now))
        assertTrue(subscription.isInTrial(now + 6 * DAY_MILLIS))
        assertFalse(subscription.isInTrial(now + 7 * DAY_MILLIS))
        assertTrue(subscription.isActive(now + 36 * DAY_MILLIS))
        assertFalse(subscription.isActive(now + 37 * DAY_MILLIS))
    }

    @Test
    fun `a plan without trial is never in trial`() {
        val subscription = Subscription.start(PricingCatalog.featuredYearly, now)

        assertFalse(subscription.isInTrial(now))
    }

    @Test
    fun `an expired plan is not in trial`() {
        val subscription = Subscription(PlanId.FEATURED_MONTHLY, now, trialEndsAtMillis = now + 10, expiresAtMillis = now + 5)

        assertTrue(subscription.isInTrial(now + 1))
        assertFalse(subscription.isInTrial(now + 6))
    }

    @Test
    fun `the lifetime plan never expires`() {
        val subscription = Subscription.start(PricingCatalog.adFreeLifetime, now)

        assertNull(subscription.expiresAtMillis)
        assertTrue(subscription.isActive(now))
        assertTrue(subscription.isActive(now + 100 * 365 * DAY_MILLIS))
        assertFalse(subscription.isInTrial(now))
    }
}

package com.example.serviciosya.domain.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingCatalogTest {
    @Test
    fun `provider plans have the reference prices`() {
        assertEquals(
            listOf(3_999, 9_999, 99_999),
            PricingCatalog.providerPlans.map { it.priceArs },
        )
        assertTrue(PricingCatalog.providerPlans.all { it.audience == Audience.PROVIDER })
    }

    @Test
    fun `client plans have the reference prices`() {
        assertEquals(listOf(4_999, 14_999), PricingCatalog.clientPlans.map { it.priceArs })
        assertTrue(PricingCatalog.clientPlans.all { it.audience == Audience.CLIENT })
    }

    @Test
    fun `only the monthly provider plan is the best offer and has a seven day trial`() {
        assertEquals(listOf(PlanId.FEATURED_MONTHLY), PricingCatalog.all.filter { it.isBestOffer }.map { it.id })
        assertEquals(7, PricingCatalog.featuredMonthly.trialDays)
        assertEquals(0, PricingCatalog.featuredWeekly.trialDays)
        assertEquals(0, PricingCatalog.featuredYearly.trialDays)
    }

    @Test
    fun `the lifetime plan is a single payment with no period`() {
        assertNull(PricingCatalog.adFreeLifetime.periodDays)
        assertEquals(365, PricingCatalog.adFreeYearly.periodDays)
    }

    @Test
    fun `yearly provider plan is worth 8333 per month`() {
        assertEquals(8_333, PricingCatalog.monthlyEquivalent(PricingCatalog.featuredYearly))
        assertEquals(9_999, PricingCatalog.monthlyEquivalent(PricingCatalog.featuredMonthly))
        assertNull(PricingCatalog.monthlyEquivalent(PricingCatalog.featuredWeekly))
        assertNull(PricingCatalog.monthlyEquivalent(PricingCatalog.adFreeLifetime))
    }

    @Test
    fun `yearly provider plan saves 19989 against twelve monthly payments`() {
        assertEquals(19_989, PricingCatalog.featuredYearlySavings)
    }

    @Test
    fun `lifetime plan equals three years of the yearly client plan`() {
        assertEquals(3, PricingCatalog.lifetimeEqualsYears)
    }

    @Test
    fun `plan ids are unique and found by id and by stored value`() {
        assertEquals(PlanId.entries.size, PricingCatalog.all.map { it.id }.toSet().size)
        PlanId.entries.forEach { id ->
            assertEquals(id, PricingCatalog.plan(id).id)
            assertEquals(id, PlanId.fromValue(id.value))
        }
        assertNull(PlanId.fromValue("unknown"))
        assertNull(PlanId.fromValue(null))
    }

    @Test
    fun `prices are formatted with the argentine thousands dot`() {
        assertEquals("$ 3.999", formatPesos(3_999))
        assertEquals("$ 99.999", formatPesos(99_999))
        assertEquals("$ 14.999", formatPesos(14_999))
        assertFalse(formatPesos(500).contains('.'))
    }
}

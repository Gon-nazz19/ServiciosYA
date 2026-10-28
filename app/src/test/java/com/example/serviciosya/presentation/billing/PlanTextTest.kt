package com.example.serviciosya.presentation.billing

import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.PurchaseResult
import com.example.serviciosya.domain.billing.Subscription
import com.example.serviciosya.domain.billing.Subscription.Companion.DAY_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTextTest {
    private val now = 1_000_000_000_000L

    @Test
    fun `every plan says how often it is charged`() {
        assertEquals("por semana", planPeriodLabel(PricingCatalog.featuredWeekly))
        assertEquals("por mes", planPeriodLabel(PricingCatalog.featuredMonthly))
        assertEquals("por año", planPeriodLabel(PricingCatalog.featuredYearly))
        assertEquals("por año", planPeriodLabel(PricingCatalog.adFreeYearly))
        assertEquals("pago único", planPeriodLabel(PricingCatalog.adFreeLifetime))
    }

    @Test
    fun `the monthly plan advertises its free trial`() {
        assertTrue(planHighlights(PricingCatalog.featuredMonthly).contains("7 días de prueba gratis"))
        assertEquals("Probar 7 días gratis", planActionLabel(PricingCatalog.featuredMonthly))
        assertEquals("Elegir plan", planActionLabel(PricingCatalog.featuredWeekly))
    }

    @Test
    fun `the yearly provider plan shows the monthly equivalent and the savings`() {
        val lines = planHighlights(PricingCatalog.featuredYearly)

        assertTrue(lines.contains("Equivale a $ 8.333 por mes"))
        assertTrue(lines.contains("Ahorrás $ 19.989 frente al plan mensual"))
    }

    @Test
    fun `the lifetime plan says it never expires and equals three years of the yearly one`() {
        val lines = planHighlights(PricingCatalog.adFreeLifetime)

        assertTrue(lines.contains("Pago único: nunca vence"))
        assertTrue(lines.contains("Equivale a 3 años del plan anual"))
    }

    @Test
    fun `only active subscriptions have a status`() {
        val active = Subscription.start(PricingCatalog.adFreeYearly, now)

        assertEquals(PlanId.ADFREE_YEARLY, active.statusAt(now)?.plan)
        assertNull(active.statusAt(now + 366 * DAY_MILLIS))
        assertNull((null as Subscription?).statusAt(now))
    }

    @Test
    fun `the status says whether the user is in the free trial`() {
        val trial = Subscription.start(PricingCatalog.featuredMonthly, now)

        assertTrue(trial.statusAt(now)?.isInTrial == true)
        assertFalse(trial.statusAt(now + 8 * DAY_MILLIS)?.isInTrial == true)
    }

    @Test
    fun `the lifetime status never expires`() {
        val status = Subscription.start(PricingCatalog.adFreeLifetime, now).statusAt(now + 100 * 365 * DAY_MILLIS)

        assertNull(status?.expiresAtMillis)
        assertEquals("Plan de por vida: nunca vence", statusText(requireNotNull(status)))
    }

    @Test
    fun `results are tracked with short stable values`() {
        assertEquals("success", PurchaseResult.Success.analyticsValue())
        assertEquals("cancelled", PurchaseResult.Cancelled.analyticsValue())
        assertEquals("pending", PurchaseResult.Pending.analyticsValue())
        assertEquals("error", PurchaseResult.Error(PurchaseResult.Error.DECLINED).analyticsValue())
        assertEquals("error", PurchaseResult.Error("anything").analyticsValue())
    }

    @Test
    fun `every result has a message for the user`() {
        assertTrue(PurchaseResult.Pending.userMessage().contains("pendiente"))
        assertTrue(PurchaseResult.Error(PurchaseResult.Error.DECLINED).userMessage().contains("Rechazaron"))
        assertTrue(PurchaseResult.Error(PurchaseResult.Error.NOT_A_PROVIDER).userMessage().contains("prestador"))
        assertTrue(PurchaseResult.Error("unknown").userMessage().contains("No pudimos procesar"))
        assertTrue(PurchaseResult.Cancelled.userMessage().contains("No se cobró nada"))
    }
}

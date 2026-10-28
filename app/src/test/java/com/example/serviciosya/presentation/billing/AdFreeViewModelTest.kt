package com.example.serviciosya.presentation.billing

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.PurchaseResult
import com.example.serviciosya.domain.billing.Subscription
import com.example.serviciosya.domain.billing.Subscription.Companion.DAY_MILLIS
import com.example.serviciosya.testutil.FakeAnalyticsTracker
import com.example.serviciosya.testutil.FakeBillingRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdFreeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = FakeBillingRepository.NOW
    private val billing = FakeBillingRepository()
    private val analytics = FakeAnalyticsTracker()

    private fun createViewModel() = AdFreeViewModel(billing, analytics) { now }

    @Test
    fun `opening the screen tracks the paywall view without user data`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val event = analytics.eventsNamed(AnalyticsEvents.PAYWALL_VIEW).single()
        assertEquals(mapOf(AnalyticsParams.SOURCE to "ad_free"), event.params)
    }

    @Test
    fun `without a plan there is no status`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.hasLifetime)
    }

    @Test
    fun `the yearly plan becomes active and is tracked`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.purchase(PlanId.ADFREE_YEARLY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PlanId.ADFREE_YEARLY, state.status?.plan)
        assertEquals(now + 365 * DAY_MILLIS, state.status?.expiresAtMillis)
        assertFalse(state.hasLifetime)
        assertEquals(PurchaseResult.Success.userMessage(), state.message)
        assertEquals(
            mapOf(AnalyticsParams.PLAN to "adfree_yearly", AnalyticsParams.RESULT to "success"),
            analytics.eventsNamed(AnalyticsEvents.SUBSCRIBE_RESULT).single().params,
        )
    }

    @Test
    fun `the lifetime plan never expires`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.purchase(PlanId.ADFREE_LIFETIME)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasLifetime)
        assertNull(state.status?.expiresAtMillis)
    }

    @Test
    fun `the lifetime plan prevails over the yearly one`() = runTest {
        billing.entitlements.value = Entitlements(adFree = Subscription.start(PricingCatalog.adFreeYearly, now))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.purchase(PlanId.ADFREE_LIFETIME)
        advanceUntilIdle()
        assertEquals(PlanId.ADFREE_LIFETIME, viewModel.uiState.value.status?.plan)

        viewModel.purchase(PlanId.ADFREE_YEARLY)
        advanceUntilIdle()
        assertEquals(PlanId.ADFREE_LIFETIME, viewModel.uiState.value.status?.plan)
        assertTrue(viewModel.uiState.value.hasLifetime)
    }

    @Test
    fun `pending, declined and cancelled payments grant nothing and are tracked`() = runTest {
        val cases = listOf(
            PurchaseResult.Pending to "pending",
            PurchaseResult.Error(PurchaseResult.Error.DECLINED) to "error",
            PurchaseResult.Cancelled to "cancelled",
        )
        cases.forEach { (result, tracked) ->
            billing.purchaseResult = result
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.purchase(PlanId.ADFREE_LIFETIME)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.status)
            assertEquals(result.userMessage(), viewModel.uiState.value.message)
            assertEquals(tracked, analytics.eventsNamed(AnalyticsEvents.SUBSCRIBE_RESULT).last().params[AnalyticsParams.RESULT])
        }
    }

    @Test
    fun `an expired yearly plan is not shown as active`() = runTest {
        billing.entitlements.value = Entitlements(
            adFree = Subscription.start(PricingCatalog.adFreeYearly, now - 400 * DAY_MILLIS),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.status)
    }

    @Test
    fun `restoring brings back the lifetime plan`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.restore()
        advanceUntilIdle()
        assertEquals(AdFreeViewModel.NOTHING_TO_RESTORE_MESSAGE, viewModel.uiState.value.message)

        billing.entitlements.value = Entitlements(adFree = Subscription.start(PricingCatalog.adFreeLifetime, now))
        viewModel.restore()
        advanceUntilIdle()

        assertEquals(AdFreeViewModel.RESTORED_MESSAGE, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.hasLifetime)
    }

    @Test
    fun `a failed restore shows an error and the message is cleared once shown`() = runTest {
        billing.restoreResult = Result.failure(IllegalStateException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()
        assertEquals(AdFreeViewModel.RESTORE_ERROR_MESSAGE, viewModel.uiState.value.message)

        viewModel.onMessageShown()
        assertNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isBusy)
    }
}

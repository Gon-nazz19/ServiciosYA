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
import com.example.serviciosya.testutil.FakeProviderAccountRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyPlanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = FakeBillingRepository.NOW
    private val provider = testProvider(id = "p1", name = "Carlos Electricidad")
    private val billing = FakeBillingRepository()
    private val accounts = FakeProviderAccountRepository(provider)
    private val analytics = FakeAnalyticsTracker()

    private fun createViewModel() = MyPlanViewModel(
        userId = provider.userId,
        billing = billing,
        providerAccounts = accounts,
        analytics = analytics,
        now = { now },
    )

    @Test
    fun `a provider account loads its profile`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isProviderAccount)
        assertEquals("Carlos Electricidad", state.providerName)
    }

    @Test
    fun `an account without a provider profile is not a provider account`() = runTest {
        accounts.provider = null

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isProviderAccount)
    }

    @Test
    fun `a failed lookup shows a message`() = runTest {
        accounts.findFails = true

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProviderAccount)
        assertEquals(MyPlanViewModel.LOAD_ERROR_MESSAGE, viewModel.uiState.value.message)
    }

    @Test
    fun `opening the screen tracks the paywall view once without user data`() = runTest {
        createViewModel()
        advanceUntilIdle()

        val event = analytics.eventsNamed(AnalyticsEvents.PAYWALL_VIEW).single()
        assertEquals(mapOf(AnalyticsParams.SOURCE to "my_plan"), event.params)
    }

    @Test
    fun `an approved plan becomes active and is tracked`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.purchase(PlanId.FEATURED_WEEKLY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PlanId.FEATURED_WEEKLY, state.status?.plan)
        assertEquals(now + 7 * DAY_MILLIS, state.status?.expiresAtMillis)
        assertFalse(state.isBusy)
        assertEquals(PurchaseResult.Success.userMessage(), state.message)
        assertEquals(listOf(PlanId.FEATURED_WEEKLY), billing.purchaseCalls)
        assertEquals(
            mapOf(AnalyticsParams.PLAN to "featured_weekly"),
            analytics.eventsNamed(AnalyticsEvents.SUBSCRIBE_START).single().params,
        )
        assertEquals(
            mapOf(AnalyticsParams.PLAN to "featured_weekly", AnalyticsParams.RESULT to "success"),
            analytics.eventsNamed(AnalyticsEvents.SUBSCRIBE_RESULT).single().params,
        )
    }

    @Test
    fun `the monthly plan starts in its free trial`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.purchase(PlanId.FEATURED_MONTHLY)
        advanceUntilIdle()

        val status = viewModel.uiState.value.status
        assertEquals(PlanId.FEATURED_MONTHLY, status?.plan)
        assertEquals(true, status?.isInTrial)
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

            viewModel.purchase(PlanId.FEATURED_YEARLY)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.status)
            assertEquals(result.userMessage(), viewModel.uiState.value.message)
            assertEquals(tracked, analytics.eventsNamed(AnalyticsEvents.SUBSCRIBE_RESULT).last().params[AnalyticsParams.RESULT])
        }
    }

    @Test
    fun `an expired plan is not shown as active`() = runTest {
        billing.entitlements.value = Entitlements(
            featured = Subscription.start(PricingCatalog.featuredWeekly, now - 30 * DAY_MILLIS),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.status)
    }

    @Test
    fun `restoring finds the active plan or says there is nothing to restore`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()
        assertEquals(MyPlanViewModel.NOTHING_TO_RESTORE_MESSAGE, viewModel.uiState.value.message)

        billing.entitlements.value = Entitlements(featured = Subscription.start(PricingCatalog.featuredYearly, now))
        viewModel.restore()
        advanceUntilIdle()
        assertEquals(MyPlanViewModel.RESTORED_MESSAGE, viewModel.uiState.value.message)
        assertEquals(2, billing.restoreCalls)
    }

    @Test
    fun `a failed restore shows an error`() = runTest {
        billing.restoreResult = Result.failure(IllegalStateException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.restore()
        advanceUntilIdle()

        assertEquals(MyPlanViewModel.RESTORE_ERROR_MESSAGE, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `the message is cleared once shown`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.purchase(PlanId.FEATURED_WEEKLY)
        advanceUntilIdle()

        viewModel.onMessageShown()

        assertNull(viewModel.uiState.value.message)
    }
}

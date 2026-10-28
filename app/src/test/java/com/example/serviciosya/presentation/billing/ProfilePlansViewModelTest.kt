package com.example.serviciosya.presentation.billing

import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.Subscription
import com.example.serviciosya.domain.billing.Subscription.Companion.DAY_MILLIS
import com.example.serviciosya.testutil.FakeBillingRepository
import com.example.serviciosya.testutil.FakeProviderAccountRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfilePlansViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = FakeBillingRepository.NOW
    private val provider = testProvider(id = "p1")
    private val billing = FakeBillingRepository()
    private val accounts = FakeProviderAccountRepository(provider)

    private fun createViewModel(userId: String = provider.userId) =
        ProfilePlansViewModel(userId, billing, accounts) { now }

    @Test
    fun `a client account has no provider plan and no active plan`() = runTest {
        val viewModel = createViewModel(userId = "just-a-client")
        advanceUntilIdle()

        assertEquals(ProfilePlansUiState(), viewModel.uiState.value)
    }

    @Test
    fun `an account linked to a provider profile shows the provider plan entry`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isProviderAccount)
    }

    @Test
    fun `a failed lookup leaves the provider entry hidden`() = runTest {
        accounts.findFails = true

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isProviderAccount)
    }

    @Test
    fun `an active client plan is reported and an expired one is not`() = runTest {
        billing.entitlements.value = Entitlements(adFree = Subscription.start(PricingCatalog.adFreeYearly, now))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.adFreeActive)

        billing.entitlements.value = Entitlements(
            adFree = Subscription.start(PricingCatalog.adFreeYearly, now - 400 * DAY_MILLIS),
        )
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.adFreeActive)
    }

    @Test
    fun `an active provider plan is reported`() = runTest {
        billing.entitlements.value = Entitlements(featured = Subscription.start(PricingCatalog.featuredMonthly, now))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.featuredActive)
        assertEquals(false, viewModel.uiState.value.adFreeActive)
    }

    @Test
    fun `the lifetime plan counts as active`() = runTest {
        billing.entitlements.value = Entitlements(adFree = Subscription.start(PricingCatalog.adFreeLifetime, now))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.adFreeActive)
    }
}

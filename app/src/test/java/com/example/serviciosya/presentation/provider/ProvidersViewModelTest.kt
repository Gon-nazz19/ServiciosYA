package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.ads.AdResult
import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.Plan
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.Subscription
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase
import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.testutil.FakeAdsProvider
import com.example.serviciosya.testutil.FakeAnalyticsTracker
import com.example.serviciosya.testutil.FakeBillingRepository
import com.example.serviciosya.testutil.FakeLocationProvider
import com.example.serviciosya.testutil.FakeProviderRepository
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

class ProvidersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProviderRepository()
    private val analytics = FakeAnalyticsTracker()
    private val locationProvider = FakeLocationProvider()
    private val userLocation = GeoPoint(-31.4280, -62.0830)

    private val adsProvider = FakeAdsProvider()
    private val billing = FakeBillingRepository()

    private fun createViewModel(categoryId: String = "electricistas") = ProvidersViewModel(
        categoryId = categoryId,
        categoryName = "Electricista",
        getActiveProvidersByCategory = GetActiveProvidersByCategoryUseCase(repository),
        locationProvider = locationProvider,
        analytics = analytics,
        adsProvider = adsProvider,
        billing = billing,
        now = { FakeBillingRepository.NOW },
    )

    private fun providersOf(count: Int) = (1..count).map { testProvider(id = "p$it") }

    private val nearbyProviders = listOf(
        testProvider(id = "far", latitude = -31.46, longitude = -62.08),
        testProvider(id = "unknown"),
        testProvider(id = "near", latitude = -31.429, longitude = -62.083),
    )

    @Test
    fun `starts loading and then shows providers of the category`() = runTest {
        val providers = listOf(testProvider(id = "a"), testProvider(id = "b"))
        repository.providersResult = Result.success(providers)

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(providers, state.providers.map { it.provider })
        assertEquals("electricistas", repository.lastCategoryId)
    }

    @Test
    fun `with location providers are sorted by distance and unknown ones go last`() = runTest {
        locationProvider.location = userLocation
        locationProvider.permissionGranted = true
        repository.providersResult = Result.success(nearbyProviders)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val items = viewModel.uiState.value.providers
        assertEquals(listOf("near", "far", "unknown"), items.map { it.provider.id })
        assertTrue(items[0].distanceKm!! < items[1].distanceKm!!)
        assertNull(items[2].distanceKm)
        assertFalse(viewModel.uiState.value.showLocationPrompt)
    }

    @Test
    fun `without location the original order is kept without distances`() = runTest {
        repository.providersResult = Result.success(nearbyProviders)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val items = viewModel.uiState.value.providers
        assertEquals(listOf("far", "unknown", "near"), items.map { it.provider.id })
        assertTrue(items.all { it.distanceKm == null })
        assertTrue(viewModel.uiState.value.showLocationPrompt)
    }

    @Test
    fun `granting the permission sorts the list and is tracked`() = runTest {
        repository.providersResult = Result.success(nearbyProviders)
        val viewModel = createViewModel()
        advanceUntilIdle()

        locationProvider.location = userLocation
        locationProvider.permissionGranted = true
        viewModel.onLocationPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals("near", viewModel.uiState.value.providers.first().provider.id)
        assertFalse(viewModel.uiState.value.showLocationPrompt)
        val event = analytics.eventsNamed(AnalyticsEvents.LOCATION_PERMISSION_RESULT).single()
        assertEquals("true", event.params[AnalyticsParams.GRANTED])
    }

    @Test
    fun `denying the permission keeps the list working and is tracked`() = runTest {
        repository.providersResult = Result.success(nearbyProviders)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onLocationPermissionResult(granted = false)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.providers.size)
        assertFalse(viewModel.uiState.value.showLocationPrompt)
        val event = analytics.eventsNamed(AnalyticsEvents.LOCATION_PERMISSION_RESULT).single()
        assertEquals("false", event.params[AnalyticsParams.GRANTED])
    }

    @Test
    fun `dismissing the prompt hides it without tracking a permission result`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.dismissLocationPrompt()

        assertFalse(viewModel.uiState.value.showLocationPrompt)
        assertTrue(analytics.eventsNamed(AnalyticsEvents.LOCATION_PERMISSION_RESULT).isEmpty())
    }

    @Test
    fun `empty result is not an error`() = runTest {
        repository.providersResult = Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.providers.isEmpty())
    }

    @Test
    fun `failure shows the friendly error message`() = runTest {
        repository.providersResult = Result.failure(RuntimeException("PERMISSION_DENIED"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ProvidersViewModel.LOAD_ERROR_MESSAGE, state.errorMessage)
    }

    @Test
    fun `retry after failure recovers the list`() = runTest {
        repository.providersResult = Result.failure(RuntimeException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.providersResult = Result.success(listOf(testProvider()))
        viewModel.loadProviders()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(1, state.providers.size)
        assertEquals(2, repository.providersByCategoryCalls)
    }

    @Test
    fun `blank category fails without querying the repository`() = runTest {
        val viewModel = createViewModel(categoryId = " ")
        advanceUntilIdle()

        assertEquals(ProvidersViewModel.LOAD_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)
        assertEquals(0, repository.providersByCategoryCalls)
    }

    @Test
    fun `category view is tracked once with id and name`() = runTest {
        repository.providersResult = Result.success(listOf(testProvider()))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadProviders()
        advanceUntilIdle()

        val event = analytics.eventsNamed(AnalyticsEvents.CATEGORY_VIEW).single()
        assertEquals("electricistas", event.params[AnalyticsParams.CATEGORY_ID])
        assertEquals("Electricista", event.params[AnalyticsParams.CATEGORY_NAME])
        assertEquals(1, analytics.events.size)
    }

    @Test
    fun `category view is not tracked when loading fails`() = runTest {
        repository.providersResult = Result.failure(RuntimeException("offline"))
        createViewModel()
        advanceUntilIdle()

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `a list with more than six providers gets one loaded ad`() = runTest {
        repository.providersResult = Result.success(providersOf(7))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.providers.size)
        assertEquals(1, state.ads.size)
        assertEquals(1, adsProvider.loadCalls)
        assertTrue(analytics.eventsNamed(AnalyticsEvents.AD_LOAD_FAILED).isEmpty())
    }

    @Test
    fun `failed ads are left out and tracked while the list keeps working`() = runTest {
        repository.providersResult = Result.success(providersOf(13))
        adsProvider.result = AdResult.NoAd("no_fill")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(13, state.providers.size)
        assertNull(state.errorMessage)
        assertTrue(state.ads.isEmpty())
        assertEquals(2, adsProvider.loadCalls)
        val failures = analytics.eventsNamed(AnalyticsEvents.AD_LOAD_FAILED)
        assertEquals(2, failures.size)
        assertTrue(failures.all { it.params[AnalyticsParams.REASON] == "no_fill" })
    }

    @Test
    fun `a list of six providers or fewer does not ask for ads`() = runTest {
        repository.providersResult = Result.success(providersOf(6))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0, adsProvider.loadCalls)
        assertTrue(viewModel.uiState.value.ads.isEmpty())
    }

    @Test
    fun `disabled ads are not tracked as a failure`() = runTest {
        repository.providersResult = Result.success(providersOf(7))
        adsProvider.result = AdResult.NoAd(AdResult.NoAd.REASON_DISABLED)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.ads.isEmpty())
        assertTrue(analytics.eventsNamed(AnalyticsEvents.AD_LOAD_FAILED).isEmpty())
    }

    private val now = FakeBillingRepository.NOW

    private fun entitlementsOf(vararg plans: Plan, startedAt: Long = now) =
        plans.fold(Entitlements.NONE) { acc, plan -> acc.withPurchase(Subscription.start(plan, startedAt)) }

    private fun providersWithFeatured(count: Int, featured: Map<Int, Long>) =
        providersOf(count).mapIndexed { index, provider -> provider.copy(featuredUntil = featured[index]) }

    private fun noAdsWith(entitlements: Entitlements) = runTest {
        billing.entitlements.value = entitlements
        repository.providersResult = Result.success(providersOf(7))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.providers.size)
        assertTrue(viewModel.uiState.value.ads.isEmpty())
        assertEquals(0, adsProvider.loadCalls)
        assertTrue(analytics.eventsNamed(AnalyticsEvents.AD_LOAD_FAILED).isEmpty())
    }

    @Test
    fun `featured providers move up to positions four and five and carry the badge`() = runTest {
        repository.providersResult = Result.success(
            providersWithFeatured(8, featured = mapOf(6 to now + 1_000, 7 to now + 1_000)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val items = viewModel.uiState.value.providers
        assertEquals(listOf("p1", "p2", "p3", "p7", "p8", "p4", "p5", "p6"), items.map { it.provider.id })
        assertEquals(listOf("p7", "p8"), items.filter { it.isFeatured }.map { it.provider.id })
    }

    @Test
    fun `an expired plan neither moves up nor shows the badge`() = runTest {
        repository.providersResult = Result.success(providersWithFeatured(8, featured = mapOf(6 to now - 1)))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val items = viewModel.uiState.value.providers
        assertEquals((1..8).map { "p$it" }, items.map { it.provider.id })
        assertTrue(items.none { it.isFeatured })
    }

    @Test
    fun `a short list keeps its order even with a featured provider`() = runTest {
        repository.providersResult = Result.success(providersWithFeatured(5, featured = mapOf(4 to now + 1_000)))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val items = viewModel.uiState.value.providers
        assertEquals((1..5).map { "p$it" }, items.map { it.provider.id })
        assertTrue(items.last().isFeatured)
    }

    @Test
    fun `a client with the yearly plan sees no ads`() = noAdsWith(entitlementsOf(PricingCatalog.adFreeYearly))

    @Test
    fun `a client with the lifetime plan sees no ads`() = noAdsWith(entitlementsOf(PricingCatalog.adFreeLifetime))

    @Test
    fun `the lifetime plan prevails over the yearly one in either order`() {
        noAdsWith(entitlementsOf(PricingCatalog.adFreeYearly, PricingCatalog.adFreeLifetime))
        noAdsWith(entitlementsOf(PricingCatalog.adFreeLifetime, PricingCatalog.adFreeYearly))
    }

    @Test
    fun `a provider with an active plan sees no ads`() = noAdsWith(entitlementsOf(PricingCatalog.featuredWeekly))

    @Test
    fun `an expired plan shows ads again`() = runTest {
        val longAgo = now - 400 * Subscription.DAY_MILLIS
        billing.entitlements.value = entitlementsOf(PricingCatalog.adFreeYearly, startedAt = longAgo)
        repository.providersResult = Result.success(providersOf(7))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.ads.size)
        assertEquals(1, adsProvider.loadCalls)
    }

    @Test
    fun `activating a plan with the list open removes the ads on screen`() = runTest {
        repository.providersResult = Result.success(providersOf(7))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.ads.size)

        billing.entitlements.value = entitlementsOf(PricingCatalog.adFreeYearly)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.ads.isEmpty())
        assertEquals(7, viewModel.uiState.value.providers.size)
    }
}

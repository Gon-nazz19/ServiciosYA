package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase
import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.testutil.FakeAnalyticsTracker
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

    private fun createViewModel(categoryId: String = "electricistas") = ProvidersViewModel(
        categoryId = categoryId,
        categoryName = "Electricista",
        getActiveProvidersByCategory = GetActiveProvidersByCategoryUseCase(repository),
        locationProvider = locationProvider,
        analytics = analytics,
    )

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
}

package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase
import com.example.serviciosya.testutil.FakeAnalyticsTracker
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

    private fun createViewModel(categoryId: String = "electricistas") = ProvidersViewModel(
        categoryId = categoryId,
        categoryName = "Electricista",
        getActiveProvidersByCategory = GetActiveProvidersByCategoryUseCase(repository),
        analytics = analytics,
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
        assertEquals(providers, state.providers)
        assertEquals("electricistas", repository.lastCategoryId)
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

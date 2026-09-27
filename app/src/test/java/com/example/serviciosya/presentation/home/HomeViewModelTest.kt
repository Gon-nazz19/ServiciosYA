package com.example.serviciosya.presentation.home

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase
import com.example.serviciosya.domain.usecase.GetActiveProvidersUseCase
import com.example.serviciosya.domain.usecase.SearchServicesUseCase
import com.example.serviciosya.testutil.FakeAnalyticsTracker
import com.example.serviciosya.testutil.FakeCategoryRepository
import com.example.serviciosya.testutil.FakeProviderRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testCategory
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categoryRepository = FakeCategoryRepository(
        categoriesResult = Result.success(
            listOf(
                testCategory(id = "electricistas", name = "Electricista", icon = "electrical_services"),
                testCategory(id = "plomeros", name = "Plomero", icon = "plumbing"),
            ),
        ),
    )
    private val providerRepository = FakeProviderRepository(
        allProvidersResult = Result.success(
            listOf(
                testProvider(id = "p1", name = "Carlos Electricidad", categoryId = "electricistas"),
                testProvider(id = "p2", name = "Plomería García", categoryId = "plomeros"),
            ),
        ),
    )
    private val analytics = FakeAnalyticsTracker()

    private fun createViewModelWithoutSession() = HomeViewModel(
        getActiveCategories = GetActiveCategoriesUseCase(categoryRepository),
        getActiveProviders = GetActiveProvidersUseCase(providerRepository),
        searchServices = SearchServicesUseCase(),
        analytics = analytics,
    )

    /** Mirrors AppNavigation: categories are requested once a user is signed in. */
    private fun createViewModel() = createViewModelWithoutSession().also { it.loadCategories() }

    @Test
    fun `loads categories mapped to home items`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("Electricista", "Plomero"), state.categories.map { it.name })
        assertEquals(CategoryIcon.ELECTRICITY, state.categories.first().icon)
    }

    @Test
    fun `category failure shows error and retry recovers`() = runTest {
        val categories = categoryRepository.categoriesResult
        categoryRepository.categoriesResult = Result.failure(RuntimeException("Sin conexión"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(HomeViewModel.LOAD_CATEGORIES_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)

        categoryRepository.categoriesResult = categories
        viewModel.loadCategories()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.categories.size)
    }

    @Test
    fun `search finds categories and providers with category name`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("plom")
        advanceUntilIdle()

        val search = viewModel.uiState.value.search
        assertTrue(search.isActive)
        assertEquals(listOf("plomeros"), search.categories.map { it.id })
        val provider = search.providers.single()
        assertEquals("p2", provider.id)
        assertEquals("Plomero", provider.categoryName)
        assertFalse(search.hasNoResults)
    }

    @Test
    fun `providers are loaded lazily once for every search`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(0, providerRepository.allProvidersCalls)

        viewModel.onSearchQueryChange("c")
        viewModel.onSearchQueryChange("ca")
        advanceUntilIdle()
        viewModel.onSearchQueryChange("car")
        advanceUntilIdle()

        assertEquals(1, providerRepository.allProvidersCalls)
        assertEquals(listOf("p1"), viewModel.uiState.value.search.providers.map { it.id })
    }

    @Test
    fun `category results are shown while providers are still loading`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("electr")

        val search = viewModel.uiState.value.search
        assertEquals(listOf("electricistas"), search.categories.map { it.id })
        runCurrent()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.search.isLoadingProviders)
    }

    @Test
    fun `no matches shows the empty search state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("cerrajero")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.search.hasNoResults)
    }

    @Test
    fun `provider failure keeps category results and retries on next query`() = runTest {
        val providers = providerRepository.allProvidersResult
        providerRepository.allProvidersResult = Result.failure(RuntimeException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("plom")
        advanceUntilIdle()
        var search = viewModel.uiState.value.search
        assertTrue(search.providersUnavailable)
        assertEquals(listOf("plomeros"), search.categories.map { it.id })

        providerRepository.allProvidersResult = providers
        viewModel.onSearchQueryChange("plome")
        advanceUntilIdle()

        search = viewModel.uiState.value.search
        assertFalse(search.providersUnavailable)
        assertEquals(listOf("p2"), search.providers.map { it.id })
    }

    @Test
    fun `clearing the query hides search results`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("plom")
        advanceUntilIdle()

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        val search = viewModel.uiState.value.search
        assertFalse(search.isActive)
        assertTrue(search.categories.isEmpty())
        assertTrue(search.providers.isEmpty())
        assertFalse(search.hasNoResults)
    }

    @Test
    fun `service search is tracked once after the user stops typing`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("p")
        viewModel.onSearchQueryChange("pl")
        advanceTimeBy(HomeViewModel.SEARCH_TRACKING_DEBOUNCE_MS / 2)
        viewModel.onSearchQueryChange("Plomería")
        advanceTimeBy(HomeViewModel.SEARCH_TRACKING_DEBOUNCE_MS - 1)
        assertTrue(analytics.eventsNamed(AnalyticsEvents.SERVICE_SEARCH).isEmpty())

        advanceUntilIdle()

        val event = analytics.eventsNamed(AnalyticsEvents.SERVICE_SEARCH).single()
        assertEquals("plomeria", event.params[AnalyticsParams.QUERY])
    }

    @Test
    fun `too short or repeated queries are not tracked`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("p")
        advanceUntilIdle()
        assertTrue(analytics.events.isEmpty())

        viewModel.onSearchQueryChange("plomero")
        advanceUntilIdle()
        viewModel.onSearchQueryChange("plomero ")
        advanceUntilIdle()

        assertEquals(1, analytics.eventsNamed(AnalyticsEvents.SERVICE_SEARCH).size)
    }

    @Test
    fun `reset search clears query results and pending tracking`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("plomero")
        runCurrent()

        viewModel.resetSearch()
        advanceUntilIdle()

        assertEquals(HomeSearchState(), viewModel.uiState.value.search)
        assertTrue(analytics.eventsNamed(AnalyticsEvents.SERVICE_SEARCH).isEmpty())
    }

    @Test
    fun `same query is tracked again after a reset`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("plomero")
        advanceUntilIdle()

        viewModel.resetSearch()
        viewModel.onSearchQueryChange("plomero")
        advanceUntilIdle()

        assertEquals(2, analytics.eventsNamed(AnalyticsEvents.SERVICE_SEARCH).size)
    }

    @Test
    fun `categories are not requested until a user signs in`() = runTest {
        val viewModel = createViewModelWithoutSession()
        advanceUntilIdle()

        assertEquals(0, categoryRepository.calls)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.loadCategories()
        advanceUntilIdle()

        assertEquals(1, categoryRepository.calls)
        assertEquals(2, viewModel.uiState.value.categories.size)
    }
}

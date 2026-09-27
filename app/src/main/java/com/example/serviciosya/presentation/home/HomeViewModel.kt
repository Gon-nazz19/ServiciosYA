package com.example.serviciosya.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase
import com.example.serviciosya.domain.usecase.GetActiveProvidersUseCase
import com.example.serviciosya.domain.usecase.SearchServicesUseCase
import com.example.serviciosya.domain.usecase.normalizeForSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val categories: List<HomeCategoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val search: HomeSearchState = HomeSearchState(),
)

data class HomeSearchState(
    val query: String = "",
    val categories: List<HomeCategoryItem> = emptyList(),
    val providers: List<ProviderSearchItem> = emptyList(),
    val isLoadingProviders: Boolean = false,
    val providersUnavailable: Boolean = false,
) {
    val isActive: Boolean get() = query.isNotBlank()
    val hasNoResults: Boolean
        get() = isActive && !isLoadingProviders && categories.isEmpty() && providers.isEmpty()
}

data class ProviderSearchItem(
    val id: String,
    val name: String,
    val categoryName: String,
    val city: String,
    val rating: Double?,
)

class HomeViewModel(
    private val getActiveCategories: GetActiveCategoriesUseCase,
    private val getActiveProviders: GetActiveProvidersUseCase,
    private val searchServices: SearchServicesUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var categories: List<Category> = emptyList()
    private var providers: List<Provider>? = null
    private var providersJob: Job? = null
    private var searchTrackingJob: Job? = null
    private var lastTrackedQuery: String? = null

    /**
     * Categories can only be read with a session (Firestore rules), so they are loaded
     * when a user signs in instead of when this ViewModel is created at app start.
     */
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getActiveCategories()
                .onSuccess { loaded ->
                    categories = loaded
                    _uiState.update {
                        it.copy(
                            categories = loaded.map(Category::toHomeItem),
                            isLoading = false,
                        )
                    }
                }
                .onFailure {
                    categories = emptyList()
                    _uiState.update {
                        it.copy(
                            categories = emptyList(),
                            isLoading = false,
                            errorMessage = LOAD_CATEGORIES_ERROR_MESSAGE,
                        )
                    }
                }
            refreshSearchResults()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(search = it.search.copy(query = query)) }
        refreshSearchResults()
        scheduleSearchTracking(query)
        if (query.isNotBlank()) loadProvidersIfNeeded()
    }

    /** Clears the search box and results, e.g. when another user signs in. */
    fun resetSearch() {
        searchTrackingJob?.cancel()
        lastTrackedQuery = null
        _uiState.update { it.copy(search = HomeSearchState()) }
    }

    private fun loadProvidersIfNeeded() {
        if (providers != null || providersJob?.isActive == true) return
        providersJob = viewModelScope.launch {
            _uiState.update { it.copy(search = it.search.copy(isLoadingProviders = true)) }
            // On failure providers stay null so the next query change retries.
            providers = getActiveProviders().getOrNull()
            _uiState.update {
                it.copy(
                    search = it.search.copy(
                        isLoadingProviders = false,
                        providersUnavailable = providers == null,
                    ),
                )
            }
            refreshSearchResults()
        }
    }

    private fun refreshSearchResults() {
        val query = _uiState.value.search.query
        val result = searchServices(query, categories, providers.orEmpty())
        val categoryNames = categories.associate { it.id to it.name }
        _uiState.update {
            it.copy(
                search = it.search.copy(
                    categories = result.categories.map(Category::toHomeItem),
                    providers = result.providers.map { provider ->
                        provider.toSearchItem(categoryNames[provider.categoryId].orEmpty())
                    },
                ),
            )
        }
    }

    private fun scheduleSearchTracking(query: String) {
        searchTrackingJob?.cancel()
        val normalized = normalizeForSearch(query)
        if (normalized.length < MIN_TRACKED_QUERY_LENGTH) return
        searchTrackingJob = viewModelScope.launch {
            delay(SEARCH_TRACKING_DEBOUNCE_MS)
            if (normalized != lastTrackedQuery) {
                lastTrackedQuery = normalized
                analytics.trackServiceSearch(normalized)
            }
        }
    }

    companion object {
        const val SEARCH_TRACKING_DEBOUNCE_MS = 800L
        const val MIN_TRACKED_QUERY_LENGTH = 2
        const val LOAD_CATEGORIES_ERROR_MESSAGE =
            "No pudimos cargar las categorías. Revisá tu conexión e intentá nuevamente."
    }
}

private fun Category.toHomeItem() = HomeCategoryItem(
    id = id,
    name = name,
    icon = CategoryIcon.fromFirestore(icon),
)

private fun Provider.toSearchItem(categoryName: String) = ProviderSearchItem(
    id = id,
    name = name,
    categoryName = categoryName,
    city = city,
    rating = rating,
)

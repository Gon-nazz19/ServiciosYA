package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.location.DistanceCalculator
import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProvidersUiState(
    val providers: List<ProviderListItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showLocationPrompt: Boolean = false,
)

/** The distance belongs to this screen only, so it lives here instead of in the domain model. */
data class ProviderListItem(
    val provider: Provider,
    val distanceKm: Double?,
)

class ProvidersViewModel(
    private val categoryId: String,
    private val categoryName: String,
    private val getActiveProvidersByCategory: GetActiveProvidersByCategoryUseCase,
    private val locationProvider: LocationProvider,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProvidersUiState(showLocationPrompt = !locationProvider.hasPermission()),
    )
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    private var categoryViewTracked = false
    private var providers: List<Provider> = emptyList()
    private var userLocation: GeoPoint? = null

    init {
        loadProviders()
        refreshLocation()
    }

    fun loadProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getActiveProvidersByCategory(categoryId)
                .onSuccess { loaded ->
                    providers = loaded
                    _uiState.update { it.copy(providers = listItems(), isLoading = false) }
                    if (!categoryViewTracked) {
                        categoryViewTracked = true
                        analytics.trackCategoryView(categoryId, categoryName)
                    }
                }
                .onFailure {
                    providers = emptyList()
                    _uiState.update {
                        it.copy(
                            providers = emptyList(),
                            isLoading = false,
                            errorMessage = LOAD_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        analytics.trackLocationPermissionResult(granted)
        _uiState.update { it.copy(showLocationPrompt = false) }
        if (granted) refreshLocation()
    }

    fun dismissLocationPrompt() {
        _uiState.update { it.copy(showLocationPrompt = false) }
    }

    private fun refreshLocation() {
        viewModelScope.launch {
            userLocation = locationProvider.getCurrentLocation() ?: return@launch
            _uiState.update { it.copy(providers = listItems()) }
        }
    }

    private fun listItems(): List<ProviderListItem> =
        DistanceCalculator.withDistances(providers, userLocation)
            .map { (provider, distanceKm) -> ProviderListItem(provider, distanceKm) }

    companion object {
        const val LOAD_ERROR_MESSAGE = "No pudimos cargar los prestadores."
    }
}

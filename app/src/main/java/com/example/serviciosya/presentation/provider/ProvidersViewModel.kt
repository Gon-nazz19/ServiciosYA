package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.ads.AdPlacement
import com.example.serviciosya.domain.ads.AdResult
import com.example.serviciosya.domain.ads.AdsProvider
import com.example.serviciosya.domain.ads.NativeAdContent
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.location.DistanceCalculator
import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase
import kotlinx.coroutines.Job
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
    /** Loaded native ads: the i-th one goes at the i-th position of [AdPlacement.positions]. */
    val ads: List<NativeAdContent> = emptyList(),
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
    private val adsProvider: AdsProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProvidersUiState(showLocationPrompt = !locationProvider.hasPermission()),
    )
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    private var categoryViewTracked = false
    private var providers: List<Provider> = emptyList()
    private var userLocation: GeoPoint? = null
    private var adsJob: Job? = null

    init {
        loadProviders()
        refreshLocation()
    }

    fun loadProviders() {
        adsJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, ads = emptyList()) }
            getActiveProvidersByCategory(categoryId)
                .onSuccess { loaded ->
                    providers = loaded
                    _uiState.update { it.copy(providers = listItems(), isLoading = false) }
                    if (!categoryViewTracked) {
                        categoryViewTracked = true
                        analytics.trackCategoryView(categoryId, categoryName)
                    }
                    loadAds(loaded.size)
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

    /** Ads load apart from the list, so providers show without waiting. A failed ad is simply left out. */
    private fun loadAds(providerCount: Int) {
        val slots = AdPlacement.positions(providerCount).size
        if (slots == 0) return
        adsJob = viewModelScope.launch {
            repeat(slots) {
                when (val result = adsProvider.loadNativeAd()) {
                    is AdResult.Loaded -> _uiState.update { it.copy(ads = it.ads + result.ad) }
                    is AdResult.NoAd ->
                        if (result.reason != AdResult.NoAd.REASON_DISABLED) analytics.trackAdLoadFailed(result.reason)
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

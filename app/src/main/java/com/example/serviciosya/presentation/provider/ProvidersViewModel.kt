package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProvidersUiState(
    val providers: List<Provider> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class ProvidersViewModel(
    private val categoryId: String,
    private val categoryName: String,
    private val getActiveProvidersByCategory: GetActiveProvidersByCategoryUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    private var categoryViewTracked = false

    init {
        loadProviders()
    }

    fun loadProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getActiveProvidersByCategory(categoryId)
                .onSuccess { providers ->
                    _uiState.update { it.copy(providers = providers, isLoading = false) }
                    if (!categoryViewTracked) {
                        categoryViewTracked = true
                        analytics.trackCategoryView(categoryId, categoryName)
                    }
                }
                .onFailure {
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

    companion object {
        const val LOAD_ERROR_MESSAGE = "No pudimos cargar los prestadores."
    }
}

package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.usecase.GetProviderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderDetailUiState(
    val provider: Provider? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class ProviderDetailViewModel(
    private val providerId: String,
    private val getProvider: GetProviderUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderDetailUiState())
    val uiState: StateFlow<ProviderDetailUiState> = _uiState.asStateFlow()

    init {
        loadProvider()
    }

    fun loadProvider() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getProvider(providerId)
                .onSuccess { provider ->
                    _uiState.update {
                        it.copy(
                            provider = provider,
                            isLoading = false,
                            errorMessage = if (provider == null) NOT_FOUND_MESSAGE else null,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(provider = null, isLoading = false, errorMessage = LOAD_ERROR_MESSAGE)
                    }
                }
        }
    }

    companion object {
        const val NOT_FOUND_MESSAGE = "No encontramos este prestador."
        const val LOAD_ERROR_MESSAGE = "No pudimos cargar el prestador."
    }
}

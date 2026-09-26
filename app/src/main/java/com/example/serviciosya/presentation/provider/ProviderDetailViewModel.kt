package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.usecase.CreateServiceRequestUseCase
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
    val message: String = "",
    val isSubmitting: Boolean = false,
    val requestSent: Boolean = false,
    val userMessage: String? = null,
)

class ProviderDetailViewModel(
    private val providerId: String,
    private val getProvider: GetProviderUseCase,
    private val createServiceRequest: CreateServiceRequestUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderDetailUiState())
    val uiState: StateFlow<ProviderDetailUiState> = _uiState.asStateFlow()

    private var providerViewTracked = false

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
                    if (provider != null && !providerViewTracked) {
                        providerViewTracked = true
                        analytics.trackProviderView(provider.id, provider.categoryId)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(provider = null, isLoading = false, errorMessage = LOAD_ERROR_MESSAGE)
                    }
                }
        }
    }

    fun onMessageChange(message: String) {
        if (message.length <= CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH) {
            _uiState.update { it.copy(message = message) }
        }
    }

    fun requestContact(categoryName: String) {
        val state = _uiState.value
        val provider = state.provider ?: return
        if (state.isSubmitting || state.requestSent) return
        // Marked synchronously so a second tap before the coroutine starts is ignored.
        _uiState.update { it.copy(isSubmitting = true, userMessage = null) }
        viewModelScope.launch {
            val result = createServiceRequest(
                NewServiceRequest(
                    providerId = provider.id,
                    categoryId = provider.categoryId,
                    providerName = provider.name,
                    categoryName = categoryName,
                    message = state.message,
                ),
            )
            if (result.isSuccess) {
                analytics.trackContactRequest(provider.id, provider.categoryId)
            }
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isSubmitting = false,
                        requestSent = true,
                        message = "",
                        userMessage = REQUEST_SENT_MESSAGE,
                    )
                } else {
                    it.copy(isSubmitting = false, userMessage = REQUEST_ERROR_MESSAGE)
                }
            }
        }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        const val NOT_FOUND_MESSAGE = "No encontramos este prestador."
        const val LOAD_ERROR_MESSAGE = "No pudimos cargar el prestador."
        const val REQUEST_SENT_MESSAGE = "Solicitud enviada correctamente."
        const val REQUEST_ERROR_MESSAGE = "No pudimos enviar la solicitud. Intentá nuevamente."
    }
}

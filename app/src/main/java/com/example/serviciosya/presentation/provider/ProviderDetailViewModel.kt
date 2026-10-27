package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.analytics.ContactChannel
import com.example.serviciosya.domain.contact.ContactLauncher
import com.example.serviciosya.domain.contact.ContactLinkBuilder
import com.example.serviciosya.domain.location.DistanceCalculator
import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.location.distanceBucket
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
    val distanceKm: Double? = null,
)

class ProviderDetailViewModel(
    private val providerId: String,
    private val getProvider: GetProviderUseCase,
    private val createServiceRequest: CreateServiceRequestUseCase,
    private val contactLauncher: ContactLauncher,
    private val locationProvider: LocationProvider,
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
                    if (provider == null) return@onSuccess
                    // Usually instant: the list screen already asked for the location moments ago.
                    val distanceKm = DistanceCalculator.distanceKm(locationProvider.getCurrentLocation(), provider)
                    _uiState.update { it.copy(distanceKm = distanceKm) }
                    if (!providerViewTracked) {
                        providerViewTracked = true
                        analytics.trackProviderView(provider.id, provider.categoryId, distanceBucket(distanceKm))
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

    /** Opens WhatsApp with a greeting plus the message typed in the request field. */
    fun contactByWhatsApp() {
        val provider = _uiState.value.provider ?: return
        val phone = ContactLinkBuilder.normalizeArgentinePhone(provider.phone)
        if (phone == null) {
            _uiState.update { it.copy(userMessage = INVALID_PHONE_MESSAGE) }
            return
        }
        val message = ContactLinkBuilder.whatsAppMessage(provider.name, _uiState.value.message)
        launchContact(provider, ContactChannel.WHATSAPP, WHATSAPP_ERROR_MESSAGE) {
            contactLauncher.openWhatsApp(ContactLinkBuilder.whatsAppUrl(phone, message))
        }
    }

    fun call() {
        val provider = _uiState.value.provider ?: return
        if (provider.phone.isBlank()) {
            _uiState.update { it.copy(userMessage = INVALID_PHONE_MESSAGE) }
            return
        }
        launchContact(provider, ContactChannel.CALL, CALL_ERROR_MESSAGE) {
            contactLauncher.dial(provider.phone)
        }
    }

    fun share(categoryName: String) {
        val provider = _uiState.value.provider ?: return
        launchContact(provider, ContactChannel.SHARE, SHARE_ERROR_MESSAGE) {
            contactLauncher.share(ContactLinkBuilder.shareText(provider, categoryName))
        }
    }

    fun openDirections() {
        val provider = _uiState.value.provider ?: return
        val location = provider.location ?: return
        if (!contactLauncher.openMap(ContactLinkBuilder.directionsUri(location, provider.name))) {
            _uiState.update { it.copy(userMessage = MAP_ERROR_MESSAGE) }
        }
    }

    private fun launchContact(
        provider: Provider,
        channel: ContactChannel,
        errorMessage: String,
        open: () -> Boolean,
    ) {
        if (open()) {
            analytics.trackContactChannel(channel, provider.id, provider.categoryId)
        } else {
            _uiState.update { it.copy(userMessage = errorMessage) }
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
        const val INVALID_PHONE_MESSAGE = "Este prestador no tiene un teléfono válido."
        const val WHATSAPP_ERROR_MESSAGE = "No se pudo abrir WhatsApp."
        const val CALL_ERROR_MESSAGE = "No se pudo abrir el teléfono."
        const val SHARE_ERROR_MESSAGE = "No se pudo compartir el prestador."
        const val MAP_ERROR_MESSAGE = "No se encontró una app de mapas."
    }
}

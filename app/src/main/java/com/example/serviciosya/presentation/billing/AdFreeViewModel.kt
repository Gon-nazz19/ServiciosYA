package com.example.serviciosya.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.billing.PlanId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdFreeUiState(
    val status: PlanStatus? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
) {
    /** With the lifetime plan the yearly one is no longer needed. */
    val hasLifetime: Boolean get() = status?.plan == PlanId.ADFREE_LIFETIME
}

/** "Sin anuncios": the yearly and lifetime plans for clients. */
class AdFreeViewModel(
    private val billing: BillingRepository,
    private val analytics: AnalyticsTracker,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdFreeUiState())
    val uiState: StateFlow<AdFreeUiState> = _uiState.asStateFlow()

    init {
        analytics.trackPaywallView(SOURCE)
        viewModelScope.launch {
            billing.observeSubscription().collect { entitlements ->
                _uiState.update { it.copy(status = entitlements.adFree.statusAt(now())) }
            }
        }
    }

    fun purchase(plan: PlanId) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val result = billing.purchaseTracked(plan, analytics)
            _uiState.update { it.copy(isBusy = false, message = result.userMessage()) }
        }
    }

    /** Also restores the lifetime plan. */
    fun restore() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val message = billing.restore().fold(
                onSuccess = { entitlements ->
                    if (entitlements.adFree.statusAt(now()) != null) RESTORED_MESSAGE else NOTHING_TO_RESTORE_MESSAGE
                },
                onFailure = { RESTORE_ERROR_MESSAGE },
            )
            _uiState.update { it.copy(isBusy = false, message = message) }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        const val SOURCE = "ad_free"
        const val RESTORED_MESSAGE = "Restauramos tu plan."
        const val NOTHING_TO_RESTORE_MESSAGE = "No encontramos compras para restaurar."
        const val RESTORE_ERROR_MESSAGE = "No pudimos restaurar tus compras. Probá de nuevo."
    }
}

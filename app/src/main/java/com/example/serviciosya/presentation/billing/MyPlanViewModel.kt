package com.example.serviciosya.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.repository.ProviderAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPlanUiState(
    val isLoading: Boolean = true,
    /** Only an account linked to a provider profile can buy the "Destacado" plans. */
    val isProviderAccount: Boolean = false,
    val providerName: String? = null,
    val status: PlanStatus? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
)

/** "Mi plan": the "Destacado" plans of a provider account. */
class MyPlanViewModel(
    private val userId: String,
    private val billing: BillingRepository,
    private val providerAccounts: ProviderAccountRepository,
    private val analytics: AnalyticsTracker,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPlanUiState())
    val uiState: StateFlow<MyPlanUiState> = _uiState.asStateFlow()

    init {
        analytics.trackPaywallView(SOURCE)
        viewModelScope.launch { loadProvider() }
        viewModelScope.launch {
            billing.observeSubscription().collect { entitlements ->
                _uiState.update { it.copy(status = entitlements.featured.statusAt(now())) }
            }
        }
    }

    private suspend fun loadProvider() {
        providerAccounts.findByUserId(userId)
            .onSuccess { provider ->
                _uiState.update {
                    it.copy(isLoading = false, isProviderAccount = provider != null, providerName = provider?.name)
                }
            }
            .onFailure {
                _uiState.update { it.copy(isLoading = false, message = LOAD_ERROR_MESSAGE) }
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

    fun restore() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val message = billing.restore().fold(
                onSuccess = { entitlements ->
                    if (entitlements.isFeatured(now())) RESTORED_MESSAGE else NOTHING_TO_RESTORE_MESSAGE
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
        const val SOURCE = "my_plan"
        const val LOAD_ERROR_MESSAGE = "No pudimos cargar tu cuenta de prestador."
        const val RESTORED_MESSAGE = "Restauramos tu plan."
        const val NOTHING_TO_RESTORE_MESSAGE = "No encontramos compras para restaurar."
        const val RESTORE_ERROR_MESSAGE = "No pudimos restaurar tus compras. Probá de nuevo."
    }
}

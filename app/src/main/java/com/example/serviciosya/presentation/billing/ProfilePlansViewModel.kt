package com.example.serviciosya.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.repository.ProviderAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilePlansUiState(
    /** "Mi plan" only shows for an account linked to a provider profile. */
    val isProviderAccount: Boolean = false,
    val adFreeActive: Boolean = false,
    val featuredActive: Boolean = false,
)

/** What the profile needs to know to show the entries to the plans. */
class ProfilePlansViewModel(
    userId: String,
    billing: BillingRepository,
    providerAccounts: ProviderAccountRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfilePlansUiState())
    val uiState: StateFlow<ProfilePlansUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val provider = providerAccounts.findByUserId(userId).getOrNull()
            _uiState.update { it.copy(isProviderAccount = provider != null) }
        }
        viewModelScope.launch {
            billing.observeSubscription().collect { entitlements ->
                _uiState.update {
                    it.copy(
                        adFreeActive = entitlements.adFree.statusAt(now()) != null,
                        featuredActive = entitlements.isFeatured(now()),
                    )
                }
            }
        }
    }
}

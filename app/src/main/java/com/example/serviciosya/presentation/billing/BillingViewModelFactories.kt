package com.example.serviciosya.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.repository.ProviderAccountRepository

class MyPlanViewModelFactory(
    private val userId: String,
    private val billing: BillingRepository,
    private val providerAccounts: ProviderAccountRepository,
    private val analytics: AnalyticsTracker,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MyPlanViewModel::class.java))
        return MyPlanViewModel(userId, billing, providerAccounts, analytics) as T
    }
}

class AdFreeViewModelFactory(
    private val billing: BillingRepository,
    private val analytics: AnalyticsTracker,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AdFreeViewModel::class.java))
        return AdFreeViewModel(billing, analytics) as T
    }
}

class ProfilePlansViewModelFactory(
    private val userId: String,
    private val billing: BillingRepository,
    private val providerAccounts: ProviderAccountRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfilePlansViewModel::class.java))
        return ProfilePlansViewModel(userId, billing, providerAccounts) as T
    }
}

package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.repository.ProviderRepository
import com.example.serviciosya.domain.repository.ServiceRequestRepository
import com.example.serviciosya.domain.usecase.CreateServiceRequestUseCase
import com.example.serviciosya.domain.usecase.GetProviderUseCase

class ProviderDetailViewModelFactory(
    private val providerId: String,
    private val providerRepository: ProviderRepository,
    private val serviceRequestRepository: ServiceRequestRepository,
    private val analytics: AnalyticsTracker,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProviderDetailViewModel::class.java))
        return ProviderDetailViewModel(
            providerId = providerId,
            getProvider = GetProviderUseCase(providerRepository),
            createServiceRequest = CreateServiceRequestUseCase(serviceRequestRepository),
            analytics = analytics,
        ) as T
    }
}

package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.repository.ProviderRepository
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase

class ProvidersViewModelFactory(
    private val categoryId: String,
    private val categoryName: String,
    private val repository: ProviderRepository,
    private val locationProvider: LocationProvider,
    private val analytics: AnalyticsTracker,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProvidersViewModel::class.java))
        return ProvidersViewModel(
            categoryId = categoryId,
            categoryName = categoryName,
            getActiveProvidersByCategory = GetActiveProvidersByCategoryUseCase(repository),
            locationProvider = locationProvider,
            analytics = analytics,
        ) as T
    }
}

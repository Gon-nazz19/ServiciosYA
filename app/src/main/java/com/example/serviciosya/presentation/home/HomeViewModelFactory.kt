package com.example.serviciosya.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.repository.CategoryRepository
import com.example.serviciosya.domain.repository.ProviderRepository
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase
import com.example.serviciosya.domain.usecase.GetActiveProvidersUseCase
import com.example.serviciosya.domain.usecase.SearchServicesUseCase

class HomeViewModelFactory(
    private val categoryRepository: CategoryRepository,
    private val providerRepository: ProviderRepository,
    private val analytics: AnalyticsTracker,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(
            getActiveCategories = GetActiveCategoriesUseCase(categoryRepository),
            getActiveProviders = GetActiveProvidersUseCase(providerRepository),
            searchServices = SearchServicesUseCase(),
            analytics = analytics,
        ) as T
    }
}

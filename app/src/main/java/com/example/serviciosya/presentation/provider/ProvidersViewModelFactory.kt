package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.repository.ProviderRepository
import com.example.serviciosya.domain.usecase.GetActiveProvidersByCategoryUseCase

class ProvidersViewModelFactory(
    private val categoryId: String,
    private val repository: ProviderRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProvidersViewModel::class.java))
        return ProvidersViewModel(
            categoryId = categoryId,
            getActiveProvidersByCategory = GetActiveProvidersByCategoryUseCase(repository),
        ) as T
    }
}

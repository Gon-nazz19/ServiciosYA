package com.example.serviciosya.presentation.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.repository.ProviderRepository
import com.example.serviciosya.domain.usecase.GetProviderUseCase

class ProviderDetailViewModelFactory(
    private val providerId: String,
    private val providerRepository: ProviderRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProviderDetailViewModel::class.java))
        return ProviderDetailViewModel(
            providerId = providerId,
            getProvider = GetProviderUseCase(providerRepository),
        ) as T
    }
}

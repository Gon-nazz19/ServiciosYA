package com.example.serviciosya.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.repository.ServiceRequestRepository
import com.example.serviciosya.domain.usecase.GetMyRequestsUseCase

class RequestsViewModelFactory(
    private val repository: ServiceRequestRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RequestsViewModel::class.java))
        return RequestsViewModel(GetMyRequestsUseCase(repository)) as T
    }
}

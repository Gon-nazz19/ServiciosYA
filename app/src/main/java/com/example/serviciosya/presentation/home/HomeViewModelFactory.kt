package com.example.serviciosya.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.repository.CategoryRepository
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase

class HomeViewModelFactory(
    private val repository: CategoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(GetActiveCategoriesUseCase(repository)) as T
    }
}

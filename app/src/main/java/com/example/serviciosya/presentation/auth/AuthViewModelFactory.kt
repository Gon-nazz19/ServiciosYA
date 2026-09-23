package com.example.serviciosya.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.repository.AuthRepository
import com.example.serviciosya.domain.usecase.ObserveCurrentUserUseCase
import com.example.serviciosya.domain.usecase.RegisterUserUseCase
import com.example.serviciosya.domain.usecase.SignInUseCase
import com.example.serviciosya.domain.usecase.SignOutUseCase

class AuthViewModelFactory(
    private val repository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AuthViewModel::class.java))
        return AuthViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(repository),
            registerUser = RegisterUserUseCase(repository),
            signInUser = SignInUseCase(repository),
            signOutUser = SignOutUseCase(repository),
        ) as T
    }
}

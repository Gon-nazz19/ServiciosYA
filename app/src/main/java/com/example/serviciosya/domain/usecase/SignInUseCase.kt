package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.repository.AuthRepository

class SignInUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        repository.signIn(email.trim(), password)
}

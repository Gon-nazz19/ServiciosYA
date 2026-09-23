package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.repository.AuthRepository

class RegisterUserUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(name: String, email: String, password: String): Result<Unit> =
        repository.register(name.trim(), email.trim(), password)
}

package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.repository.AuthRepository

class SignOutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.signOut()
}

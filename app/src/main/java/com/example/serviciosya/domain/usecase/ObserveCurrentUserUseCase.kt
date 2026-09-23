package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.User
import com.example.serviciosya.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveCurrentUserUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<User?> = repository.observeCurrentUser()
}

package com.example.serviciosya.domain.repository

import com.example.serviciosya.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<User?>

    suspend fun register(name: String, email: String, password: String): Result<Unit>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signOut(): Result<Unit>
}

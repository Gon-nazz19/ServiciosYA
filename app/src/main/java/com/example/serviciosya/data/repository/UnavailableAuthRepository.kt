package com.example.serviciosya.data.repository

import com.example.serviciosya.domain.model.AuthErrorReason
import com.example.serviciosya.domain.model.AuthException
import com.example.serviciosya.domain.model.User
import com.example.serviciosya.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UnavailableAuthRepository : AuthRepository {
    override fun observeCurrentUser(): Flow<User?> = flowOf(null)

    override suspend fun register(name: String, email: String, password: String): Result<Unit> =
        unavailableResult()

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        unavailableResult()

    override suspend fun signOut(): Result<Unit> = Result.success(Unit)

    private fun unavailableResult(): Result<Unit> = Result.failure(
        AuthException(AuthErrorReason.NOT_CONFIGURED),
    )
}

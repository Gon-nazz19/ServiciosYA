package com.example.serviciosya.testutil

import com.example.serviciosya.domain.model.User
import com.example.serviciosya.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(
    initialUser: User? = null,
) : AuthRepository {
    val currentUser = MutableStateFlow(initialUser)
    var registerResult: Result<Unit> = Result.success(Unit)
    var signInResult: Result<Unit> = Result.success(Unit)
    var signOutResult: Result<Unit> = Result.success(Unit)

    val registerCalls = mutableListOf<Triple<String, String, String>>()
    val signInCalls = mutableListOf<Pair<String, String>>()
    var signOutCalls = 0
        private set

    override fun observeCurrentUser(): Flow<User?> = currentUser

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        registerCalls += Triple(name, email, password)
        return registerResult
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        signInCalls += email to password
        return signInResult
    }

    override suspend fun signOut(): Result<Unit> {
        signOutCalls++
        if (signOutResult.isSuccess) currentUser.value = null
        return signOutResult
    }
}

package com.example.serviciosya.data.repository

import com.example.serviciosya.domain.model.User
import com.example.serviciosya.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            trySend(
                firebaseUser?.let { user ->
                    User(
                        id = user.uid,
                        name = user.displayName.orEmpty().ifBlank {
                            user.email?.substringBefore('@').orEmpty()
                        },
                        email = user.email.orEmpty(),
                    )
                },
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = requireNotNull(result.user) { "No se pudo crear el usuario." }
        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name).build(),
        ).await()
        firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).set(
            mapOf(
                "name" to name,
                "email" to email,
                "role" to User.ROLE_CLIENT,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        Unit
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}

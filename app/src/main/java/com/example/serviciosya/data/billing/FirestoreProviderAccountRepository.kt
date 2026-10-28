package com.example.serviciosya.data.billing

import com.example.serviciosya.data.model.toDomain
import com.example.serviciosya.data.model.toProviderDto
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderAccountRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreProviderAccountRepository(
    private val firestore: FirebaseFirestore,
) : ProviderAccountRepository {
    override suspend fun findByUserId(userId: String): Result<Provider?> = runCatching {
        firestore.collection(PROVIDERS_COLLECTION)
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toProviderDto()
            ?.toDomain()
    }

    override suspend fun setFeaturedUntil(providerId: String, untilMillis: Long): Result<Unit> = runCatching {
        firestore.collection(PROVIDERS_COLLECTION)
            .document(providerId)
            .update("featuredUntil", Timestamp(Date(untilMillis)))
            .await()
        Unit
    }

    private companion object {
        const val PROVIDERS_COLLECTION = "providers"
    }
}

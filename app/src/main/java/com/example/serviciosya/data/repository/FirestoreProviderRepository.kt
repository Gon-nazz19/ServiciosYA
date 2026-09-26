package com.example.serviciosya.data.repository

import com.example.serviciosya.data.model.toDomain
import com.example.serviciosya.data.model.toProviderDto
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.repository.ProviderRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreProviderRepository(
    private val firestore: FirebaseFirestore,
) : ProviderRepository {
    override suspend fun getActiveProvidersByCategory(
        categoryId: String,
    ): Result<List<Provider>> = runCatching {
        firestore.collection(PROVIDERS_COLLECTION)
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .map { it.toProviderDto().toDomain() }
            .filter { it.name.isNotBlank() }
            .sortedByDescending { it.rating ?: 0.0 }
    }

    override suspend fun getProvider(providerId: String): Result<Provider?> = runCatching {
        val document = firestore.collection(PROVIDERS_COLLECTION)
            .document(providerId)
            .get()
            .await()
        if (document.exists()) document.toProviderDto().toDomain() else null
    }

    override suspend fun getActiveProviders(): Result<List<Provider>> = runCatching {
        firestore.collection(PROVIDERS_COLLECTION)
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .map { it.toProviderDto().toDomain() }
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name }
    }

    private companion object {
        const val PROVIDERS_COLLECTION = "providers"
    }
}

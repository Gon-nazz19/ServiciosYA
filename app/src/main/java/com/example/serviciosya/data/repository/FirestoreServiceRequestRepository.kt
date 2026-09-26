package com.example.serviciosya.data.repository

import com.example.serviciosya.data.model.toDomain
import com.example.serviciosya.data.model.toServiceRequestDto
import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.model.RequestStatus
import com.example.serviciosya.domain.model.ServiceRequest
import com.example.serviciosya.domain.repository.ServiceRequestRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreServiceRequestRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ServiceRequestRepository {
    override suspend fun createRequest(request: NewServiceRequest): Result<Unit> = runCatching {
        val clientId = auth.currentUser?.uid
            ?: throw IllegalStateException(NOT_AUTHENTICATED_MESSAGE)
        firestore.collection(SERVICE_REQUESTS_COLLECTION).add(
            mapOf(
                "clientId" to clientId,
                "providerId" to request.providerId,
                "categoryId" to request.categoryId,
                "providerName" to request.providerName,
                "categoryName" to request.categoryName,
                "message" to request.message,
                "status" to RequestStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        Unit
    }

    override suspend fun getMyRequests(): Result<List<ServiceRequest>> = runCatching {
        val clientId = auth.currentUser?.uid
            ?: throw IllegalStateException(NOT_AUTHENTICATED_MESSAGE)
        // Filtered only by clientId: ordering happens in the use case to avoid a composite index.
        firestore.collection(SERVICE_REQUESTS_COLLECTION)
            .whereEqualTo("clientId", clientId)
            .get()
            .await()
            .documents
            .map { it.toServiceRequestDto().toDomain() }
    }

    private companion object {
        const val SERVICE_REQUESTS_COLLECTION = "serviceRequests"
        const val NOT_AUTHENTICATED_MESSAGE = "Tenés que iniciar sesión para ver o enviar solicitudes."
    }
}

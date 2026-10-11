package com.example.serviciosya.data.repository

import android.content.Context
import com.example.serviciosya.data.model.toDomain
import com.example.serviciosya.data.model.toServiceRequestDto
import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.model.ServiceRequest
import com.example.serviciosya.domain.repository.ServiceRequestRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.tasks.await

class FirestoreServiceRequestRepository(
    context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : ServiceRequestRepository {
    private val pendingKeys = context.getSharedPreferences("pending_service_requests", Context.MODE_PRIVATE)

    override suspend fun createRequest(request: NewServiceRequest): Result<Unit> = runCatching {
        val clientId = auth.currentUser?.uid
            ?: throw IllegalStateException(NOT_AUTHENTICATED_MESSAGE)
        val message = request.message.trim()
        val fingerprint = sha256("$clientId\u0000${request.providerId}\u0000$message")
        val key = synchronized(pendingKeys) {
            pendingKeys.getString(fingerprint, null) ?: UUID.randomUUID().toString().replace("-", "")
                .also { check(pendingKeys.edit().putString(fingerprint, it).commit()) }
        }
        val result = functions.getHttpsCallable("createServiceRequest").call(
            mapOf("providerId" to request.providerId, "message" to message, "idempotencyKey" to key),
        ).await()
        val requestId = (result.data as? Map<*, *>)?.get("requestId") as? String
        check(!requestId.isNullOrBlank()) { "Respuesta inválida al crear la solicitud." }
        synchronized(pendingKeys) { pendingKeys.edit().remove(fingerprint).commit() }
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
        fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        const val SERVICE_REQUESTS_COLLECTION = "serviceRequests"
        const val NOT_AUTHENTICATED_MESSAGE = "Tenés que iniciar sesión para ver o enviar solicitudes."
    }
}

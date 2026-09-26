package com.example.serviciosya.data.repository

import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.repository.ServiceRequestRepository

class UnavailableServiceRequestRepository : ServiceRequestRepository {
    override suspend fun createRequest(request: NewServiceRequest): Result<Unit> = unavailableResult()

    private fun <T> unavailableResult(): Result<T> = Result.failure(
        IllegalStateException(
            "Firebase no está configurado. Agregá app/google-services.json para enviar solicitudes.",
        ),
    )
}

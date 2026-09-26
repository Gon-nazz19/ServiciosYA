package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.repository.ServiceRequestRepository

class CreateServiceRequestUseCase(
    private val repository: ServiceRequestRepository,
) {
    suspend operator fun invoke(request: NewServiceRequest): Result<Unit> {
        if (request.providerId.isBlank() || request.categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Faltan datos del prestador."))
        }
        if (request.message.length > MAX_MESSAGE_LENGTH) {
            return Result.failure(
                IllegalArgumentException("El mensaje no puede superar los $MAX_MESSAGE_LENGTH caracteres."),
            )
        }
        return repository.createRequest(request.copy(message = request.message.trim()))
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 500
    }
}

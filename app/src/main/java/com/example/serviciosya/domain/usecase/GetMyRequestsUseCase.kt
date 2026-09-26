package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.ServiceRequest
import com.example.serviciosya.domain.repository.ServiceRequestRepository

class GetMyRequestsUseCase(
    private val repository: ServiceRequestRepository,
) {
    suspend operator fun invoke(): Result<List<ServiceRequest>> =
        repository.getMyRequests().map { it.sortedNewestFirst() }
}

/**
 * Newest first. Requests without createdAt (server timestamp not yet resolved)
 * were just created, so they go on top.
 */
internal fun List<ServiceRequest>.sortedNewestFirst(): List<ServiceRequest> =
    sortedByDescending { it.createdAtMillis ?: Long.MAX_VALUE }

package com.example.serviciosya.domain.repository

import com.example.serviciosya.domain.model.NewServiceRequest

interface ServiceRequestRepository {
    suspend fun createRequest(request: NewServiceRequest): Result<Unit>
}

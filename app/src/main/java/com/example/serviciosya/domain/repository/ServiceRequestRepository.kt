package com.example.serviciosya.domain.repository

import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.model.ServiceRequest

interface ServiceRequestRepository {
    suspend fun createRequest(request: NewServiceRequest): Result<Unit>

    suspend fun getMyRequests(): Result<List<ServiceRequest>>
}

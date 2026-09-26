package com.example.serviciosya.testutil

import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.repository.ServiceRequestRepository

class FakeServiceRequestRepository(
    var createResult: Result<Unit> = Result.success(Unit),
) : ServiceRequestRepository {
    val createdRequests = mutableListOf<NewServiceRequest>()

    override suspend fun createRequest(request: NewServiceRequest): Result<Unit> {
        createdRequests += request
        return createResult
    }
}

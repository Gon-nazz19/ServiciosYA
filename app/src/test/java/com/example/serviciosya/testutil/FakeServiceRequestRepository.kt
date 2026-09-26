package com.example.serviciosya.testutil

import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.model.ServiceRequest
import com.example.serviciosya.domain.repository.ServiceRequestRepository

class FakeServiceRequestRepository(
    var createResult: Result<Unit> = Result.success(Unit),
    var myRequestsResult: Result<List<ServiceRequest>> = Result.success(emptyList()),
) : ServiceRequestRepository {
    val createdRequests = mutableListOf<NewServiceRequest>()
    var getMyRequestsCalls = 0
        private set

    override suspend fun createRequest(request: NewServiceRequest): Result<Unit> {
        createdRequests += request
        return createResult
    }

    override suspend fun getMyRequests(): Result<List<ServiceRequest>> {
        getMyRequestsCalls++
        return myRequestsResult
    }
}

package com.example.serviciosya.data.model

import com.example.serviciosya.domain.model.RequestStatus
import com.example.serviciosya.domain.model.ServiceRequest
import com.google.firebase.firestore.DocumentSnapshot

data class ServiceRequestDto(
    val id: String,
    val clientId: String,
    val providerId: String,
    val categoryId: String,
    val providerName: String,
    val categoryName: String,
    val message: String,
    val status: String,
    val createdAtMillis: Long?,
)

fun DocumentSnapshot.toServiceRequestDto(): ServiceRequestDto = ServiceRequestDto(
    id = id,
    clientId = getString("clientId").orEmpty(),
    providerId = getString("providerId").orEmpty(),
    categoryId = getString("categoryId").orEmpty(),
    providerName = getString("providerName").orEmpty(),
    categoryName = getString("categoryName").orEmpty(),
    message = getString("message").orEmpty(),
    status = getString("status").orEmpty(),
    createdAtMillis = getTimestamp("createdAt")?.toDate()?.time,
)

fun ServiceRequestDto.toDomain(): ServiceRequest = ServiceRequest(
    id = id,
    clientId = clientId,
    providerId = providerId,
    categoryId = categoryId,
    providerName = providerName,
    categoryName = categoryName,
    message = message,
    status = RequestStatus.fromValue(status),
    createdAtMillis = createdAtMillis,
)

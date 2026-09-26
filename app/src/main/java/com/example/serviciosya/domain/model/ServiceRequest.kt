package com.example.serviciosya.domain.model

data class ServiceRequest(
    val id: String,
    val clientId: String,
    val providerId: String,
    val categoryId: String,
    val providerName: String,
    val categoryName: String,
    val message: String,
    val status: RequestStatus,
    val createdAtMillis: Long?,
)

data class NewServiceRequest(
    val providerId: String,
    val categoryId: String,
    val providerName: String,
    val categoryName: String,
    val message: String,
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromValue(value: String?): RequestStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: PENDING
    }
}

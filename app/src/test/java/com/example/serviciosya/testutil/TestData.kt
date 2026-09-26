package com.example.serviciosya.testutil

import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.model.RequestStatus
import com.example.serviciosya.domain.model.ServiceRequest

fun testProvider(
    id: String = "provider-1",
    name: String = "Carlos Electricidad",
    categoryId: String = "electricistas",
    city: String = "San Francisco",
    rating: Double? = 4.7,
) = Provider(
    id = id,
    userId = "user-$id",
    name = name,
    description = "Instalaciones y reparaciones eléctricas.",
    categoryId = categoryId,
    city = city,
    profileImageUrl = "",
    phone = "3564-000000",
    rating = rating,
    reviewCount = 0,
    verified = false,
    active = true,
)

fun testCategory(
    id: String = "electricistas",
    name: String = "Electricista",
    icon: String = "electrical_services",
) = Category(id = id, name = name, icon = icon, active = true)

fun testRequest(
    id: String = "request-1",
    providerName: String = "Carlos Electricidad",
    categoryName: String = "Electricista",
    status: RequestStatus = RequestStatus.PENDING,
    createdAtMillis: Long? = 1_000L,
) = ServiceRequest(
    id = id,
    clientId = "client-1",
    providerId = "provider-1",
    categoryId = "electricistas",
    providerName = providerName,
    categoryName = categoryName,
    message = "",
    status = status,
    createdAtMillis = createdAtMillis,
)

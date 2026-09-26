package com.example.serviciosya.data.model

import com.example.serviciosya.domain.model.RequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceRequestDtoTest {
    @Test
    fun `service request dto maps to domain`() {
        val request = ServiceRequestDto(
            id = "request-1",
            clientId = "client-1",
            providerId = "provider-1",
            categoryId = "electricistas",
            providerName = "Carlos Electricidad",
            categoryName = "Electricista",
            message = "Hola",
            status = "ACCEPTED",
            createdAtMillis = 1_000L,
        ).toDomain()

        assertEquals("request-1", request.id)
        assertEquals("client-1", request.clientId)
        assertEquals("Carlos Electricidad", request.providerName)
        assertEquals(RequestStatus.ACCEPTED, request.status)
        assertEquals(1_000L, request.createdAtMillis)
    }

    @Test
    fun `unknown or missing status falls back to pending`() {
        assertEquals(RequestStatus.PENDING, RequestStatus.fromValue(null))
        assertEquals(RequestStatus.PENDING, RequestStatus.fromValue("SOMETHING"))
        assertEquals(RequestStatus.COMPLETED, RequestStatus.fromValue(" completed "))
    }

    @Test
    fun `pending server timestamp maps to null`() {
        val request = ServiceRequestDto(
            id = "r",
            clientId = "c",
            providerId = "p",
            categoryId = "cat",
            providerName = "",
            categoryName = "",
            message = "",
            status = "PENDING",
            createdAtMillis = null,
        ).toDomain()

        assertNull(request.createdAtMillis)
    }
}

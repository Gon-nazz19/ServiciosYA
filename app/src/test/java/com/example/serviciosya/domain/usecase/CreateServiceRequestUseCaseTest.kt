package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.testutil.FakeServiceRequestRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateServiceRequestUseCaseTest {
    private val repository = FakeServiceRequestRepository()
    private val useCase = CreateServiceRequestUseCase(repository)

    private val validRequest = NewServiceRequest(
        providerId = "provider-1",
        categoryId = "electricistas",
        providerName = "Carlos Electricidad",
        categoryName = "Electricista",
        message = "  Hola  ",
    )

    @Test
    fun `valid request is stored with trimmed message`() = runTest {
        assertTrue(useCase(validRequest).isSuccess)
        assertEquals("Hola", repository.createdRequests.single().message)
    }

    @Test
    fun `missing ids are rejected before reaching the repository`() = runTest {
        assertTrue(useCase(validRequest.copy(providerId = " ")).isFailure)
        assertTrue(useCase(validRequest.copy(categoryId = "")).isFailure)
        assertTrue(repository.createdRequests.isEmpty())
    }

    @Test
    fun `too long message is rejected`() = runTest {
        val longMessage = "a".repeat(CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH + 1)

        assertTrue(useCase(validRequest.copy(message = longMessage)).isFailure)
        assertTrue(repository.createdRequests.isEmpty())
    }
}

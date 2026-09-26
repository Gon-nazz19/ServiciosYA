package com.example.serviciosya.domain.usecase

import com.example.serviciosya.testutil.FakeProviderRepository
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderUseCasesTest {
    private val repository = FakeProviderRepository()

    @Test
    fun `providers by category delegates to repository`() = runTest {
        repository.providersResult = Result.success(listOf(testProvider()))

        val result = GetActiveProvidersByCategoryUseCase(repository)("plomeros")

        assertEquals(1, result.getOrThrow().size)
        assertEquals("plomeros", repository.lastCategoryId)
    }

    @Test
    fun `providers by blank category fails`() = runTest {
        val result = GetActiveProvidersByCategoryUseCase(repository)("")

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, repository.providersByCategoryCalls)
    }

    @Test
    fun `get provider by blank id fails and valid id delegates`() = runTest {
        val provider = testProvider()
        repository.providerResult = Result.success(provider)
        val useCase = GetProviderUseCase(repository)

        assertTrue(useCase(" ").exceptionOrNull() is IllegalArgumentException)
        assertEquals(provider, useCase("provider-1").getOrThrow())
    }
}

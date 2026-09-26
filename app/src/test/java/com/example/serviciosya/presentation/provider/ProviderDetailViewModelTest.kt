package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.usecase.GetProviderUseCase
import com.example.serviciosya.testutil.FakeProviderRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProviderDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProviderRepository()

    private fun createViewModel(providerId: String = "provider-1") = ProviderDetailViewModel(
        providerId = providerId,
        getProvider = GetProviderUseCase(repository),
    )

    @Test
    fun `loads the provider by id`() = runTest {
        val provider = testProvider()
        repository.providerResult = Result.success(provider)

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(provider, state.provider)
    }

    @Test
    fun `missing provider shows not found`() = runTest {
        repository.providerResult = Result.success(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.provider)
        assertEquals(ProviderDetailViewModel.NOT_FOUND_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `failure shows error and retry recovers`() = runTest {
        repository.providerResult = Result.failure(RuntimeException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(ProviderDetailViewModel.LOAD_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)

        repository.providerResult = Result.success(testProvider())
        viewModel.loadProvider()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("provider-1", viewModel.uiState.value.provider?.id)
    }
}

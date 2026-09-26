package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.usecase.CreateServiceRequestUseCase
import com.example.serviciosya.domain.usecase.GetProviderUseCase
import com.example.serviciosya.testutil.FakeProviderRepository
import com.example.serviciosya.testutil.FakeServiceRequestRepository
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

    private val repository = FakeProviderRepository(
        providerResult = Result.success(testProvider()),
    )
    private val requestRepository = FakeServiceRequestRepository()

    private fun createViewModel(providerId: String = "provider-1") = ProviderDetailViewModel(
        providerId = providerId,
        getProvider = GetProviderUseCase(repository),
        createServiceRequest = CreateServiceRequestUseCase(requestRepository),
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

    @Test
    fun `request contact creates a pending request with provider data`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMessageChange("  Necesito cambiar un enchufe  ")
        viewModel.requestContact(categoryName = "Electricista")
        assertTrue(viewModel.uiState.value.isSubmitting)
        advanceUntilIdle()

        val request = requestRepository.createdRequests.single()
        assertEquals("provider-1", request.providerId)
        assertEquals("electricistas", request.categoryId)
        assertEquals("Carlos Electricidad", request.providerName)
        assertEquals("Electricista", request.categoryName)
        assertEquals("Necesito cambiar un enchufe", request.message)

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.requestSent)
        assertEquals("", state.message)
        assertEquals(ProviderDetailViewModel.REQUEST_SENT_MESSAGE, state.userMessage)
    }

    @Test
    fun `double tap creates a single request`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.requestContact(categoryName = "Electricista")
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        assertEquals(1, requestRepository.createdRequests.size)
    }

    @Test
    fun `failed request shows error and allows retrying`() = runTest {
        requestRepository.createResult = Result.failure(RuntimeException("PERMISSION_DENIED"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMessageChange("Hola")
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.requestSent)
        assertEquals("Hola", state.message)
        assertEquals(ProviderDetailViewModel.REQUEST_ERROR_MESSAGE, state.userMessage)

        viewModel.onUserMessageShown()
        assertNull(viewModel.uiState.value.userMessage)

        requestRepository.createResult = Result.success(Unit)
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.requestSent)
        assertEquals(2, requestRepository.createdRequests.size)
    }

    @Test
    fun `request is ignored while the provider is not loaded`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        assertTrue(requestRepository.createdRequests.isEmpty())
    }

    @Test
    fun `message longer than the limit is not accepted`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMessageChange("a".repeat(CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH + 1))

        assertEquals("", viewModel.uiState.value.message)
    }
}

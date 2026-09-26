package com.example.serviciosya.presentation.request

import com.example.serviciosya.domain.model.RequestStatus
import com.example.serviciosya.domain.usecase.GetMyRequestsUseCase
import com.example.serviciosya.testutil.FakeServiceRequestRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testRequest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RequestsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeServiceRequestRepository()
    private val viewModel = RequestsViewModel(GetMyRequestsUseCase(repository))

    @Test
    fun `loads the user requests newest first`() = runTest {
        repository.myRequestsResult = Result.success(
            listOf(
                testRequest(id = "old", createdAtMillis = 1_000L),
                testRequest(id = "new", createdAtMillis = 3_000L),
                testRequest(id = "mid", createdAtMillis = 2_000L),
            ),
        )

        viewModel.loadRequests()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("new", "mid", "old"), state.requests.map { it.id })
    }

    @Test
    fun `empty list is not an error`() = runTest {
        viewModel.loadRequests()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.requests.isEmpty())
    }

    @Test
    fun `failure shows error and retry recovers`() = runTest {
        repository.myRequestsResult = Result.failure(RuntimeException("offline"))
        viewModel.loadRequests()
        advanceUntilIdle()
        assertEquals(RequestsViewModel.LOAD_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)

        repository.myRequestsResult = Result.success(listOf(testRequest()))
        viewModel.loadRequests()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.requests.size)
    }

    @Test
    fun `refresh keeps current list visible and picks up new requests`() = runTest {
        repository.myRequestsResult = Result.success(listOf(testRequest(id = "a")))
        viewModel.loadRequests()
        advanceUntilIdle()

        repository.myRequestsResult = Result.success(
            listOf(testRequest(id = "a", createdAtMillis = 1L), testRequest(id = "b", createdAtMillis = null)),
        )
        viewModel.loadRequests()
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertEquals(listOf("b", "a"), viewModel.uiState.value.requests.map { it.id })
    }

    @Test
    fun `concurrent loads query the repository once`() = runTest {
        viewModel.loadRequests()
        viewModel.loadRequests()
        advanceUntilIdle()

        assertEquals(1, repository.getMyRequestsCalls)
    }

    @Test
    fun `every status has a spanish label`() {
        assertEquals("Pendiente", RequestStatus.PENDING.label())
        assertEquals("Aceptada", RequestStatus.ACCEPTED.label())
        assertEquals("Rechazada", RequestStatus.REJECTED.label())
        assertEquals("Completada", RequestStatus.COMPLETED.label())
        assertEquals("Cancelada", RequestStatus.CANCELLED.label())
    }
}

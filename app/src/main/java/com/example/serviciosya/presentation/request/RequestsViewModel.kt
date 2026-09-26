package com.example.serviciosya.presentation.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.model.RequestStatus
import com.example.serviciosya.domain.model.ServiceRequest
import com.example.serviciosya.domain.usecase.GetMyRequestsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestsUiState(
    val requests: List<ServiceRequest> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class RequestsViewModel(
    private val getMyRequests: GetMyRequestsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    private var isRefreshing = false

    /** Called every time the screen is shown so new requests appear without manual refresh. */
    fun loadRequests() {
        if (isRefreshing) return
        isRefreshing = true
        viewModelScope.launch {
            // Keep the current list visible while refreshing; only show the spinner when empty.
            _uiState.update { it.copy(isLoading = it.requests.isEmpty(), errorMessage = null) }
            getMyRequests()
                .onSuccess { requests ->
                    _uiState.update { it.copy(requests = requests, isLoading = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(requests = emptyList(), isLoading = false, errorMessage = LOAD_ERROR_MESSAGE)
                    }
                }
            isRefreshing = false
        }
    }

    companion object {
        const val LOAD_ERROR_MESSAGE = "No pudimos cargar tus solicitudes."
    }
}

fun RequestStatus.label(): String = when (this) {
    RequestStatus.PENDING -> "Pendiente"
    RequestStatus.ACCEPTED -> "Aceptada"
    RequestStatus.REJECTED -> "Rechazada"
    RequestStatus.COMPLETED -> "Completada"
    RequestStatus.CANCELLED -> "Cancelada"
}

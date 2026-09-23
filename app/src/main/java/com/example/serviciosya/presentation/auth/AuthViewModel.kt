package com.example.serviciosya.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.model.User
import com.example.serviciosya.domain.usecase.ObserveCurrentUserUseCase
import com.example.serviciosya.domain.usecase.RegisterUserUseCase
import com.example.serviciosya.domain.usecase.SignInUseCase
import com.example.serviciosya.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: User? = null,
    val isSessionLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val registerUser: RegisterUserUseCase,
    private val signInUser: SignInUseCase,
    private val signOutUser: SignOutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCurrentUser()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isSessionLoading = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
                .collect { user ->
                    _uiState.update {
                        it.copy(user = user, isSessionLoading = false)
                    }
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        val validationError = AuthValidator.validateName(name)
            ?: AuthValidator.validateEmail(email)
            ?: AuthValidator.validatePassword(password)
        submit(validationError) { registerUser(name, email, password) }
    }

    fun signIn(email: String, password: String) {
        val validationError = AuthValidator.validateEmail(email)
            ?: AuthValidator.validatePassword(password)
        submit(validationError) { signInUser(email, password) }
    }

    fun signOut() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = signOutUser()
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = result.exceptionOrNull()?.toUserMessage(),
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun submit(
        validationError: String?,
        action: suspend () -> Result<Unit>,
    ) {
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = action()
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = result.exceptionOrNull()?.toUserMessage(),
                )
            }
        }
    }
}

private fun Throwable.toUserMessage(): String = when {
    message?.contains("password", ignoreCase = true) == true ->
        "No pudimos validar la contraseña. Revisala e intentá nuevamente."
    message?.contains("email", ignoreCase = true) == true ->
        "No pudimos validar ese email. Revisalo e intentá nuevamente."
    else -> localizedMessage ?: "Ocurrió un error. Intentá nuevamente."
}

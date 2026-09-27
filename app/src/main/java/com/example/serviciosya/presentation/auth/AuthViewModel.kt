package com.example.serviciosya.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.model.AuthErrorReason
import com.example.serviciosya.domain.model.AuthException
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
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
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
        // Marked before launching so a second tap is ignored even if the coroutine has not started.
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
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

internal fun Throwable.toUserMessage(): String {
    val reason = (this as? AuthException)?.reason ?: AuthErrorReason.UNKNOWN
    return when (reason) {
        AuthErrorReason.EMAIL_ALREADY_IN_USE -> "Ya existe una cuenta con ese email. Iniciá sesión o usá otro email."
        AuthErrorReason.INVALID_EMAIL -> "Ingresá un email válido."
        AuthErrorReason.INVALID_CREDENTIALS -> "El email o la contraseña no son correctos."
        AuthErrorReason.WEAK_PASSWORD -> "La contraseña es muy débil. Usá al menos 6 caracteres."
        AuthErrorReason.USER_DISABLED -> "Esta cuenta está deshabilitada."
        AuthErrorReason.TOO_MANY_REQUESTS -> "Hiciste demasiados intentos. Esperá unos minutos e intentá nuevamente."
        AuthErrorReason.NETWORK -> "No hay conexión a internet. Revisá tu conexión e intentá nuevamente."
        AuthErrorReason.NOT_CONFIGURED -> "La app no está configurada para conectarse al servidor."
        AuthErrorReason.UNKNOWN -> "Ocurrió un error. Intentá nuevamente."
    }
}

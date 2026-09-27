package com.example.serviciosya.presentation.auth

import com.example.serviciosya.domain.model.AuthErrorReason
import com.example.serviciosya.domain.model.AuthException
import com.example.serviciosya.domain.model.User
import com.example.serviciosya.domain.usecase.ObserveCurrentUserUseCase
import com.example.serviciosya.domain.usecase.RegisterUserUseCase
import com.example.serviciosya.domain.usecase.SignInUseCase
import com.example.serviciosya.domain.usecase.SignOutUseCase
import com.example.serviciosya.testutil.FakeAuthRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun createViewModel() = AuthViewModel(
        observeCurrentUser = ObserveCurrentUserUseCase(repository),
        registerUser = RegisterUserUseCase(repository),
        signInUser = SignInUseCase(repository),
        signOutUser = SignOutUseCase(repository),
    )

    @Test
    fun `session starts loading and resolves to the current user`() = runTest {
        val user = User(id = "u1", name = "Ana", email = "ana@example.com")
        repository.currentUser.value = user

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isSessionLoading)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSessionLoading)
        assertEquals(user, viewModel.uiState.value.user)
    }

    @Test
    fun `no session resolves to a null user`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSessionLoading)
        assertNull(viewModel.uiState.value.user)
    }

    @Test
    fun `sign in with invalid fields shows validation error without calling the backend`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signIn("", "secret1")
        assertEquals("Ingresá tu email.", viewModel.uiState.value.errorMessage)
        viewModel.signIn("ana@example.com", "123")
        assertEquals("La contraseña debe tener al menos 6 caracteres.", viewModel.uiState.value.errorMessage)

        assertTrue(repository.signInCalls.isEmpty())
    }

    @Test
    fun `sign in trims the email and shows loading while submitting`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signIn("  ana@example.com ", "secret1")
        assertTrue(viewModel.uiState.value.isSubmitting)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("ana@example.com" to "secret1", repository.signInCalls.single())
    }

    @Test
    fun `wrong credentials show a spanish message`() = runTest {
        repository.signInResult = Result.failure(AuthException(AuthErrorReason.INVALID_CREDENTIALS))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signIn("ana@example.com", "secret1")
        advanceUntilIdle()

        assertEquals("El email o la contraseña no son correctos.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `double tap on sign in calls the backend once`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signIn("ana@example.com", "secret1")
        viewModel.signIn("ana@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(1, repository.signInCalls.size)
    }

    @Test
    fun `register validates name email and password in order`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register(" ", "ana@example.com", "secret1")
        assertEquals("Ingresá tu nombre.", viewModel.uiState.value.errorMessage)
        viewModel.register("Ana", "ana@", "secret1")
        assertEquals("Ingresá un email válido.", viewModel.uiState.value.errorMessage)
        viewModel.register("Ana", "ana@example.com", "")
        assertEquals("Ingresá tu contraseña.", viewModel.uiState.value.errorMessage)

        assertTrue(repository.registerCalls.isEmpty())
    }

    @Test
    fun `register sends trimmed data and double tap registers once`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register(" Ana ", " ana@example.com ", "secret1")
        viewModel.register(" Ana ", " ana@example.com ", "secret1")
        advanceUntilIdle()

        assertEquals(Triple("Ana", "ana@example.com", "secret1"), repository.registerCalls.single())
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `email already in use shows a spanish message`() = runTest {
        repository.registerResult = Result.failure(AuthException(AuthErrorReason.EMAIL_ALREADY_IN_USE))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register("Ana", "ana@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(
            "Ya existe una cuenta con ese email. Iniciá sesión o usá otro email.",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `clear error removes the message`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.signIn("", "")

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sign out clears the user`() = runTest {
        repository.currentUser.value = User(id = "u1", name = "Ana", email = "ana@example.com")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signOut()
        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(1, repository.signOutCalls)
        assertNull(viewModel.uiState.value.user)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `every auth error reason has a spanish message and unknown errors are generic`() {
        AuthErrorReason.entries.forEach { reason ->
            val message = AuthException(reason).toUserMessage()
            assertTrue(message.isNotBlank())
            assertFalse("$reason leaks the enum name", message.contains(reason.name))
        }
        assertEquals("Ocurrió un error. Intentá nuevamente.", RuntimeException("socket timeout").toUserMessage())
        assertEquals(
            "No hay conexión a internet. Revisá tu conexión e intentá nuevamente.",
            AuthException(AuthErrorReason.NETWORK).toUserMessage(),
        )
    }
}

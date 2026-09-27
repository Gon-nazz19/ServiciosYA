package com.example.serviciosya.data.repository

import com.example.serviciosya.domain.model.AuthErrorReason
import com.example.serviciosya.domain.model.AuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AuthErrorMapperTest {
    @Test
    fun `firebase auth error codes map to domain reasons`() {
        assertEquals(AuthErrorReason.EMAIL_ALREADY_IN_USE, authErrorReasonFor("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(AuthErrorReason.INVALID_EMAIL, authErrorReasonFor("ERROR_INVALID_EMAIL"))
        assertEquals(AuthErrorReason.INVALID_CREDENTIALS, authErrorReasonFor("ERROR_WRONG_PASSWORD"))
        assertEquals(AuthErrorReason.INVALID_CREDENTIALS, authErrorReasonFor("ERROR_USER_NOT_FOUND"))
        assertEquals(AuthErrorReason.INVALID_CREDENTIALS, authErrorReasonFor("ERROR_INVALID_CREDENTIAL"))
        assertEquals(AuthErrorReason.WEAK_PASSWORD, authErrorReasonFor("ERROR_WEAK_PASSWORD"))
        assertEquals(AuthErrorReason.USER_DISABLED, authErrorReasonFor("ERROR_USER_DISABLED"))
        assertEquals(AuthErrorReason.TOO_MANY_REQUESTS, authErrorReasonFor("ERROR_TOO_MANY_REQUESTS"))
        assertEquals(AuthErrorReason.NETWORK, authErrorReasonFor("ERROR_NETWORK_REQUEST_FAILED"))
        assertEquals(AuthErrorReason.UNKNOWN, authErrorReasonFor("ERROR_SOMETHING_NEW"))
        assertEquals(AuthErrorReason.UNKNOWN, authErrorReasonFor(null))
    }

    @Test
    fun `non firebase errors become unknown and domain errors are kept`() {
        assertEquals(AuthErrorReason.UNKNOWN, IllegalStateException("boom").toAuthException().reason)
        val domain = AuthException(AuthErrorReason.NOT_CONFIGURED)
        assertSame(domain, domain.toAuthException())
    }

    @Test
    fun `authCall wraps failures and keeps successes`() {
        assertEquals(5, authCall { 5 }.getOrThrow())
        val failure = authCall<Unit> { throw IllegalArgumentException("x") }.exceptionOrNull()
        assertEquals(AuthErrorReason.UNKNOWN, (failure as AuthException).reason)
    }
}

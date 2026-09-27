package com.example.serviciosya.data.repository

import com.example.serviciosya.domain.model.AuthErrorReason
import com.example.serviciosya.domain.model.AuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException

internal fun Throwable.toAuthException(): AuthException = when (this) {
    is AuthException -> this
    is FirebaseNetworkException -> AuthException(AuthErrorReason.NETWORK, this)
    is FirebaseTooManyRequestsException -> AuthException(AuthErrorReason.TOO_MANY_REQUESTS, this)
    is FirebaseAuthException -> AuthException(authErrorReasonFor(errorCode), this)
    else -> AuthException(AuthErrorReason.UNKNOWN, this)
}

/** Maps FirebaseAuthException.errorCode values to domain reasons. */
internal fun authErrorReasonFor(errorCode: String?): AuthErrorReason = when (errorCode) {
    "ERROR_EMAIL_ALREADY_IN_USE" -> AuthErrorReason.EMAIL_ALREADY_IN_USE
    "ERROR_INVALID_EMAIL" -> AuthErrorReason.INVALID_EMAIL
    // With email enumeration protection Firebase reports both cases as INVALID_CREDENTIAL.
    "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" ->
        AuthErrorReason.INVALID_CREDENTIALS
    "ERROR_WEAK_PASSWORD" -> AuthErrorReason.WEAK_PASSWORD
    "ERROR_USER_DISABLED" -> AuthErrorReason.USER_DISABLED
    "ERROR_TOO_MANY_REQUESTS" -> AuthErrorReason.TOO_MANY_REQUESTS
    "ERROR_NETWORK_REQUEST_FAILED" -> AuthErrorReason.NETWORK
    else -> AuthErrorReason.UNKNOWN
}

internal inline fun <T> authCall(block: () -> T): Result<T> =
    runCatching(block).fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(it.toAuthException()) },
    )

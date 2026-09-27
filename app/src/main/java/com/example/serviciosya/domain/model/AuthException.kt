package com.example.serviciosya.domain.model

/** Authentication failure translated from the backend so the UI never depends on Firebase types. */
class AuthException(
    val reason: AuthErrorReason,
    cause: Throwable? = null,
) : Exception(reason.name, cause)

enum class AuthErrorReason {
    EMAIL_ALREADY_IN_USE,
    INVALID_EMAIL,
    INVALID_CREDENTIALS,
    WEAK_PASSWORD,
    USER_DISABLED,
    TOO_MANY_REQUESTS,
    NETWORK,
    NOT_CONFIGURED,
    UNKNOWN,
}

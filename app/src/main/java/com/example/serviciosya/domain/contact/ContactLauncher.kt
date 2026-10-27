package com.example.serviciosya.domain.contact

/** Opens other apps to contact a provider. Every method returns false when no app could handle it. */
interface ContactLauncher {
    fun openWhatsApp(url: String): Boolean

    fun dial(phone: String): Boolean

    fun share(text: String): Boolean
}

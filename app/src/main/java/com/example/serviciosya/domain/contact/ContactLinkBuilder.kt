package com.example.serviciosya.domain.contact

import com.example.serviciosya.domain.model.Provider
import java.net.URLEncoder

/** Builds the links and texts used to contact a provider outside the app. */
object ContactLinkBuilder {
    private const val COUNTRY_CODE = "54"
    private const val MOBILE_PREFIX = "9"
    private const val NATIONAL_NUMBER_LENGTH = 10

    /**
     * Converts an Argentine phone number written in any common way ("3564-400101", "03564 15400101",
     * "+54 9 3564 400101") to the international mobile format WhatsApp expects ("5493564400101").
     * Returns null when the number does not have the 10 digits of a national number.
     */
    fun normalizeArgentinePhone(phone: String): String? {
        var digits = phone.filter(Char::isDigit)
        if (phone.trim().startsWith("+") || digits.length > NATIONAL_NUMBER_LENGTH + 1) {
            digits = digits.removePrefix(COUNTRY_CODE).removePrefix(MOBILE_PREFIX)
        }
        digits = digits.removePrefix("0")
        // Local mobile numbers are often written with "15" after the area code (2 to 4 digits).
        if (digits.length == NATIONAL_NUMBER_LENGTH + 2) {
            digits = (4 downTo 2)
                .firstOrNull { areaLength -> digits.startsWith("15", areaLength) }
                ?.let { areaLength -> digits.removeRange(areaLength, areaLength + 2) }
                ?: return null
        }
        if (digits.length != NATIONAL_NUMBER_LENGTH) return null
        return COUNTRY_CODE + MOBILE_PREFIX + digits
    }

    fun whatsAppMessage(providerName: String, userMessage: String): String =
        listOf("Hola $providerName, te encontré en ServiciosYA.", userMessage.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")

    fun whatsAppUrl(normalizedPhone: String, message: String): String =
        "https://wa.me/$normalizedPhone?text=${encode(message)}"

    fun shareText(provider: Provider, categoryName: String): String = buildString {
        append(provider.name)
        val details = listOf(categoryName, provider.city).filter { it.isNotBlank() }
        if (details.isNotEmpty()) append(" - ").append(details.joinToString(", "))
        if (provider.phone.isNotBlank()) append("\nTeléfono: ").append(provider.phone)
        append("\nLo encontré en ServiciosYA.")
    }

    /** URLEncoder writes spaces as "+", which wa.me shows literally; %20 is safe everywhere. */
    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}

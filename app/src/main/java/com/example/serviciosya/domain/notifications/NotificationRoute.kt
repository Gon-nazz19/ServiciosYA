package com.example.serviciosya.domain.notifications

/**
 * Where a tapped notification takes the user. Campaigns send it as the "route" data field, with
 * the same shape as the app routes: "category/{id}", "provider/{id}" or "requests".
 */
sealed interface NotificationRoute {
    data class Category(val categoryId: String) : NotificationRoute

    data class Provider(val providerId: String) : NotificationRoute

    data object Requests : NotificationRoute

    companion object {
        private val ID = Regex("[A-Za-z0-9_-]+")

        /** Null for an empty or unknown route: the app then just opens on its usual screen. */
        fun parse(raw: String?): NotificationRoute? {
            val parts = raw?.trim()?.trim('/')?.split('/') ?: return null
            return when {
                parts == listOf("requests") -> Requests
                parts.size == 2 && parts[1].matches(ID) -> when (parts[0]) {
                    "category" -> Category(parts[1])
                    "provider" -> Provider(parts[1])
                    else -> null
                }
                else -> null
            }
        }

        fun category(categoryId: String) = "category/$categoryId"

        fun provider(providerId: String) = "provider/$providerId"
    }
}

/** A notification the user tapped. [type] is "campaign" or "reminder"; [campaignId] is a short code. */
data class NotificationOpen(
    val route: NotificationRoute?,
    val type: String,
    val campaignId: String,
) {
    companion object {
        const val TYPE_CAMPAIGN = "campaign"
        const val TYPE_REMINDER = "reminder"

        /** Intent extras and FCM data keys. */
        const val EXTRA_ROUTE = "route"
        const val EXTRA_TYPE = "type"
        const val EXTRA_CAMPAIGN_ID = "campaign_id"
    }
}

/** Follow-up reminder some time after the user contacted a provider (strategy 2). */
interface ReminderScheduler {
    fun scheduleFollowUp(providerId: String, providerName: String)
}

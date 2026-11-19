package com.example.serviciosya.domain.notifications

import com.example.serviciosya.domain.usecase.normalizeForSearch

/**
 * Push campaigns are segmented by FCM topics, so the app never stores tokens: subscribing the
 * device to a topic is enough for a campaign sent from the console or from tools/push to reach it.
 */
interface PushMessaging {
    /** False when the user denied the notification permission or turned notifications off. */
    fun areNotificationsEnabled(): Boolean

    suspend fun subscribe(topic: String): Boolean

    suspend fun unsubscribe(topic: String): Boolean
}

object Topics {
    /** Every user who accepted notifications: news and re-engagement campaigns. */
    const val NEWS = "novedades"

    private const val CATEGORY_PREFIX = "categoria_"

    /** FCM topic names only allow letters, digits and -_.~%, so accents are dropped too. */
    fun category(categoryId: String): String =
        CATEGORY_PREFIX + normalizeForSearch(categoryId).replace(INVALID, "_")

    private val INVALID = Regex("[^a-z0-9_.~%-]")
}

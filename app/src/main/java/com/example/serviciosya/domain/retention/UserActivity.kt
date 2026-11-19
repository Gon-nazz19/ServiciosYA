package com.example.serviciosya.domain.retention

import kotlinx.coroutines.flow.Flow

/**
 * What the signed-in user did in the app, kept on the device. It feeds the personalized
 * "Para vos" section and the achievements; nothing of it is sent to Analytics.
 */
data class UserActivity(
    /** Most recent first, at most [MAX_RECENT]. */
    val recentProviderIds: List<String> = emptyList(),
    /** Most recently added first. */
    val favoriteProviderIds: List<String> = emptyList(),
    val exploredCategoryIds: Set<String> = emptySet(),
    val milestones: Set<Milestone> = emptySet(),
    /** Null until the user answers the notifications prompt. Belongs to the device, not to the user. */
    val notificationsOptIn: Boolean? = null,
) {
    companion object {
        const val MAX_RECENT = 5
    }
}

enum class Milestone {
    ASSISTANT_USED,
    FIRST_CONTACT,
    NOTIFICATIONS_ENABLED,
}

interface UserActivityStore {
    /** Emits again whenever something changes, and when another user signs in. */
    fun observe(): Flow<UserActivity>

    suspend fun snapshot(): UserActivity

    suspend fun addRecentProvider(providerId: String)

    suspend fun addExploredCategory(categoryId: String)

    suspend fun setFavorite(providerId: String, favorite: Boolean)

    suspend fun addMilestone(milestone: Milestone)

    suspend fun setNotificationsOptIn(optIn: Boolean)
}

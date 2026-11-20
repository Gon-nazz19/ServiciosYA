package com.example.serviciosya.domain.retention

import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.notifications.PushMessaging
import com.example.serviciosya.domain.notifications.Topics

/**
 * Single entry point the screens use to record activity. It keeps the store, the category topics
 * and the achievement events consistent, so no ViewModel repeats that logic.
 */
class RetentionRecorder(
    private val store: UserActivityStore,
    private val push: PushMessaging,
    private val analytics: AnalyticsTracker,
) {
    suspend fun providerViewed(providerId: String) = store.addRecentProvider(providerId)

    /** Also subscribes to the category topic when the user accepted notifications (strategy 1). */
    suspend fun categoryViewed(categoryId: String) {
        trackingAchievements { store.addExploredCategory(categoryId) }
        if (store.snapshot().notificationsOptIn == true && push.areNotificationsEnabled()) {
            push.subscribe(Topics.category(categoryId))
        }
    }

    suspend fun assistantUsed() = trackingAchievements { store.addMilestone(Milestone.ASSISTANT_USED) }

    suspend fun contactMade() = trackingAchievements { store.addMilestone(Milestone.FIRST_CONTACT) }

    suspend fun setFavorite(providerId: String, favorite: Boolean) {
        trackingAchievements { store.setFavorite(providerId, favorite) }
        analytics.trackFavoriteChanged(providerId, added = favorite)
    }

    suspend fun isFavorite(providerId: String): Boolean = providerId in store.snapshot().favoriteProviderIds

    /**
     * Answer to the notifications prompt or the switch in the profile. Turning them on subscribes to
     * the news topic and to every category already explored; turning them off undoes it.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.setNotificationsOptIn(enabled)
        val topics = listOf(Topics.NEWS) + store.snapshot().exploredCategoryIds.map(Topics::category)
        if (enabled) {
            topics.forEach { push.subscribe(it) }
            trackingAchievements { store.addMilestone(Milestone.NOTIFICATIONS_ENABLED) }
        } else {
            topics.forEach { push.unsubscribe(it) }
        }
    }

    private suspend fun trackingAchievements(change: suspend () -> Unit) {
        val before = Achievements.unlockedIds(store.snapshot())
        change()
        (Achievements.unlockedIds(store.snapshot()) - before).forEach(analytics::trackAchievementUnlocked)
    }
}

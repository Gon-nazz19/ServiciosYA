package com.example.serviciosya.data.retention

import android.content.Context
import android.content.SharedPreferences
import com.example.serviciosya.domain.retention.Milestone
import com.example.serviciosya.domain.retention.UserActivity
import com.example.serviciosya.domain.retention.UserActivityStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Keeps the activity on the device. Keys are prefixed with the user id so two accounts on the same
 * phone never see each other's history; the notifications answer is per device.
 */
class SharedPrefsUserActivityStore(
    context: Context,
    private val currentUserId: () -> String?,
) : UserActivityStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun observe(): Flow<UserActivity> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        trySend(read())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun snapshot(): UserActivity = read()

    override suspend fun addRecentProvider(providerId: String) {
        val recent = listOf(providerId) + (list(KEY_RECENT) - providerId)
        putList(KEY_RECENT, recent.take(UserActivity.MAX_RECENT))
    }

    override suspend fun addExploredCategory(categoryId: String) {
        putList(KEY_CATEGORIES, (list(KEY_CATEGORIES) + categoryId).distinct())
    }

    override suspend fun setFavorite(providerId: String, favorite: Boolean) {
        val others = list(KEY_FAVORITES) - providerId
        putList(KEY_FAVORITES, if (favorite) listOf(providerId) + others else others)
    }

    override suspend fun addMilestone(milestone: Milestone) {
        putList(KEY_MILESTONES, (list(KEY_MILESTONES) + milestone.name).distinct())
    }

    override suspend fun setNotificationsOptIn(optIn: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, optIn).apply()
    }

    private fun read() = UserActivity(
        recentProviderIds = list(KEY_RECENT),
        favoriteProviderIds = list(KEY_FAVORITES),
        exploredCategoryIds = list(KEY_CATEGORIES).toSet(),
        milestones = list(KEY_MILESTONES).mapNotNullTo(mutableSetOf()) { name ->
            Milestone.entries.firstOrNull { it.name == name }
        },
        notificationsOptIn = if (prefs.contains(KEY_NOTIFICATIONS)) prefs.getBoolean(KEY_NOTIFICATIONS, false) else null,
    )

    private fun userKey(key: String) = "${currentUserId() ?: "anonymous"}.$key"

    private fun list(key: String): List<String> =
        prefs.getString(userKey(key), null)?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private fun putList(key: String, values: List<String>) {
        prefs.edit().putString(userKey(key), values.joinToString(SEPARATOR)).apply()
    }

    private companion object {
        const val PREFS = "user_activity"
        const val SEPARATOR = ","
        const val KEY_RECENT = "recent_providers"
        const val KEY_FAVORITES = "favorite_providers"
        const val KEY_CATEGORIES = "explored_categories"
        const val KEY_MILESTONES = "milestones"
        const val KEY_NOTIFICATIONS = "notifications_opt_in"
    }
}

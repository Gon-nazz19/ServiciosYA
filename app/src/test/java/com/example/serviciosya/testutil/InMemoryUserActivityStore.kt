package com.example.serviciosya.testutil

import com.example.serviciosya.domain.retention.Milestone
import com.example.serviciosya.domain.retention.UserActivity
import com.example.serviciosya.domain.retention.UserActivityStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryUserActivityStore(
    initial: UserActivity = UserActivity(),
) : UserActivityStore {
    val state = MutableStateFlow(initial)

    override fun observe(): Flow<UserActivity> = state

    override suspend fun snapshot(): UserActivity = state.value

    override suspend fun addRecentProvider(providerId: String) = state.update {
        it.copy(recentProviderIds = (listOf(providerId) + (it.recentProviderIds - providerId)).take(UserActivity.MAX_RECENT))
    }

    override suspend fun addExploredCategory(categoryId: String) = state.update {
        it.copy(exploredCategoryIds = it.exploredCategoryIds + categoryId)
    }

    override suspend fun setFavorite(providerId: String, favorite: Boolean) = state.update {
        val others = it.favoriteProviderIds - providerId
        it.copy(favoriteProviderIds = if (favorite) listOf(providerId) + others else others)
    }

    override suspend fun addMilestone(milestone: Milestone) = state.update {
        it.copy(milestones = it.milestones + milestone)
    }

    override suspend fun setNotificationsOptIn(optIn: Boolean) = state.update {
        it.copy(notificationsOptIn = optIn)
    }
}

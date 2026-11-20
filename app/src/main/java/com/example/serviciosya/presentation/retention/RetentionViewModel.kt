package com.example.serviciosya.presentation.retention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.notifications.PushMessaging
import com.example.serviciosya.domain.repository.ProviderRepository
import com.example.serviciosya.domain.retention.Achievement
import com.example.serviciosya.domain.retention.Achievements
import com.example.serviciosya.domain.retention.Milestone
import com.example.serviciosya.domain.retention.RetentionRecorder
import com.example.serviciosya.domain.retention.UserActivity
import com.example.serviciosya.domain.retention.UserActivityStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RetentionUiState(
    /** "Para vos" in Home (strategy 3). */
    val recentProviders: List<Provider> = emptyList(),
    val favoriteProviders: List<Provider> = emptyList(),
    /** Asked once, after the first contact: the moment the user sees why reminders are useful. */
    val showNotificationPrompt: Boolean = false,
    val notificationsEnabled: Boolean = false,
    /** Strategy 4. */
    val achievements: List<Achievement> = Achievements.of(UserActivity()),
) {
    val unlockedCount: Int get() = achievements.count { it.unlocked }
}

/**
 * Lives at app level, like HomeViewModel: Home and Profile show the same activity. It is restarted
 * when a user signs in so it never shows another account's history.
 */
class RetentionViewModel(
    private val store: UserActivityStore,
    private val recorder: RetentionRecorder,
    private val providerRepository: ProviderRepository,
    private val push: PushMessaging,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RetentionUiState())
    val uiState: StateFlow<RetentionUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var providersById: Map<String, Provider> = emptyMap()

    fun start() {
        observeJob?.cancel()
        providersById = emptyMap()
        _uiState.value = RetentionUiState()
        observeJob = viewModelScope.launch {
            store.observe().collect { activity -> publish(activity) }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        analytics.trackNotificationPermissionResult(granted)
        viewModelScope.launch { recorder.setNotificationsEnabled(granted) }
    }

    /** "Ahora no" in the prompt, or turning the switch off in the profile. */
    fun disableNotifications() {
        viewModelScope.launch { recorder.setNotificationsEnabled(false) }
    }

    private suspend fun publish(activity: UserActivity) {
        val ids = activity.recentProviderIds + activity.favoriteProviderIds
        if (ids.any { it !in providersById }) {
            // One read of the catalog resolves every id; a provider removed from it is just skipped.
            providerRepository.getActiveProviders().onSuccess { providers ->
                providersById = providers.associateBy { it.id }
            }
        }
        _uiState.update {
            it.copy(
                recentProviders = activity.recentProviderIds.mapNotNull(providersById::get),
                favoriteProviders = activity.favoriteProviderIds.mapNotNull(providersById::get),
                showNotificationPrompt = activity.notificationsOptIn == null &&
                    Milestone.FIRST_CONTACT in activity.milestones,
                notificationsEnabled = activity.notificationsOptIn == true && push.areNotificationsEnabled(),
                achievements = Achievements.of(activity),
            )
        }
    }
}

class RetentionViewModelFactory(
    private val store: UserActivityStore,
    private val recorder: RetentionRecorder,
    private val providerRepository: ProviderRepository,
    private val push: PushMessaging,
    private val analytics: AnalyticsTracker,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RetentionViewModel::class.java))
        return RetentionViewModel(store, recorder, providerRepository, push, analytics) as T
    }
}

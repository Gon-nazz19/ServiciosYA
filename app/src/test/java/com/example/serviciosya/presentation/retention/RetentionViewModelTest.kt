package com.example.serviciosya.presentation.retention

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.retention.Milestone
import com.example.serviciosya.domain.retention.RetentionRecorder
import com.example.serviciosya.domain.retention.UserActivity
import com.example.serviciosya.testutil.FakeAnalyticsTracker
import com.example.serviciosya.testutil.FakeProviderRepository
import com.example.serviciosya.testutil.FakePushMessaging
import com.example.serviciosya.testutil.InMemoryUserActivityStore
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RetentionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemoryUserActivityStore()
    private val push = FakePushMessaging()
    private val analytics = FakeAnalyticsTracker()
    private val providers = FakeProviderRepository(
        allProvidersResult = Result.success(
            listOf(testProvider(id = "a", name = "Carlos"), testProvider(id = "b", name = "Plomería García")),
        ),
    )

    private fun createViewModel() = RetentionViewModel(
        store = store,
        recorder = RetentionRecorder(store, push, analytics),
        providerRepository = providers,
        push = push,
        analytics = analytics,
    ).also { it.start() }

    @Test
    fun `recent and favorite ids are resolved to providers in order`() = runTest {
        store.state.value = UserActivity(recentProviderIds = listOf("b", "missing", "a"), favoriteProviderIds = listOf("a"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("b", "a"), state.recentProviders.map { it.id })
        assertEquals(listOf("a"), state.favoriteProviders.map { it.id })
    }

    @Test
    fun `the catalog is read once while the ids are known`() = runTest {
        store.state.value = UserActivity(recentProviderIds = listOf("a"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        store.addRecentProvider("b")
        advanceUntilIdle()

        assertEquals(listOf("b", "a"), viewModel.uiState.value.recentProviders.map { it.id })
        assertEquals(1, providers.allProvidersCalls)
    }

    @Test
    fun `the notifications prompt appears only after the first contact and until answered`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showNotificationPrompt)

        store.addMilestone(Milestone.FIRST_CONTACT)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showNotificationPrompt)

        viewModel.disableNotifications()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showNotificationPrompt)
    }

    @Test
    fun `granting the permission enables notifications and is tracked`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNotificationPermissionResult(granted = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notificationsEnabled)
        assertTrue("novedades" in push.subscribedTopics)
        val event = analytics.eventsNamed(AnalyticsEvents.NOTIFICATION_PERMISSION_RESULT).single()
        assertEquals("true", event.params[AnalyticsParams.GRANTED])
        assertTrue(viewModel.uiState.value.achievements.first { it.id == "notifications_on" }.unlocked)
    }

    @Test
    fun `denying the permission keeps notifications off and hides the prompt`() = runTest {
        store.addMilestone(Milestone.FIRST_CONTACT)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNotificationPermissionResult(granted = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.notificationsEnabled)
        assertFalse(state.showNotificationPrompt)
        assertTrue(push.subscribedTopics.isEmpty())
    }

    @Test
    fun `notifications show as off when the system blocks them`() = runTest {
        store.setNotificationsOptIn(true)
        push.enabled = false

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun `restarting for another user clears the previous state first`() = runTest {
        store.state.value = UserActivity(recentProviderIds = listOf("a"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        store.state.value = UserActivity()
        viewModel.start()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.recentProviders.isEmpty())
        assertEquals(0, viewModel.uiState.value.unlockedCount)
    }
}

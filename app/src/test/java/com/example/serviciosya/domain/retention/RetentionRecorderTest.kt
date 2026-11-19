package com.example.serviciosya.domain.retention

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.testutil.FakeAnalyticsTracker
import com.example.serviciosya.testutil.FakePushMessaging
import com.example.serviciosya.testutil.InMemoryUserActivityStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionRecorderTest {
    private val store = InMemoryUserActivityStore()
    private val push = FakePushMessaging()
    private val analytics = FakeAnalyticsTracker()
    private val recorder = RetentionRecorder(store, push, analytics)

    private fun unlocked() = analytics.eventsNamed(AnalyticsEvents.ACHIEVEMENT_UNLOCKED)
        .map { it.params[AnalyticsParams.ACHIEVEMENT_ID] }

    @Test
    fun `recent providers keep the latest five without duplicates`() = runTest {
        listOf("a", "b", "c", "d", "e", "f", "b").forEach { recorder.providerViewed(it) }

        assertEquals(listOf("b", "f", "e", "d", "c"), store.state.value.recentProviderIds)
    }

    @Test
    fun `each achievement is tracked once when it unlocks`() = runTest {
        recorder.contactMade()
        recorder.contactMade()
        recorder.assistantUsed()

        assertEquals(listOf("first_contact", "first_assistant"), unlocked())
    }

    @Test
    fun `explorer unlocks at the third different category`() = runTest {
        recorder.categoryViewed("electricistas")
        recorder.categoryViewed("electricistas")
        recorder.categoryViewed("plomeros")
        assertTrue(unlocked().isEmpty())

        recorder.categoryViewed("jardineros")

        assertEquals(listOf("explorer"), unlocked())
    }

    @Test
    fun `category topic is subscribed only after accepting notifications`() = runTest {
        recorder.categoryViewed("electricistas")
        assertTrue(push.subscribedTopics.isEmpty())

        recorder.setNotificationsEnabled(true)
        recorder.categoryViewed("plomeros")

        assertEquals(setOf("novedades", "categoria_electricistas", "categoria_plomeros"), push.subscribedTopics)
        assertTrue("notifications_on" in unlocked())
    }

    @Test
    fun `no topic is subscribed when the system blocks notifications`() = runTest {
        store.setNotificationsOptIn(true)
        push.enabled = false

        recorder.categoryViewed("electricistas")

        assertTrue(push.subscribedTopics.isEmpty())
    }

    @Test
    fun `turning notifications off leaves every topic`() = runTest {
        recorder.setNotificationsEnabled(true)
        recorder.categoryViewed("electricistas")

        recorder.setNotificationsEnabled(false)

        assertTrue(push.subscribedTopics.isEmpty())
        assertEquals(setOf("novedades", "categoria_electricistas"), push.unsubscribedTopics.toSet())
        assertEquals(false, store.state.value.notificationsOptIn)
    }

    @Test
    fun `favorites are tracked without personal data and unlock their badge`() = runTest {
        recorder.setFavorite("carlos", favorite = true)
        assertTrue(recorder.isFavorite("carlos"))
        recorder.setFavorite("carlos", favorite = false)

        assertFalse(recorder.isFavorite("carlos"))
        assertEquals(1, analytics.eventsNamed(AnalyticsEvents.FAVORITE_ADDED).size)
        assertEquals("carlos", analytics.eventsNamed(AnalyticsEvents.FAVORITE_REMOVED).single().params[AnalyticsParams.PROVIDER_ID])
        assertEquals(listOf("first_favorite"), unlocked())
    }

    @Test
    fun `achievements list reflects the activity`() {
        val activity = UserActivity(
            exploredCategoryIds = setOf("a", "b", "c"),
            milestones = setOf(Milestone.FIRST_CONTACT),
        )

        val achievements = Achievements.of(activity)

        assertEquals(5, achievements.size)
        assertEquals(setOf("first_contact", "explorer"), Achievements.unlockedIds(activity))
    }
}

package com.example.serviciosya.testutil

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.analytics.AnalyticsTracker

data class TrackedEvent(val name: String, val params: Map<String, String>)

class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<TrackedEvent>()

    fun eventsNamed(name: String) = events.filter { it.name == name }

    override fun trackCategoryView(categoryId: String, categoryName: String) {
        events += TrackedEvent(
            AnalyticsEvents.CATEGORY_VIEW,
            mapOf(AnalyticsParams.CATEGORY_ID to categoryId, AnalyticsParams.CATEGORY_NAME to categoryName),
        )
    }

    override fun trackProviderView(providerId: String, categoryId: String) {
        events += TrackedEvent(
            AnalyticsEvents.PROVIDER_VIEW,
            mapOf(AnalyticsParams.PROVIDER_ID to providerId, AnalyticsParams.CATEGORY_ID to categoryId),
        )
    }

    override fun trackContactRequest(providerId: String, categoryId: String) {
        events += TrackedEvent(
            AnalyticsEvents.CONTACT_REQUEST,
            mapOf(AnalyticsParams.PROVIDER_ID to providerId, AnalyticsParams.CATEGORY_ID to categoryId),
        )
    }

    override fun trackServiceSearch(query: String) {
        events += TrackedEvent(AnalyticsEvents.SERVICE_SEARCH, mapOf(AnalyticsParams.QUERY to query))
    }
}

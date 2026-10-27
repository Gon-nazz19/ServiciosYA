package com.example.serviciosya.data.analytics

import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.analytics.ContactChannel

/** Used when Firebase is not configured, so screens can track events unconditionally. */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun trackCategoryView(categoryId: String, categoryName: String) = Unit

    override fun trackProviderView(providerId: String, categoryId: String, distanceBucket: String) = Unit

    override fun trackContactRequest(providerId: String, categoryId: String) = Unit

    override fun trackServiceSearch(query: String) = Unit

    override fun trackContactChannel(channel: ContactChannel, providerId: String, categoryId: String) = Unit

    override fun trackLocationPermissionResult(granted: Boolean) = Unit
}

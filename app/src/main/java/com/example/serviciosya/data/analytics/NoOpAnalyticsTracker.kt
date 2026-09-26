package com.example.serviciosya.data.analytics

import com.example.serviciosya.domain.analytics.AnalyticsTracker

/** Used when Firebase is not configured, so screens can track events unconditionally. */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun trackCategoryView(categoryId: String, categoryName: String) = Unit

    override fun trackProviderView(providerId: String, categoryId: String) = Unit

    override fun trackContactRequest(providerId: String, categoryId: String) = Unit

    override fun trackServiceSearch(query: String) = Unit
}

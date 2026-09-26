package com.example.serviciosya.data.analytics

import android.os.Bundle
import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {
    override fun trackCategoryView(categoryId: String, categoryName: String) = log(
        AnalyticsEvents.CATEGORY_VIEW,
        AnalyticsParams.CATEGORY_ID to categoryId,
        AnalyticsParams.CATEGORY_NAME to categoryName,
    )

    override fun trackProviderView(providerId: String, categoryId: String) = log(
        AnalyticsEvents.PROVIDER_VIEW,
        AnalyticsParams.PROVIDER_ID to providerId,
        AnalyticsParams.CATEGORY_ID to categoryId,
    )

    override fun trackContactRequest(providerId: String, categoryId: String) = log(
        AnalyticsEvents.CONTACT_REQUEST,
        AnalyticsParams.PROVIDER_ID to providerId,
        AnalyticsParams.CATEGORY_ID to categoryId,
    )

    override fun trackServiceSearch(query: String) = log(
        AnalyticsEvents.SERVICE_SEARCH,
        AnalyticsParams.QUERY to query,
    )

    private fun log(event: String, vararg params: Pair<String, String>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            bundle.putString(key, value.take(AnalyticsParams.MAX_VALUE_LENGTH))
        }
        analytics.logEvent(event, bundle)
    }
}

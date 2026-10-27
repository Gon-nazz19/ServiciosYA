package com.example.serviciosya.data.analytics

import android.os.Bundle
import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.analytics.AssistantResultType
import com.example.serviciosya.domain.analytics.ContactChannel
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {
    override fun trackCategoryView(categoryId: String, categoryName: String) = log(
        AnalyticsEvents.CATEGORY_VIEW,
        AnalyticsParams.CATEGORY_ID to categoryId,
        AnalyticsParams.CATEGORY_NAME to categoryName,
    )

    override fun trackProviderView(providerId: String, categoryId: String, distanceBucket: String) = log(
        AnalyticsEvents.PROVIDER_VIEW,
        AnalyticsParams.PROVIDER_ID to providerId,
        AnalyticsParams.CATEGORY_ID to categoryId,
        AnalyticsParams.DISTANCE_BUCKET to distanceBucket,
    )

    override fun trackContactRequest(providerId: String, categoryId: String, fromAssistant: Boolean) = log(
        AnalyticsEvents.CONTACT_REQUEST,
        AnalyticsParams.PROVIDER_ID to providerId,
        AnalyticsParams.CATEGORY_ID to categoryId,
        AnalyticsParams.FROM_ASSISTANT to fromAssistant.toString(),
    )

    override fun trackServiceSearch(query: String) = log(
        AnalyticsEvents.SERVICE_SEARCH,
        AnalyticsParams.QUERY to query,
    )

    override fun trackContactChannel(channel: ContactChannel, providerId: String, categoryId: String) = log(
        AnalyticsEvents.CONTACT_CHANNEL,
        AnalyticsParams.CHANNEL to channel.value,
        AnalyticsParams.PROVIDER_ID to providerId,
        AnalyticsParams.CATEGORY_ID to categoryId,
    )

    override fun trackLocationPermissionResult(granted: Boolean) = log(
        AnalyticsEvents.LOCATION_PERMISSION_RESULT,
        AnalyticsParams.GRANTED to granted.toString(),
    )

    override fun trackAssistantUsed(result: AssistantResultType, categoryId: String) = log(
        AnalyticsEvents.ASSISTANT_USED,
        AnalyticsParams.RESULT to result.value,
        AnalyticsParams.CATEGORY_ID to categoryId,
    )

    override fun trackAssistantUnmatched(service: String) = log(
        AnalyticsEvents.ASSISTANT_UNMATCHED,
        AnalyticsParams.SERVICE to service,
    )

    private fun log(event: String, vararg params: Pair<String, String>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            bundle.putString(key, value.take(AnalyticsParams.MAX_VALUE_LENGTH))
        }
        analytics.logEvent(event, bundle)
    }
}

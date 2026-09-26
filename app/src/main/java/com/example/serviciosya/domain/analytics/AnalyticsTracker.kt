package com.example.serviciosya.domain.analytics

/**
 * Usage events of the MVP funnel. Implementations must never send personal data
 * (name, email, phone or user id).
 */
interface AnalyticsTracker {
    fun trackCategoryView(categoryId: String, categoryName: String)

    fun trackProviderView(providerId: String, categoryId: String)

    fun trackContactRequest(providerId: String, categoryId: String)

    fun trackServiceSearch(query: String)
}

object AnalyticsEvents {
    const val CATEGORY_VIEW = "category_view"
    const val PROVIDER_VIEW = "provider_view"
    const val CONTACT_REQUEST = "contact_request"
    const val SERVICE_SEARCH = "service_search"
}

object AnalyticsParams {
    const val CATEGORY_ID = "category_id"
    const val CATEGORY_NAME = "category_name"
    const val PROVIDER_ID = "provider_id"
    const val QUERY = "query"

    /** Firebase Analytics drops parameter values longer than 100 characters. */
    const val MAX_VALUE_LENGTH = 100
}

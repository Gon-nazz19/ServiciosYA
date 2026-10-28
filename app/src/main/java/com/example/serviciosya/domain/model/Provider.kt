package com.example.serviciosya.domain.model

data class Provider(
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val city: String,
    /** Null for providers created before locations were added; they are listed last. */
    val latitude: Double?,
    val longitude: Double?,
    val profileImageUrl: String,
    val phone: String,
    val rating: Double?,
    val reviewCount: Int,
    val verified: Boolean,
    val active: Boolean,
    /** Epoch millis until which the provider has an active "Destacado" plan; null when it never had one. */
    val featuredUntil: Long? = null,
) {
    val location: GeoPoint?
        get() = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null

    /** An expired plan does not count. */
    fun isFeaturedAt(nowMillis: Long): Boolean = featuredUntil != null && featuredUntil > nowMillis
}

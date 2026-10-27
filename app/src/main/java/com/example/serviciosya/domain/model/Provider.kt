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
) {
    val location: GeoPoint?
        get() = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null
}

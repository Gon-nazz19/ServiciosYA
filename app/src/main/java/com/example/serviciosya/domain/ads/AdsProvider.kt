package com.example.serviciosya.domain.ads

/**
 * Source of native ads for the provider list. Requests must never carry user data
 * (uid, location or searches).
 */
interface AdsProvider {
    suspend fun loadNativeAd(): AdResult
}

sealed interface AdResult {
    data class Loaded(val ad: NativeAdContent) : AdResult

    /** [reason] is a short code such as "no_fill" or "network", never free text. */
    data class NoAd(val reason: String) : AdResult {
        companion object {
            /** Ads are switched off, so nothing was requested and nothing failed. */
            const val REASON_DISABLED = "disabled"
        }
    }
}

/**
 * What the screen needs to draw a native ad. [source] is the opaque ad object of the SDK, kept so the
 * UI can register the view with it; the domain never looks inside.
 */
data class NativeAdContent(
    val headline: String,
    val body: String? = null,
    val callToAction: String? = null,
    val advertiser: String? = null,
    val source: Any? = null,
)

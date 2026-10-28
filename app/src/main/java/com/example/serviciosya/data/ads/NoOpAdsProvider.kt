package com.example.serviciosya.data.ads

import com.example.serviciosya.domain.ads.AdResult
import com.example.serviciosya.domain.ads.AdsProvider

/** Used when ads are not available or not configured: it asks for nothing and shows nothing. */
class NoOpAdsProvider : AdsProvider {
    override suspend fun loadNativeAd(): AdResult = AdResult.NoAd(AdResult.NoAd.REASON_DISABLED)
}

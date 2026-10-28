package com.example.serviciosya.testutil

import com.example.serviciosya.domain.ads.AdResult
import com.example.serviciosya.domain.ads.AdsProvider
import com.example.serviciosya.domain.ads.NativeAdContent

class FakeAdsProvider : AdsProvider {
    var result: AdResult = AdResult.Loaded(NativeAdContent(headline = "Anuncio de prueba"))
    var loadCalls = 0
        private set

    override suspend fun loadNativeAd(): AdResult {
        loadCalls++
        return result
    }
}

package com.example.serviciosya.data.ads

import android.content.Context
import android.util.Log
import com.example.serviciosya.domain.ads.AdResult
import com.example.serviciosya.domain.ads.AdsProvider
import com.example.serviciosya.domain.ads.NativeAdContent
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Native ads through the Google Mobile Ads SDK (GMA Next-Gen). The request carries no user data:
 * no uid, location, keywords or search text. Impressions and clicks are reported to Analytics
 * without parameters.
 */
class AdMobAdsProvider(
    context: Context,
    private val appId: String,
    private val nativeUnitId: String,
    private val analytics: AnalyticsTracker,
) : AdsProvider {
    private val appContext = context.applicationContext
    private val initMutex = Mutex()
    private var initialized = false

    override suspend fun loadNativeAd(): AdResult =
        try {
            ensureInitialized()
            requestNativeAd()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Native ad request failed", e)
            AdResult.NoAd(REASON_ERROR)
        }

    private suspend fun ensureInitialized() {
        initMutex.withLock {
            if (initialized) return
            // The SDK asks to be initialized off the main thread.
            withContext(Dispatchers.IO) {
                MobileAds.initialize(appContext, InitializationConfig.Builder(appId).build())
            }
            initialized = true
        }
    }

    private suspend fun requestNativeAd(): AdResult = suspendCancellableCoroutine { continuation ->
        val request = NativeAdRequest.Builder(nativeUnitId, listOf(NativeAd.NativeAdType.NATIVE)).build()
        NativeAdLoader.load(
            request,
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdImpression() = analytics.trackAdImpression()

                        override fun onAdClicked() = analytics.trackAdClick()
                    }
                    if (continuation.isActive) continuation.resume(AdResult.Loaded(nativeAd.toContent()))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${adError.code}")
                    if (continuation.isActive) continuation.resume(AdResult.NoAd(adError.reason()))
                }
            },
        )
    }

    private fun NativeAd.toContent() = NativeAdContent(
        headline = headline.orEmpty(),
        body = body,
        callToAction = callToAction,
        advertiser = advertiser,
        source = this,
    )

    private fun LoadAdError.reason(): String = when (code) {
        LoadAdError.ErrorCode.NO_FILL -> "no_fill"
        LoadAdError.ErrorCode.NETWORK_ERROR -> "network"
        LoadAdError.ErrorCode.TIMEOUT -> "timeout"
        else -> REASON_ERROR
    }

    private companion object {
        const val TAG = "AdMobAdsProvider"
        const val REASON_ERROR = "error"
    }
}

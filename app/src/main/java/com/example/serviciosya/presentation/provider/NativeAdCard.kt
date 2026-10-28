package com.example.serviciosya.presentation.provider

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.serviciosya.domain.ads.NativeAdContent
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

/**
 * Native ad drawn like a provider card. The "Anuncio" label is our own view and is always visible.
 * The SDK records impressions and clicks on the registered views.
 */
@Composable
fun NativeAdCard(ad: NativeAdContent, modifier: Modifier = Modifier) {
    val nativeAd = ad.source as? NativeAd ?: return
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val accentColor = MaterialTheme.colorScheme.primary.toArgb()

    Card(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context -> AdViews.create(context, textColor, secondaryColor, accentColor) },
            update = { adView -> AdViews.bind(adView, nativeAd) },
            onRelease = { adView -> (adView as? NativeAdView)?.destroy() },
        )
    }
}

/** Builds the view tree of the ad in code, so no layout resources are needed. */
private object AdViews {
    private const val TAG_LABEL = "label"
    private const val TAG_ADVERTISER = "advertiser"
    private const val TAG_HEADLINE = "headline"
    private const val TAG_BODY = "body"
    private const val TAG_MEDIA = "media"
    private const val TAG_CTA = "cta"

    fun create(context: Context, textColor: Int, secondaryColor: Int, accentColor: Int): View {
        val adView = NativeAdView(context)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = context.dp(16)
            setPadding(padding, padding, padding, padding)
        }

        val label = TextView(context).apply {
            tag = TAG_LABEL
            text = "Anuncio"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accentColor)
        }
        val advertiser = TextView(context).apply {
            tag = TAG_ADVERTISER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(secondaryColor)
            setPadding(context.dp(8), 0, 0, 0)
        }
        column.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(label)
                addView(advertiser)
            },
        )

        val headline = TextView(context).apply {
            tag = TAG_HEADLINE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, context.dp(8), 0, 0)
        }
        val body = TextView(context).apply {
            tag = TAG_BODY
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(secondaryColor)
            setPadding(0, context.dp(4), 0, 0)
        }
        val media = MediaView(context).apply {
            tag = TAG_MEDIA
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(180)).apply {
                topMargin = context.dp(8)
            }
        }
        val cta = Button(context).apply {
            tag = TAG_CTA
            isAllCaps = false
        }
        column.addView(headline)
        column.addView(body)
        column.addView(media)
        column.addView(
            cta,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(8)
            },
        )

        adView.addView(column)
        adView.headlineView = headline
        adView.bodyView = body
        adView.advertiserView = advertiser
        adView.callToActionView = cta
        return adView
    }

    fun bind(view: View, nativeAd: NativeAd) {
        val adView = view as NativeAdView
        adView.text(TAG_HEADLINE, nativeAd.headline)
        adView.text(TAG_BODY, nativeAd.body)
        adView.text(TAG_ADVERTISER, nativeAd.advertiser)
        adView.text(TAG_CTA, nativeAd.callToAction)
        val media = adView.findViewWithTag<MediaView>(TAG_MEDIA)
        media.visibility = if (nativeAd.mediaContent != null) View.VISIBLE else View.GONE
        adView.registerNativeAd(nativeAd, media)
    }

    private fun View.text(tag: String, value: String?) {
        val target = findViewWithTag<View>(tag) as TextView
        target.text = value.orEmpty()
        target.visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun Context.dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}

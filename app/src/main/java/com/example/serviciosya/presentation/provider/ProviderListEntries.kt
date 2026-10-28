package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.ads.AdPlacement
import com.example.serviciosya.domain.ads.NativeAdContent

/** One row of the provider list: a provider or a native ad between providers. */
sealed interface ProviderListEntry {
    data class ProviderEntry(val item: ProviderListItem) : ProviderListEntry

    data class AdEntry(val index: Int, val ad: NativeAdContent) : ProviderListEntry
}

/**
 * Puts the i-th loaded ad after the provider at the i-th ad position. Ads that did not load leave
 * no gap, and an ad never ends up after the last provider.
 */
fun providerListEntries(
    providers: List<ProviderListItem>,
    ads: List<NativeAdContent>,
): List<ProviderListEntry> {
    val positions = AdPlacement.positions(providers.size)
    return buildList {
        providers.forEachIndexed { index, item ->
            add(ProviderListEntry.ProviderEntry(item))
            val adIndex = positions.indexOf(index + 1)
            if (adIndex != -1 && adIndex < ads.size) add(ProviderListEntry.AdEntry(adIndex, ads[adIndex]))
        }
    }
}

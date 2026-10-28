package com.example.serviciosya.domain.ads

/** Decides where the provider list carries a native ad. */
object AdPlacement {
    /** One ad after every this many providers. */
    const val PROVIDERS_PER_AD = 6
    const val MAX_ADS = 3

    /**
     * Number of providers shown before each ad, in list order (6, 12, 18). An ad is only placed
     * between providers, never at the end, so a list of [PROVIDERS_PER_AD] or fewer has none.
     */
    fun positions(providerCount: Int): List<Int> =
        (1..MAX_ADS)
            .map { it * PROVIDERS_PER_AD }
            .filter { it < providerCount }
}

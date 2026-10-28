package com.example.serviciosya.domain.billing

import com.example.serviciosya.domain.model.Provider

/**
 * Gives featured providers priority among the first results without disturbing the rest.
 * It runs after the proximity order, which stays as the base.
 */
object FeaturedRanking {
    /** Only lists longer than this are reordered. */
    const val WINDOW_SIZE = 5
    const val MAX_PRIORITY = 2

    /** Zero-based slots of positions 4 and 5, where priority providers from further down go. */
    private val PRIORITY_SLOTS = listOf(3, 4)

    /**
     * At most [MAX_PRIORITY] featured providers, the nearest ones, get a slot within the first
     * [WINDOW_SIZE]. Those already inside stay where they are; the ones further down take positions
     * 4 and 5 and everybody else keeps their relative order behind. A featured provider that does not
     * get a slot stays in place. Without a location ([distanceKm] null) the incoming order decides.
     */
    fun rank(providers: List<Provider>, nowMillis: Long, distanceKm: (Provider) -> Double?): List<Provider> {
        if (providers.size <= WINDOW_SIZE) return providers

        val priority = providers.withIndex()
            .filter { it.value.isFeaturedAt(nowMillis) }
            .sortedWith(compareBy<IndexedValue<Provider>, Double?>(nullsLast()) { distanceKm(it.value) }.thenBy { it.index })
            .take(MAX_PRIORITY)
        if (priority.isEmpty()) return providers

        val placed = mutableMapOf<Int, Provider>()
        priority.filter { it.index < WINDOW_SIZE }.forEach { placed[it.index] = it.value }
        val freeSlots = PRIORITY_SLOTS.filter { it !in placed }
        priority.filter { it.index >= WINDOW_SIZE }
            .zip(freeSlots)
            .forEach { (item, slot) -> placed[slot] = item.value }

        val placedIds = placed.values.map { it.id }.toSet()
        val others = providers.filter { it.id !in placedIds }.iterator()
        return List(providers.size) { position -> placed[position] ?: others.next() }
    }
}

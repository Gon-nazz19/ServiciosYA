package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.model.Provider
import java.text.Normalizer

data class ServiceSearchResult(
    val categories: List<Category>,
    val providers: List<Provider>,
) {
    val isEmpty: Boolean get() = categories.isEmpty() && providers.isEmpty()

    companion object {
        val EMPTY = ServiceSearchResult(emptyList(), emptyList())
    }
}

/**
 * Simple MVP search: case and accent insensitive "contains" match over
 * category names and provider names.
 */
class SearchServicesUseCase {
    operator fun invoke(
        query: String,
        categories: List<Category>,
        providers: List<Provider>,
    ): ServiceSearchResult {
        val normalizedQuery = normalizeForSearch(query)
        if (normalizedQuery.isEmpty()) return ServiceSearchResult.EMPTY
        return ServiceSearchResult(
            categories = categories.filter { normalizeForSearch(it.name).contains(normalizedQuery) },
            providers = providers.filter { normalizeForSearch(it.name).contains(normalizedQuery) },
        )
    }
}

internal fun normalizeForSearch(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
        .replace(WHITESPACE, " ")

private val DIACRITICS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

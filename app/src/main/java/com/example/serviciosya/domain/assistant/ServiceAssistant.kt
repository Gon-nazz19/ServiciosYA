package com.example.serviciosya.domain.assistant

import com.example.serviciosya.domain.model.Category

/**
 * Suggests a category and a message from the problem the user describes in their own words.
 * Only the problem text and the category list are sent: never the name, email or user id.
 */
interface ServiceAssistant {
    suspend fun analyze(problem: String, categories: List<Category>): Result<AssistantSuggestion>
}

data class AssistantSuggestion(
    /** Null when none of the categories fits the problem. */
    val categoryId: String?,
    val suggestedMessage: String,
    /** Short name of the missing service ("herrero") when no category fits. */
    val unmatchedService: String?,
)

/** The assistant could not answer, or its answer could not be understood. */
class AssistantException(cause: Throwable? = null) : Exception(MESSAGE, cause) {
    companion object {
        const val MESSAGE = "No pudimos analizar tu consulta, elegí la categoría manualmente."
    }
}

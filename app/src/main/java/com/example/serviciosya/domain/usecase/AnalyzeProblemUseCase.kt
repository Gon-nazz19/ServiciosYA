package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.assistant.AssistantSuggestion
import com.example.serviciosya.domain.assistant.ServiceAssistant
import com.example.serviciosya.domain.model.Category

/**
 * Validates what goes to the AI and what comes back: the model is not deterministic and may
 * return an id that does not exist, so only active categories are accepted.
 */
class AnalyzeProblemUseCase(
    private val assistant: ServiceAssistant,
) {
    suspend operator fun invoke(problem: String, categories: List<Category>): Result<AssistantSuggestion> {
        val trimmed = problem.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException(EMPTY_PROBLEM_MESSAGE))
        }
        if (trimmed.length > MAX_PROBLEM_LENGTH) {
            return Result.failure(IllegalArgumentException(PROBLEM_TOO_LONG_MESSAGE))
        }
        val activeCategories = categories.filter { it.active }
        return assistant.analyze(trimmed, activeCategories).map { suggestion ->
            val categoryId = suggestion.categoryId?.takeIf { id -> activeCategories.any { it.id == id } }
            AssistantSuggestion(
                categoryId = categoryId,
                suggestedMessage = suggestion.suggestedMessage.trim()
                    .take(CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH)
                    .trim(),
                unmatchedService = if (categoryId == null) {
                    suggestion.unmatchedService?.trim()?.take(MAX_SERVICE_LENGTH)?.ifBlank { null }
                } else {
                    null
                },
            )
        }
    }

    companion object {
        const val MAX_PROBLEM_LENGTH = 300
        const val MAX_SERVICE_LENGTH = 40
        const val EMPTY_PROBLEM_MESSAGE = "Contanos qué necesitás resolver."
        const val PROBLEM_TOO_LONG_MESSAGE = "Contalo en menos de $MAX_PROBLEM_LENGTH caracteres."
    }
}

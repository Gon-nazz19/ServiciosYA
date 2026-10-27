package com.example.serviciosya.testutil

import com.example.serviciosya.domain.assistant.AssistantSuggestion
import com.example.serviciosya.domain.assistant.ServiceAssistant
import com.example.serviciosya.domain.model.Category

class FakeServiceAssistant(
    var result: Result<AssistantSuggestion> = Result.success(
        AssistantSuggestion(categoryId = null, suggestedMessage = "", unmatchedService = null),
    ),
) : ServiceAssistant {
    val calls = mutableListOf<Pair<String, List<Category>>>()

    override suspend fun analyze(problem: String, categories: List<Category>): Result<AssistantSuggestion> {
        calls += problem to categories
        return result
    }
}

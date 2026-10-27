package com.example.serviciosya.data.assistant

import com.example.serviciosya.domain.assistant.AssistantException
import com.example.serviciosya.domain.assistant.AssistantSuggestion
import com.example.serviciosya.domain.assistant.ServiceAssistant
import com.example.serviciosya.domain.model.Category

/** Used when Firebase is not configured: the user is told to pick the category manually. */
class UnavailableServiceAssistant : ServiceAssistant {
    override suspend fun analyze(problem: String, categories: List<Category>): Result<AssistantSuggestion> =
        Result.failure(AssistantException())
}

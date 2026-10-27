package com.example.serviciosya.data.assistant

import android.util.Log
import com.example.serviciosya.domain.assistant.AssistantException
import com.example.serviciosya.domain.assistant.AssistantPrompt
import com.example.serviciosya.domain.assistant.AssistantSuggestion
import com.example.serviciosya.domain.assistant.ServiceAssistant
import com.example.serviciosya.domain.model.Category
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException

/** Gemini through Firebase AI Logic with the Gemini Developer API (works on the free Spark plan). */
class GeminiServiceAssistant(
    private val model: GenerativeModel,
) : ServiceAssistant {
    override suspend fun analyze(problem: String, categories: List<Category>): Result<AssistantSuggestion> =
        try {
            val response = model.generateContent(AssistantPrompt.userPrompt(problem, categories))
            val suggestion = response.text?.let(AssistantPrompt::parse)
            if (suggestion == null) Log.w(TAG, "Unreadable answer from $MODEL_NAME")
            suggestion?.let { Result.success(it) } ?: Result.failure(AssistantException())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Network errors, quota exceeded or AI Logic not enabled in the Firebase console.
            Log.w(TAG, "Gemini request failed", e)
            Result.failure(AssistantException(e))
        }

    companion object {
        private const val TAG = "GeminiAssistant"

        /** Stable Flash model of the Gemini Developer API (see the Firebase AI Logic model list). */
        const val MODEL_NAME = "gemini-3.5-flash"

        fun create(): GeminiServiceAssistant = GeminiServiceAssistant(
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME,
                generationConfig = generationConfig {
                    temperature = 0.2f
                    responseMimeType = "application/json"
                    responseSchema = Schema.obj(
                        mapOf(
                            AssistantPrompt.FIELD_CATEGORY_ID to Schema.string(nullable = true),
                            AssistantPrompt.FIELD_SUGGESTED_MESSAGE to Schema.string(),
                            AssistantPrompt.FIELD_UNMATCHED_SERVICE to Schema.string(nullable = true),
                        ),
                    )
                },
                systemInstruction = content { text(AssistantPrompt.SYSTEM_INSTRUCTION) },
            ),
        )
    }
}

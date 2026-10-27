package com.example.serviciosya.domain.assistant

import com.example.serviciosya.domain.model.Category
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Prompt sent to the model and parsing of its JSON answer. Pure Kotlin, so both are unit tested. */
object AssistantPrompt {
    const val FIELD_CATEGORY_ID = "categoryId"
    const val FIELD_SUGGESTED_MESSAGE = "suggestedMessage"
    const val FIELD_UNMATCHED_SERVICE = "unmatchedService"

    val SYSTEM_INSTRUCTION = """
        Sos el asistente de ServiciosYA, una app para encontrar prestadores de servicios domiciliarios
        en San Francisco, Córdoba. El usuario describe un problema de su casa.
        - Elegí como máximo una categoría de la lista que se te da y devolvé su id exacto en "$FIELD_CATEGORY_ID".
        - Redactá en "$FIELD_SUGGESTED_MESSAGE" un mensaje cordial de 1 a 3 oraciones, en español rioplatense,
          que el usuario le enviará al prestador contando el problema. No inventes nombres, direcciones,
          teléfonos, horarios ni precios.
        - Si el problema no corresponde a ninguna categoría, devolvé "$FIELD_CATEGORY_ID" en null y en
          "$FIELD_UNMATCHED_SERVICE" el oficio que haría falta en una o dos palabras (por ejemplo "herrero").
          Si hay categoría, "$FIELD_UNMATCHED_SERVICE" va en null.
        Respondé solo con el JSON pedido.
    """.trimIndent()

    fun userPrompt(problem: String, categories: List<Category>): String = buildString {
        appendLine("Categorías disponibles (id: nombre):")
        categories.forEach { appendLine("- ${it.id}: ${it.name}") }
        appendLine()
        append("Problema del usuario: ").append(problem)
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Null when the answer is not a JSON object or has neither a category nor a message. */
    fun parse(response: String): AssistantSuggestion? {
        val body = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
        val suggestion = AssistantSuggestion(
            categoryId = root.text(FIELD_CATEGORY_ID),
            suggestedMessage = root.text(FIELD_SUGGESTED_MESSAGE).orEmpty(),
            unmatchedService = root.text(FIELD_UNMATCHED_SERVICE),
        )
        val isEmpty = suggestion.categoryId == null &&
            suggestion.suggestedMessage.isEmpty() &&
            suggestion.unmatchedService == null
        return suggestion.takeUnless { isEmpty }
    }

    private fun JsonObject.text(field: String): String? =
        (get(field) as? JsonPrimitive)
            ?.takeIf { it.isString } // JsonNull and numbers are not text.
            ?.content
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}

package com.example.serviciosya.domain.assistant

import com.example.serviciosya.testutil.testCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPromptTest {
    @Test
    fun `valid answer is parsed`() {
        val suggestion = AssistantPrompt.parse(
            """{"categoryId":"electricistas","suggestedMessage":" Hola, se me cortó la luz. ","unmatchedService":null}""",
        )

        assertEquals(
            AssistantSuggestion("electricistas", "Hola, se me cortó la luz.", null),
            suggestion,
        )
    }

    @Test
    fun `unmatched answer keeps the missing service`() {
        val suggestion = AssistantPrompt.parse(
            """{"categoryId":null,"suggestedMessage":"Necesito soldar una reja.","unmatchedService":"herrero"}""",
        )

        assertNull(suggestion?.categoryId)
        assertEquals("herrero", suggestion?.unmatchedService)
    }

    @Test
    fun `answer wrapped in a markdown code block is accepted`() {
        val suggestion = AssistantPrompt.parse(
            "```json\n{\"categoryId\":\"plomeros\",\"suggestedMessage\":\"Pierde la canilla.\"}\n```",
        )

        assertEquals("plomeros", suggestion?.categoryId)
    }

    @Test
    fun `missing fields are treated as empty`() {
        val suggestion = AssistantPrompt.parse("""{"categoryId":"tecnicos-pc"}""")

        assertEquals(AssistantSuggestion("tecnicos-pc", "", null), suggestion)
    }

    @Test
    fun `fields of the wrong type are ignored`() {
        val suggestion = AssistantPrompt.parse("""{"categoryId":5,"suggestedMessage":"Hola","extra":true}""")

        assertEquals(AssistantSuggestion(null, "Hola", null), suggestion)
    }

    @Test
    fun `invalid or empty answers return null`() {
        assertNull(AssistantPrompt.parse("No sé qué responder"))
        assertNull(AssistantPrompt.parse("""["electricistas"]"""))
        assertNull(AssistantPrompt.parse("""{"categoryId":null,"suggestedMessage":"  "}"""))
        assertNull(AssistantPrompt.parse(""))
    }

    @Test
    fun `user prompt lists every category and the problem only`() {
        val prompt = AssistantPrompt.userPrompt(
            problem = "La compu no prende",
            categories = listOf(testCategory(), testCategory(id = "tecnicos-pc", name = "Técnico PC")),
        )

        assertTrue(prompt.contains("- electricistas: Electricista"))
        assertTrue(prompt.contains("- tecnicos-pc: Técnico PC"))
        assertTrue(prompt.endsWith("Problema del usuario: La compu no prende"))
    }
}

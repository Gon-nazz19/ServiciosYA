package com.example.serviciosya.domain.usecase

import com.example.serviciosya.domain.assistant.AssistantException
import com.example.serviciosya.domain.assistant.AssistantSuggestion
import com.example.serviciosya.testutil.FakeServiceAssistant
import com.example.serviciosya.testutil.testCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeProblemUseCaseTest {
    private val assistant = FakeServiceAssistant()
    private val useCase = AnalyzeProblemUseCase(assistant)
    private val categories = listOf(
        testCategory(id = "electricistas", name = "Electricista"),
        testCategory(id = "plomeros", name = "Plomero"),
        testCategory(id = "cerrajeros", name = "Cerrajero").copy(active = false),
    )

    @Test
    fun `blank problem fails without calling the assistant`() = runTest {
        val result = useCase("   ", categories)

        assertEquals(AnalyzeProblemUseCase.EMPTY_PROBLEM_MESSAGE, result.exceptionOrNull()?.message)
        assertTrue(assistant.calls.isEmpty())
    }

    @Test
    fun `too long problem fails without calling the assistant`() = runTest {
        val result = useCase("a".repeat(AnalyzeProblemUseCase.MAX_PROBLEM_LENGTH + 1), categories)

        assertEquals(AnalyzeProblemUseCase.PROBLEM_TOO_LONG_MESSAGE, result.exceptionOrNull()?.message)
        assertTrue(assistant.calls.isEmpty())
    }

    @Test
    fun `only the trimmed problem and active categories are sent`() = runTest {
        useCase("  Pierde agua la canilla  ", categories)

        val (problem, sentCategories) = assistant.calls.single()
        assertEquals("Pierde agua la canilla", problem)
        assertEquals(listOf("electricistas", "plomeros"), sentCategories.map { it.id })
    }

    @Test
    fun `valid category is kept and unmatched service is dropped`() = runTest {
        assistant.result = Result.success(AssistantSuggestion("plomeros", "Hola", "plomero"))

        val suggestion = useCase("Pierde agua la canilla", categories).getOrThrow()

        assertEquals("plomeros", suggestion.categoryId)
        assertNull(suggestion.unmatchedService)
    }

    @Test
    fun `invented category id is treated as no category`() = runTest {
        assistant.result = Result.success(AssistantSuggestion("gasistas", "Hola", null))

        assertNull(useCase("Huele a gas", categories).getOrThrow().categoryId)
    }

    @Test
    fun `inactive category is treated as no category`() = runTest {
        assistant.result = Result.success(AssistantSuggestion("cerrajeros", "Hola", "cerrajero"))

        val suggestion = useCase("Perdí la llave", categories).getOrThrow()

        assertNull(suggestion.categoryId)
        assertEquals("cerrajero", suggestion.unmatchedService)
    }

    @Test
    fun `suggested message is trimmed to the request limit`() = runTest {
        val long = "x".repeat(CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH + 50)
        assistant.result = Result.success(AssistantSuggestion("electricistas", "  $long", null))

        val suggestion = useCase("Se cortó la luz", categories).getOrThrow()

        assertEquals(CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH, suggestion.suggestedMessage.length)
    }

    @Test
    fun `assistant failure is returned as is`() = runTest {
        assistant.result = Result.failure(AssistantException())

        val result = useCase("Se cortó la luz", categories)

        assertTrue(result.exceptionOrNull() is AssistantException)
    }
}

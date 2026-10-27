package com.example.serviciosya.presentation.assistant

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.assistant.AssistantException
import com.example.serviciosya.domain.assistant.AssistantSuggestion
import com.example.serviciosya.domain.usecase.AnalyzeProblemUseCase
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase
import com.example.serviciosya.testutil.FakeAnalyticsTracker
import com.example.serviciosya.testutil.FakeCategoryRepository
import com.example.serviciosya.testutil.FakeServiceAssistant
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testCategory
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AssistantViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val assistant = FakeServiceAssistant()
    private val categoryRepository = FakeCategoryRepository(
        categoriesResult = Result.success(
            listOf(
                testCategory(id = "electricistas", name = "Electricista"),
                testCategory(id = "plomeros", name = "Plomero"),
            ),
        ),
    )
    private val analytics = FakeAnalyticsTracker()

    private fun createViewModel() = AssistantViewModel(
        analyzeProblem = AnalyzeProblemUseCase(assistant),
        getActiveCategories = GetActiveCategoriesUseCase(categoryRepository),
        analytics = analytics,
    )

    @Test
    fun `matched result shows the category and tracks it`() = runTest {
        assistant.result = Result.success(AssistantSuggestion("electricistas", "Se me cortó la luz.", null))
        val viewModel = createViewModel()

        viewModel.onProblemChange("Se me cortó la luz en la cocina")
        viewModel.analyze()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(
            AssistantResult.Matched("electricistas", "Electricista", "Se me cortó la luz."),
            state.result,
        )
        val event = analytics.eventsNamed(AnalyticsEvents.ASSISTANT_USED).single()
        assertEquals("matched", event.params[AnalyticsParams.RESULT])
        assertEquals("electricistas", event.params[AnalyticsParams.CATEGORY_ID])
    }

    @Test
    fun `unmatched result tracks the missing service`() = runTest {
        assistant.result = Result.success(AssistantSuggestion(null, "Necesito soldar una reja.", "Herrero"))
        val viewModel = createViewModel()

        viewModel.onProblemChange("Necesito soldar una reja")
        viewModel.analyze()
        advanceUntilIdle()

        assertEquals(
            AssistantResult.Unmatched("Herrero", "Necesito soldar una reja."),
            viewModel.uiState.value.result,
        )
        assertEquals(
            "unmatched",
            analytics.eventsNamed(AnalyticsEvents.ASSISTANT_USED).single().params[AnalyticsParams.RESULT],
        )
        val unmatched = analytics.eventsNamed(AnalyticsEvents.ASSISTANT_UNMATCHED).single()
        assertEquals("herrero", unmatched.params[AnalyticsParams.SERVICE])
    }

    @Test
    fun `assistant error shows the manual fallback message and tracks the error`() = runTest {
        assistant.result = Result.failure(AssistantException(RuntimeException("quota")))
        val viewModel = createViewModel()

        viewModel.onProblemChange("La compu no prende")
        viewModel.analyze()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.result)
        assertEquals(AssistantException.MESSAGE, state.errorMessage)
        assertEquals("error", analytics.eventsNamed(AnalyticsEvents.ASSISTANT_USED).single().params[AnalyticsParams.RESULT])
    }

    @Test
    fun `categories that cannot be loaded are an error without calling the assistant`() = runTest {
        categoryRepository.categoriesResult = Result.failure(RuntimeException("offline"))
        val viewModel = createViewModel()

        viewModel.onProblemChange("La compu no prende")
        viewModel.analyze()
        advanceUntilIdle()

        assertEquals(AssistantException.MESSAGE, viewModel.uiState.value.errorMessage)
        assertTrue(assistant.calls.isEmpty())
    }

    @Test
    fun `validation errors are shown but not tracked as assistant errors`() = runTest {
        val viewModel = createViewModel()

        viewModel.analyze()
        advanceUntilIdle()

        assertEquals(AnalyzeProblemUseCase.EMPTY_PROBLEM_MESSAGE, viewModel.uiState.value.errorMessage)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `categories are loaded once for several questions`() = runTest {
        val viewModel = createViewModel()
        viewModel.onProblemChange("Se cortó la luz")

        viewModel.analyze()
        advanceUntilIdle()
        viewModel.analyze()
        advanceUntilIdle()

        assertEquals(1, categoryRepository.calls)
        assertEquals(2, assistant.calls.size)
    }

    @Test
    fun `problem longer than the limit is not accepted`() {
        val viewModel = createViewModel()

        viewModel.onProblemChange("a".repeat(AnalyzeProblemUseCase.MAX_PROBLEM_LENGTH + 1))

        assertEquals("", viewModel.uiState.value.problem)
    }

    @Test
    fun `suggested message is offered only for the suggested category`() = runTest {
        assistant.result = Result.success(AssistantSuggestion("electricistas", "Se me cortó la luz.", null))
        val viewModel = createViewModel()
        viewModel.onProblemChange("Se me cortó la luz")
        viewModel.analyze()
        advanceUntilIdle()

        assertEquals("Se me cortó la luz.", viewModel.suggestedMessageFor("electricistas"))
        assertNull(viewModel.suggestedMessageFor("plomeros"))
    }

    @Test
    fun `reset clears the problem and the suggestion`() = runTest {
        assistant.result = Result.success(AssistantSuggestion("electricistas", "Hola", null))
        val viewModel = createViewModel()
        viewModel.onProblemChange("Se me cortó la luz")
        viewModel.analyze()
        advanceUntilIdle()

        viewModel.reset()

        assertEquals(AssistantUiState(), viewModel.uiState.value)
        assertNull(viewModel.suggestedMessageFor("electricistas"))
    }
}

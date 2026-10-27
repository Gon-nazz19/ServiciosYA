package com.example.serviciosya.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.analytics.AssistantResultType
import com.example.serviciosya.domain.assistant.AssistantException
import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.usecase.AnalyzeProblemUseCase
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase
import com.example.serviciosya.domain.usecase.normalizeForSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val problem: String = "",
    val isLoading: Boolean = false,
    val result: AssistantResult? = null,
    val errorMessage: String? = null,
)

sealed interface AssistantResult {
    val suggestedMessage: String

    data class Matched(
        val categoryId: String,
        val categoryName: String,
        override val suggestedMessage: String,
    ) : AssistantResult

    /** [service] is null when the assistant could not even name the missing service. */
    data class Unmatched(
        val service: String?,
        override val suggestedMessage: String,
    ) : AssistantResult
}

/**
 * Lives at app level (like HomeViewModel) so the message it drafts is still available when the
 * user opens a provider of the suggested category.
 */
class AssistantViewModel(
    private val analyzeProblem: AnalyzeProblemUseCase,
    private val getActiveCategories: GetActiveCategoriesUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var categories: List<Category>? = null
    private var analyzeJob: Job? = null

    fun onProblemChange(problem: String) {
        if (problem.length <= AnalyzeProblemUseCase.MAX_PROBLEM_LENGTH) {
            _uiState.update { it.copy(problem = problem, errorMessage = null) }
        }
    }

    fun analyze() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, result = null, errorMessage = null) }
        analyzeJob = viewModelScope.launch {
            val available = categories ?: getActiveCategories().getOrNull()?.also { categories = it }
            if (available == null) {
                finishWithError(AssistantException.MESSAGE)
                return@launch
            }
            analyzeProblem(state.problem, available)
                .onSuccess { suggestion ->
                    val category = available.firstOrNull { it.id == suggestion.categoryId }
                    val result = if (category != null) {
                        AssistantResult.Matched(category.id, category.name, suggestion.suggestedMessage)
                    } else {
                        AssistantResult.Unmatched(suggestion.unmatchedService, suggestion.suggestedMessage)
                    }
                    _uiState.update { it.copy(isLoading = false, result = result) }
                    track(result)
                }
                .onFailure { error ->
                    // Input validation errors are the user's to fix and are not an assistant failure.
                    if (error is IllegalArgumentException) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    } else {
                        finishWithError(AssistantException.MESSAGE)
                    }
                }
        }
    }

    /** The drafted message, only for providers of the category the assistant suggested. */
    fun suggestedMessageFor(categoryId: String): String? =
        (_uiState.value.result as? AssistantResult.Matched)
            ?.takeIf { it.categoryId == categoryId }
            ?.suggestedMessage
            ?.ifBlank { null }

    /** Clears everything, e.g. when another user signs in. */
    fun reset() {
        analyzeJob?.cancel()
        categories = null
        _uiState.value = AssistantUiState()
    }

    private fun finishWithError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
        analytics.trackAssistantUsed(AssistantResultType.ERROR, categoryId = "")
    }

    private fun track(result: AssistantResult) {
        when (result) {
            is AssistantResult.Matched -> analytics.trackAssistantUsed(AssistantResultType.MATCHED, result.categoryId)
            is AssistantResult.Unmatched -> {
                analytics.trackAssistantUsed(AssistantResultType.UNMATCHED, categoryId = "")
                result.service?.let { analytics.trackAssistantUnmatched(normalizeForSearch(it)) }
            }
        }
    }
}

package com.example.serviciosya.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.assistant.ServiceAssistant
import com.example.serviciosya.domain.repository.CategoryRepository
import com.example.serviciosya.domain.usecase.AnalyzeProblemUseCase
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase

class AssistantViewModelFactory(
    private val assistant: ServiceAssistant,
    private val categoryRepository: CategoryRepository,
    private val analytics: AnalyticsTracker,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AssistantViewModel::class.java))
        return AssistantViewModel(
            analyzeProblem = AnalyzeProblemUseCase(assistant),
            getActiveCategories = GetActiveCategoriesUseCase(categoryRepository),
            analytics = analytics,
        ) as T
    }
}

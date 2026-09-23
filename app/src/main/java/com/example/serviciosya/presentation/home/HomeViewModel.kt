package com.example.serviciosya.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.usecase.GetActiveCategoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val categories: List<HomeCategoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val getActiveCategories: GetActiveCategoriesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getActiveCategories()
                .onSuccess { categories ->
                    _uiState.update {
                        it.copy(
                            categories = categories.map(Category::toHomeItem),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            categories = emptyList(),
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "No pudimos cargar las categorías.",
                        )
                    }
                }
        }
    }
}

private fun Category.toHomeItem() = HomeCategoryItem(
    id = id,
    name = name,
    icon = CategoryIcon.fromFirestore(icon),
)

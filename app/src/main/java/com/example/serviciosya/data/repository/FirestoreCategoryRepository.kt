package com.example.serviciosya.data.repository

import com.example.serviciosya.data.model.toCategoryDto
import com.example.serviciosya.data.model.toDomain
import com.example.serviciosya.domain.model.Category
import com.example.serviciosya.domain.repository.CategoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreCategoryRepository(
    private val firestore: FirebaseFirestore,
) : CategoryRepository {
    override suspend fun getActiveCategories(): Result<List<Category>> = runCatching {
        firestore.collection(CATEGORIES_COLLECTION)
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .map { it.toCategoryDto().toDomain() }
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name }
    }

    private companion object {
        const val CATEGORIES_COLLECTION = "categories"
    }
}

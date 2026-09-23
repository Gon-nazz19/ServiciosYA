package com.example.serviciosya.di

import android.content.Context
import com.example.serviciosya.data.repository.FirebaseAuthRepository
import com.example.serviciosya.data.repository.FirestoreCategoryRepository
import com.example.serviciosya.data.repository.UnavailableAuthRepository
import com.example.serviciosya.data.repository.UnavailableCategoryRepository
import com.example.serviciosya.domain.repository.AuthRepository
import com.example.serviciosya.domain.repository.CategoryRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val isFirebaseConfigured = FirebaseApp.getApps(context).isNotEmpty()

    val authRepository: AuthRepository = if (isFirebaseConfigured) {
        FirebaseAuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
        )
    } else {
        UnavailableAuthRepository()
    }

    val categoryRepository: CategoryRepository = if (isFirebaseConfigured) {
        FirestoreCategoryRepository(FirebaseFirestore.getInstance())
    } else {
        UnavailableCategoryRepository()
    }
}

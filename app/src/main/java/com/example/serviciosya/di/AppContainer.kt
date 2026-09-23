package com.example.serviciosya.di

import android.content.Context
import com.example.serviciosya.data.repository.FirebaseAuthRepository
import com.example.serviciosya.data.repository.UnavailableAuthRepository
import com.example.serviciosya.domain.repository.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    val authRepository: AuthRepository = if (FirebaseApp.getApps(context).isNotEmpty()) {
        FirebaseAuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
        )
    } else {
        UnavailableAuthRepository()
    }
}

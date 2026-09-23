package com.example.serviciosya

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviciosya.di.AppContainer
import com.example.serviciosya.navigation.AppNavigation
import com.example.serviciosya.presentation.auth.AuthViewModel
import com.example.serviciosya.presentation.auth.AuthViewModelFactory

@Composable
fun ServiciosYaApp(container: AppContainer) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(container.authRepository),
    )
    AppNavigation(authViewModel = authViewModel)
}

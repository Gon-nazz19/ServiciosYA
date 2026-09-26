package com.example.serviciosya

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviciosya.di.AppContainer
import com.example.serviciosya.navigation.AppNavigation
import com.example.serviciosya.presentation.auth.AuthViewModel
import com.example.serviciosya.presentation.auth.AuthViewModelFactory
import com.example.serviciosya.presentation.home.HomeViewModel
import com.example.serviciosya.presentation.home.HomeViewModelFactory

@Composable
fun ServiciosYaApp(container: AppContainer) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(container.authRepository),
    )
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            categoryRepository = container.categoryRepository,
            providerRepository = container.providerRepository,
            analytics = container.analyticsTracker,
        ),
    )
    AppNavigation(
        container = container,
        authViewModel = authViewModel,
        homeViewModel = homeViewModel,
    )
}

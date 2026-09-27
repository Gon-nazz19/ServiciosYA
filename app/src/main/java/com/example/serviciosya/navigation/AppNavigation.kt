package com.example.serviciosya.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.serviciosya.di.AppContainer
import com.example.serviciosya.presentation.auth.AuthViewModel
import com.example.serviciosya.presentation.auth.LoginScreen
import com.example.serviciosya.presentation.auth.RegisterScreen
import com.example.serviciosya.presentation.home.HomeScreen
import com.example.serviciosya.presentation.home.HomeViewModel
import com.example.serviciosya.presentation.provider.ProviderDetailScreen
import com.example.serviciosya.presentation.provider.ProviderDetailViewModel
import com.example.serviciosya.presentation.provider.ProviderDetailViewModelFactory
import com.example.serviciosya.presentation.provider.ProvidersScreen
import com.example.serviciosya.presentation.provider.ProvidersViewModel
import com.example.serviciosya.presentation.provider.ProvidersViewModelFactory
import com.example.serviciosya.presentation.profile.ProfileScreen
import com.example.serviciosya.presentation.request.RequestsScreen
import com.example.serviciosya.presentation.request.RequestsViewModel
import com.example.serviciosya.presentation.request.RequestsViewModelFactory

@Composable
fun AppNavigation(
    container: AppContainer,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(authState.isSessionLoading, authState.user?.id) {
        if (!authState.isSessionLoading) {
            val destination = if (authState.user == null) Routes.LOGIN else Routes.HOME
            navController.navigate(destination) {
                // Clear the whole back stack so Back never returns to a screen of the previous session.
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            // Tabs saved with saveState keep their ViewModels (e.g. the previous user's requests);
            // drop them so a new session never restores another user's data.
            TopLevelDestination.entries.forEach { navController.clearBackStack(it.route) }
            homeViewModel.resetSearch()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (authState.user != null && isTopLevelRoute(currentRoute)) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = { Icon(destination.icon(), contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SESSION,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.SESSION) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    isLoading = authState.isSubmitting,
                    errorMessage = authState.errorMessage,
                    onSignIn = authViewModel::signIn,
                    onRegisterClick = {
                        authViewModel.clearError()
                        navController.navigate(Routes.REGISTER)
                    },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    isLoading = authState.isSubmitting,
                    errorMessage = authState.errorMessage,
                    onRegister = authViewModel::register,
                    onLoginClick = {
                        authViewModel.clearError()
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.HOME) {
                authState.user?.let { user ->
                    HomeScreen(
                        userName = user.name,
                        categories = homeState.categories,
                        isLoading = homeState.isLoading,
                        errorMessage = homeState.errorMessage,
                        search = homeState.search,
                        onRetry = homeViewModel::loadCategories,
                        onSearchQueryChange = homeViewModel::onSearchQueryChange,
                        onCategoryClick = { category ->
                            navController.navigate(Routes.category(category.id))
                        },
                        onProviderClick = { provider ->
                            navController.navigate(Routes.provider(provider.id))
                        },
                    )
                }
            }
            composable(Routes.CATEGORY) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId").orEmpty()
                val categoryName = homeState.categories
                    .firstOrNull { it.id == categoryId }
                    ?.name
                    ?: "Categoría"
                val providersViewModel: ProvidersViewModel = viewModel(
                    factory = ProvidersViewModelFactory(
                        categoryId = categoryId,
                        categoryName = categoryName,
                        repository = container.providerRepository,
                        analytics = container.analyticsTracker,
                    ),
                )
                val providersState by providersViewModel.uiState.collectAsStateWithLifecycle()
                ProvidersScreen(
                    categoryName = categoryName,
                    uiState = providersState,
                    onRetry = providersViewModel::loadProviders,
                    onProviderClick = { provider ->
                        navController.navigate(Routes.provider(provider.id))
                    },
                    onBack = navController::popBackStack,
                )
            }
            composable(Routes.PROVIDER) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId").orEmpty()
                val detailViewModel: ProviderDetailViewModel = viewModel(
                    factory = ProviderDetailViewModelFactory(
                        providerId = providerId,
                        providerRepository = container.providerRepository,
                        serviceRequestRepository = container.serviceRequestRepository,
                        analytics = container.analyticsTracker,
                    ),
                )
                val detailState by detailViewModel.uiState.collectAsStateWithLifecycle()
                val categoryName = homeState.categories
                    .firstOrNull { it.id == detailState.provider?.categoryId }
                    ?.name
                    .orEmpty()
                ProviderDetailScreen(
                    uiState = detailState,
                    categoryName = categoryName,
                    onRetry = detailViewModel::loadProvider,
                    onMessageChange = detailViewModel::onMessageChange,
                    onRequestContact = { detailViewModel.requestContact(categoryName) },
                    onUserMessageShown = detailViewModel::onUserMessageShown,
                    onBack = navController::popBackStack,
                )
            }
            composable(Routes.REQUESTS) {
                authState.user?.let { user ->
                    // Keyed by user so a restored tab can never show another account's requests.
                    val requestsViewModel: RequestsViewModel = viewModel(
                        key = "requests-${user.id}",
                        factory = RequestsViewModelFactory(container.serviceRequestRepository),
                    )
                    val requestsState by requestsViewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) { requestsViewModel.loadRequests() }
                    RequestsScreen(
                        uiState = requestsState,
                        onRetry = requestsViewModel::loadRequests,
                    )
                }
            }
            composable(Routes.PROFILE) {
                authState.user?.let { user ->
                    ProfileScreen(
                        user = user,
                        isLoading = authState.isSubmitting,
                        errorMessage = authState.errorMessage,
                        onSignOut = authViewModel::signOut,
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        // Home is the root of the signed-in graph; keep one instance of each tab and restore its state.
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun TopLevelDestination.icon(): ImageVector = when (this) {
    TopLevelDestination.HOME -> Icons.Outlined.Home
    TopLevelDestination.REQUESTS -> Icons.AutoMirrored.Outlined.Assignment
    TopLevelDestination.PROFILE -> Icons.Outlined.Person
}

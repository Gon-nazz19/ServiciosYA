package com.example.serviciosya.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.serviciosya.presentation.auth.AuthViewModel
import com.example.serviciosya.presentation.auth.LoginScreen
import com.example.serviciosya.presentation.auth.RegisterScreen
import com.example.serviciosya.presentation.category.CategoryPreviewScreen
import com.example.serviciosya.presentation.home.HomeScreen
import com.example.serviciosya.presentation.home.InitialHomeCategories
import com.example.serviciosya.presentation.profile.ProfileScreen

private object Routes {
    const val SESSION = "session"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val CATEGORY = "category/{categoryId}"

    fun category(categoryId: String) = "category/$categoryId"
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.isSessionLoading, authState.user?.id) {
        if (!authState.isSessionLoading) {
            val destination = if (authState.user == null) Routes.LOGIN else Routes.HOME
            navController.navigate(destination) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SESSION) {
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
                    categories = InitialHomeCategories,
                    onCategoryClick = { category ->
                        navController.navigate(Routes.category(category.id))
                    },
                    onProfileClick = { navController.navigate(Routes.PROFILE) },
                )
            }
        }
        composable(Routes.CATEGORY) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId").orEmpty()
            val categoryName = InitialHomeCategories
                .firstOrNull { it.id == categoryId }
                ?.name
                ?: "Categoría"
            CategoryPreviewScreen(
                categoryName = categoryName,
                onBack = navController::popBackStack,
            )
        }
        composable(Routes.PROFILE) {
            authState.user?.let { user ->
                ProfileScreen(
                    user = user,
                    isLoading = authState.isSubmitting,
                    errorMessage = authState.errorMessage,
                    onBack = navController::popBackStack,
                    onSignOut = authViewModel::signOut,
                )
            }
        }
    }
}

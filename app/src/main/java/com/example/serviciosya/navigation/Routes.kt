package com.example.serviciosya.navigation

internal object Routes {
    const val SESSION = "session"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val REQUESTS = "requests"
    const val CATEGORY = "category/{categoryId}"
    const val PROVIDER = "provider/{providerId}"

    fun category(categoryId: String) = "category/$categoryId"
    fun provider(providerId: String) = "provider/$providerId"
}

internal enum class TopLevelDestination(val route: String, val label: String) {
    HOME(Routes.HOME, "Inicio"),
    REQUESTS(Routes.REQUESTS, "Solicitudes"),
    PROFILE(Routes.PROFILE, "Perfil"),
}

internal fun isTopLevelRoute(route: String?): Boolean =
    TopLevelDestination.entries.any { it.route == route }

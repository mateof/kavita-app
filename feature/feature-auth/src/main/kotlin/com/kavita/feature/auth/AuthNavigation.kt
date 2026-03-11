package com.kavita.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.HomeRoute
import com.kavita.core.common.navigation.LoginRoute
import com.kavita.core.common.navigation.OpdsBrowseRoute
import com.kavita.core.common.navigation.ServerManagementRoute

fun NavGraphBuilder.authGraph(navController: NavController) {
    composable<LoginRoute> {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(HomeRoute) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onOpdsConnected = { serverUrl ->
                navController.navigate(OpdsBrowseRoute(serverUrl)) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToServers = {
                navController.navigate(ServerManagementRoute)
            },
        )
    }
    composable<ServerManagementRoute> {
        ServerManagementScreen(
            onNavigateBack = { navController.popBackStack() },
            onAddServer = {
                navController.navigate(LoginRoute) {
                    launchSingleTop = true
                }
            },
            onNavigateToHome = {
                navController.navigate(HomeRoute) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToOpds = { serverUrl ->
                navController.navigate(OpdsBrowseRoute(serverUrl)) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
}

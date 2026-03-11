package com.kavita.feature.admin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.AdminRoute

fun NavGraphBuilder.adminScreen(navController: NavController) {
    composable<AdminRoute> {
        AdminScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

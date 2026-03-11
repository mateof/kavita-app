package com.kavita.feature.opds

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.LoginRoute
import com.kavita.core.common.navigation.OpdsBrowseRoute

fun NavGraphBuilder.opdsBrowseScreen(navController: NavController) {
    composable<OpdsBrowseRoute> {
        OpdsBrowseScreen(
            onNavigateBack = {
                navController.navigate(LoginRoute) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
}

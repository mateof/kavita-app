package com.kavita.feature.stats

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.StatsRoute

fun NavGraphBuilder.statsScreen(navController: NavController) {
    composable<StatsRoute> {
        StatsScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

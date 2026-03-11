package com.kavita.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.AdminRoute
import com.kavita.core.common.navigation.CollectionsRoute
import com.kavita.core.common.navigation.MoreRoute
import com.kavita.core.common.navigation.ReadingListsRoute
import com.kavita.core.common.navigation.ServerManagementRoute
import com.kavita.core.common.navigation.SettingsRoute
import com.kavita.core.common.navigation.StatsRoute

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable<MoreRoute> {
        MoreScreen(
            onNavigateToSettings = { navController.navigate(SettingsRoute) },
            onNavigateToStats = { navController.navigate(StatsRoute) },
            onNavigateToAdmin = { navController.navigate(AdminRoute) },
            onNavigateToServers = { navController.navigate(ServerManagementRoute) },
            onNavigateToCollections = { navController.navigate(CollectionsRoute) },
            onNavigateToReadingLists = { navController.navigate(ReadingListsRoute) },
        )
    }
    composable<SettingsRoute> {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToServers = { navController.navigate(ServerManagementRoute) },
        )
    }
}

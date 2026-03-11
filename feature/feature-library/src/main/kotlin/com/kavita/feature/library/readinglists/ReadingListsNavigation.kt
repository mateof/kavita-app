package com.kavita.feature.library.readinglists

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kavita.core.common.navigation.ReadingListDetailRoute
import com.kavita.core.common.navigation.ReadingListsRoute
import com.kavita.core.common.navigation.SeriesDetailRoute

fun NavGraphBuilder.readingListsScreen(navController: NavController) {
    composable<ReadingListsRoute> {
        ReadingListsScreen(
            onReadingListClick = { readingListId, serverId ->
                navController.navigate(ReadingListDetailRoute(readingListId, serverId))
            },
            onNavigateBack = { navController.popBackStack() },
        )
    }
    composable<ReadingListDetailRoute> { entry ->
        val route = entry.toRoute<ReadingListDetailRoute>()
        ReadingListDetailScreen(
            onSeriesClick = { seriesId, serverId ->
                navController.navigate(SeriesDetailRoute(seriesId, serverId))
            },
            onNavigateBack = { navController.popBackStack() },
            serverId = route.serverId,
        )
    }
}

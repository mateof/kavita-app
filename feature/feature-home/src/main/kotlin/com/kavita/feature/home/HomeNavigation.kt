package com.kavita.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.HomeRoute
import com.kavita.core.common.navigation.ReaderRoute
import com.kavita.core.common.navigation.SeriesDetailRoute

fun NavGraphBuilder.homeScreen(navController: NavController) {
    composable<HomeRoute> {
        HomeScreen(
            onSeriesClick = { seriesId, serverId ->
                navController.navigate(SeriesDetailRoute(seriesId, serverId))
            },
            onOpenReader = { chapterId, seriesId, volumeId, format ->
                navController.navigate(ReaderRoute(chapterId, seriesId, volumeId, format))
            },
        )
    }
}

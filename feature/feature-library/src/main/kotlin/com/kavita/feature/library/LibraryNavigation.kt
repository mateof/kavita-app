package com.kavita.feature.library

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.LibraryRoute
import com.kavita.core.common.navigation.SeriesDetailRoute

fun NavGraphBuilder.libraryScreen(navController: NavController) {
    composable<LibraryRoute> {
        LibraryScreen(
            onSeriesClick = { seriesId, serverId ->
                navController.navigate(SeriesDetailRoute(seriesId, serverId))
            },
        )
    }
}

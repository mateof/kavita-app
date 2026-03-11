package com.kavita.feature.search

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.SearchRoute
import com.kavita.core.common.navigation.SeriesDetailRoute

fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable<SearchRoute> {
        SearchScreen(
            onSeriesClick = { seriesId, serverId ->
                navController.navigate(SeriesDetailRoute(seriesId, serverId))
            },
        )
    }
}

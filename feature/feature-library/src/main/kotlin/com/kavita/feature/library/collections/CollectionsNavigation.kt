package com.kavita.feature.library.collections

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.CollectionDetailRoute
import com.kavita.core.common.navigation.CollectionsRoute
import com.kavita.core.common.navigation.SeriesDetailRoute

fun NavGraphBuilder.collectionsScreen(navController: NavController) {
    composable<CollectionsRoute> {
        CollectionsScreen(
            onCollectionClick = { collectionId, serverId ->
                navController.navigate(CollectionDetailRoute(collectionId, serverId))
            },
            onNavigateBack = { navController.popBackStack() },
        )
    }
    composable<CollectionDetailRoute> {
        CollectionDetailScreen(
            onSeriesClick = { seriesId, serverId ->
                navController.navigate(SeriesDetailRoute(seriesId, serverId))
            },
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

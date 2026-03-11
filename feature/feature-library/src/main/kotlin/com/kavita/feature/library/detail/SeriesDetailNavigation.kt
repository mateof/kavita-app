package com.kavita.feature.library.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.ReaderRoute
import com.kavita.core.common.navigation.SeriesDetailRoute

fun NavGraphBuilder.seriesDetailScreen(navController: NavController) {
    composable<SeriesDetailRoute> {
        SeriesDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onReadChapter = { chapterId, seriesId, volumeId, format ->
                navController.navigate(ReaderRoute(chapterId, seriesId, volumeId, format))
            },
        )
    }
}

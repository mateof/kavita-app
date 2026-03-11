package com.kavita.feature.reader

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.ReaderRoute

fun NavGraphBuilder.readerScreen(navController: NavController) {
    composable<ReaderRoute> {
        ReaderScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToChapter = { chapterId, seriesId, volumeId, format ->
                navController.navigate(ReaderRoute(chapterId, seriesId, volumeId, format)) {
                    popUpTo(navController.currentBackStackEntry?.destination?.id ?: 0) {
                        inclusive = true
                    }
                }
            },
        )
    }
}

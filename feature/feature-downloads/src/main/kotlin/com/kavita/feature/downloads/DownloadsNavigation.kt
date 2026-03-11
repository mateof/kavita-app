package com.kavita.feature.downloads

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kavita.core.common.navigation.DownloadsRoute

fun NavGraphBuilder.downloadsScreen() {
    composable<DownloadsRoute> {
        DownloadsScreen()
    }
}

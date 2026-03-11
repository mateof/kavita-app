package com.kavita.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kavita.core.common.navigation.HomeRoute
import com.kavita.feature.admin.adminScreen
import com.kavita.feature.auth.authGraph
import com.kavita.feature.downloads.downloadsScreen
import com.kavita.feature.home.homeScreen
import com.kavita.feature.library.libraryScreen
import com.kavita.feature.library.collections.collectionsScreen
import com.kavita.feature.library.detail.seriesDetailScreen
import com.kavita.feature.library.readinglists.readingListsScreen
import com.kavita.feature.opds.opdsBrowseScreen
import com.kavita.feature.reader.readerScreen
import com.kavita.feature.search.searchScreen
import com.kavita.feature.settings.settingsGraph
import com.kavita.feature.stats.statsScreen

@Composable
fun KavitaNavHost(
    navController: NavHostController,
    startDestination: Any = HomeRoute,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // Auth
        authGraph(navController)

        // Home
        homeScreen(navController)

        // Library + Series Detail
        libraryScreen(navController)
        seriesDetailScreen(navController)

        // Search
        searchScreen(navController)

        // Reader
        readerScreen(navController)

        // Downloads
        downloadsScreen()

        // Settings & More
        settingsGraph(navController)

        // Stats
        statsScreen(navController)

        // Admin
        adminScreen(navController)

        // Collections & Reading Lists
        collectionsScreen(navController)
        readingListsScreen(navController)

        // OPDS Browse
        opdsBrowseScreen(navController)
    }
}

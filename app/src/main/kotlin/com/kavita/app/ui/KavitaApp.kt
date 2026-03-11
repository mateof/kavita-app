package com.kavita.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kavita.app.StartupState
import com.kavita.app.StartupViewModel
import com.kavita.app.navigation.KavitaNavHost
import com.kavita.core.common.navigation.DownloadsRoute
import com.kavita.core.common.navigation.HomeRoute
import com.kavita.core.common.navigation.LibraryRoute
import com.kavita.core.common.navigation.LoginRoute
import com.kavita.core.common.navigation.MoreRoute
import com.kavita.core.common.navigation.SearchRoute
import com.kavita.core.ui.components.LoadingIndicator
import com.kavita.core.ui.theme.KavitaTheme

enum class TopLevelDestination(
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
) {
    HOME(HomeRoute, Icons.Filled.Home, Icons.Outlined.Home, "Inicio"),
    LIBRARY(LibraryRoute, Icons.Filled.LocalLibrary, Icons.Outlined.LocalLibrary, "Biblioteca"),
    SEARCH(SearchRoute, Icons.Filled.Search, Icons.Outlined.Search, "Buscar"),
    DOWNLOADS(DownloadsRoute, Icons.Filled.Download, Icons.Outlined.Download, "Descargas"),
    MORE(MoreRoute, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "Mas"),
}

@Composable
fun KavitaApp(startupViewModel: StartupViewModel = hiltViewModel()) {
    KavitaTheme {
        val startupState by startupViewModel.state.collectAsStateWithLifecycle()

        when (startupState) {
            StartupState.Loading -> LoadingIndicator()
            StartupState.NeedsLogin -> KavitaAppContent(startDestination = LoginRoute)
            StartupState.Ready -> KavitaAppContent(startDestination = HomeRoute)
        }
    }
}

@Composable
private fun KavitaAppContent(startDestination: Any) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = TopLevelDestination.entries.any { dest ->
        currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(destination.route::class)
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        KavitaNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

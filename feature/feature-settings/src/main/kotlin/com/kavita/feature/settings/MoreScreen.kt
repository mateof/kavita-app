package com.kavita.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kavita.core.core.ui.R

@Composable
fun MoreScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToCollections: () -> Unit = {},
    onNavigateToReadingLists: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Cabecera
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Kavita",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.your_digital_library),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Menu
        MoreMenuItem(
            icon = Icons.Filled.CollectionsBookmark,
            title = stringResource(R.string.collections),
            subtitle = stringResource(R.string.collections_subtitle),
            onClick = onNavigateToCollections,
        )

        MoreMenuItem(
            icon = Icons.AutoMirrored.Filled.List,
            title = stringResource(R.string.reading_lists),
            subtitle = stringResource(R.string.reading_lists_subtitle),
            onClick = onNavigateToReadingLists,
        )

        MoreMenuItem(
            icon = Icons.Filled.BarChart,
            title = stringResource(R.string.statistics),
            subtitle = stringResource(R.string.stats_subtitle),
            onClick = onNavigateToStats,
        )

        MoreMenuItem(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.servers),
            subtitle = stringResource(R.string.servers_subtitle),
            onClick = onNavigateToServers,
        )

        MoreMenuItem(
            icon = Icons.Filled.AdminPanelSettings,
            title = stringResource(R.string.administration),
            subtitle = stringResource(R.string.admin_subtitle),
            onClick = onNavigateToAdmin,
        )

        MoreMenuItem(
            icon = Icons.Filled.Settings,
            title = stringResource(R.string.settings),
            subtitle = stringResource(R.string.settings_subtitle),
            onClick = onNavigateToSettings,
        )
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
